package net.consler.librelauncherlib.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.LibraryException;
import net.consler.librelauncherlib.modloader.*;
import net.consler.librelauncherlib.util.DownloadManager;
import net.consler.librelauncherlib.util.DownloadTask;
import net.consler.librelauncherlib.util.JavaVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.consler.librelauncherlib.modloader.ModloaderProfile.*;

/**
 * Class for installing Minecraft.
 */
public class MinecraftInstaller
{

    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Set<String> SUPPORTED_LOADERS = Set.of(VANILLA_ID, FABRIC_ID, QUILT_ID, FORGE_ID, NEOFORGE_ID);

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
     * Installs the selected Minecraft version to the supplied directory. Uses the Java executable that runs LibreLauncherLib.
     *
     * @param version The version of Minecraft to install (e.g. "26.2")
     * @param gameDir The directory that will hold client.jar, libraries, assets, natives, and version metadata
     * @param modloaderProfile The modloader profile to use for installation
     */
    public void install(String version, Path gameDir, ModloaderProfile modloaderProfile)
    {
        install(version, gameDir, modloaderProfile, JavaVersion.current().executable());
    }


    /**
     * Installs the selected Minecraft version to the supplied directory.
     *
     * @param version The version of Minecraft to install (e.g. "26.2")
     * @param gameDir The directory that will hold client.jar, libraries, assets, natives, and version metadata
     * @param modloaderProfile The modloader profile to use for installation
     * @param javaBin The path to the Java executable that will be used to install modern Forge(1.13+) or Neoforge. Can be null if you are installing Fabric or Quilt
     */
    public void install(String version, Path gameDir, ModloaderProfile modloaderProfile, Path javaBin)
    {
        try
        {
            if (!SUPPORTED_LOADERS.contains(modloaderProfile.loaderId())) throw new IllegalArgumentException("Unsupported modloader: " + modloaderProfile.loaderId());

            downloadManager.setListener(listener);
            listener.onStart();

            Path librariesDir = gameDir.resolve("libraries");
            Path assetsDir = gameDir.resolve("assets");
            Path nativesDir = gameDir.resolve("natives");

            Files.createDirectories(librariesDir);
            Files.createDirectories(assetsDir);
            Files.createDirectories(nativesDir);

            JsonObject manifest = downloadManager.fetchJson(MANIFEST_URL);
            String versionJsonUrl = findVersionUrl(manifest, version);

            JsonObject versionDetails = downloadManager.fetchJson(versionJsonUrl);

            List<DownloadTask> tasks = new ArrayList<>();
            List<Path> nativeZipsToExtract = new ArrayList<>();

            JsonObject downloads = versionDetails.getAsJsonObject("downloads");
            tasks.add(DownloadTask.of(downloads.getAsJsonObject("client"), gameDir.resolve("client.jar")));

            libraryProcessor.processLibraries(versionDetails.getAsJsonArray("libraries"), librariesDir, tasks, nativeZipsToExtract);

            assetProcessor.processAssets(versionDetails.getAsJsonObject("assetIndex"), assetsDir, tasks);

            downloadManager.downloadBatch(tasks);

            nativeExtractor.extractNatives(nativeZipsToExtract, nativesDir);

            Files.writeString(gameDir.resolve("manifest.json"), versionDetails.toString());

            if(!modloaderProfile.loaderId().equals(VANILLA_ID))
            {
                listener.onModloaderInstallation(modloaderProfile.loaderId());
                switch (modloaderProfile.loaderId())
                {
                    case FABRIC_ID -> FabricInstaller.install(modloaderProfile, gameDir, version);
                    case QUILT_ID -> QuiltInstaller.install(modloaderProfile, gameDir, version);
                    case FORGE_ID -> ForgeInstaller.install(modloaderProfile, gameDir, version, javaBin);
                    case NEOFORGE_ID -> NeoforgeInstaller.install(modloaderProfile, gameDir, version, javaBin);
                    default -> throw new IllegalArgumentException("Unsupported modloader: " + modloaderProfile.loaderId());
                }
            }

            listener.onFinish();
        }
        catch (Exception e)
        {
            if (e instanceof LibraryException) throw (LibraryException) e;
            throw new InstallationException("Failed to install Minecraft version " + version, e);
        }
    }

    private String findVersionUrl(JsonObject manifest, String versionId)
    {
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (JsonElement elem : versions)
        {
            JsonObject v = elem.getAsJsonObject();
            if (v.get("id").getAsString().equals(versionId)) return v.get("url").getAsString();
        }
        throw new VersionNotFoundException("Version not found: " + versionId);
    }

    private InstallerListener listener = new InstallerListener()
    {
    };

    public void setListener(InstallerListener listener)
    {
        this.listener = (listener != null) ? listener : new InstallerListener()
        {
        };
    }
}