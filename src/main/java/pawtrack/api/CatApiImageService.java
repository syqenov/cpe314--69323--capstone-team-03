package pawtrack.api;

import pawtrack.model.Species;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for <a href="https://thecatapi.com">TheCatAPI</a>.
 *
 * <p>A free API key is available after registration and is read from the
 * {@code CAT_API_KEY} environment variable (or the {@code cat.api.key} system
 * property). The public image-search endpoint also works without a key at a low
 * rate limit, so the demo runs out of the box.</p>
 */
public final class CatApiImageService implements PetImageService {

    private static final String BASE = "https://api.thecatapi.com/v1";

    private final String apiKey;

    public CatApiImageService() {
        this(resolveKey());
    }

    public CatApiImageService(String apiKey) {
        this.apiKey = apiKey;
    }

    private static String resolveKey() {
        String key = System.getProperty("cat.api.key");
        if (key == null || key.isBlank()) {
            key = System.getenv("CAT_API_KEY");
        }
        return key;
    }

    @Override
    public String providerName() {
        return "TheCatAPI";
    }

    @Override
    public boolean supports(Species species) {
        return species == Species.CAT;
    }

    @Override
    public PetImageResult fetchRandomImage(String breed) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.put("x-api-key", apiKey);
        }
        String json = HttpJson.get(BASE + "/images/search?limit=1&has_breeds=1", headers);
        return new PetImageResult(parseImageUrl(json), Species.CAT, breed == null ? "" : breed, providerName());
    }

    /** Parses {@code [{"id":"...","url":"https://...","breeds":[...]}]}. */
    public static String parseImageUrl(String json) {
        JSONArray array = new JSONArray(json);
        if (array.isEmpty()) {
            throw new IllegalArgumentException("TheCatAPI returned no image");
        }
        String url = array.getJSONObject(0).optString("url", "");
        if (url.isBlank()) {
            throw new IllegalArgumentException("TheCatAPI image url is missing");
        }
        return url;
    }

    /** Parses {@code [{"id":"abys","name":"Abyssinian", ...}]} into id -&gt; name. */
    public static Map<String, String> parseBreedList(String json) {
        Map<String, String> breeds = new LinkedHashMap<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject breed = array.getJSONObject(i);
            String id = breed.optString("id", "");
            String name = breed.optString("name", id);
            if (!id.isBlank()) {
                breeds.put(id, name);
            }
        }
        return breeds;
    }
}