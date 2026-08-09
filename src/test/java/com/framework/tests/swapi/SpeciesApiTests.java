package com.framework.tests.swapi;

import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.services.swapi.model.Species;
import com.framework.tests.base.SwapiBaseTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


public class SpeciesApiTests extends SwapiBaseTest {

    @Test(description = "GET /species returns the full, un-paginated species array")
    public void listSpecies_returnsAllSpecies() {
        ApiResponse response = speciesService.listSpecies();

        ApiAssertions.assertThat(response)
                .hasStatusCode(200)
                .respondsWithin(5000);

        Species[] species = response.as(Species[].class);
        Assert.assertTrue(species.length > 0, "Expected at least one species");

        Species human = java.util.Arrays.stream(species)
                .filter(s -> "Human".equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected 'Human' in the species list"));
        Assert.assertEquals(human.getClassification(), "mammal");
        Assert.assertEquals(human.getDesignation(), "sentient");
    }

    @Test(description = "GET /species/{id} returns a single species with the documented fields")
    public void getSpecies_returnsSpeciesById() {
        ApiResponse response = speciesService.getSpecies(1);

        ApiAssertions.assertThat(response)
                .hasStatusCode(200)
                .bodyMatches("name", equalTo("Human"))
                .bodyMatches("classification", equalTo("mammal"))
                .bodyMatches("homeworld", notNullValue());

        Species human = response.as(Species.class);
        Assert.assertEquals(human.getLanguage(), "Galactic Basic");
        Assert.assertFalse(human.getFilms().isEmpty(), "Expected Human to appear in at least one film");
        Assert.assertTrue(human.getUrl().endsWith("/species/1"));
    }

    @Test(description = "GET /species/{id} for a non-existent numeric id returns 404")
    public void getSpecies_unknownNumericId_returnsNotFound() {
        ApiResponse response = speciesService.getSpecies(9999);

        ApiAssertions.assertThat(response).hasStatusCode(404);
    }

    @DataProvider(name = "knownSpeciesIds", parallel = true)
    public Object[][] knownSpeciesIds() {
        // A handful of the 37 species on record; run through the same assertion concurrently.
        return new Object[][]{{1}, {2}, {3}, {4}, {5}};
    }

    @Test(description = "Every known species id resolves to a 200 with a non-blank name",
            dataProvider = "knownSpeciesIds")
    public void getSpecies_knownIds_alwaysResolve(int id) {
        ApiResponse response = speciesService.getSpecies(id);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        Species species = response.as(Species.class);
        Assert.assertNotNull(species.getName());
        Assert.assertFalse(species.getName().isBlank());
    }
}
