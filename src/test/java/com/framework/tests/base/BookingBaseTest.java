package com.framework.tests.base;

import com.framework.auth.AuthConfig;
import com.framework.auth.AuthStrategyFactory;
import com.framework.auth.AuthType;
import com.framework.config.ConfigManager;
import com.framework.core.ApiClient;
import com.framework.core.ApiClientBuilder;
import com.framework.services.booking.BookingService;
import com.framework.services.booking.BookingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;


public abstract class BookingBaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BookingBaseTest.class);

    protected BookingService bookingService;

    @BeforeClass(alwaysRun = true)
    public void setUpApiClient() {
        ConfigManager config = ConfigManager.getInstance();

        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.COOKIE_TOKEN)
                .tokenUrl(config.getRequired("booking.auth.url"))
                .username(config.getRequired("booking.auth.username"))
                .password(config.getRequired("booking.auth.password"))
                .build();

        ApiClient apiClient = ApiClientBuilder.create()
                .baseUri(config.getRequired("booking.base.uri"))
                .connectionTimeoutMs(config.getInt("connection.timeout.ms", 10_000))
                .readTimeoutMs(config.getInt("read.timeout.ms", 10_000))
                .authStrategy(AuthStrategyFactory.from(authConfig))
                .build();

        bookingService = new BookingServiceImpl(apiClient);
    }
}
