package pawtrack.service;

import pawtrack.db.AdopterDao;
import pawtrack.db.AdoptionRecordDao;
import pawtrack.db.AnimalDao;
import pawtrack.db.Database;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Seeds a small, realistic starter dataset the first time the database is
 * empty, so the GUI and analytics panel have something to show immediately.
 */
public final class ShelterSeeder {

    private ShelterSeeder() {
    }

    /**
     * @return the number of animals inserted (0 when the database already had data)
     */
    public static int seedIfEmpty(Database database, ClassificationService classification) throws SQLException {
        AnimalDao animalDao = new AnimalDao(database);
        if (animalDao.count() > 0) {
            return 0;
        }
        AdopterDao adopterDao = new AdopterDao(database);
        AdoptionRecordDao recordDao = new AdoptionRecordDao(database);
        int created = 0;

        created += addAvailable(animalDao, classification, "Buddy", Species.DOG, "Golden Retriever", 18);
        created += addAvailable(animalDao, classification, "Luna", Species.DOG, "Labrador Retriever", 4);
        created += addAvailable(animalDao, classification, "Max", Species.DOG, "German Shepherd", 96);
        created += addAvailable(animalDao, classification, "Bella", Species.CAT, "Domestic Shorthair", 24);
        created += addAvailable(animalDao, classification, "Milo", Species.CAT, "Siamese", 5);
        created += addAvailable(animalDao, classification, "Rocky", Species.CAT, "Rabbit", 8);

        created += addAdopted(animalDao, adopterDao, recordDao, classification,
                "Daisy", Species.DOG, "Poodle", 30, 60, 12, "John Carter", "john.carter@example.com");
        created += addAdopted(animalDao, adopterDao, recordDao, classification,
                "Simba", Species.CAT, "Persian", 36, 90, 45, "Amelia Stone", "amelia.stone@example.com");
        created += addAdopted(animalDao, adopterDao, recordDao, classification,
                "Charlie", Species.DOG, "Beagle", 10, 30, 9, "Noah Patel", "noah.patel@example.com");

        return created;
    }

    private static int addAvailable(AnimalDao animalDao, ClassificationService classification,
                                    String name, Species species, String breed, int ageMonths) throws SQLException {
        Animal animal = Animal.create(species, 0, name, breed, ageMonths,
                LocalDate.now().minusDays(14), AnimalStatus.AVAILABLE);
        classification.classifyByBreed(breed)
                .or(() -> classification.speciesRoot(species))
                .ifPresent(node -> animal.setClassificationId(node.getId()));
        animalDao.insert(animal);
        return 1;
    }

    private static int addAdopted(AnimalDao animalDao, AdopterDao adopterDao, AdoptionRecordDao recordDao,
                                  ClassificationService classification, String name, Species species, String breed,
                                  int ageMonths, int daysInShelter, int daysAgoAdopted,
                                  String adopterName, String adopterEmail) throws SQLException {
        LocalDate adoptionDate = LocalDate.now().minusDays(daysAgoAdopted);
        LocalDate intakeDate = adoptionDate.minusDays(daysInShelter);
        Animal animal = Animal.create(species, 0, name, breed, ageMonths, intakeDate, AnimalStatus.ADOPTED);
        classification.classifyByBreed(breed)
                .or(() -> classification.speciesRoot(species))
                .ifPresent(node -> animal.setClassificationId(node.getId()));
        animalDao.insert(animal);

        Adopter adopter = adopterDao.findOrCreate(new Adopter(adopterName, adopterEmail, ""));
        recordDao.insert(new AdoptionRecord(animal.getId(), adopter.getId(), adoptionDate, animal.adoptionFee()));
        return 1;
    }
}