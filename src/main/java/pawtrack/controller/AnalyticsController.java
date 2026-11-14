package pawtrack.controller;

import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.model.Animal;
import pawtrack.service.AnalyticsReport;
import pawtrack.service.AnalyticsService;
import pawtrack.service.ClassificationService;
import pawtrack.service.WaitlistService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AnalyticsController {
    private static final Logger LOGGER = Logger.getLogger(AnalyticsController.class.getName());

    private final AnimalDao animalDao;
    private final AdoptionRecordDao recordDao;
    private final AnalyticsService analytics;
    private final WaitlistService waitlist;
    private ClassificationService classification;

    public AnalyticsController(AnimalDao animalDao, AdoptionRecordDao recordDao,
                               AnalyticsService analytics,
                               WaitlistService waitlist, ClassificationService classification) {
        this.animalDao = animalDao;
        this.recordDao = recordDao;
        this.analytics = analytics;
        this.waitlist = waitlist;
        this.classification = classification;
    }

    public AnalyticsSnapshot compute() {
        try {
            List<Animal> animals = animalDao.findAll();
            var records = recordDao.findAll();
            AnalyticsReport report = analytics.analyze(animals, records);
            Map<Integer, List<Animal>> grouped = AnalyticsService.groupByClassification(animals);
            Map<String, Long> classCounts = analytics.adoptionCountByClassification(classification.root(), grouped);
            return new AnalyticsSnapshot(report, classCounts, waitlist.totalWaiters(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Analytics failed", e);
            return new AnalyticsSnapshot(null, Map.of(), 0, e.getMessage());
        }
    }

    public void setClassification(ClassificationService classification) {
        this.classification = classification;
    }

    public record AnalyticsSnapshot(AnalyticsReport report, Map<String, Long> classCounts,
                                    int totalWaiters, String error) {}
}
