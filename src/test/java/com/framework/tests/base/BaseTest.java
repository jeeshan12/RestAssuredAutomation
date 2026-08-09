package com.framework.tests.base;

import com.framework.auth.ApiKeyAuthStrategy;
import com.framework.config.ConfigManager;
import com.framework.core.ApiClient;
import com.framework.core.ApiClientBuilder;
import com.framework.services.reqres.UserService;
import com.framework.services.reqres.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;


public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    protected UserService userService;

    @BeforeClass(alwaysRun = true)
    public void setUpApiClient() {
        ConfigManager config = ConfigManager.getInstance();
        String apiKey = config.get("reqres.api.key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new SkipException("Skipping: reqres.api.key is not configured. Get a free key at "
                    + "https://app.reqres.in and pass it via -Dreqres.api.key=... (see README.md).");
        }

        ApiClient apiClient = ApiClientBuilder.create()
                .baseUri(config.getRequired("base.uri"))
                .connectionTimeoutMs(config.getInt("connection.timeout.ms", 10_000))
                .readTimeoutMs(config.getInt("read.timeout.ms", 10_000))
                .authStrategy(new ApiKeyAuthStrategy("x-api-key", apiKey))
                .build();

        userService = new UserServiceImpl(apiClient);
    }
}
