package pawtrack.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of a data-mining pass. Every map is ordered from highest to
 * lowest frequency/aggregate so it can feed the bar chart directly.
 */
public record AnalyticsReport(
        int totalAnimals,
        int totalAdoptions,
        Map<String, Long> adoptionsBySpecies,
        Map<String, Long> adoptionsByBreed,
        Map<String, Double> averageStayDaysBySpecies,
        double averageStayDaysOverall) {

    public boolean hasAdoptions() {
        return totalAdoptions > 0;
    }

    public List<Map.Entry<String, Long>> topBreeds(int limit) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(adoptionsByBreed.entrySet());
        if (limit >= 0 && entries.size() > limit) {
            return new ArrayList<>(entries.subList(0, limit));
        }
        return entries;
    }

    public String mostAdoptedSpecies() {
        return adoptionsBySpecies.isEmpty() ? "-" : adoptionsBySpecies.keySet().iterator().next();
    }

    public String mostAdoptedBreed() {
        return adoptionsByBreed.isEmpty() ? "-" : adoptionsByBreed.keySet().iterator().next();
    }
}
