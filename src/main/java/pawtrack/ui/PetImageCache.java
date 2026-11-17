package pawtrack.ui;

import pawtrack.api.HttpJson;
import pawtrack.api.PetImageResult;
import pawtrack.api.PetImageServiceFactory;
import pawtrack.model.Animal;
import pawtrack.model.Species;
import pawtrack.service.PetPhotoStore;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The single place that supplies a picture for an animal (cards, profile box,
 * PetProfilePanel).
 *
 * Order of preference: (1) an actual photo staff uploaded to PetPhotoStore,
 * (2) a sample image from the Dog CEO / TheCatAPI service, (3) failure, and the
 * caller keeps its text placeholder.
 *
 * - Network and disk work run on 3 daemon threads, never on the EDT.
 * - The image chosen for an animal is remembered, so the card and the profile
 *   box always show the SAME picture (the APIs return a random one per call).
 * - Concurrent requests for the same animal share one download.
 * - A failed animal is not retried for 60 s (offline mode does not spam requests).
 * - Callbacks run on the EDT. Call load() from the EDT.
 */
public final class PetImageCache {

    public record Loaded(ImageIcon icon, String provider, boolean actualPhoto) {
    }

    private record Source(BufferedImage image, String provider, boolean actual) {
    }

    private record Callback(int width, int height, Consumer<Loaded> onSuccess, Consumer<Exception> onFailure) {
    }

    private static final int MAX_SOURCES = 60;
    private static final int SOURCE_MAX_WIDTH = 480;
    private static final long RETRY_AFTER_MS = 60_000;
    private static final PetImageCache INSTANCE = new PetImageCache();

    public static PetImageCache get() {
        return INSTANCE;
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(3, runnable -> {
        Thread t = new Thread(runnable, "pet-image-loader");
        t.setDaemon(true);
        return t;
    });

    private final Map<Integer, Source> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Source> eldest) {
            return size() > MAX_SOURCES;
        }
    };
    private final Map<Integer, List<Callback>> waiting = new HashMap<>();
    private final Map<Integer, Long> failedAt = new HashMap<>();

    private PetImageCache() {
    }

    /** The public image APIs cover dogs and cats only. */
    public static boolean supports(Species species) {
        return species == Species.DOG || species == Species.CAT;
    }

    public void load(Animal animal, int width, int height,
                     Consumer<Loaded> onSuccess, Consumer<Exception> onFailure) {
        load(animal.getId(), animal.species(), animal.getBreed(), width, height, onSuccess, onFailure);
    }

    /** species may be null (then only an uploaded photo can be shown). */
    public void load(int id, Species species, String breed, int width, int height,
                     Consumer<Loaded> onSuccess, Consumer<Exception> onFailure) {
        Callback callback = new Callback(width, height, onSuccess, onFailure);
        Source hit;
        boolean recentlyFailed = false;
        boolean startDownload = false;
        synchronized (this) {
            hit = cache.get(id);
            if (hit == null) {
                Long when = failedAt.get(id);
                if (when != null && System.currentTimeMillis() - when < RETRY_AFTER_MS) {
                    recentlyFailed = true;
                } else {
                    List<Callback> list = waiting.get(id);
                    if (list == null) {
                        list = new ArrayList<>();
                        waiting.put(id, list);
                        startDownload = true;
                    }
                    list.add(callback);
                }
            }
        }
        if (hit != null) {
            deliver(hit, callback);
            return;
        }
        if (recentlyFailed) {
            onFailure.accept(new IOException("Image unavailable, will retry shortly"));
            return;
        }
        if (!startDownload) return; // another request for this animal is already running

        pool.execute(() -> {
            Source found = null;
            Exception error = null;
            try {
                found = fetch(id, species, breed);
            } catch (Exception failure) {
                error = failure;
            }
            final Source source = found;
            final Exception problem = error;
            SwingUtilities.invokeLater(() -> finish(id, source, problem));
        });
    }

    /** Call after staff add or remove an actual photo so the next load picks it up. */
    public synchronized void invalidate(int id) {
        cache.remove(id);
        failedAt.remove(id);
    }

    private void finish(int id, Source source, Exception error) {
        List<Callback> callbacks;
        synchronized (this) {
            if (source != null) {
                cache.put(id, source);
                failedAt.remove(id);
            } else {
                failedAt.put(id, System.currentTimeMillis());
            }
            callbacks = waiting.remove(id);
        }
        if (callbacks == null) return;
        for (Callback callback : callbacks) {
            if (source != null) deliver(source, callback);
            else callback.onFailure().accept(error);
        }
    }

    private static void deliver(Source source, Callback callback) {
        Loaded loaded;
        try {
            loaded = new Loaded(new ImageIcon(scaleCover(source.image(), callback.width(), callback.height())),
                    source.provider(), source.actual());
        } catch (RuntimeException failure) {
            callback.onFailure().accept(failure);
            return;
        }
        callback.onSuccess().accept(loaded);
    }

    /** Runs on a pool thread. */
    private static Source fetch(int id, Species species, String breed) throws IOException {
        try {
            List<Path> local = PetPhotoStore.defaultStore().list(id);
            if (!local.isEmpty()) {
                BufferedImage image = ImageIO.read(local.get(0).toFile());
                if (image != null) return new Source(limitWidth(image), "Shelter photo", true);
            }
        } catch (IOException ignored) {
            // fall through to the sample image
        }
        if (!supports(species)) throw new IOException("No sample image for this species");
        PetImageResult info = PetImageServiceFactory.forSpecies(species).fetchRandomImage(breed);
        byte[] bytes = HttpJson.getBytes(info.imageUrl());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IOException("Unsupported image format");
        return new Source(limitWidth(image), info.provider(), false);
    }

    private static BufferedImage limitWidth(BufferedImage src) {
        if (src.getWidth() <= SOURCE_MAX_WIDTH) return src;
        int h = (int) Math.round(src.getHeight() * (SOURCE_MAX_WIDTH / (double) src.getWidth()));
        return draw(src, SOURCE_MAX_WIDTH, h, 0, 0, SOURCE_MAX_WIDTH, h);
    }

    /** Fills width x height completely, cropping the overflow (like CSS object-fit: cover). */
    public static BufferedImage scaleCover(BufferedImage src, int w, int h) {
        double scale = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int sw = (int) Math.ceil(src.getWidth() * scale);
        int sh = (int) Math.ceil(src.getHeight() * scale);
        return draw(src, w, h, (w - sw) / 2, (h - sh) / 2, sw, sh);
    }

    private static BufferedImage draw(BufferedImage src, int outW, int outH, int x, int y, int w, int h) {
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, x, y, w, h, null);
        g.dispose();
        return out;
    }
}
