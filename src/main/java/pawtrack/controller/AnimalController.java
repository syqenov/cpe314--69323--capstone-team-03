package pawtrack.controller;

import pawtrack.dao.AnimalDao;
import pawtrack.model.Animal;

import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AnimalController {
    private static final Logger LOGGER = Logger.getLogger(AnimalController.class.getName());

    private final AnimalDao animalDao;
    private List<Animal> animals = new ArrayList<>();
    private Animal selected;
    private String status = "Ready";
    private Consumer<String> statusListener;
    private Runnable onChange;

    public AnimalController(AnimalDao animalDao) {
        this.animalDao = animalDao;
    }

    public void setStatusListener(Consumer<String> statusListener) {
        this.statusListener = statusListener;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void refresh() {
        try {
            animals = animalDao.findAll();
            notifyChange();
            setStatus("Loaded " + animals.size() + " animal(s)");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load animals", e);
            setStatus("Error loading animals: " + e.getMessage());
        }
    }

    public void refreshAsync() {
        new SwingWorker<List<Animal>, Void>() {
            @Override
            protected List<Animal> doInBackground() throws Exception {
                return animalDao.findAll();
            }

            @Override
            protected void done() {
                try {
                    animals = get();
                    notifyChange();
                    setStatus("Loaded " + animals.size() + " animal(s)");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to load animals", e);
                    setStatus("Error loading animals: " + e.getMessage());
                }
            }
        }.execute();
    }

    public List<Animal> getAnimals() {
        return Collections.unmodifiableList(animals);
    }

    /** Inserts a new animal and refreshes the in-memory list. */
    public Animal addAnimal(Animal animal) throws SQLException {
        animalDao.insert(animal);
        animals.add(animal);
        notifyChange();
        setStatus("Added " + animal.getName());
        return animal;
    }

    /** Updates an existing animal's details and status. */
    public boolean updateAnimal(Animal animal) throws SQLException {
        boolean updated = animalDao.updateDetails(animal.getId(), animal.getName(), animal.getBreed(),
                animal.getAgeMonths(), animal.getIntakeDate());
        animalDao.updateStatus(animal.getId(), animal.getStatus());
        refresh();
        return updated;
    }

    /** Deletes an animal by id and refreshes the in-memory list. */
    public boolean deleteAnimal(int id) throws SQLException {
        boolean deleted = animalDao.deleteById(id);
        animals.removeIf(a -> a.getId() == id);
        notifyChange();
        setStatus(deleted ? "Animal deleted" : "Animal not found");
        return deleted;
    }

    public void setAnimals(List<Animal> animals) {
        this.animals = new ArrayList<>(animals);
        notifyChange();
    }

    public Animal getSelected() {
        return selected;
    }

    public void setSelected(Animal selected) {
        this.selected = selected;
    }

    public String getStatus() {
        return status;
    }

    private void setStatus(String status) {
        this.status = status;
        if (statusListener != null) {
            statusListener.accept(status);
        }
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
