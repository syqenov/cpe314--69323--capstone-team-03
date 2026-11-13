package pawtrack.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import pawtrack.dto.CatImageDto;
import pawtrack.model.Species;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for <a href="https://thecatapi.com">TheCatAPI</a>.
 */
public final class CatApiImageService implements PetImageService {

    private static final String BASE = "https://api.thecatapi.com/v1";
    private static final Gson GSON = new Gson();

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

        String url = BASE + "/images/search?limit=1&has_breeds=1";
        if (breed != null && !breed.isBlank()) {
            try {
                String breedsJson = HttpJson.get(BASE + "/breeds", headers);
                Map<String, String> breedMap = parseBreedList(breedsJson);
                String breedId = null;
                for (Map.Entry<String, String> entry : breedMap.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(breed.trim()) || entry.getKey().equalsIgnoreCase(breed.trim())) {
                        breedId = entry.getKey();
                        break;
                    }
                }
                if (breedId != null) {
                    url = BASE + "/images/search?limit=1&breed_ids=" + URLEncoder.encode(breedId, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
                // Fallback to general search if breed lookup fails
            }
        }

        String json = HttpJson.get(url, headers);
        return new PetImageResult(parseImageUrl(json), Species.CAT, breed == null ? "" : breed, providerName());
    }

    /** Parses {@code [{"id":"...","url":"https://...","breeds":[...]}]}. */
    public static String parseImageUrl(String json) {
        try {
            CatImageDto[] arr = GSON.fromJson(json, CatImageDto[].class);
            if (arr == null || arr.length == 0) {
                throw new IllegalArgumentException("TheCatAPI returned no image");
            }
            String url = arr[0].url();
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("TheCatAPI image url is missing");
            }
            return url;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("TheCatAPI returned invalid JSON", e);
        }
    }

    /** Parses {@code [{"id":"abys","name":"Abyssinian", ...}]} into id -&gt; name. */
    public static Map<String, String> parseBreedList(String json) {
        Map<String, String> breeds = new LinkedHashMap<>();
        try {
            com.google.gson.JsonArray array = GSON.fromJson(json, com.google.gson.JsonArray.class);
            if (array == null) {
                return breeds;
            }
            for (com.google.gson.JsonElement el : array) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                String name = obj.has("name") ? obj.get("name").getAsString() : id;
                if (!id.isBlank()) {
                    breeds.put(id, name);
                }
            }
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("TheCatAPI returned invalid JSON", e);
        }
        return breeds;
    }
}
