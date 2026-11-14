package pawtrack.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Minimal HTTP helper around {@link HttpURLConnection} used by the pet-image API
 * clients. Kept separate so each API client focuses only on parsing JSON
 * (Single Responsibility Principle).
 */
public final class HttpJson {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    private HttpJson() {
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = open(url, headers);
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readText(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " from " + url + (body.isBlank() ? "" : ": " + body));
            }
            return body;
        } finally {
            connection.disconnect();
        }
    }

    public static byte[] getBytes(String url) throws IOException {
        HttpURLConnection connection = open(url, Map.of());
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("HTTP " + status + " downloading image from " + url);
        }
        try (InputStream stream = connection.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "PetShelterTracker/1.0 (student project)");
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        return connection;
    }

    private static String readText(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
}
