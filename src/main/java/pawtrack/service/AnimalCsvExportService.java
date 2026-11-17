package pawtrack.service;

import pawtrack.model.Animal;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Exports animal records without accessing the Swing UI or database. */
public final class AnimalCsvExportService {
    private AnimalCsvExportService() { }

    public static int export(Path destination, List<Animal> animals) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(animals, "animals");
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("ID,Name,Species,Breed,Age (months),Gender,Status,Adoption Fee,Classification ID,Intake Date");
            writer.newLine();
            for (Animal animal : animals) {
                Objects.requireNonNull(animal, "animal");
                writer.write(String.format(java.util.Locale.ROOT,
                        "%d,%s,%s,%s,%d,,%s,%.2f,%s,%s",
                        animal.getId(), escapeCsv(animal.getName()),
                        escapeCsv(animal.species().label()), escapeCsv(animal.getBreed()),
                        animal.getAgeMonths(), escapeCsv(animal.getStatus().label()),
                        animal.adoptionFee(),
                        animal.getClassificationId() == null ? "" : animal.getClassificationId(),
                        animal.getIntakeDate()));
                writer.newLine();
            }
        }
        return animals.size();
    }

    /** CSV field escaping, including embedded quotes, commas and line breaks. */
    static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
