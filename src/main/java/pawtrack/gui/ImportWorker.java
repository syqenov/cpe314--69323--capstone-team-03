package com.petshelter.gui;

import com.petshelter.service.CsvImportResult;
import com.petshelter.service.CsvImportService;
import com.petshelter.service.ImportListener;

import javax.swing.SwingWorker;
import java.nio.file.Path;

/**
 * Runs the bulk CSV import on a background thread so the Event Dispatch Thread
 * (and therefore the GUI) stays responsive. Progress is reported through
 * {@link SwingWorker#setProgress(int)}, which fires property change events the
 * frame listens to.
 */
public final class ImportWorker extends SwingWorker<CsvImportResult, Void> {

    private final CsvImportService service;
    private final Path path;

    public ImportWorker(CsvImportService service, Path path) {
        this.service = service;
        this.path = path;
    }

    @Override
    protected CsvImportResult doInBackground() throws Exception {
        return service.importFile(path, ImportListener.of(
                (processed, total) -> {
                    int percent = total > 0 ? (int) Math.min(100, processed * 100L / total) : 0;
                    setProgress(percent);
                },
                result -> {
                }));
    }
}
