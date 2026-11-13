package pawtrack.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import pawtrack.dto.DogCeoResponse;
import pawtrack.model.Species;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client for the Dog CEO API.
 */
public final class DogCeoImageService implements PetImageService {

    private static final String BASE = "https://dog.ceo/api";
    private static final Gson GSON = new Gson();
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(DogCeoImageService.class.getName());

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
            DogCeoImageService.LOGGER.fine("Breed catalogue unavailable, using best-effort slug for: " + breed);
        }
        String[] words = breed.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return words[words.length - 1].replaceAll("[^a-z0-9]", "");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Parses {@code {"message":"https://...","status":"success"}}. */
    public static String parseImageUrl(String json) {
        try {
            DogCeoResponse response = GSON.fromJson(json, DogCeoResponse.class);
            if (response == null || !"success".equalsIgnoreCase(response.status())) {
                throw new IllegalArgumentException("Dog CEO API error: " + (response == null ? "" : response.message()));
            }
            String message = response.message();
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Dog CEO API returned an empty image url");
            }
            return message;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Dog CEO API returned invalid JSON", e);
        }
    }

    /**
     * Recursively flattens the nested {@code message} object of
     * {@code /breeds/list/all} into a list of breed entries.
     */
    public static List<BreedEntry> parseBreedList(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                throw new IllegalArgumentException("Dog CEO breed list request failed");
            }
            List<BreedEntry> breeds = new ArrayList<>();
            JSONObject message = obj.optJSONObject("message");
            if (message != null) {
                collectBreeds(message, breeds);
            }
            return breeds;
        } catch (Exception e) {
            throw new IllegalArgumentException("Dog CEO breed list request failed", e);
        }
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
}
