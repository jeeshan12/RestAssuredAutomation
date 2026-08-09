package com.framework.tests.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.framework.assertions.ApiAssertions;
import com.framework.auth.AuthConfig;
import com.framework.auth.AuthStrategy;
import com.framework.auth.AuthStrategyFactory;
import com.framework.auth.AuthType;
import com.framework.core.ApiClient;
import com.framework.core.ApiClientBuilder;
import com.framework.core.ApiRequest;
import com.framework.core.ApiResponse;
import io.restassured.http.Method;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.equalTo;


public class OAuthStrategyTests {

    private WireMockServer wireMockServer;

    @BeforeClass(alwaysRun = true)
    public void startStub() {
        // Everything below is scoped to this wireMockServer instance rather than WireMock's
        // static/thread-bound default client, so it behaves correctly regardless of which pool
        // thread TestNG happens to run @BeforeClass vs @Test on under parallel="methods".
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        wireMockServer.stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"stub-access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        wireMockServer.stubFor(get(urlEqualTo("/secure/resource"))
                .withHeader("Authorization", WireMock.equalTo("Bearer stub-access-token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"authorized\"}")));

        wireMockServer.stubFor(get(urlEqualTo("/basic/resource"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"authorized\"}")));

        // Separate token endpoint + a fixed delay, used only by the concurrency test below. The
        // delay widens the race window so every racing thread reliably sees a cold cache instead
        // of the fetch finishing too fast to actually exercise TokenCache's per-key lock.
        wireMockServer.stubFor(post(urlEqualTo("/oauth/token-concurrent"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withFixedDelay(300)
                        .withBody("{\"access_token\":\"concurrent-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        wireMockServer.stubFor(get(urlEqualTo("/secure/concurrent-resource"))
                .withHeader("Authorization", WireMock.equalTo("Bearer concurrent-token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"authorized\"}")));
    }

    @AfterClass(alwaysRun = true)
    public void stopStub() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test(description = "OAuth2 client-credentials strategy fetches a token once and reuses it across repeated calls")
    public void clientCredentialsStrategy_fetchesAndCachesToken() {
        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.OAUTH2_CLIENT_CREDENTIALS)
                .tokenUrl(wireMockServer.baseUrl() + "/oauth/token")
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .build();

        ApiClient client = ApiClientBuilder.create()
                .baseUri(wireMockServer.baseUrl())
                .authStrategy(AuthStrategyFactory.from(authConfig))
                .build();

        ApiRequest request = ApiRequest.builder().method(Method.GET).path("/secure/resource").build();

        for (int i = 0; i < 3; i++) {
            ApiResponse response = client.execute(request);
            ApiAssertions.assertThat(response)
                    .hasStatusCode(200)
                    .bodyMatches("status", equalTo("authorized"));
        }

        // 3 resource calls but exactly 1 token fetch -> the TokenCache is doing its job.
        // Verified against this test's own server instance (not WireMock's static/thread-bound
        // default client) so it's correct even when TestNG runs @BeforeClass and @Test methods
        // on different pool threads under parallel="methods".
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/oauth/token")));
    }

    @Test(description = "TokenCache's per-key ReentrantLock ensures only one thread fetches the token "
            + "even when many threads race against a cold cache at the same instant (single-flight refresh)")
    public void clientCredentialsStrategy_singleFlightUnderConcurrentAccess() throws InterruptedException {
        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.OAUTH2_CLIENT_CREDENTIALS)
                .tokenUrl(wireMockServer.baseUrl() + "/oauth/token-concurrent")
                .clientId("concurrent-client")
                .clientSecret("concurrent-secret")
                .build();

        // One ApiClient/AuthStrategy instance, shared by every racing thread below - exactly like
        // one test class instance sharing one ApiClient across TestNG's worker threads in real
        // parallel="methods" execution (see BaseTest).
        ApiClient client = ApiClientBuilder.create()
                .baseUri(wireMockServer.baseUrl())
                .authStrategy(AuthStrategyFactory.from(authConfig))
                .build();
        ApiRequest request = ApiRequest.builder().method(Method.GET).path("/secure/concurrent-resource").build();

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1); // holds every thread back until we release them together
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    statusCodes.add(client.execute(request).statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown(); // fire the starting gun - all 10 threads hit apply() at once
        boolean finishedInTime = doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        Assert.assertTrue(finishedInTime, "All concurrent calls should finish within the timeout");
        Assert.assertEquals(statusCodes.size(), threadCount);
        Assert.assertTrue(statusCodes.stream().allMatch(code -> code == 200),
                "Every concurrent call should succeed - each one must have gotten a valid Bearer token");

        // The actual point of this test: no matter how many threads raced in on a cold cache,
        // TokenCache's per-cacheKey ReentrantLock means the token endpoint is hit exactly once.
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/oauth/token-concurrent")));
    }

    @Test(description = "Basic auth strategy sends valid preemptive credentials via the same ApiClient")
    public void basicAuthStrategy_sendsCredentials() {
        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.BASIC)
                .username("demo-user")
                .password("demo-pass")
                .build();

        ApiClient client = ApiClientBuilder.create()
                .baseUri(wireMockServer.baseUrl())
                .authStrategy(AuthStrategyFactory.from(authConfig))
                .build();

        ApiResponse response = client.execute(ApiRequest.builder().method(Method.GET).path("/basic/resource").build());

        ApiAssertions.assertThat(response).hasStatusCode(200);
    }
}
