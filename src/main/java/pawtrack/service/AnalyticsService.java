package pawtrack.service;

import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.ClassificationNode;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-mining service. Runs a single pass over the adoption records to produce
 * frequency analysis (most adopted breed/species) and an aggregate (average
 * shelter stay length), then offers a recursive classification roll-up.
 */
public final class AnalyticsService {

    /**
     * Mines the given dataset.
     *
     * @param animals every animal known to the shelter
     * @param records every completed adoption
     */
    public AnalyticsReport analyze(List<Animal> animals, List<AdoptionRecord> records) {
        Map<Integer, Animal> byId = new HashMap<>();
        for (Animal animal : animals) {
            if (animal.getId() != 0) {
                byId.put(animal.getId(), animal);
            }
        }

        Map<String, Long> bySpecies = new LinkedHashMap<>();
        Map<String, Long> byBreed = new LinkedHashMap<>();
        Map<String, List<Double>> staysBySpecies = new LinkedHashMap<>();
        List<Double> allStays = new ArrayList<>();

        for (AdoptionRecord record : records) {
            Animal animal = byId.get(record.getAnimalId());
            if (animal == null) {
                continue;
            }
            bySpecies.merge(animal.species().label(), 1L, Long::sum);
            byBreed.merge(animal.getBreed(), 1L, Long::sum);

            double stay = stayDays(animal, record);
            allStays.add(stay);
            staysBySpecies.computeIfAbsent(animal.species().label(), key -> new ArrayList<>()).add(stay);
        }

        Map<String, Double> averageBySpecies = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : staysBySpecies.entrySet()) {
            averageBySpecies.put(entry.getKey(), average(entry.getValue()));
        }

        return new AnalyticsReport(
                animals.size(),
                records.size(),
                sortByValueDesc(bySpecies),
                sortByValueDesc(byBreed),
                averageBySpecies,
                average(allStays));
    }

  
    /**
     * Shelter stay in whole days. Negative values (bad historical data where an
     * adoption predates the intake) are clamped to zero.
     */
    public static double stayDays(Animal animal, AdoptionRecord record) {
        long days = ChronoUnit.DAYS.between(animal.getIntakeDate(), record.getAdoptionDate());
        return Math.max(0, days);
    }

    public static double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /** Orders a frequency map from most to least frequent (name is the tie-breaker). */
    public static <K> Map<K, Long> sortByValueDesc(Map<K, Long> source) {
        List<Map.Entry<K, Long>> entries = new ArrayList<>(source.entrySet());
        entries.sort(Comparator
                .comparingLong((Map.Entry<K, Long> entry) -> entry.getValue()).reversed()
                .thenComparing(entry -> String.valueOf(entry.getKey())));
        Map<K, Long> ordered = new LinkedHashMap<>();
        for (Map.Entry<K, Long> entry : entries) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return ordered;
    }

    public static Map<Integer, List<Animal>> groupByClassification(List<Animal> animals) {
        Map<Integer, List<Animal>> grouped = new HashMap<>();
        for (Animal animal : animals) {
            if (animal.getClassificationId() != null) {
                grouped.computeIfAbsent(animal.getClassificationId(), key -> new ArrayList<>()).add(animal);
            }
        }
        return grouped;
    }

    /**
     * Recursive roll-up of adopted animals per classification node. Each node's
     * value is the total for that node <em>plus every descendant</em>, which is
     * why a plain loop cannot replace this method: the result of each recursive
     * call feeds its parent's running total.
     *
     * @return ordered map of classification path -&gt; adopted animal count
     */
    public Map<String, Long> adoptionCountByClassification(
            ClassificationNode root, Map<Integer, List<Animal>> animalsByClassificationId) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (root != null) {
            countRecursive(root, animalsByClassificationId, result);
        }
        return result;
    }

    private long countRecursive(ClassificationNode node,
                                Map<Integer, List<Animal>> animalsByClassificationId,
                                Map<String, Long> result) {
        long total = 0;
        List<Animal> direct = animalsByClassificationId.get(node.getId());
        if (direct != null) {
            for (Animal animal : direct) {
                if (animal.getStatus() == AnimalStatus.ADOPTED) {
                    total++;
                }
            }
        }
        for (ClassificationNode child : node.getChildren()) {
            total += countRecursive(child, animalsByClassificationId, result);
        }
        result.put(node.path(), total);
        return total;
    }
}
