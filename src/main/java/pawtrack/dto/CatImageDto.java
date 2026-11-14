package pawtrack.dto;

import java.util.List;

public record CatImageDto(String id, String url, Integer width, Integer height, List<Breed> breeds) {
    public record Breed(String id, String name) {}
}
