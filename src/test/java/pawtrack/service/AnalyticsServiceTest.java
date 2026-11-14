package pawtrack.service;

import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.ClassificationNode;
import pawtrack.model.Species;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-mining tests: frequency analysis (most adopted breed/species), aggregate
 * average shelter stay, and the recursive classification roll-up.
 */
class AnalyticsServiceTest {

    private static final LocalDate INTAKE = LocalDate.of(2024, 1, 1);
    private final AnalyticsService analytics = new AnalyticsService();

    private static Animal animal(int id, Species species, String breed, LocalDate intake, AnimalStatus status) {
        return Animal.create(species, id, "Pet" + id, breed, 24, intake, status);
    }

    private static AdoptionRecord record(int animalId, LocalDate adoptionDate) {
        return new AdoptionRecord(0, animalId, 1, adoptionDate, 100.0);
    }

    @Test
    @DisplayName("Frequency analysis finds the most adopted species and breed")
    void mostAdoptedSpeciesAndBreed() {
        List<Animal> animals = List.of(
                animal(1, Species.DOG, "Golden Retriever", INTAKE, AnimalStatus.ADOPTED),
                animal(2, Species.DOG, "Golden Retriever", INTAKE, AnimalStatus.ADOPTED),
                animal(3, Species.CAT, "Siamese", INTAKE, AnimalStatus.ADOPTED),
                animal(4, Species.DOG, "Beagle", INTAKE, AnimalStatus.AVAILABLE));
        List<AdoptionRecord> records = List.of(record(1, INTAKE), record(2, INTAKE), record(3, INTAKE));

        AnalyticsReport report = analytics.analyze(animals, records);

        assertEquals(3, report.totalAdoptions());
        assertEquals(4, report.totalAnimals());
        assertEquals("Dog", report.mostAdoptedSpecies());
        assertEquals("Golden Retriever", report.mostAdoptedBreed());
        assertEquals(2L, report.adoptionsBySpecies().get("Dog"));
        assertEquals(1L, report.adoptionsBySpecies().get("Cat"));
        assertTrue(report.hasAdoptions());
    }

    @Test
    @DisplayName("Average shelter stay is aggregated overall and per species")
    void averageStayLength() {
        List<Animal> animals = List.of(
                animal(1, Species.DOG, "Beagle", LocalDate.of(2024, 1, 1), AnimalStatus.ADOPTED),
                animal(2, Species.DOG, "Poodle", LocalDate.of(2024, 1, 1), AnimalStatus.ADOPTED));
        List<AdoptionRecord> records = List.of(
                record(1, LocalDate.of(2024, 1, 11)),   // 10 days
                record(2, LocalDate.of(2024, 1, 21)));  // 20 days

        AnalyticsReport report = analytics.analyze(animals, records);

        assertEquals(15.0, report.averageStayDaysOverall(), 0.001);
        assertEquals(15.0, report.averageStayDaysBySpecies().get("Dog"), 0.001);
    }

    @Test
    @DisplayName("Adoption recorded before intake clamps the stay to zero (edge case)")
    void negativeStayClamped() {
        List<Animal> animals = List.of(animal(1, Species.CAT, "Siamese", LocalDate.of(2024, 2, 1), AnimalStatus.ADOPTED));
        List<AdoptionRecord> records = List.of(record(1, LocalDate.of(2024, 1, 1)));

        AnalyticsReport report = analytics.analyze(animals, records);

        assertEquals(0.0, report.averageStayDaysOverall(), 0.001);
    }

    @Test
    @DisplayName("Empty dataset yields zeroed, non-crashing results (edge case)")
    void emptyDataset() {
        AnalyticsReport report = analytics.analyze(List.of(), List.of());

        assertEquals(0, report.totalAdoptions());
        assertFalse(report.hasAdoptions());
        assertEquals(0.0, report.averageStayDaysOverall(), 0.001);
        assertEquals("-", report.mostAdoptedSpecies());
        assertEquals("-", report.mostAdoptedBreed());
    }

    @Test
    @DisplayName("Adoption records for unknown animals are ignored in the frequency maps")
    void unknownAnimalRecordsIgnored() {
        List<Animal> animals = List.of(animal(1, Species.DOG, "Beagle", INTAKE, AnimalStatus.ADOPTED));
        List<AdoptionRecord> records = List.of(record(1, INTAKE), record(999, INTAKE));

        AnalyticsReport report = analytics.analyze(animals, records);

        assertEquals(1L, report.adoptionsBySpecies().get("Dog"));
    }

    @Test
    @DisplayName("Frequency ordering is descending with an alphabetical tie-break")
    void orderingTieBreak() {
        Map<String, Long> source = new LinkedHashMap<>();
        source.put("Zebra", 2L);
        source.put("Apple", 2L);
        source.put("Mango", 1L);

        Map<String, Long> sorted = AnalyticsService.sortByValueDesc(source);

        assertEquals(new ArrayList<>(List.of("Apple", "Zebra", "Mango")), new ArrayList<>(sorted.keySet()));
    }

    @Test
    @DisplayName("Recursive classification roll-up accumulates counts through the whole subtree")
    void recursiveClassificationRollup() {
        ClassificationNode all = new ClassificationNode(1, "All");
        all.addChild(new ClassificationNode(2, "Dog"));
        ClassificationNode dog = all.getChildren().get(0);
        dog.addChild(new ClassificationNode(3, "Retriever"));
        ClassificationNode retriever = dog.getChildren().get(0);
        retriever.addChild(new ClassificationNode(4, "Golden Retriever"));
        retriever.addChild(new ClassificationNode(5, "Labrador"));

        Animal adoptedGolden = animal(10, Species.DOG, "Golden Retriever", INTAKE, AnimalStatus.ADOPTED);
        adoptedGolden.setClassificationId(4);
        Animal availableGolden = animal(11, Species.DOG, "Golden Retriever", INTAKE, AnimalStatus.AVAILABLE);
        availableGolden.setClassificationId(4);
        Animal adoptedLab = animal(12, Species.DOG, "Labrador", INTAKE, AnimalStatus.ADOPTED);
        adoptedLab.setClassificationId(5);

        Map<Integer, List<Animal>> grouped = AnalyticsService.groupByClassification(
                List.of(adoptedGolden, availableGolden, adoptedLab));

        Map<String, Long> counts = analytics.adoptionCountByClassification(all, grouped);

        assertEquals(1L, counts.get("All > Dog > Retriever > Golden Retriever"));
        assertEquals(1L, counts.get("All > Dog > Retriever > Labrador"));
        assertEquals(2L, counts.get("All > Dog > Retriever"));
        assertEquals(2L, counts.get("All > Dog"));
        assertEquals(2L, counts.get("All"));
    }
}
