package pawtrack.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimalCsvExportServiceTest {
    @TempDir Path temp;

    @Test
    void createsCsvWithHeaderAndRecords() throws Exception {
        Path output = temp.resolve("animals.csv");
        Animal dog = new Dog(7, "Cooper", "Mixed", 13,
                LocalDate.of(2026, 5, 10), AnimalStatus.AVAILABLE);
        assertEquals(1, AnimalCsvExportService.export(output, List.of(dog)));
        String csv = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("ID,Name,Species,Breed"));
        assertTrue(csv.contains("7,Cooper"));
        assertTrue(csv.contains("2026-05-10"));
    }

    @Test
    void escapesSpecialCharactersAndSupportsEmptyList() throws Exception {
        assertEquals("\"A,B\"", AnimalCsvExportService.escapeCsv("A,B"));
        assertEquals("\"a\"\"b\"", AnimalCsvExportService.escapeCsv("a\"b"));
        assertEquals("\"a\nb\"", AnimalCsvExportService.escapeCsv("a\nb"));
        assertEquals("", AnimalCsvExportService.escapeCsv(null));
        Path output = temp.resolve("empty.csv");
        assertEquals(0, AnimalCsvExportService.export(output, List.of()));
        assertEquals(1, Files.readAllLines(output).size());
    }
}
