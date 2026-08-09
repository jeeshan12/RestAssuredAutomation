package com.framework.tests.base;

import com.framework.auth.NoAuthStrategy;
import com.framework.config.ConfigManager;
import com.framework.core.ApiClient;
import com.framework.core.ApiClientBuilder;
import com.framework.services.swapi.SpeciesService;
import com.framework.services.swapi.SpeciesServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;


public abstract class SwapiBaseTest {

    protected static final Logger log = LoggerFactory.getLogger(SwapiBaseTest.class);

    protected SpeciesService speciesService;

    @BeforeClass(alwaysRun = true)
    public void setUpApiClient() {
        ConfigManager config = ConfigManager.getInstance();

        ApiClient apiClient = ApiClientBuilder.create()
                .baseUri(config.getRequired("swapi.base.uri"))
                .connectionTimeoutMs(config.getInt("connection.timeout.ms", 10_000))
                .readTimeoutMs(config.getInt("read.timeout.ms", 10_000))
                .authStrategy(new NoAuthStrategy())
                .build();

        speciesService = new SpeciesServiceImpl(apiClient);
    }
}
