package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * A Modrinth project version.
 */
public record ModrinthVersion(
        String id,
        @SerializedName("project_id") String projectId,
        @SerializedName("author_id") String authorId,
        String name,
        @SerializedName("version_number") String versionNumber,
        String changelog,
        List<Dependency> dependencies,
        @SerializedName("game_versions") List<String> gameVersions,
        @SerializedName("version_type") VersionType versionType,
        List<String> loaders,
        boolean featured,
        String status,
        @SerializedName("date_published") String datePublished,
        long downloads,
        List<VersionFile> files
)
{
    /** Replaces missing collection fields with empty lists. */
    public ModrinthVersion
    {
        dependencies = dependencies == null ? List.of() : dependencies;
        gameVersions = gameVersions == null ? List.of() : gameVersions;
        loaders = loaders == null ? List.of() : loaders;
        files = files == null ? List.of() : files;
    }

    /** @return Dependencies of type REQUIRED. */
    public List<Dependency> requiredDependencies()
    {
        return dependencies.stream().filter(Dependency::isRequired).toList();
    }

    /** @return The primary file, or the first file if none is flagged primary, or null if there are no files. */
    public VersionFile primaryFile()
    {
        return files.stream().filter(VersionFile::primary).findFirst()
                .orElse(files.isEmpty() ? null : files.getFirst());
    }

    /** Release channel of a version. null in a record means Modrinth sent a value this wrapper doesn't know. */
    public enum VersionType
    {
        @SerializedName("release") RELEASE,
        @SerializedName("beta") BETA,
        @SerializedName("alpha") ALPHA
    }

    /** Kind of dependency. null in a record means Modrinth sent a value this wrapper doesn't know. */
    public enum DependencyType
    {
        @SerializedName("required") REQUIRED,
        @SerializedName("optional") OPTIONAL,
        @SerializedName("incompatible") INCOMPATIBLE,
        @SerializedName("embedded") EMBEDDED
    }

    /**
     * A dependency of a version. versionId, projectId and fileName may each be null.
     */
    public record Dependency(
            @SerializedName("version_id") String versionId,
            @SerializedName("project_id") String projectId,
            @SerializedName("file_name") String fileName,
            @SerializedName("dependency_type") DependencyType dependencyType
    )
    {
        /** @return Whether this dependency is required. */
        public boolean isRequired()
        {
            return dependencyType == DependencyType.REQUIRED;
        }
        /** @return Whether this dependency is optional. */
        public boolean isOptional()
        {
            return dependencyType == DependencyType.OPTIONAL;
        }
        /** @return Whether this dependency is incompatible. */
        public boolean isIncompatible()
        {
            return dependencyType == DependencyType.INCOMPATIBLE;
        }
        /** @return Whether this dependency is embedded. */
        public boolean isEmbedded()
        {
            return dependencyType == DependencyType.EMBEDDED;
        }
    }

    /**
     * A downloadable file of a version.
     *
     * @param hashes Map of algorithm to hash (sha1, sha512).
     */
    public record VersionFile(
            Map<String, String> hashes,
            String url,
            String filename,
            boolean primary,
            long size,
            @SerializedName("file_type") String fileType
    )
    {
        /** Replaces missing hashes with an empty map. */
        public VersionFile
        {
            hashes = hashes == null ? Map.of() : hashes;
        }
    }
}