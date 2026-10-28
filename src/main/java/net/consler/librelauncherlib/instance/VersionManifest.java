package net.consler.librelauncherlib.instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.consler.librelauncherlib.util.MavenHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parsed version metadata stored with a Minecraft instance. */
public final class VersionManifest
{
    public record LibraryDownload(String path, String url, String sha1, long size) {}

    /** A library entry from a version or loader profile. */
    public static final class Library
    {
        private final JsonObject data;

        private Library(JsonObject data)
        {
            this.data = data.deepCopy();
        }

        public String name() { return string(data, "name"); }

        public String repositoryUrl() { return string(data, "url"); }

        public JsonArray rules() { return array(data, "rules"); }

        public JsonObject toJson() { return data.deepCopy(); }

        public boolean serverOnly()
        {
            String clientRequired = string(data, "clientreq");
            return clientRequired != null && !Boolean.parseBoolean(clientRequired.trim());
        }

        /** Artifact path, deriving it from the Maven coordinate when metadata omits it. */
        public String path()
        {
            JsonObject artifact = object(object(data, "downloads"), "artifact");
            String artifactPath = string(artifact, "path");
            if (artifactPath != null) return artifactPath;
            if (data.has("natives")) return null;

            JsonObject downloads = object(data, "downloads");
            if (downloads != null && downloads.has("classifiers")) return null;
            String coordinate = name();
            if (coordinate == null) return null;
            try
            {
                return MavenHelper.toJarPath(coordinate);
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        }

        public LibraryDownload artifactDownload()
        {
            return download(object(object(data, "downloads"), "artifact"));
        }

        public LibraryDownload nativeDownload(String osName, String archBits)
        {
            String classifier = string(object(data, "natives"), osName);
            if (classifier == null) return null;
            String resolvedClassifier = classifier.replace("${arch}", archBits);
            JsonObject classifiers = object(object(data, "downloads"), "classifiers");
            return download(object(classifiers, resolvedClassifier));
        }

        public String repositoryDownloadUrl(String path)
        {
            String base = repositoryUrl();
            if (base == null || base.isEmpty()) base = "https://libraries.minecraft.net/";
            if (!base.endsWith("/")) base += "/";
            return base + path;
        }

        public String sha1() { return string(data, "sha1"); }

        public long size()
        {
            return longValue(data, "size", -1);
        }

        public boolean nativeArchive()
        {
            String[] coordinates = name() == null ? new String[0] : name().split(":");
            return coordinates.length > 3 && coordinates[3].startsWith("native");
        }

        private static LibraryDownload download(JsonObject json)
        {
            if (json == null) return null;
            return new LibraryDownload(string(json, "path"), string(json, "url"),
                    string(json, "sha1"), longValue(json, "size", -1));
        }
    }

    private final JsonObject data;

    public VersionManifest(JsonObject data)
    {
        if (data == null) throw new IllegalArgumentException("Version manifest data must not be null");
        this.data = data.deepCopy();
    }

    /** Loads version metadata from a JSON file. */
    public static VersionManifest fromFile(Path path) throws IOException
    {
        return new VersionManifest(readJson(path));
    }

    /** The version id declared by this manifest, or null when it is absent. */
    public String id()
    {
        return string("id");
    }

    /** The asset index id, falling back to the legacy assets id. */
    public String assetIndexId()
    {
        String indexId = string(object("assetIndex"), "id");
        String assetsId = string("assets");
        return indexId != null ? indexId : assetsId != null ? assetsId : "legacy";
    }

    /** The main class declared by this version or loader profile, if present. */
    public String mainClass()
    {
        return string("mainClass");
    }

    /** The version this profile inherits from, if present. */
    public String inheritsFrom()
    {
        return string("inheritsFrom");
    }

    /** The explicitly declared client jar version, if present. */
    public String jar()
    {
        return string("jar");
    }

    /** Version type, defaulting to {@code release} when metadata omits it. */
    public String versionType()
    {
        String type = string("type");
        return type != null ? type : "release";
    }

    /** Minimum Java major version declared in this metadata, or zero when absent/invalid. */
    public int javaMajorVersion()
    {
        JsonObject javaVersion = object("javaVersion");
        String major = string(javaVersion, "majorVersion");
        if (major == null) return 0;
        try
        {
            return Integer.parseInt(major.trim());
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    /** Libraries declared by this metadata, or null when the field is absent. */
    public JsonArray libraries()
    {
        JsonElement element = data.get("libraries");
        return element != null && element.isJsonArray() ? element.getAsJsonArray().deepCopy() : null;
    }

    /** Library entries as typed metadata objects. */
    public List<Library> libraryEntries()
    {
        JsonArray entries = libraries();
        if (entries == null) return List.of();
        List<Library> result = new ArrayList<>(entries.size());
        for (JsonElement entry : entries)
        {
            if (entry.isJsonObject()) result.add(new Library(entry.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    /** Builds a de-duplication key for Maven library coordinates. */
    public static String libraryKey(String name)
    {
        if (name == null) return null;
        String ext = "";
        int at = name.indexOf('@');
        if (at >= 0)
        {
            ext = name.substring(at);
            name = name.substring(0, at);
        }
        String[] parts = name.split(":");
        if (parts.length < 3) return name + ext;
        StringBuilder key = new StringBuilder(parts[0]).append(':').append(parts[1]);
        if (parts.length > 3) key.append(':').append(parts[3]);
        return key.append(ext).toString();
    }

    /** JVM argument templates declared by this metadata, or null when absent. */
    public JsonArray jvmArguments()
    {
        return array(object("arguments"), "jvm");
    }

    /** Game argument templates declared by this metadata, or null when absent. */
    public JsonArray gameArguments()
    {
        return array(object("arguments"), "game");
    }

    /** Legacy whitespace-separated game arguments, if present. */
    public String minecraftArguments()
    {
        return string("minecraftArguments");
    }

    /** Logging launch argument, if declared. */
    public String loggingArgument()
    {
        return string(object(object("logging"), "client"), "argument");
    }

    /** Logging configuration file id, if declared. */
    public String loggingConfigId()
    {
        return string(object(object(object("logging"), "client"), "file"), "id");
    }

    /** Logging configuration file metadata, or null when absent. */
    public JsonObject loggingFile()
    {
        JsonObject file = object(object(object("logging"), "client"), "file");
        return file == null ? null : file.deepCopy();
    }

    /** Returns a copy of the parsed JSON data. */
    public JsonObject toJson()
    {
        return data.deepCopy();
    }

    private String string(String key)
    {
        return string(data, key);
    }

    private JsonObject object(String key)
    {
        return object(data, key);
    }

    private static String string(JsonObject object, String key)
    {
        if (object == null) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static JsonObject object(JsonObject object, String key)
    {
        if (object == null) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject object, String key)
    {
        if (object == null) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray().deepCopy() : null;
    }

    private static long longValue(JsonObject object, String key, long fallback)
    {
        String value = string(object, key);
        if (value == null) return fallback;
        try
        {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    /** Reads a JSON object from a file. */
    public static JsonObject readJson(Path path) throws IOException
    {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
