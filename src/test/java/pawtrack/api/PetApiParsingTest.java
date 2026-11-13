package pawtrack.api;

import pawtrack.model.Species;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON parsing tests for the live REST API clients. These deliberately feed
 * captured JSON strings so the suite never needs network access.
 */
class PetApiParsingTest {

    @Test
    @DisplayName("Dog CEO image response is parsed")
    void dogCeoImageUrl() {
        String json = "{\"message\":\"https://images.dog.ceo/breeds/beagle/n02088364_1.jpg\",\"status\":\"success\"}";
        assertEquals("https://images.dog.ceo/breeds/beagle/n02088364_1.jpg", DogCeoImageService.parseImageUrl(json));
    }

    @Test
    @DisplayName("Dog CEO error status raises instead of returning junk (edge case)")
    void dogCeoErrorStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> DogCeoImageService.parseImageUrl("{\"message\":\"Breed not found\",\"status\":\"error\"}"));
    }

    @Test
    @DisplayName("Nested breed catalogue is flattened recursively")
    void dogCeoBreedCatalogueRecursive() {
        String json = "{\"message\":{\"retriever\":[\"golden\",\"labrador\"],\"beagle\":[],\"hound\":[\"afghan\"]},"
                + "\"status\":\"success\"}";

        List<DogCeoImageService.BreedEntry> breeds = DogCeoImageService.parseBreedList(json);

        assertEquals(4, breeds.size());
        assertTrue(breeds.stream().anyMatch(entry ->
                entry.slug().equals("retriever/golden") && entry.displayName().equals("Golden Retriever")));
        assertTrue(breeds.stream().anyMatch(entry ->
                entry.slug().equals("beagle") && entry.displayName().equals("Beagle")));
    }

    @Test
    @DisplayName("Provider capabilities select the right species")
    void providerCapabilities() {
        PetImageService dog = new DogCeoImageService();
        PetImageService cat = new CatApiImageService("dummy-key");

        assertTrue(dog.supports(Species.DOG));
        assertFalse(dog.supports(Species.CAT));
        assertTrue(cat.supports(Species.CAT));
        assertEquals("Dog CEO API", dog.providerName());
        assertEquals("TheCatAPI", cat.providerName());
    }

    @Test
    @DisplayName("TheCatAPI image response is parsed")
    void catApiImageUrl() {
        String json = "[{\"id\":\"abc\",\"url\":\"https://cdn2.thecatapi.com/images/abc.jpg\",\"breeds\":[]}]";
        assertEquals("https://cdn2.thecatapi.com/images/abc.jpg", CatApiImageService.parseImageUrl(json));
    }

    @Test
    @DisplayName("TheCatAPI empty response raises (edge case)")
    void catApiEmptyResponse() {
        assertThrows(IllegalArgumentException.class, () -> CatApiImageService.parseImageUrl("[]"));
    }

    @Test
    @DisplayName("TheCatAPI breed list is parsed into id -> name")
    void catApiBreedList() {
        String json = "[{\"id\":\"abys\",\"name\":\"Abyssinian\"},{\"id\":\"beng\",\"name\":\"Bengal\"}]";

        Map<String, String> breeds = CatApiImageService.parseBreedList(json);

        assertEquals(2, breeds.size());
        assertEquals("Abyssinian", breeds.get("abys"));
        assertEquals("Bengal", breeds.get("beng"));
    }
}
