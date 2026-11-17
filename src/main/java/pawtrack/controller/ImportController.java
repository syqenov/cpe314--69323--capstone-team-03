package pawtrack.controller;

import pawtrack.dao.Database;
import pawtrack.service.ClassificationService;
import pawtrack.service.CsvImportResult;
import pawtrack.service.CsvImportService;
import pawtrack.service.ImportListener;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class ImportController {
    private final Database database;
    private final ClassificationService classification;
    private final AuthController auth;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private String status = "";

    public ImportController(Database database, ClassificationService classification, AuthController auth) {
        this.database = database;
        this.classification = classification;
        this.auth = auth;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public String getStatus() {
        return status;
    }

    public void importCsv(Path path, IntConsumer onProgress, Consumer<CsvImportResult> onDone, Consumer<Exception> onError) {
        auth.requireStaff();
        if (!busy.compareAndSet(false, true)) {
            throw new IllegalStateException("A CSV import is already in progress");
        }
        status = "Importing...";
        SwingWorker<CsvImportResult, Integer> worker = new SwingWorker<>() {
            @Override
            protected CsvImportResult doInBackground() throws Exception {
                CsvImportService service = new CsvImportService(database, classification);
                return service.importFile(path, new ImportListener() {
                    @Override
                    public void onProgress(int processed, int total) {
                        if (onProgress != null) {
                            int percent = total > 0 ? (int) (processed * 100.0 / total) : 0;
                            publish(percent);
                        }
                    }

                    @Override
                    public void onFinished(CsvImportResult result) {
                    }
                });
            }

            @Override
            protected void process(java.util.List<Integer> updates) {
                if (onProgress != null && !updates.isEmpty()) {
                    onProgress.accept(updates.get(updates.size() - 1));
                }
            }

            @Override
            protected void done() {
                busy.set(false);
                try {
                    CsvImportResult result = get();
                    status = "Done";
                    if (onDone != null) {
                        onDone.accept(result);
                    }
                } catch (Exception e) {
                    status = "Failed";
                    if (onError != null) {
                        onError.accept(e);
                    }
                }
            }
        };
        try {
            worker.execute();
        } catch (RuntimeException launchFailure) {
            busy.set(false);
            status = "Failed";
            throw launchFailure;
        }
    }
}
