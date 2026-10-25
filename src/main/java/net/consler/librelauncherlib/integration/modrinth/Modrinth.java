package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.annotations.SerializedName;
import net.consler.librelauncherlib.util.DownloadManager;
import net.consler.librelauncherlib.util.DownloadTask;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for Modrinth API v2 search and project operations.
 */
public class Modrinth
{
    /**
     * Runs a search and returns one page including paging info (offset, limit, total hits).
     */
    public static CompletableFuture<SearchResult> search(SearchQuery query, String userAgent)
    {
        if (query == null) throw new IllegalArgumentException("query must not be null");
        return fetchPageAsync(query, userAgent);
    }

    /**
     * Runs a search and keeps paging until maxResults hits were collected or the results run out.
     * Pass Integer.MAX_VALUE to fetch everything that matches. Starts at the query's own offset.
     * Requests are sent one after another, so large values take a while.
     */
    public static CompletableFuture<List<ModrinthProject>> searchAll(SearchQuery query, int maxResults, String userAgent)
    {
        if (query == null) throw new IllegalArgumentException("query must not be null");
        if (maxResults < 1) throw new IllegalArgumentException("maxResults must be >= 1");
        return collectPages(query, maxResults, query.getOffset(), userAgent, new ArrayList<>());
    }

    /**
     * Searches all Modrinth projects across all categories (first page, server default size).
     */
    public static CompletableFuture<List<ModrinthProject>> searchProjects(String query, String userAgent)
    {
        return searchProjects(query, null, userAgent);
    }

    /**
     * Searches Modrinth projects filtered by type (first page, server default size).
     *
     * @param type Project type filter, or null for all types.
     */
    public static CompletableFuture<List<ModrinthProject>> searchProjects(String query, ProjectType type, String userAgent)
    {
        SearchQuery searchQuery = new SearchQuery(query);
        if (type != null) searchQuery.projectType(type);
        return search(searchQuery, userAgent).thenApply(SearchResult::hits);
    }

