package pawtrack.service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Callback used by {@link CsvImportService} to report progress while it runs on
 * a background thread (a Swing {@code SwingWorker} in the GUI).
 */
public interface ImportListener {

    void onProgress(int processed, int total);

    void onFinished(CsvImportResult result);

    /** No-op listener for tests and headless usage. */
    ImportListener NONE = new ImportListener() {
        @Override
        public void onProgress(int processed, int total) {
        }

        @Override
        public void onFinished(CsvImportResult result) {
        }
    };

    static ImportListener of(BiConsumer<Integer, Integer> progress, Consumer<CsvImportResult> finished) {
        return new ImportListener() {
            @Override
            public void onProgress(int processed, int total) {
                progress.accept(processed, total);
            }

            @Override
            public void onFinished(CsvImportResult result) {
                finished.accept(result);
            }
        };
    }
}
}
