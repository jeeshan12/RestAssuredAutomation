package com.framework.services.swapi;

import com.framework.core.ApiResponse;


public interface SpeciesService {

    ApiResponse listSpecies();

    ApiResponse getSpecies(int id);
}
