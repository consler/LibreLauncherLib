package net.consler.librelauncherlib.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MinecraftInstaller
{

    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final DownloadManager downloadManager;
    private final LibraryProcessor libraryProcessor;
    private final AssetProcessor assetProcessor;
    private final NativeExtractor nativeExtractor;

    public MinecraftInstaller()
    {
        this.downloadManager = new DownloadManager();
        this.libraryProcessor = new LibraryProcessor();
        this.assetProcessor = new AssetProcessor(downloadManager);
        this.nativeExtractor = new NativeExtractor();
    }

    /**
     * Installs the selected Minecraft version to the supplied root directory.
     *
     * @param version The version of Minecraft to install (e.g. "26.2")
     * @param rootDirectory The directory that will hold client.jar, libraries, assets, natives, and version metadata
     */
    public void install(String version, Path rootDirectory)
    {
        try
        {
            Path librariesDir = rootDirectory.resolve("libraries");
            Path assetsDir = rootDirectory.resolve("assets");
            Path nativesDir = rootDirectory.resolve("natives");

            Files.createDirectories(librariesDir);
            Files.createDirectories(assetsDir);
            Files.createDirectories(nativesDir);

            JsonObject manifest = downloadManager.fetchJson(MANIFEST_URL);
            String versionJsonUrl = findVersionUrl(manifest, version);

            JsonObject versionDetails = downloadManager.fetchJson(versionJsonUrl);
            Files.writeString(rootDirectory.resolve(version + ".json"), versionDetails.toString());

            List<DownloadTask> tasks = new ArrayList<>();
            List<Path> nativeZipsToExtract = new ArrayList<>();

            JsonObject downloads = versionDetails.getAsJsonObject("downloads");
            String clientUrl = downloads.getAsJsonObject("client").get("url").getAsString();
            tasks.add(new DownloadTask(clientUrl, rootDirectory.resolve(version + ".jar")));

            libraryProcessor.processLibraries(versionDetails.getAsJsonArray("libraries"), librariesDir, tasks, nativeZipsToExtract);
            assetProcessor.processAssets(versionDetails.getAsJsonObject("assetIndex"), assetsDir, tasks);

            downloadManager.downloadBatch(tasks);

            nativeExtractor.extractNatives(nativeZipsToExtract, nativesDir);
            downloadManager.shutdown();
        }
        catch (Exception e)
        {
            if (e instanceof net.consler.librelauncherlib.exception.LibraryException) throw (net.consler.librelauncherlib.exception.LibraryException) e;
            throw new net.consler.librelauncherlib.exception.InstallationException("Failed to install Minecraft version " + version, e);
        }
    }

    private String findVersionUrl(JsonObject manifest, String versionId)
    {
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (JsonElement elem : versions)
        {
            JsonObject v = elem.getAsJsonObject();
            if (v.get("id").getAsString().equals(versionId))
            {
                return v.get("url").getAsString();
            }
        }
        throw new net.consler.librelauncherlib.exception.VersionNotFoundException("Version not found: " + versionId);
    }
}