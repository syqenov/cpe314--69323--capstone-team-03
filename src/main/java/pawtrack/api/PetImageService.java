package pawtrack.api;

import pawtrack.model.Species;

import java.io.IOException;

/**
 * Abstraction over a public pet-image REST API. Implemented by
 * {@link DogCeoImageService} (Dog CEO API) and {@link CatApiImageService}
 * (TheCatAPI), which is the second interface with two concrete implementations
 * in the project.
 */
public interface PetImageService {

    /** @return the display name of the underlying API. */
    String providerName();

    /** @return true when this provider can serve the given species. */
    boolean supports(Species species);

    /**
     * Fetches a random image URL for the given breed.
     *
     * @param breed breed name, or {@code null}/blank for any breed
     */
    PetImageResult fetchRandomImage(String breed) throws IOException;
}