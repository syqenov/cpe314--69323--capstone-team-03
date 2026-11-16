package pawtrack.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Local, validated photo storage keyed by persisted animal ID. Never stores user-supplied paths. */
public final class PetPhotoStore {
    public static final int MAX_PHOTOS = 8;
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path root;

    public PetPhotoStore(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public static PetPhotoStore defaultStore() { return new PetPhotoStore(Path.of("pet-photos")); }

    private Path directory(int animalId) {
        if (animalId <= 0) throw new IllegalArgumentException("A saved animal ID is required");
        return root.resolve(Integer.toString(animalId));
    }

    public List<Path> list(int animalId) throws IOException {
        Path dir = directory(animalId);
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    public Path add(int animalId, Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) throw new IOException("Select an existing image file");
        if (Files.size(source) <= 0 || Files.size(source) > MAX_BYTES)
            throw new IOException("Image must be 5 MB or smaller");
        // Decode fully: extension and reported MIME type alone are not trustworthy.
        BufferedImage decoded;
        try (InputStream in = Files.newInputStream(source)) {
            decoded = ImageIO.read(in);
        }
        if (decoded == null) throw new IOException("Not a supported image");
        if (decoded.getWidth() <= 0 || decoded.getHeight() <= 0 ||
                (long) decoded.getWidth() * decoded.getHeight() > 20_000_000L)
            throw new IOException("Image dimensions are not supported");
        if (list(animalId).size() >= MAX_PHOTOS)
            throw new IOException("This pet already has eight photos");
        Path folder = directory(animalId);
        Files.createDirectories(folder);
        Path destination = folder.resolve(UUID.randomUUID() + ".png");
        // Convert to PNG so stored files always contain decoded image pixels, not untrusted originals.
        if (!ImageIO.write(decoded, "png", destination.toFile()))
            throw new IOException("Unable to save image");
        return destination;
    }

    public void remove(int animalId, Path photo) throws IOException {
        Path dir = directory(animalId).toRealPath();
        Path target = photo.toRealPath();
        if (!target.getParent().equals(dir) || !target.getFileName().toString().endsWith(".png"))
            throw new IOException("Photo does not belong to this animal");
        Files.delete(target);
    }
}
