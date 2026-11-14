package pawtrack.service;

import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.model.Animal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bulk import tests: happy path, malformed-row handling and quoted CSV parsing.
 */
class CsvImportServiceTest {

    private static final String HEADER =
            "name,species,breed,age_months,intake_date,status,adoption_date,adopter_name,adopter_email\n";

    @Test
    @DisplayName("Valid rows are imported, adopted rows create adopters and records")
    void importsValidRows() throws Exception {
        String csv = HEADER
                + "Rex,Dog,Beagle,24,2024-01-01,Available,,\n"
                + "Mia,Cat,Siamese,12,2024-01-05,Available,2024-02-01,Jane Doe,jane@example.com\n";

        try (Database database = Database.openInMemory()) {
            CsvImportService service = new CsvImportService(database);

            CsvImportResult result = service.importReader(new StringReader(csv), 2, ImportListener.NONE);

            assertEquals(2, result.imported());
            assertEquals(1, result.adoptions());
            assertEquals(1, result.newAdopters());
            assertEquals(0, result.skipped());

            AnimalDao dao = new AnimalDao(database);
            assertEquals(2, dao.count());
            assertTrue(dao.findAll().stream().allMatch(animal -> animal.getClassificationId() != null),
                    "imported animals should be linked to the classification tree");
        }
    }

    @Test
    @DisplayName("Malformed rows are skipped without aborting the import (edge case)")
    void skipsMalformedRows() throws Exception {
        String csv = HEADER
                + "Rex,Dog,Beagle,24,2024-01-01,Available,,\n"             // valid
                + "Broken,Dog,Beagle,notanumber,2024-01-01,Available,,\n"  // bad age
                + ",Dog,Beagle,12,2024-01-01,Available,,\n"                // blank name
                + "Ghost,Dog,Beagle,-5,2024-01-01,Available,,\n"           // negative age
                + "NoDate,Dog,Beagle,12,not-a-date,Available,,\n";         // bad date

        try (Database database = Database.openInMemory()) {
            CsvImportService service = new CsvImportService(database);

            CsvImportResult result = service.importReader(new StringReader(csv), 5, ImportListener.NONE);

            assertEquals(1, result.imported());
            assertEquals(4, result.skipped());
            assertEquals(1, new AnimalDao(database).count());
        }
    }

    @Test
    @DisplayName("Quoted fields containing commas are parsed as a single value")
    void quotedFields() throws Exception {
        String csv = HEADER + "\"Rex, Jr.\",Dog,Beagle,24,2024-01-01,Available,,\n";

        try (Database database = Database.openInMemory()) {
            CsvImportService service = new CsvImportService(database);
            CsvImportResult result = service.importReader(new StringReader(csv), 1, ImportListener.NONE);

            assertEquals(1, result.imported());
            Animal imported = new AnimalDao(database).findAll().get(0);
            assertEquals("Rex, Jr.", imported.getName());
        }
    }

    @Test
    @DisplayName("splitCsv handles quotes, escaped quotes and empty cells")
    void splitCsvHandlesQuotes() {
        List<String> cells = CsvImportService.splitCsv("a,\"b,c\",\"d\"\"e\",,");

        assertEquals(5, cells.size());
        assertEquals("a", cells.get(0));
        assertEquals("b,c", cells.get(1));
        assertEquals("d\"e", cells.get(2));
        assertEquals("", cells.get(3));
        assertEquals("", cells.get(4));
    }

    @Test
    @DisplayName("Blank lines, comments and a missing header are tolerated")
    void blankAndHeaderTolerance() throws Exception {
        String csv = "Rex,Dog,Beagle,24,2024-01-01,Available,,\n"
                + "\n"
                + "# a comment line\n"
                + "Mia,Cat,Siamese,12,2024-01-05,Available,,\n";

        try (Database database = Database.openInMemory()) {
            CsvImportService service = new CsvImportService(database);
            CsvImportResult result = service.importReader(new StringReader(csv), 4, ImportListener.NONE);

            assertEquals(2, result.imported());
            assertFalse(result.skipped() > 1, "only the comment line is skipped");
        }
    }
}
