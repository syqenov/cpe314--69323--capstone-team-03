package pawtrack.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoParsingTest {

    private static final Gson GSON = new Gson();

    @Test
    void parseDogCeoResponse() {
        String json = "{\"message\":\"https://images.dog.ceo/breeds/hound-afghan/n02088094_1003.jpg\",\"status\":\"success\"}";
        DogCeoResponse dto = GSON.fromJson(json, DogCeoResponse.class);
        assertEquals("success", dto.status());
        assertTrue(dto.message().contains("dog.ceo"));
    }

    @Test
    void parseDogCeoErrorPayload() {
        String json = "{\"message\":\"Breed not found\",\"status\":\"error\"}";
        DogCeoResponse dto = GSON.fromJson(json, DogCeoResponse.class);
        assertEquals("error", dto.status());
    }

    @Test
    void parseCatImageDtoArray() {
        String json = "[{\"id\":\"abc\",\"url\":\"https://cdn2.thecatapi.com/images/abc.jpg\",\"width\":500,\"height\":400,\"breeds\":[]}]";
        CatImageDto[] arr = GSON.fromJson(json, CatImageDto[].class);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals("abc", arr[0].id());
        assertEquals("https://cdn2.thecatapi.com/images/abc.jpg", arr[0].url());
    }

    @Test
    void parseEmptyCatResponse() {
        String json = "[]";
        CatImageDto[] arr = GSON.fromJson(json, CatImageDto[].class);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }
}
