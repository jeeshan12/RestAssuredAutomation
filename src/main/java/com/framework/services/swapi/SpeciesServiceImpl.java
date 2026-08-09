package com.framework.services.swapi;

import com.framework.core.ApiClient;
import com.framework.core.ApiResponse;
import com.framework.services.BaseService;

import java.util.Map;

public class SpeciesServiceImpl extends BaseService implements SpeciesService {

    public SpeciesServiceImpl(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    public ApiResponse listSpecies() {
        return get(SpeciesEndpoints.SPECIES);
    }

    @Override
    public ApiResponse getSpecies(int id) {
        return get(SpeciesEndpoints.SPECIES_BY_ID, Map.of("id", id));
    }
}
