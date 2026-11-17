package pawtrack.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PetPhotoStoreTest {
    @TempDir Path temp;

    private Path validImage() throws IOException {
        Path source = temp.resolve("source.jpg");
        ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "jpg", source.toFile());
        return source;
    }

    @Test void addPersistsPhotoAndCanReadItAfterRecreatingStore() throws Exception {
        PetPhotoStore store = new PetPhotoStore(temp.resolve("photos"));
        Path saved = store.add(17, validImage());
        assertTrue(saved.getFileName().toString().endsWith(".png"));
        assertNotNull(ImageIO.read(saved.toFile()));
        assertEquals(1, new PetPhotoStore(temp.resolve("photos")).list(17).size());
        assertTrue(store.list(18).isEmpty());
    }

    @Test void rejectsFakeImagesAndInvalidAnimalIds() throws Exception {
        PetPhotoStore store = new PetPhotoStore(temp.resolve("photos"));
        Path text = temp.resolve("not-a-photo.jpg");
        Files.writeString(text, "not an image");
        assertThrows(IOException.class, () -> store.add(17, text));
        assertThrows(IllegalArgumentException.class, () -> store.add(-1, validImage()));
    }

    @Test void deletionRejectsOtherAnimalsPhotos() throws Exception {
        PetPhotoStore store = new PetPhotoStore(temp.resolve("photos"));
        Path photo = store.add(17, validImage());
        assertThrows(IOException.class, () -> store.remove(18, photo));
        assertTrue(Files.exists(photo));
        store.remove(17, photo);
        assertTrue(store.list(17).isEmpty());
    }

    @Test void limitsEachAnimalToEightPhotos() throws Exception {
        PetPhotoStore store = new PetPhotoStore(temp.resolve("photos"));
        Path source = validImage();
        for (int i = 0; i < PetPhotoStore.MAX_PHOTOS; i++) store.add(17, source);
        assertThrows(IOException.class, () -> store.add(17, source));
        assertEquals(PetPhotoStore.MAX_PHOTOS, store.list(17).size());
    }
}
