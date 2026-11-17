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
    private final AuthController auth;
    private List<Animal> animals = new ArrayList<>();
    private Animal selected;
    private String status = "Ready";
    private Consumer<String> statusListener;
    private Runnable onChange;

    /** Authenticated constructor used by the application. */
    public AnimalController(AnimalDao animalDao, AuthController auth) {
        this.animalDao = java.util.Objects.requireNonNull(animalDao, "animalDao");
        this.auth = java.util.Objects.requireNonNull(auth, "auth");
    }

    /** Compatibility constructor for read-only legacy clients; mutations are denied. */
    @Deprecated
    public AnimalController(AnimalDao animalDao) {
        this.animalDao = java.util.Objects.requireNonNull(animalDao, "animalDao");
        this.auth = null;
    }

    private void requireStaff() {
        if (auth == null) {
            throw new UnauthorizedException("Authenticated staff session required");
        }
        auth.requireStaff();
    }

    public java.util.Optional<Animal> findById(int id) throws SQLException {
        return animalDao.findById(id);
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
        requireStaff();
        animalDao.insert(animal);
        animals.add(animal);
        notifyChange();
        setStatus("Added " + animal.getName());
        return animal;
    }

    /** Updates an existing animal's details and status. */
    public boolean updateAnimal(Animal animal) throws SQLException {
        requireStaff();
        boolean updated = animalDao.updateDetails(animal.getId(), animal.getName(), animal.getBreed(),
                animal.getAgeMonths(), animal.getIntakeDate());
        if (!updated) {
            setStatus("Animal not found");
            return false;
        }
        animalDao.updateStatus(animal.getId(), animal.getStatus());
        refresh();
        return true;
    }

    /** Staff-only: changes how the animal came to the shelter (e.g. stray, surrender). */
    public boolean updateAnimalSource(int animalId, String source) throws SQLException {
        requireStaff();
        boolean updated = animalDao.updateSource(animalId, source);
        if (updated) {
            refresh();
        }
        return updated;
    }

    /** Deletes an animal by id and refreshes the in-memory list. */
    public boolean deleteAnimal(int id) throws SQLException {
        requireStaff();
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
