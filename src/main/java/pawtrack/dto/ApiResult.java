package pawtrack.dto;

public sealed interface ApiResult<T> permits ApiResult.Ok, ApiResult.Empty, ApiResult.Failed {
    record Ok<T>(T value) implements ApiResult<T> {}
    record Empty<T>() implements ApiResult<T> {}
    record Failed<T>(String reason) implements ApiResult<T> {}
}
