package pawtrack.controller;

import pawtrack.dao.AnimalDao;
import pawtrack.model.Animal;
import pawtrack.model.ClassificationNode;
import pawtrack.service.ClassificationService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClassificationController {
    private static final Logger LOGGER = Logger.getLogger(ClassificationController.class.getName());
    private final ClassificationService classification;
    private final AnimalDao animalDao;

    public ClassificationController(ClassificationService classification, AnimalDao animalDao) {
        this.classification = classification;
        this.animalDao = animalDao;
    }

    public List<Animal> filterByPath(String path) {
        if (path == null || "<All animals>".equals(path)) {
            try {
                return animalDao.findAll();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load animals", e);
                return List.of();
            }
        }
        Optional<ClassificationNode> node = classification.findByPath(path);
        if (node.isEmpty()) {
            return List.of();
        }
        Set<Integer> ids = new HashSet<>();
        for (ClassificationNode descendant : node.get().flatten()) {
            ids.add(descendant.getId());
        }
        try {
            List<Animal> matches = new ArrayList<>();
            for (Animal animal : animalDao.findAll()) {
                if (animal.getClassificationId() != null && ids.contains(animal.getClassificationId())) {
                    matches.add(animal);
                }
            }
            return matches;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Filter failed", e);
            return List.of();
        }
    }

    public List<String> allPaths() {
        return classification.allPaths();
    }

    public Optional<ClassificationNode> findByPath(String path) {
        return classification.findByPath(path);
    }

    public ClassificationService getClassification() {
        return classification;
    }
}
