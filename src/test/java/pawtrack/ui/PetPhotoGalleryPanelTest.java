package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;
import pawtrack.service.PetPhotoStore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PetPhotoGalleryPanelTest {
    @TempDir Path temp;

    private Animal animal() {
        return Animal.create(Species.DOG, 17, "Buddy", "Beagle", 18,
                LocalDate.of(2026, 1, 5), AnimalStatus.AVAILABLE);
    }

    @Test void existingLocalPhotoIsVisibleToAdopter() throws Exception {
        PetPhotoStore store = new PetPhotoStore(temp.resolve("photos"));
        Path source = temp.resolve("sample.png");
        ImageIO.write(new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB), "png", source.toFile());
        store.add(17, source);
        PetProfilePanel panel = new PetProfilePanel(animal(), false, store);
        assertEquals(1, panel.getActualPhotoCount());
        assertFalse(panel.canAddActualPhotos());
    }

    @Test void staffCanManagePhotosFromProfile() {
        PetProfilePanel panel = new PetProfilePanel(animal(), true,
                new PetPhotoStore(temp.resolve("photos")));
        assertEquals(0, panel.getActualPhotoCount());
        assertTrue(panel.canAddActualPhotos());
    }
}
