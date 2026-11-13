package pawtrack.api;

import pawtrack.model.Species;

import java.util.List;

/**
 * Chooses the correct {@link PetImageService} implementation for a species.
 */
public final class PetImageServiceFactory {

    private PetImageServiceFactory() {
    }

    public static PetImageService forSpecies(Species species) {
        if (species == Species.CAT) {
            return new CatApiImageService();
        }
        return new DogCeoImageService();
    }

    public static List<PetImageService> all() {
        return List.of(new DogCeoImageService(), new CatApiImageService());
    }
}
