# RestAssured Automation

A REST Assured + TestNG API automation framework built around a **service-client architecture**:
every endpoint is called through a small, interface-driven service class instead of inline
REST Assured calls, so tests read like business actions rather than HTTP plumbing.

```java
ApiResponse response = userService.getUser(2);

ApiAssertions.assertThat(response)
    .hasStatusCode(200)
    .bodyMatches("data.email", notNullValue());
```

It ships wired up against [reqres.in](https://reqres.in) as a working example, but every seam
(auth, HTTP client, target API) is pluggable. This document explains **how it's built and, in
detail, how it behaves at runtime** — request by request, token by token, thread by thread.

> An HTML rendering of this same document (`README.html`) is included for offline/browser
> reading with diagrams styled as boxes instead of ASCII art.

## Table of contents

1. [Design philosophy (SOLID)](#1-design-philosophy-solid)
2. [Package map](#2-package-map)
3. [The request lifecycle, step by step](#3-the-request-lifecycle-step-by-step)
4. [Authentication deep dive](#4-authentication-deep-dive)
5. [OAuth2 token lifecycle, in detail](#5-oauth2-token-lifecycle-in-detail)
6. [Test execution lifecycle (TestNG + Maven)](#6-test-execution-lifecycle-testng--maven)
7. [Parallel vs. Serial: what actually happens on the wire clock](#7-parallel-vs-serial-what-actually-happens-on-the-wire-clock)
8. [Configuration resolution lifecycle](#8-configuration-resolution-lifecycle)
9. [Error handling](#9-error-handling)
10. [Plugging in a different API](#10-plugging-in-a-different-api)
11. [Reporting & logging](#11-reporting--logging)
12. [Running it — command cheat sheet](#12-running-it--command-cheat-sheet)
13. [FAQ / troubleshooting](#13-faq--troubleshooting)
14. [Full file map](#14-full-file-map)

---

## 1. Design philosophy (SOLID)

| Principle | Where | Why it matters here |
|---|---|---|
| **S**ingle Responsibility | `RestAssuredApiClient` only executes requests. `UserServiceImpl` only knows reqres's endpoints. `ApiAssertions` only asserts. `TokenCache` only caches. | Each class has exactly one reason to change; a new endpoint never touches auth code, and a new auth mechanism never touches endpoint code. |
| **O**pen/Closed | New auth mechanism → new `AuthStrategy` implementation. New API → new `services/<api>` package. | Extension happens by *adding* a class, never by *editing* an existing one. |
| **L**iskov Substitution | Any `AuthStrategy`/`ApiClient` implementation is a drop-in replacement anywhere the interface is used — including test doubles. | Lets `OAuthStrategyTests` swap in a WireMock-backed setup with zero changes to `ApiClientBuilder`/`RestAssuredApiClient`. |
| **I**nterface Segregation | `UserService` exposes only the reqres "users" operations. | Callers never depend on a god-object API client with 40 unrelated methods. |
| **D**ependency Inversion | Services depend on the `ApiClient` **interface**, never on `RestAssuredApiClient` directly. `AuthStrategyFactory` hides every concrete `AuthStrategy` behind `AuthConfig`. | The one seam (`ApiClient`) is what makes the whole thing swappable/testable. |

Two extra patterns worth naming because they show up constantly in the code:

- **Builder** — `ApiRequest`, `AuthConfig`, `OAuth2Token` (Lombok `@Builder`), and `ApiClientBuilder`
  (hand-written, because it also validates and wires in defaults).
- **Strategy + Factory** — `AuthStrategy` is the strategy interface; `AuthStrategyFactory.from(AuthConfig)`
  is the factory that picks the right implementation from a declarative config object.

## 2. Package map

```
src/main/java/com/snc/icw/framework/
├── annotations/  Serial.java                    - opt a test into single-threaded execution
├── config/       ConfigManager.java              - env-aware, overridable configuration
├── core/         ApiRequest, ApiResponse,        - the client boundary (DIP seam)
│                 ApiClient, RestAssuredApiClient,
│                 ApiClientBuilder
├── specs/        RequestSpecFactory,             - shared REST Assured spec/filter setup
│                 ResponseSpecFactory
├── filters/      RequestResponseLoggingFilter    - SLF4J request/response logging
├── auth/         AuthStrategy + 6 implementations, AuthConfig, AuthStrategyFactory,
│                 OAuth2Client, OAuth2Token, TokenCache
├── services/     BaseService (shared HTTP verbs) + services/reqres/* (example plugin)
├── assertions/   ApiAssertions                   - fluent, readable response assertions
├── listeners/    SerialAnnotationTransformer, RetryAnalyzer, SuiteLifecycleListener
├── utils/        JsonUtils, TestDataFactory (datafaker)
└── exceptions/   FrameworkException hierarchy

src/main/resources/
├── config/*.properties   base + per-environment overrides
└── logback.xml

src/test/java/com/snc/icw/tests/
├── base/BaseTest.java                            builds the ApiClient/UserService per class
├── reqres/ReqResUserApiTests.java                parallel-safe reads (list/get/404)
├── reqres/ReqResUserCreateTests.java             parallel-safe creates (+ parallel DataProvider)
├── reqres/ReqResUserLifecycleSerialTests.java    @Serial create → update → delete
└── auth/OAuthStrategyTests.java                  self-contained OAuth2 + Basic auth demo (WireMock)

src/test/resources/testng.xml    the Serial/Parallel suite wiring
```

## 3. The request lifecycle, step by step

Every single HTTP call in the framework — regardless of which API or auth type — goes through
the exact same nine steps. This is the mechanism that makes tests "read like business actions":
all of steps 2–9 are invisible to the test author.

```
 TEST                    userService.getUser(2)
   │
   ▼
 (1) UserServiceImpl.getUser(int id)
     └─ return get(ReqResEndpoints.USER_BY_ID, Map.of("id", id));      // "/users/{id}"
   │
   ▼
 (2) BaseService.get(path, pathParams)
     └─ execute(Method.GET, path, pathParams, /*query*/null, /*body*/null)
   │
   ▼
 (3) BaseService.execute(...) builds an immutable ApiRequest
     └─ ApiRequest.builder().method(GET).path("/users/{id}").pathParams({"id":2}).build()
   │
   ▼
 (4) apiClient.execute(request)      // apiClient is the injected ApiClient INTERFACE
   │
   ▼
 (5) RestAssuredApiClient.execute(request)
     └─ RequestSpecification spec = RestAssured.given().spec(baseSpec);
        // baseSpec = the shared spec built once by RequestSpecFactory:
        //   baseUri, Content-Type/Accept=JSON, timeouts, logging filter, AllureRestAssured filter
   │
   ▼
 (6) spec = authStrategy.apply(spec);
     // <-- THIS is where the Authorization / x-api-key / Basic header gets attached.
     //     See §4 and §5 for exactly what happens inside here.
   │
   ▼
 (7) request.getHeaders()   .forEach(spec::header);       // any extra per-call headers
     request.getPathParams().forEach(spec::pathParam);    // {id} -> 2
     request.getQueryParams().forEach(spec::queryParam);  // ?page=2
     if (request.getBody() != null) spec.body(request.getBody());   // Jackson-serialized
   │
   ▼
 (8) Response response = spec.request(request.getMethod(), request.getPath());
     // <-- the actual HTTP call happens here, inside REST Assured / Apache HttpClient5
   │
   ▼
 (9) return new ApiResponse(response);
   │
   ▼
 TEST reads it back:
   ApiAssertions.assertThat(response).hasStatusCode(200).bodyMatches("data.id", equalTo(2));
   User user = response.as(SingleUserResponse.class).getData();
```

Things worth calling out explicitly:

- **Step 6 runs before step 7.** The auth strategy's header is applied first; any headers passed
  explicitly on the `ApiRequest` are layered on *after*. REST Assured's `.header(name, value)`
  **adds** a header rather than replacing one of the same name — so if you ever pass your own
  `Authorization` header through `ApiRequest.headers`, you'll send two `Authorization` headers.
  In practice this never happens because callers use `AuthStrategy` for auth and `ApiRequest`
  only for business headers — but it's the one sharp edge in the pipeline worth knowing about.
- **`baseSpec` is built exactly once** per `ApiClient` (in `ApiClientBuilder.build()` →
  `RequestSpecFactory.create(...)`), and reused on every call via
  `RestAssured.given().spec(baseSpec)`. That single line is *why* one `ApiClientBuilder` call at
  the top of `BaseTest` is enough to configure logging, Allure attachment, timeouts, and
  content-type for every subsequent call a service makes.
- **Exceptions are normalized.** Anything thrown inside steps 5–9 (network failure, serialization
  error, auth failure bubbling up) is caught in `RestAssuredApiClient.execute` and rethrown as an
  `ApiExecutionException` with the method+path in the message — so a stack trace always tells you
  which call failed, even under 8 concurrent threads.

## 4. Authentication deep dive

`AuthStrategy` is deliberately the smallest interface in the framework:

```java
public interface AuthStrategy {
    RequestSpecification apply(RequestSpecification requestSpecification);
}
```

Take a `RequestSpecification`, attach whatever credentials this mechanism needs, return it.
Everything about "how do I authenticate" lives inside that one method, on one class, per
mechanism. `AuthStrategyFactory.from(AuthConfig)` is a `switch` over `AuthType` that returns the
right implementation — it's the *only* place in the codebase that maps a declarative config to a
concrete strategy class.

| `AuthType` | Class | What ends up on the wire |
|---|---|---|
| `NONE` | `NoAuthStrategy` | nothing — passthrough |
| `BASIC` | `BasicAuthStrategy` | `Authorization: Basic base64(username:password)`, sent **preemptively** (no 401 challenge round-trip) |
| `BEARER` | `BearerTokenAuthStrategy` | `Authorization: Bearer <token>` — token comes from a `Supplier<String>`, read fresh on every call (so an externally-rotated token is picked up without rebuilding the client) |
| `API_KEY` | `ApiKeyAuthStrategy` | an arbitrary header, e.g. `x-api-key: <value>` — this is what `BaseTest` uses against reqres.in |
| `OAUTH2_CLIENT_CREDENTIALS` | `OAuth2ClientCredentialsStrategy` | `Authorization: Bearer <access_token>` obtained via RFC 6749 §4.4, cached, auto-refreshed — see §5 |
| `OAUTH2_PASSWORD` | `OAuth2PasswordGrantStrategy` | same as above via RFC 6749 §4.3 (resource-owner password credentials), cache key additionally scoped by username |

Wiring one up is always the same three lines, regardless of mechanism:

```java
AuthConfig config = AuthConfig.builder()
        .authType(AuthType.OAUTH2_CLIENT_CREDENTIALS)   // pick the mechanism
        .tokenUrl("https://idp.example.com/oauth/token")
        .clientId("...")
        .clientSecret("...")
        .scope("read:things")
        .build();

ApiClient client = ApiClientBuilder.create()
        .baseUri("https://api.example.com")
        .authStrategy(AuthStrategyFactory.from(config))  // factory picks the strategy
        .build();
```

`AuthConfig` is one flat POJO covering every mechanism's fields (`tokenUrl`/`clientId`/`clientSecret`/
`username`/`password`/`scope` for OAuth2, `headerName`/`headerValue` for API key, `bearerToken` for
static bearer) — you only ever fill in the fields the chosen `AuthType` actually reads.

## 5. OAuth2 token lifecycle, in detail

This is the part of the framework most worth understanding precisely, since it's also the part
that has to behave correctly under 8+ concurrent threads.

### 5.1 First call — cold cache

```
Test thread                 OAuth2ClientCredentialsStrategy    TokenCache            OAuth2Client        Token endpoint
    │  execute(request)              │                             │                     │                    │
    ├──────────────────────────────► │                             │                     │                    │
    │                                │ getOrFetch(cacheKey, fn)    │                     │                    │
    │                                ├───────────────────────────► │                     │                    │
    │                                │                             │ tokens.get(key)     │                    │
    │                                │                             │  → null (cold)      │                    │
    │                                │                             │ lock(key).lock()    │                    │
    │                                │                             │ fn.get() ──────────►│                    │
    │                                │                             │                     │ POST /oauth/token  │
    │                                │                             │                     │  grant_type=       │
    │                                │                             │                     │  client_credentials│
    │                                │                             │                     ├───────────────────►│
    │                                │                             │                     │ 200 { access_token,│
    │                                │                             │                     │◄────────token_type,│
    │                                │                             │  ◄──OAuth2Token──────┤   expires_in }     │
    │                                │                             │ tokens.put(key, tok) │                    │
    │                                │                             │ lock(key).unlock()   │                    │
    │                                │ ◄────────OAuth2Token──────  │                     │                    │
    │                                │ spec.header("Authorization",│                     │                    │
    │                                │   "Bearer " + accessToken)  │                     │                    │
    │  ◄── spec with header ──────── │                             │                     │                    │
    ▼
  request proceeds to the real API with a valid Bearer token attached
```

Concretely, `OAuth2ClientCredentialsStrategy.apply(spec)` does:

```java
OAuth2Token token = TokenCache.getInstance().getOrFetch(cacheKey,
        () -> client.fetchClientCredentialsToken(config.getClientId(), config.getClientSecret(), config.getScope()));
return requestSpecification.header("Authorization", token.authorizationHeaderValue());
```

- **`cacheKey`** is computed once, in the strategy's constructor:
  `"client_credentials|" + tokenUrl + "|" + clientId + "|" + scope`. Two `AuthConfig`s with the
  same token URL, client id, and scope share a cache entry — even across different `ApiClient`
  instances — because `TokenCache` is a process-wide singleton (`TokenCache.getInstance()`).
- **`OAuth2Client.fetchClientCredentialsToken(...)`** builds the RFC 6749 token request as
  `application/x-www-form-urlencoded`: `grant_type=client_credentials`, `client_id`,
  `client_secret`, and `scope` only if it's non-blank. It POSTs to `tokenUrl` and requires an HTTP
  200 — anything else throws `AuthenticationException` with the endpoint's response body inlined,
  so a misconfigured client secret fails loudly and immediately, not three requests later with a
  cryptic 401 from the *real* API.
- **The response is parsed into an `OAuth2Token`**: `access_token`, `token_type`, `refresh_token`,
  `scope` come straight off the JSON body; `expires_in` defaults to `3600` if the provider omits
  it; `obtainedAtEpochMillis` is stamped at parse time (`System.currentTimeMillis()`), which is
  what expiry math is measured from.
- **`token.authorizationHeaderValue()`** is `(tokenType, or "Bearer" if blank) + " " + accessToken`
  — this is the literal string that lands in the `Authorization` header.

### 5.2 Every subsequent call — warm cache, no network call

```java
public boolean isExpired(long bufferSeconds) {
    long expiryEpochMillis = obtainedAtEpochMillis + (expiresInSeconds - bufferSeconds) * 1000L;
    return System.currentTimeMillis() >= expiryEpochMillis;
}
```

`TokenCache.getOrFetch` checks `tokens.get(cacheKey)` first; if it's present and
`!isExpired(30)`, it's returned **immediately — no lock, no network call**. The 30-second buffer
means a token is treated as stale 30 seconds before its real expiry, so a request that's already
in flight never races the token expiring mid-call.

This is exactly what `OAuthStrategyTests.clientCredentialsStrategy_fetchesAndCachesToken()`
proves: three calls to a protected resource, only **one** POST to `/oauth/token` (asserted via
`wireMockServer.verify(1, postRequestedFor(urlEqualTo("/oauth/token")))`).

### 5.3 Concurrent cold-cache access — single-flight refresh

If two test threads both discover a missing/expired token for the same `cacheKey` at the same
moment (very plausible right after suite start, with 8 parallel threads), naively they'd both
fire a token request. `TokenCache` prevents that with a **per-cache-key `ReentrantLock`**:

```java
ReentrantLock lock = locks.computeIfAbsent(cacheKey, key -> new ReentrantLock());
lock.lock();
try {
    OAuth2Token recheck = tokens.get(cacheKey);      // <-- re-check AFTER acquiring the lock
    if (recheck != null && !recheck.isExpired(30)) {
        return recheck;                               // the other thread already refreshed it
    }
    OAuth2Token fresh = fetcher.get();                // only ONE thread reaches this line
    tokens.put(cacheKey, fresh);
    return fresh;
} finally {
    lock.unlock();
}
```

Thread A acquires the lock and fetches; thread B blocks on `lock.lock()`, then — critically —
**re-checks the cache after acquiring the lock**, sees thread A's fresh token, and returns it
without a second network call. Locks are scoped per `cacheKey` (different tokens for different
providers/clients don't block each other), and the lock map itself (`ConcurrentHashMap`) is safe
for concurrent `computeIfAbsent`.

### 5.4 Password grant — the same machinery, one more dimension

`OAuth2PasswordGrantStrategy` is structurally identical, except the cache key also folds in the
username (`"password|" + tokenUrl + "|" + clientId + "|" + username + "|" + scope`) and the form
body sent to `OAuth2Client.fetchPasswordGrantToken(...)` adds `grant_type=password`, `username`,
`password`. Two different test users authenticating against the same IdP get two independent
cache entries and never share a token.

### 5.5 Forcing a refresh

`TokenCache.getInstance().evict(cacheKey)` drops a cache entry so the next `apply(...)` call
re-fetches. There's no public API to compute a strategy's `cacheKey` from outside (it's
private/final on the strategy instance) — this is intentional; eviction is meant for white-box
tests of the cache itself, not everyday use.

## 6. Test execution lifecycle (TestNG + Maven)

This is the full sequence from `mvn test` to the JVM exiting, in order:

```
 mvn test
   │
   ▼
 (A) Maven resource filtering
     src/test/resources/testng.xml is copied to target/test-classes/testng.xml with
     ${parallel.threads} substituted by the actual Maven property value
     (default 8, or whatever -Dparallel.threads=N supplied).
   │
   ▼
 (B) maven-surefire-plugin launches one forked JVM (forkCount=1, reuseForks=true)
     and hands TestNG the filtered suite file:
     ${project.build.testOutputDirectory}/testng.xml
   │
   ▼
 (C) TestNG parses the <suite>, instantiates the <listeners>:
       SerialAnnotationTransformer  (an IAnnotationTransformer)
       SuiteLifecycleListener       (ISuiteListener + ITestListener)
       AllureTestNg
   │
   ▼
 (D) Method discovery + annotation transformation
     For every @Test method TestNG finds under com.framework.tests.*, it calls
     SerialAnnotationTransformer.transform(annotation, testClass, ctor, testMethod)
     BEFORE any thread/execution plan is built. If @Serial is present on the method,
     its declaring class, or testClass, "serial" is merged into that method's
     TestNG groups. This happens once, up front, for every method in the suite —
     independent of which <test> block will end up running it.
   │
   ▼
 (E) onStart(ISuite) fires  →  SuiteLifecycleListener logs "Suite started"
   │
   ▼
 (F) <test name="Serial-Execution"> runs to completion first
       - parallel="none", thread-count="1"
       - group filter: include "serial" only
       - every @Serial method across the whole suite runs here, one at a time,
         in @Test(priority=...)/dependsOnMethods order
   │
   ▼
 (G) <test name="Parallel-Execution"> runs next
       - parallel="methods", thread-count="${parallel.threads}"
       - group filter: exclude "serial"
       - TestNG's dynamic graph scheduler dispatches every remaining method to a
         free worker thread as soon as one is available (not batched/round-robin -
         a thread that finishes early immediately picks up the next queued method)
   │
   ▼
 (H) Per test class, once, on whichever thread first needs it:
       @BeforeClass BaseTest.setUpApiClient()
         - ConfigManager.getInstance() (built lazily, thread-safe, see §8)
         - checks reqres.api.key -> SkipException if blank (whole class skips, not fails)
         - ApiClientBuilder.create()....build()  -> one ApiClient for this class
         - new UserServiceImpl(apiClient)
     NOTE: TestNG shares ONE instance of the test class across every worker thread
     assigned to its methods (parallel="methods", not "instances") - so this
     ApiClient/UserService pair is shared, concurrently, by every @Test method in
     the class. Safe here because nothing under BaseService holds mutable state.
   │
   ▼
 (I) Each @Test method executes on its assigned thread:
       onTestStart -> method body (service calls, per §3/§5) -> assertions
       -> onTestSuccess / onTestFailure / onTestSkipped
     RetryAnalyzer, if wired via @Test(retryAnalyzer = RetryAnalyzer.class), can
     re-run a failed method up to retry.max.attempts times before the failure
     is reported to TestNG.
   │
   ▼
 (J) onFinish(ISuite) fires -> SuiteLifecycleListener logs "Suite finished"
   │
   ▼
 (K) Surefire aggregates pass/fail/skip counts. The Maven build FAILS if any test
     failed; SKIPPED tests (e.g. reqres.* with no API key) do not fail the build.
```

Why the two `<test>` blocks run **sequentially, not concurrently**: the `<suite>` element itself
is declared `parallel="none"` (`testng.xml`), and that attribute controls whether multiple
`<test>` blocks *within the same suite* run against each other in parallel. With it set to
`"none"`, TestNG runs the `<test>` elements in file order, one fully finishing before the next
starts. This is deliberate: it guarantees every `@Serial` method across the entire suite has
finished — deterministically, one at a time — *before* the parallel block (which may include
unrelated classes touching overlapping test data) even starts.

## 7. Parallel vs. Serial: what actually happens on the wire clock

Observed from an actual run (thread names straight from the log):

```
t=0ms   Serial-Execution block starts (thread: main)
        ├─ ReqResUserLifecycleSerialTests#step1_createUser   [main]
        ├─ ReqResUserLifecycleSerialTests#step2_updateUser    [main]   (waits for step1 - dependsOnMethods)
        └─ ReqResUserLifecycleSerialTests#step3_deleteUser    [main]   (waits for step2)
        ── entire block is single-threaded, methods execute back-to-back ──

t=+Xms  Parallel-Execution block starts, thread pool of size `parallel.threads` spins up
        ├─ ReqResUserApiTests#listUsers_returnsPaginatedList         [TestNG-test-Parallel-Execution-5]
        ├─ ReqResUserApiTests#getUser_returnsUser                    [TestNG-test-Parallel-Execution-3]  ─┐
        ├─ ReqResUserApiTests#getUser_unknownId_returnsNotFound      [TestNG-test-Parallel-Execution-4]   │ all
        ├─ ReqResUserCreateTests#createUser_returnsCreatedUser       [TestNG-test-Parallel-Execution-7]   │ running
        ├─ ReqResUserCreateTests#...eachSucceeds[0]                  [TestNG-PoolService-1]                │ at the
        ├─ ReqResUserCreateTests#...eachSucceeds[1]                  [TestNG-PoolService-2]                │ same
        ├─ ReqResUserCreateTests#...eachSucceeds[2]                  [TestNG-PoolService-3]                │ instant
        ├─ OAuthStrategyTests#basicAuthStrategy_sendsCredentials     [TestNG-test-Parallel-Execution-1]   │
        └─ OAuthStrategyTests#clientCredentialsStrategy_...          [TestNG-test-Parallel-Execution-2]  ─┘
```

Two distinct thread pools show up by design:

- **`TestNG-test-Parallel-Execution-N`** — the method-level pool sized by
  `<test parallel="methods" thread-count="...">`.
- **`TestNG-PoolService-N`** — a *separate* pool TestNG spins up specifically for
  `@DataProvider(parallel = true)` invocations (see `ReqResUserCreateTests.randomUsers()`), so a
  single data-driven test method's invocations fan out concurrently too, independent of the
  outer method-level thread count.

## 8. Configuration resolution lifecycle

`ConfigManager` is a lazily-initialized, thread-safe singleton (double-checked locking on
`getInstance()`). Its constructor runs exactly once per JVM and loads, in order:

```
1. config/config.properties        (required - baseline defaults for every environment)
2. config/<env>.properties          (optional - env resolved from -Denv, else ENV var, else "qa")
```

Both are `Properties.load()`'d into the *same* `Properties` object, so step 2's keys overwrite
step 1's. After construction, the properties are never mutated again — safe to read from any
number of threads without synchronization.

Every subsequent `get(key)` call resolves in this order, **checked fresh on every call** (so a
system property set mid-run, e.g. by a test, takes effect immediately):

```
1. System.getProperty(key)                          -Dbase.uri=...
2. System.getenv(TO_ENV_VAR_NAME(key))               base.uri -> BASE_URI
3. the merged Properties loaded above
4. the caller-supplied default (or null / FrameworkConfigException for getRequired)
```

This is what lets the exact same test code run against dev/qa/prod, or against a completely
different pluggable API, by changing only a properties file, an env var, or a `-D` flag — never
the Java code.

## 9. Error handling

Everything unexpected in the framework surfaces as one of four unchecked exceptions, all rooted at
`FrameworkException`:

| Exception | Thrown when |
|---|---|
| `ApiExecutionException` | Anything goes wrong inside `RestAssuredApiClient.execute(...)` — network failure, serialization error, or an exception propagating up from the auth strategy. Message always includes the HTTP method + path. |
| `AuthenticationException` | The OAuth2 token endpoint returns a non-200 response (`OAuth2Client.requestToken`). Message includes the endpoint URL, status code, and response body — the credential problem is visible immediately, not three requests later as a mysterious 401 from the *actual* API. |
| `FrameworkConfigException` | A required config key is missing (`ConfigManager.getRequired`), a required classpath resource is missing, or `ApiClientBuilder.build()` is called without a `baseUri`. |
| `FrameworkException` | Base class; also used directly by `JsonUtils` for (de)serialization failures. |

All four are unchecked (`RuntimeException`) deliberately: a broken HTTP call, bad config, or
failed auth handshake means the test can't meaningfully continue, so forcing every caller to
declare/catch them would be pure ceremony.

## 10. Plugging in a different API

1. `mkdir src/main/java/com/snc/icw/framework/services/<yourapi>` (+ a `model` sub-package for
   its request/response POJOs).
2. Define an endpoints constants class, a narrow `<Resource>Service` interface, and a
   `<Resource>ServiceImpl extends BaseService implements <Resource>Service` — copy
   `services/reqres` as a template; it's ~90 lines across 3 files.
3. In your `BaseTest` (or a new one), point `ApiClientBuilder.baseUri(...)` at the new API and
   pick whichever `AuthStrategy` it needs via `AuthStrategyFactory.from(AuthConfig)`.
4. Write tests against the new service interface using `ApiAssertions`; mark any
   order-dependent/shared-state tests `@Serial`.

Nothing in `core`, `auth`, `listeners`, or `testng.xml` needs to change — that's the entire point
of the `ApiClient`/`AuthStrategy` seams described in §3 and §4.

## 11. Reporting & logging

- **SLF4J/Logback**: console + `logs/automation.log` (rolled daily, 7-day history —
  `logback.xml`). `RequestResponseLoggingFilter` logs method+URL and status+timing at INFO on
  every call; request/response **bodies** log at DEBUG only, so default output stays readable
  even with dozens of concurrent calls.
- **Allure**: `allure-testng` + `allure-rest-assured` are wired in automatically —
  `RequestSpecFactory.create(...)` adds an `AllureRestAssured` filter to the shared base spec, so
  every call through every service is attached to the Allure report with no per-test setup.
  Results land in `./allure-results`; view with `allure serve allure-results` (needs the
  [Allure CLI](https://allurereport.org/docs/install/)).

## 12. Running it — command cheat sheet

```bash
# reqres.in now requires a personal API key (the old shared demo key was revoked).
# Get a free one at https://app.reqres.in, then:
mvn test -Dreqres.api.key=YOUR_KEY

# Without a key, the reqres.* test classes SKIP with a clear message instead of failing on a
# confusing 401 - everything else (the OAuth2/Basic auth demo) still runs and passes:
mvn test

# Point at a different environment (falls back to config/qa.properties by default):
mvn test -Denv=prod

# Change the parallel worker count (default 8):
mvn test -Dparallel.threads=16

# Run one class/method directly:
mvn test -Dtest=OAuthStrategyTests
mvn test -Dtest=OAuthStrategyTests#basicAuthStrategy_sendsCredentials
```

## 13. FAQ / troubleshooting

**Why did all my `reqres.*` tests show as SKIPPED instead of PASSED/FAILED?**
`BaseTest.setUpApiClient()` throws a `SkipException` in `@BeforeClass` when `reqres.api.key` is
blank. TestNG treats a `@BeforeClass` skip as "skip every method in this class" — a deliberate
choice over letting every test fail with a confusing 401. Set `-Dreqres.api.key=...`.

**I see `WARN ... Ignoring duplicate listener: io.qameta.allure.testng.AllureTestNg` — is that a problem?**
No. `allure-testng` self-registers via a `META-INF/services` entry *and* it's also declared
explicitly in `testng.xml`'s `<listeners>` — TestNG notices the duplicate and ignores the second
registration. Harmless.

**How do I force an OAuth2 strategy to re-fetch a token in a test?**
`TokenCache.getInstance().evict(cacheKey)` — but the cache key is private to the strategy
instance by design (see §5.5). This is meant to be exercised as a white-box unit test of
`TokenCache` itself, not from application/test code.

**Can the Serial and Parallel blocks run at the same time to save wall-clock time?**
Not with the current suite (`<suite parallel="none">`) — see §6 for why that's intentional
(deterministic, fully-finished serial execution before parallel execution starts). If you're
confident your `@Serial` classes never touch data any parallel test also touches, you could
change the suite to `parallel="tests"`; that's a one-line, backwards-compatible change to
`testng.xml`.

**Why is `UserService` (and every other service interface) return type just `ApiResponse` instead of a typed model?**
Deliberate: keeps the service layer a pure, uninterpreted transport shim (Single Responsibility) —
status-code assertions and typed extraction (`response.as(User.class)`) are the test's job. It
also means every service method has one, consistent, predictable return type.

## 14. Full file map

```
RestAssuredAutomation/
├── pom.xml
├── README.md / README.html
├── .gitignore
├── src/main/java/com/snc/icw/framework/
│   ├── annotations/Serial.java
│   ├── config/ConfigManager.java
│   ├── core/{ApiRequest,ApiResponse,ApiClient,RestAssuredApiClient,ApiClientBuilder}.java
│   ├── specs/{RequestSpecFactory,ResponseSpecFactory}.java
│   ├── filters/RequestResponseLoggingFilter.java
│   ├── auth/
│   │   ├── AuthType.java, AuthStrategy.java, AuthConfig.java, AuthStrategyFactory.java
│   │   ├── NoAuthStrategy.java, BasicAuthStrategy.java, BearerTokenAuthStrategy.java, ApiKeyAuthStrategy.java
│   │   └── OAuth2Token.java, OAuth2Client.java, TokenCache.java,
│   │       OAuth2ClientCredentialsStrategy.java, OAuth2PasswordGrantStrategy.java
│   ├── services/
│   │   ├── BaseService.java
│   │   └── reqres/{ReqResEndpoints,UserService,UserServiceImpl}.java + model/*.java
│   ├── assertions/ApiAssertions.java
│   ├── listeners/{SerialAnnotationTransformer,RetryAnalyzer,SuiteLifecycleListener}.java
│   ├── utils/{JsonUtils,TestDataFactory}.java
│   └── exceptions/{FrameworkException,ApiExecutionException,AuthenticationException,FrameworkConfigException}.java
├── src/main/resources/
│   ├── config/{config,dev,qa,prod}.properties
│   └── logback.xml
├── src/test/java/com/snc/icw/tests/
│   ├── base/BaseTest.java
│   ├── reqres/{ReqResUserApiTests,ReqResUserCreateTests,ReqResUserLifecycleSerialTests}.java
│   └── auth/OAuthStrategyTests.java
└── src/test/resources/testng.xml
```