    /** Searches Modrinth mods. */
    public static CompletableFuture<List<ModrinthProject>> searchMods(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.MOD, userAgent);
    }

    /** Searches Modrinth modpacks. */
    public static CompletableFuture<List<ModrinthProject>> searchModpacks(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.MODPACK, userAgent);
    }

    /** Searches Modrinth resource packs. */
    public static CompletableFuture<List<ModrinthProject>> searchResourcePacks(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.RESOURCE_PACK, userAgent);
    }

    /** Searches Modrinth shaders. */
    public static CompletableFuture<List<ModrinthProject>> searchShaders(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.SHADER, userAgent);
    }

    /** Searches Modrinth plugins. */
    public static CompletableFuture<List<ModrinthProject>> searchPlugins(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.PLUGIN, userAgent);
    }

    /** Searches Modrinth datapacks. */
    public static CompletableFuture<List<ModrinthProject>> searchDatapacks(String query, String userAgent)
    {
        return searchProjects(query, ProjectType.DATAPACK, userAgent);
    }

    /**
     * Fetches detailed project metadata by ID or URL slug.
     * Fails with a NOT_FOUND {@link ModrinthException} if the project doesn't exist.
     */
    public static CompletableFuture<ModrinthProject> getProject(String slugOrId, String userAgent)
    {
        requireSlugOrId(slugOrId);
        return ModrinthApiClient.get("/project/" + enc(slugOrId), userAgent, ModrinthProject.class);
    }

    /**
     * Fetches all projects and versions that the given project depends upon.
     * Fails with a NOT_FOUND {@link ModrinthException} if the project doesn't exist.
     */
    public static CompletableFuture<Dependencies> getDependencies(String slugOrId, String userAgent)
    {
        requireSlugOrId(slugOrId);
        return ModrinthApiClient.get("/project/" + enc(slugOrId) + "/dependencies", userAgent, Dependencies.class);
    }

    /** Fetches all project versions without filters. */
    public static CompletableFuture<List<ModrinthVersion>> getProjectVersions(String slugOrId, String userAgent)
    {
        return getProjectVersions(slugOrId, new VersionQuery(), userAgent);
    }

    /**
     * Fetches project versions filtered by loaders, game versions, or featured status.
     */
    public static CompletableFuture<List<ModrinthVersion>> getProjectVersions(
            String slugOrId, VersionQuery query, String userAgent)
    {
        requireSlugOrId(slugOrId);
        if (query == null)
            throw new IllegalArgumentException("query must not be null");

        String queryString = query.toQueryString();
        String path = "/project/" + enc(slugOrId) + "/version" + (queryString.isEmpty() ? "" : "?" + queryString);

        return ModrinthApiClient.get(path, userAgent, ModrinthVersion[].class).thenApply(List::of);
    }

    /**
     * Fetches a version by its Modrinth version ID.
     */
    public static CompletableFuture<ModrinthVersion> getVersion(String versionId, String userAgent)
    {
        requireSlugOrId(versionId);
        return ModrinthApiClient.get("/version/" + enc(versionId), userAgent, ModrinthVersion.class);
    }

    /**
     * Downloads a version file without setting a User-Agent header.
     *
     * @param file Version file metadata.
     * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
     * @return A future containing the normalized destination path.
     */
    public static CompletableFuture<Path> downloadVersionFile(ModrinthVersion.VersionFile file, Path destination)
    {
        return downloadVersionFile(file, destination, null);
    }

    /**
     * Downloads a version file and sends the given User-Agent with the request.
     *
     * @param file Version file metadata.
     * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
     * @param userAgent Descriptive HTTP User-Agent value.
     * @return A future containing the normalized destination path.
     */
    public static CompletableFuture<Path> downloadVersionFile(
            ModrinthVersion.VersionFile file, Path destination, String userAgent)
    {
        if (file == null)
            throw new IllegalArgumentException("file must not be null");
        if (destination == null)
            throw new IllegalArgumentException("destination must not be null");
        if (userAgent != null)
            requireUserAgent(userAgent);
        if (file.url() == null || file.url().isBlank())
            throw new IllegalArgumentException("file URL must not be empty");

        URI fileUri = URI.create(file.url());
        if (!"https".equalsIgnoreCase(fileUri.getScheme()) || fileUri.getHost() == null || fileUri.getUserInfo() != null)
            throw new IllegalArgumentException("Modrinth file URL must be a valid HTTPS URL");

        Path target = resolveDownloadDestination(file, destination);
        if (target.getFileName() == null) throw new IllegalArgumentException("destination must be a file path");

        String sha1 = file.hashes().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("sha1"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        return CompletableFuture.supplyAsync(() ->
        {
            DownloadTask task = new DownloadTask(file.url(), target, sha1, file.size());
            DownloadManager downloadManager = new DownloadManager();
            if (userAgent == null) downloadManager.downloadFile(task);
            else downloadManager.downloadFile(task, Map.of("User-Agent", userAgent));
            return target;
        });
    }

    static Path resolveDownloadDestination(ModrinthVersion.VersionFile file, Path destination)
    {
        Path target = destination.toAbsolutePath().normalize();
        if (!Files.isDirectory(target)) return target;

        String filename = file.filename();
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")
                || filename.equals(".") || filename.equals(".."))
            throw new IllegalArgumentException("Modrinth file has an invalid filename");

        return target.resolve(filename);
    }

    /**
     * One page of search results.
     */
    public record SearchResult(
            List<ModrinthProject> hits,
            int offset,
            int limit,
            @SerializedName("total_hits") int totalHits
    )
    {
        /** Returns an empty hit list when the API response omits hits. */
        public SearchResult
        {
            hits = hits == null ? List.of() : hits;
        }

        /** @return The offset to use to request the next page. */
        public int nextOffset()
        {
            return offset + hits.size();
        }

        /** @return True if there are more results after this page. */
        public boolean hasMore()
        {
            return nextOffset() < totalHits;
        }
    }

    /**
     * Response of GET /project/{id|slug}/dependencies.
     *
     * @param projects Projects the project depends upon.
     * @param versions Versions the project depends upon.
     */
    public record Dependencies(List<ModrinthProject> projects, List<ModrinthVersion> versions)
    {
        /** Replaces missing project or version lists with empty lists. */
        public Dependencies
        {
            projects = projects == null ? List.of() : projects;
            versions = versions == null ? List.of() : versions;
        }
    }

    private static CompletableFuture<SearchResult> fetchPageAsync(SearchQuery query, String userAgent)
    {
        return ModrinthApiClient.get("/search?" + query.toQueryString(), userAgent, SearchResult.class);
    }

    private static CompletableFuture<List<ModrinthProject>> collectPages(
            SearchQuery query, int maxResults, int offset, String userAgent, List<ModrinthProject> collected)
    {
        if (collected.size() >= maxResults)
            return CompletableFuture.completedFuture(List.copyOf(collected));

        int pageSize = Math.min(SearchQuery.MAX_LIMIT, maxResults - collected.size());
        return fetchPageAsync(query.copy().offset(offset).limit(pageSize), userAgent).thenCompose(page ->
        {
            collected.addAll(page.hits());
            if (page.hits().isEmpty() || !page.hasMore() || collected.size() >= maxResults)
                return CompletableFuture.completedFuture(List.copyOf(collected));
            return collectPages(query, maxResults, page.nextOffset(), userAgent, collected);
        });
    }

    private static void requireSlugOrId(String slugOrId)
    {
        if (slugOrId == null || slugOrId.isBlank())
            throw new IllegalArgumentException("slugOrId must not be empty");
    }

    private static void requireUserAgent(String userAgent)
    {
        if (userAgent == null || userAgent.isBlank())
            throw new IllegalArgumentException("Modrinth requires a User-Agent");
    }

    private static String enc(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}