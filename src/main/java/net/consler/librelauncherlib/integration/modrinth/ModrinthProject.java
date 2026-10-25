package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.annotations.SerializedName;
import net.consler.librelauncherlib.instance.Datapack;
import net.consler.librelauncherlib.instance.Mod;
import net.consler.librelauncherlib.instance.Modpack;
import net.consler.librelauncherlib.instance.Plugin;
import net.consler.librelauncherlib.instance.ResourcePack;
import net.consler.librelauncherlib.instance.Shader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Project metadata returned by the Modrinth API.
 * Call {@link #fetchDetails(String)} to retrieve complete project metadata.
 */
public record ModrinthProject(
        @SerializedName(value = "id", alternate = {"project_id"})
        String id,
        String slug,
        String title,
        String description,
        String body,
        @SerializedName("project_type")
        String projectType,
        List<String> environment,
        @SerializedName("icon_url")
        String iconUrl,
        String author,
        String status,
        long downloads,
        @SerializedName(value = "followers", alternate = {"follows"})
        long followers,
        List<String> categories,
        @SerializedName("additional_categories")
        List<String> additionalCategories,
        @SerializedName("game_versions")
        List<String> gameVersions,
        List<String> loaders,
        List<String> versions,
        @SerializedName("latest_version")
        String latestVersion,
        @SerializedName(value = "published", alternate = {"date_created"})
        String published,
        @SerializedName(value = "updated", alternate = {"date_modified"})
        String updated,
        @SerializedName("source_url")
        String sourceUrl,
        @SerializedName("issues_url")
        String issuesUrl,
        @SerializedName("wiki_url")
        String wikiUrl,
        @SerializedName("discord_url")
        String discordUrl
)
{
        public ModrinthProject
        {
                environment = environment == null ? List.of() : environment;
                categories = categories == null ? List.of() : categories;
                additionalCategories = additionalCategories == null ? List.of() : additionalCategories;
                gameVersions = gameVersions == null ? List.of() : gameVersions;
                loaders = loaders == null ? List.of() : loaders;
                versions = versions == null ? List.of() : versions;
        }

        /**
         * @return The project type as an enum, or null if unknown.
         */
        public ProjectType type()
        {
                return ProjectType.fromId(projectType);
        }

        /**
         * @return The project's page on modrinth.com.
         */
        public String pageUrl()
        {
                return "https://modrinth.com/" + (projectType != null ? projectType : "project") + "/" + (slug != null ? slug : id);
        }

        /**
         * Fetches complete project metadata by this project's ID.
         * This record is immutable, so use the returned project instead of this search result.
         *
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the full project response; call {@code join()} or compose
         *         the future to access it.
         */
        public CompletableFuture<ModrinthProject> fetchDetails(String userAgent)
        {
                if (id == null || id.isBlank())
                        throw new IllegalStateException("Project has no ID");
                return Modrinth.getProject(id, userAgent);
        }

        /**
         * Converts a downloaded mod file to a {@link Mod}.
         *
         * @param downloadedFile Path to the downloaded file.
         * @return Parsed mod metadata.
         */
        public Mod toMod(Path downloadedFile)
        {
                requireType(ProjectType.MOD);
                requireDownloadedFile(downloadedFile);
                return new Mod(downloadedFile.toFile());
        }

        /**
         * Converts a downloaded datapack file to a {@link Datapack}.
         *
         * @param downloadedFile Path to the downloaded file.
         * @return Parsed datapack metadata.
         */
        public Datapack toDatapack(Path downloadedFile)
        {
                requireType(ProjectType.DATAPACK);
                requireDownloadedFile(downloadedFile);
                return new Datapack(downloadedFile);
        }

        /**
         * Converts a downloaded Modrinth .mrpack file to a {@link Modpack}.
         *
         * @param downloadedFile Path to the downloaded archive.
         * @return Parsed modpack index metadata.
         */
        public Modpack toModpack(Path downloadedFile)
        {
                requireType(ProjectType.MODPACK);
                requireDownloadedFile(downloadedFile);
                return new Modpack(downloadedFile);
        }

        /**
         * Converts a downloaded resource pack file to a {@link ResourcePack}.
         *
         * @param downloadedFile Path to the downloaded file.
         * @return Parsed resource pack metadata.
         */
        public ResourcePack toResourcePack(Path downloadedFile)
        {
                requireType(ProjectType.RESOURCE_PACK);
                requireDownloadedFile(downloadedFile);
                return new ResourcePack(downloadedFile);
        }

        /**
         * Converts a downloaded shader file to a {@link Shader}.
         *
         * @param downloadedFile Path to the downloaded file.
         * @return Shader file wrapper.
         */
        public Shader toShader(Path downloadedFile)
        {
                requireType(ProjectType.SHADER);
                requireDownloadedFile(downloadedFile);
                return new Shader(downloadedFile.toFile());
        }

        /**
         * Converts a downloaded plugin file to a {@link Plugin}.
         *
         * @param downloadedFile Path to the downloaded file.
         * @return Plugin file wrapper.
         */
        public Plugin toPlugin(Path downloadedFile)
        {
                requireType(ProjectType.PLUGIN);
                requireDownloadedFile(downloadedFile);
                return new Plugin(downloadedFile.toFile());
        }

        /**
         * Downloads the version's primary file and converts it to a {@link Mod}.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the parsed mod.
         */
        public CompletableFuture<Mod> downloadAsMod(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.MOD, this::toMod);
        }

        /**
         * Downloads the version's primary file and converts it to a {@link Datapack}.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the parsed datapack.
         */
        public CompletableFuture<Datapack> downloadAsDatapack(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.DATAPACK, this::toDatapack);
        }

        /**
         * Downloads the version's primary file and reads its Modrinth modpack index.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the parsed modpack.
         */
        public CompletableFuture<Modpack> downloadAsModpack(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.MODPACK, this::toModpack);
        }

        /**
         * Downloads the version's primary file and converts it to a {@link ResourcePack}.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the parsed resource pack.
         */
        public CompletableFuture<ResourcePack> downloadAsResourcePack(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.RESOURCE_PACK, this::toResourcePack);
        }

        /**
         * Downloads the version's primary file and converts it to a {@link Shader}.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the shader wrapper.
         */
        public CompletableFuture<Shader> downloadAsShader(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.SHADER, this::toShader);
        }

        /**
         * Downloads the version's primary file and converts it to a {@link Plugin}.
         *
         * @param version Project version to download.
         * @param destination Destination file path, or an existing directory to save the file into using its Modrinth filename.
         * @param userAgent Descriptive HTTP User-Agent value.
         * @return A future containing the plugin wrapper.
         */
        public CompletableFuture<Plugin> downloadAsPlugin(
                ModrinthVersion version, Path destination, String userAgent)
        {
                return downloadAs(version, destination, userAgent, ProjectType.PLUGIN, this::toPlugin);
        }

        /**
         * @return Icon as a BufferedImage
         */
        public BufferedImage getIcon() throws IOException, URISyntaxException
        {
                BufferedImage image = ImageIO.read(new URI(iconUrl).toURL());
                if (image == null) throw new IOException("Unsupported or invalid image: " + iconUrl);
                return image;
        }

        private <T> CompletableFuture<T> downloadAs(
                ModrinthVersion version, Path destination, String userAgent,
                ProjectType expectedType, Function<Path, T> converter)
        {
                requireType(expectedType);
                if (version == null)
                        throw new IllegalArgumentException("version must not be null");
                if (version.projectId() != null && id != null && !id.equals(version.projectId()))
                        throw new IllegalArgumentException("version does not belong to this project");
                if (userAgent == null || userAgent.isBlank())
                        throw new IllegalArgumentException("Modrinth requires a User-Agent");
                ModrinthVersion.VersionFile file = version.primaryFile();
                if (file == null)
                        throw new IllegalArgumentException("version has no downloadable files");

                return Modrinth.downloadVersionFile(file, destination, userAgent).thenApply(converter);
        }

        private void requireType(ProjectType expected)
        {
                if (type() != expected)
                        throw new IllegalStateException("Project is " + projectType + ", not " + expected.getId());
        }

        private static void requireDownloadedFile(Path downloadedFile)
        {
                if (downloadedFile == null)
                        throw new IllegalArgumentException("downloadedFile must not be null");
        }
}