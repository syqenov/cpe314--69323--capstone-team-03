package pawtrack.api;

import pawtrack.model.Species;

/**
 * Result of a pet-image API lookup.
 *
 * @param imageUrl remote URL of the returned image
 * @param species  species the image belongs to
 * @param breed    breed query that produced the image (may be blank)
 * @param provider human readable API name
 */
public record PetImageResult(String imageUrl, Species species, String breed, String provider) {
}
