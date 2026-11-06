package pawtrack.api;

import pawtrack.model.Species;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client for the <a href="https://dog.ceo/dog-api/documentation">Dog CEO API</a>
 * (no API key required).
 *
 * <p>It pulls both sample images and the breed catalogue, which is a JSON tree
 * that is walked recursively by {@link #collectBreeds(JSONObject, List)}.</p>
 */
public final class DogCeoImageService implements PetImageService {

    private static final String BASE = "https://dog.ceo/api";

    /** Flattened breed entry from {@code /breeds/list/all}. */
    public record BreedEntry(String slug, String displayName) {
    }

    @Override
    public String providerName() {
        return "Dog CEO API";
    }

    @Override
    public boolean supports(Species species) {
        return species == Species.DOG;
    }

    @Override
    public PetImageResult fetchRandomImage(String breed) throws IOException {
        String path = resolveBreedPath(breed);
        String url = path == null ? BASE + "/breeds/image/random" : BASE + "/breed/" + path + "/images/random";
        String json = HttpJson.get(url, Map.of());
        return new PetImageResult(parseImageUrl(json), Species.DOG, breed == null ? "" : breed, providerName());
    }

    /** Downloads and flattens the full breed catalogue. */
    public List<BreedEntry> fetchBreedCatalogue() throws IOException {
        String json = HttpJson.get(BASE + "/breeds/list/all", Map.of());
        return parseBreedList(json);
    }

    /**
     * Maps a free-text breed onto a Dog CEO path such as {@code retriever/golden}
     * by matching against the live breed catalogue from the API.
     */
    String resolveBreedPath(String breed) throws IOException {
        if (breed == null || breed.isBlank()) {
            return null;
        }
        String target = normalize(breed);
        try {
            for (BreedEntry entry : fetchBreedCatalogue()) {
                if (normalize(entry.slug()).equals(target) || normalize(entry.displayName()).equals(target)) {
                    return entry.slug();
                }
            }
        } catch (IOException ignored) {
            // offline / rate limited: fall back to a best-effort slug below
        }
        String[] words = breed.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return words[words.length - 1].replaceAll("[^a-z0-9]", "");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Parses {@code {"message":"https://...","status":"success"}}. */
    public static String parseImageUrl(String json) {
        JSONObject root = new JSONObject(json);
        if (!"success".equalsIgnoreCase(root.optString("status"))) {
            throw new IllegalArgumentException("Dog CEO API error: " + root.optString("message"));
        }
        String message = root.optString("message", "");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Dog CEO API returned an empty image url");
        }
        return message;
    }

    /**
     * Recursively flattens the nested {@code message} object of
     * {@code /breeds/list/all} into a list of breed entries.
     */
    public static List<BreedEntry> parseBreedList(String json) {
        JSONObject root = new JSONObject(json);
        if (!"success".equalsIgnoreCase(root.optString("status"))) {
            throw new IllegalArgumentException("Dog CEO breed list request failed");
        }
        JSONObject message = root.optJSONObject("message");
        List<BreedEntry> breeds = new ArrayList<>();
        if (message != null) {
            collectBreeds(message, breeds);
        }
        return breeds;
    }

    private static void collectBreeds(JSONObject node, List<BreedEntry> out) {
        for (String breed : node.keySet()) {
            JSONArray subBreeds = node.optJSONArray(breed);
            if (subBreeds == null || subBreeds.isEmpty()) {
                out.add(new BreedEntry(breed, titleCase(breed)));
            } else {
                for (int i = 0; i < subBreeds.length(); i++) {
                    String sub = subBreeds.getString(i);
                    out.add(new BreedEntry(breed + "/" + sub, titleCase(sub) + " " + titleCase(breed)));
                }
            }
        }
    }

    private static String titleCase(String value) {
        String[] words = value.split("[\\s-]+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    /** Utility used by the GUI to show which breeds the provider knows. */
    public Map<String, String> breedDisplayNames() throws IOException {
        Map<String, String> names = new LinkedHashMap<>();
        for (BreedEntry entry : fetchBreedCatalogue()) {
            names.put(entry.slug(), entry.displayName());
        }
        return names;
    }
}