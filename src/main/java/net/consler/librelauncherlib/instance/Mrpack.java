package net.consler.librelauncherlib.instance;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads metadata from a Modrinth modpack archive (.mrpack).
 */
public final class Mrpack
{
    private static final Gson GSON = new Gson();
    private static final String INDEX_ENTRY = "modrinth.index.json";

    private final Path packFile;
    private final Index index;

    /**
     * Reads the Modrinth index from an archive.
     *
     * @param packFile Path to a .mrpack archive.
     * @throws ModpackReadException If the archive has no valid Modrinth index.
     */
    public Mrpack(Path packFile)
    {
        if (packFile == null) throw new IllegalArgumentException("packFile must not be null");
        this.packFile = packFile.toAbsolutePath().normalize();

        try (ZipFile zip = new ZipFile(this.packFile.toFile()))
        {
            ZipEntry entry = zip.getEntry(INDEX_ENTRY);
            if (entry == null) throw new IOException("Archive does not contain " + INDEX_ENTRY);

            try (InputStream input = zip.getInputStream(entry))
            {
                String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                Index parsed = GSON.fromJson(json, Index.class);
                if (parsed == null) throw new IOException("Modrinth modpack index is empty");
                if (parsed.formatVersion() != 1)
                    throw new IOException("Unsupported Modrinth modpack format version: " + parsed.formatVersion());
                this.index = parsed;
            }
        }
        catch (IOException | JsonParseException e)
        {
            throw new ModpackReadException("Failed to read Modrinth modpack: " + this.packFile, e);
        }
    }

    /** @return The normalized archive path. */
    public Path getPackFile()
    {
        return packFile;
    }

    /** @return The Modrinth pack index format version. */
    public int getFormatVersion()
    {
        return index.formatVersion();
    }

    /** @return The game identifier, normally {@code minecraft}. */
    public String getGame()
    {
        return index.game();
    }

    /** @return The modpack name from its index. */
    public String getName()
    {
        return index.name();
    }

    /** @return The modpack version identifier. */
    public String getVersionId()
    {
        return index.versionId();
    }

    /** @return The modpack summary, or an empty string when omitted. */
    public String getSummary()
    {
        return index.summary() == null ? "" : index.summary();
    }

    /** @return Files listed in the modpack index. */
    public List<IndexFile> getFiles()
    {
        return index.files();
    }

    /** @return Required game and loader versions. */
    public Map<String, String> getDependencies()
    {
        return index.dependencies();
    }

    /** @return The archive directory containing override files. */
    public String getOverrides()
    {
        return index.overrides() == null ? "overrides" : index.overrides();
    }

    public record Index(
            int formatVersion,
            String game,
            String name,
            String versionId,
            String summary,
            List<IndexFile> files,
            Map<String, String> dependencies,
            String overrides
    )
    {
        public Index
        {
            files = files == null ? List.of() : List.copyOf(files);
            dependencies = dependencies == null ? Map.of() : Map.copyOf(dependencies);
        }
    }

    public record IndexFile(
            String path,
            Map<String, String> hashes,
            Map<String, String> env,
            List<String> downloads,
            long fileSize
    )
    {
        public IndexFile
        {
            hashes = hashes == null ? Map.of() : Map.copyOf(hashes);
            env = env == null ? Map.of() : Map.copyOf(env);
            downloads = downloads == null ? List.of() : List.copyOf(downloads);
        }
    }
}
