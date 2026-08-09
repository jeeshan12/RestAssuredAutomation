# RestAssuredAutomation

A REST Assured + TestNG API automation framework built around a **service-client architecture**:
every endpoint is called through a small, interface-driven service class instead of inline
REST Assured calls.

```java
ApiResponse response = userService.getUser(2);

ApiAssertions.assertThat(response)
    .hasStatusCode(200)
    .bodyMatches("data.email", notNullValue());
```

## Highlights

- **SOLID service-client design** — `ApiClient` interface, `AuthStrategy` interface, one small class per responsibility. New API or new auth mechanism = a new class, never an edit to an existing one.
- **Parallel by default, serial by exception** — TestNG runs every test on its own thread; annotate order-dependent tests `@Serial` to opt them into a single-threaded block instead. No group names or suite XML editing required.
- **Auth handled for you** — Basic, static/dynamic Bearer, API key, OAuth2 (client-credentials + password grant), and cookie-based login tokens. All cached and single-flight-refreshed under concurrent access via a shared `TokenCache`.
- **Pluggable by design** — three real APIs wired up today (see below) as a template for adding your own.
- **Reporting built in** — Allure + SLF4J/Logback wired into every request automatically.

## Quick start

Requires Java 21 and Maven.

```bash
mvn test                                    # runs everything; reqres.* skips without a key (see below)
mvn test -Dreqres.api.key=YOUR_KEY          # get a free key at https://app.reqres.in
mvn test -Denv=prod                         # switch environment (config/<env>.properties)
mvn test -Dparallel.threads=16              # change the parallel worker count (default 8)
mvn test -Dtest=OAuthStrategyTests          # run a single class
```

## Architecture at a glance

```
core/        ApiClient (interface) + RestAssuredApiClient + ApiClientBuilder  - the DIP seam
auth/        AuthStrategy (interface) + 7 implementations + TokenCache
services/    BaseService + one package per pluggable API (reqres / swapi / booking)
config/      ConfigManager - env-aware, -D / env var / properties-file resolution
specs/       Shared REST Assured spec + filter setup (logging, Allure)
listeners/   @Serial support, retry-with-backoff, suite/test lifecycle logging
assertions/  Fluent ApiAssertions for readable tests
annotations/ @Serial
exceptions/  FrameworkException hierarchy
```

Services depend only on the `ApiClient` interface; tests depend only on a service interface and
`ApiAssertions`. That's the whole seam that makes auth, HTTP client, and target API all swappable
independently.

## APIs wired up today

| API | What it demonstrates | Auth |
|---|---|---|
| [reqres.in](https://reqres.in) | Full CRUD, parallel + `@Serial` lifecycle test | `x-api-key` header (needs a free personal key) |
| [swapi.info](https://swapi.info) species | Read-only plugin (the real API has no write endpoints) | none |
| [restful-booker](https://restful-booker.herokuapp.com) | Full CRUD incl. `PATCH`, login-token auth | `CookieTokenAuthStrategy` (login once, cache, reuse) |

## Adding your own API

1. New package under `services/<yourapi>` (+ a `model` sub-package) — copy `services/reqres` as a template.
2. Narrow `<Resource>Service` interface + `<Resource>ServiceImpl extends BaseService`.
3. Point `ApiClientBuilder.baseUri(...)` at it and pick an `AuthStrategy` via `AuthStrategyFactory.from(AuthConfig)`.
4. Write tests with `ApiAssertions`; mark shared-state or order-dependent ones `@Serial`.

Nothing in `core`, `auth`, `listeners`, or `testng.xml` needs to change.

## Reporting

```bash
mvn test
allure generate allure-results --clean -o allure-report && allure open allure-report
```
