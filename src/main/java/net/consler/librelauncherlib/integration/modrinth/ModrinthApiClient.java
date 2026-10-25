package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.ModrinthException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP transport and response handling for the Modrinth API.
 */
final class ModrinthApiClient
{
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Gson GSON = new Gson();

    private ModrinthApiClient() {}

    static <T> CompletableFuture<T> get(String path, String userAgent, Class<T> responseType)
    {
        requireUserAgent(userAgent);
        return CompletableFuture.supplyAsync(() -> sendGet(path, userAgent, responseType));
    }

    private static <T> T sendGet(String path, String userAgent, Class<T> responseType)
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent)
                .GET()
                .build();

        HttpResponse<String> response;
        try
        {
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException e)
        {
            throw ModrinthException.network("Could not reach Modrinth: " + e.getMessage(), e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw ModrinthException.network("Request to Modrinth was interrupted", e);
        }

        String body = response.body();
        if (response.statusCode() != 200) throw toApiException(response);

        try
        {
            T result = GSON.fromJson(body, responseType);
            if (result == null)
                throw ModrinthException.parse("Modrinth returned an empty response for " + path, body, null);
            return result;
        }
        catch (JsonParseException e)
        {
            throw ModrinthException.parse("Could not parse Modrinth's response for " + path, body, e);
        }
    }

    private static ModrinthException toApiException(HttpResponse<String> response)
    {
        String body = response.body();
        String error = null;
        String description = null;

        try
        {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error") && json.get("error").isJsonPrimitive()) error = json.get("error").getAsString();
            if (json.has("description") && json.get("description").isJsonPrimitive())
                description = json.get("description").getAsString();
        }
        catch (RuntimeException ignored)
        {
        }

        return ModrinthException.api(response.statusCode(), error, description, body, retryAfterSeconds(response));
    }

    private static long retryAfterSeconds(HttpResponse<String> response)
    {
        if (response.statusCode() != 429) return -1;

        for (String header : new String[]{"X-Ratelimit-Reset", "Retry-After"})
        {
            var value = response.headers().firstValue(header);
            if (value.isEmpty()) continue;

            try
            {
                return Long.parseLong(value.get().trim());
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return -1;
    }

    private static void requireUserAgent(String userAgent)
    {
        if (userAgent == null || userAgent.isBlank())
            throw new IllegalArgumentException("Modrinth requires a User-Agent");
    }
}
