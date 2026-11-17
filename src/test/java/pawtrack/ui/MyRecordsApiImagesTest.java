package pawtrack.ui;

import org.junit.jupiter.api.Test;
import pawtrack.model.Species;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** All image behavior tests are deterministic and never contact external APIs. */
class MyRecordsApiImagesTest {
    @Test
    void correctlyParsesSpeciesAndBreedForExistingWaitlistRows() {
        assertEquals(Species.DOG, MyRecordsPanel.speciesFor("Dog / Golden Retriever"));
        assertEquals("Golden Retriever", MyRecordsPanel.breedForApi("Dog / Golden Retriever"));
        assertEquals(Species.CAT, MyRecordsPanel.speciesFor("Cat / Siamese"));
        assertEquals("Siamese", MyRecordsPanel.breedForApi("Cat / Siamese"));
        assertEquals(null, MyRecordsPanel.speciesFor("Unknown breed"));
        assertEquals("", MyRecordsPanel.breedForApi("Unknown breed"));
    }

    @Test
    void displaysSharedCacheSampleWithClearDisclosure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Species> species = new AtomicReference<>();
        AtomicReference<String> breed = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            MyRecordsPanel panel = new MyRecordsPanel(Color.WHITE, (id, sp, name, w, h, yes, no) -> {
                calls.incrementAndGet();
                assertEquals(7, id);
                assertEquals(76, w);
                assertEquals(80, h);
                species.set(sp);
                breed.set(name);
                yes.accept(sample(false));
            });
            panel.showRecords(List.of(row(7, "Dog / Golden Retriever")), List.of());
            firstTable(panel).setRowSelectionInterval(0, 0);
            assertEquals(1, calls.get());
            assertEquals(Species.DOG, species.get());
            assertEquals("Golden Retriever", breed.get());
            assertNotNull(field(panel, "selectionPhoto", JLabel.class).getIcon());
            assertTrue(field(panel, "selectionPhoto", JLabel.class).getToolTipText().contains("not this specific pet"));
            assertTrue(field(panel, "selectedDescription", JTextArea.class).getText().contains("Sample image"));
        });
    }

    @Test
    void locallyUploadedPhotoIsNotMisrepresentedAsApiSample() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MyRecordsPanel panel = new MyRecordsPanel(Color.WHITE, (id, sp, breed, w, h, yes, no) ->
                    yes.accept(sample(true)));
            panel.showRecords(List.of(row(9, "Cat / Siamese")), List.of());
            firstTable(panel).setRowSelectionInterval(0, 0);
            assertEquals("Photo uploaded by the shelter",
                    field(panel, "selectionPhoto", JLabel.class).getToolTipText());
            assertTrue(!field(panel, "selectedDescription", JTextArea.class).getText().contains("Sample image"));
        });
    }

    @Test
    void offlineImageFailureKeepsPlaceholderAndAvoidsRapidRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            MyRecordsPanel panel = new MyRecordsPanel(Color.WHITE, (id, sp, breed, w, h, yes, no) -> {
                calls.incrementAndGet();
                no.accept(new java.io.IOException("Offline"));
            });
            panel.showRecords(List.of(row(3, "Cat / Persian")), List.of());
            JTable table = firstTable(panel);
            table.setRowSelectionInterval(0, 0);
            table.clearSelection();
            table.setRowSelectionInterval(0, 0);
            assertEquals(1, calls.get());
            assertNotNull(field(panel, "selectionPhoto", JLabel.class).getIcon());
            assertTrue(field(panel, "selectedDescription", JTextArea.class).getText().contains("waitlist"));
        });
    }

    private static MyRecordsPanel.ApplicationRow row(int id, String breed) {
        return new MyRecordsPanel.ApplicationRow(id, "Pet" + id, breed,
                "2026-11-17", 1, "WAITING");
    }

    private static PetImageCache.Loaded sample(boolean actual) {
        BufferedImage img = new BufferedImage(76, 80, BufferedImage.TYPE_INT_RGB);
        return new PetImageCache.Loaded(new ImageIcon(img), actual ? "Shelter photo" : "Dog CEO", actual);
    }

    private static JTable firstTable(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JTable table) return table;
            if (c instanceof Container child) {
                JTable found = findTable(child);
                if (found != null) return found;
            }
        }
        throw new AssertionError("My Waitlist table not found");
    }

    private static JTable findTable(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JTable table) return table;
            if (c instanceof Container child) {
                JTable found = findTable(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object object, String name, Class<T> clazz) {
        try {
            Field f = object.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) clazz.cast(f.get(object));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
