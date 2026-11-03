package pawtrack.service;

import pawtrack.db.AdopterDao;
import pawtrack.db.AdoptionRecordDao;
import pawtrack.db.AnimalDao;
import pawtrack.db.ClassificationDao;
import pawtrack.db.Database;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bulk importer for the historical intake/adoption CSV log.
 *
 * <p>The whole import runs inside a single transaction using {@link AnimalDao},
 * {@link AdopterDao} and {@link AdoptionRecordDao} (all backed by
 * {@code PreparedStatement}s). It is intentionally thread-agnostic so the GUI can
 * run it on a background worker while reporting progress through an
 * {@link ImportListener}.</p>
 */
public final class CsvImportService {

    public static final String[] EXPECTED_HEADER = {
            "name", "species", "breed", "age_months", "intake_date", "status", "adoption_date", "adopter_name",
            "adopter_email"
    };

    private static final int PROGRESS_INTERVAL = 250;

    private final Database database;
    private final ClassificationService classification;
    private final AnimalDao animalDao;
    private final AdopterDao adopterDao;
    private final AdoptionRecordDao recordDao;

    public CsvImportService(Database database, ClassificationService classification) {
        this.database = database;
        this.classification = classification;
        this.animalDao = new AnimalDao(database);
        this.adopterDao = new AdopterDao(database);
        this.recordDao = new AdoptionRecordDao(database);
    }

    /** Convenience constructor that seeds/loads the classification tree if needed. */
    public CsvImportService(Database database) throws SQLException {
        this(database, ClassificationService.loadOrSeed(new ClassificationDao(database)));
    }

    /** Counts the lines of a file so a progress bar can be driven from it. */
    public static int countLines(Path path) throws IOException {
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) {
                lines++;
            }
        }
        return lines;
    }

    public CsvImportResult importFile(Path path, ImportListener listener) throws IOException, SQLException {
        int estimatedTotal = countLines(path);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return importReader(reader, estimatedTotal, listener);
        }
    }

    /**
     * Imports from any character stream within one transaction.
     *
     * @param source         the CSV content (header optional)
     * @param estimatedTotal total line count used for progress, or 0 if unknown
     */
    public CsvImportResult importReader(Reader source, int estimatedTotal, ImportListener listener)
            throws IOException, SQLException {
        ImportListener progress = listener == null ? ImportListener.NONE : listener;
        long startedAt = System.currentTimeMillis();
        int imported = 0;
        int skipped = 0;
        int adoptions = 0;
        int newAdopters = 0;

        Connection connection = database.connection();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        Map<String, Adopter> adopterCache = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            boolean headerHandled = false;
            int processed = 0;

            while ((line = reader.readLine()) != null) {
                processed++;
                if (line.isBlank()) {
                    continue;
                }
                if (!headerHandled) {
                    headerHandled = true;
                    if (looksLikeHeader(line)) {
                        continue;
                    }
                }

                CsvRow row = parse(line);
                if (row == null) {
                    skipped++;
                    continue;
                }

                try {
                    Animal animal = Animal.create(row.species(), 0, row.name(), row.breed(),
                            row.ageMonths(), row.intakeDate(), row.status());
                    if (row.hasAdoption()) {
                        animal.setStatus(AnimalStatus.ADOPTED);
                    }
                    assignClassification(animal, row.breed());
                    animalDao.insert(animal);
                    imported++;

                    if (row.hasAdoption()) {
                        String key = row.adopterEmail().toLowerCase(Locale.ROOT);
                        Adopter adopter = adopterCache.get(key);
                        if (adopter == null) {
                            boolean alreadyStored = adopterDao.findByEmail(row.adopterEmail()).isPresent();
                            String adopterName = row.adopterName() == null || row.adopterName().isBlank()
                                    ? row.adopterEmail() : row.adopterName();
                            adopter = adopterDao.findOrCreate(new Adopter(adopterName, row.adopterEmail(), ""));
                            if (!alreadyStored) {
                                newAdopters++;
                            }
                            adopterCache.put(key, adopter);
                        }
                        AdoptionRecord record = new AdoptionRecord(
                                animal.getId(), adopter.getId(), row.adoptionDate(), animal.adoptionFee());
                        recordDao.insert(record);
                        adoptions++;
                    }
                } catch (IllegalArgumentException malformedRow) {
                    skipped++;
                }

                if (processed % PROGRESS_INTERVAL == 0) {
                    progress.onProgress(processed, Math.max(estimatedTotal, processed));
                }
            }
            connection.commit();
            progress.onProgress(processed, Math.max(estimatedTotal, processed));
        } catch (IOException | SQLException | RuntimeException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }

        CsvImportResult result = new CsvImportResult(imported, skipped, adoptions, newAdopters,
                System.currentTimeMillis() - startedAt);
        progress.onFinished(result);
        return result;
    }

    private void assignClassification(Animal animal, String breed) {
        classification.classifyByBreed(breed)
                .or(() -> classification.speciesRoot(animal.species()))
                .ifPresent(node -> animal.setClassificationId(node.getId()));
    }

    private static boolean looksLikeHeader(String line) {
        List<String> cells = splitCsv(line);
        return !cells.isEmpty() && cells.get(0).trim().equalsIgnoreCase("name");
    }

    /**
     * Parses a single CSV line.
     *
     * @return a {@link CsvRow}, or {@code null} for blank/comment/short/malformed lines
     */
    public CsvRow parse(String line) {
        if (line == null || line.isBlank() || line.trim().startsWith("#")) {
            return null;
        }
        List<String> cells = splitCsv(line);
        if (cells.size() < 5) {
            return null;
        }
        try {
            String name = cells.get(0).trim();
            Species species = Species.fromLabel(cells.get(1));
            String breed = cells.get(2).trim();
            int ageMonths = Integer.parseInt(cells.get(3).trim());
            LocalDate intakeDate = LocalDate.parse(cells.get(4).trim());
            if (name.isEmpty() || breed.isEmpty()) {
                return null;
            }
            AnimalStatus status = cells.size() > 5 && !cells.get(5).isBlank()
                    ? AnimalStatus.fromLabel(cells.get(5))
                    : AnimalStatus.AVAILABLE;
            LocalDate adoptionDate = cells.size() > 6 && !cells.get(6).isBlank()
                    ? LocalDate.parse(cells.get(6).trim())
                    : null;
            String adopterName = cells.size() > 7 ? cells.get(7).trim() : "";
            String adopterEmail = cells.size() > 8 ? cells.get(8).trim() : "";
            return new CsvRow(name, species, breed, ageMonths, intakeDate, status, adoptionDate,
                    adopterName, adopterEmail);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    /** Quote-aware CSV split. */
    public static List<String> splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
