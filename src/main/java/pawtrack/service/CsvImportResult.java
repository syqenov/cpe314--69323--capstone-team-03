package pawtrack.service;

/**
 * Summary produced by a bulk CSV import.
 */
public record CsvImportResult(int imported, int skipped, int adoptions, int newAdopters, long durationMillis) {

    public static CsvImportResult empty() {
        return new CsvImportResult(0, 0, 0, 0, 0);
    }

    public String summary() {
        return String.format(
                "Imported %d animals (%d adopted, %d new adopters), skipped %d malformed rows in %.2fs",
                imported, adoptions, newAdopters, skipped, durationMillis / 1000.0);
    }
}

