package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wrapper for Modrinth API v2 search and project operations.
 */
public class Modrinth
{
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final String USER_AGENT = "consler/LibreLauncherLib";

    /**
     * Searches all Modrinth projects across all categories.
     *
     * @param query Search term.
     * @return Matching projects.
     */
    public static List<ModrinthProject> searchProjects(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, null);
    }

    /**
     * Searches Modrinth projects filtered by type.
     *
     * @param query Search term.
     * @param type  Project type filter, or null for all types.
     * @return Matching projects.
     */
    public static List<ModrinthProject> searchProjects(String query, ProjectType type) throws IOException, InterruptedException
    {
        String url = API_BASE + "/search?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        if (type != null)
        {
            String facet = URLEncoder.encode("[[\"project_type:" + type.getId() + "\"]]", StandardCharsets.UTF_8);
            url += "&facets=" + facet;
        }

        String json = fetch(url);
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonArray hits = jsonObject.getAsJsonArray("hits");

        Type listType = new TypeToken<List<ModrinthProject>>(){}.getType();
        return GSON.fromJson(hits, listType);
    }

    /**
     * Searches specifically for mods.
     */
    public static List<ModrinthProject> searchMods(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.MOD);
    }

    /**
     * Searches specifically for modpacks.
     */
    public static List<ModrinthProject> searchModpacks(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.MODPACK);
    }

    /**
     * Searches specifically for resource packs.
     */
    public static List<ModrinthProject> searchResourcePacks(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.RESOURCE_PACK);
    }

    /**
     * Searches specifically for shaders.
     */
    public static List<ModrinthProject> searchShaders(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.SHADER);
    }

    /**
     * Searches specifically for plugins.
     */
    public static List<ModrinthProject> searchPlugins(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.PLUGIN);
    }

    /**
     * Searches specifically for datapacks.
     */
    public static List<ModrinthProject> searchDatapacks(String query) throws IOException, InterruptedException
    {
        return searchProjects(query, ProjectType.DATAPACK);
    }

    /**
     * Fetches detailed project metadata by ID or URL slug.
     *
     * @param slugOrId Project ID or URL slug.
     * @return Project metadata.
     */
    public static ModrinthProject getProject(String slugOrId) throws IOException, InterruptedException
    {
        String json = fetch(API_BASE + "/project/" + slugOrId);
        return GSON.fromJson(json, ModrinthProject.class);
    }

    private static String fetch(String url) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).GET().build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) throw new RuntimeException("Modrinth API error " + response.statusCode() + ": " + response.body());

        return response.body();
    }
}