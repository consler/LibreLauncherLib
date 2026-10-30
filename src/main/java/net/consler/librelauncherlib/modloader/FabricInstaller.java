package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.util.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the installation of Fabric.
 */
public class FabricInstaller
{
    /**
     * Removes Fabric's profile and the libraries listed by that profile.
     *
     * @param gameDir the game directory containing the Fabric installation
     */
    public static void uninstall(Path gameDir)
    {
        ModLoaderUninstaller.uninstall(gameDir, "fabric-profile.json", "fabric");
    }

    /**
     * Installs Fabric into the provided game directory.
     *
     * @param modloaderProfile The version of Fabric to install (e.g. "0.19.5")
     * @param gameDir The directory where Fabric will be installed.
     * @param minecraftVersion The version of Minecraft in the provided directory.
     */
    public static void install(ModloaderProfile modloaderProfile, Path gameDir, String minecraftVersion)
    {
        try
        {
            DownloadManager downloadManager = new DownloadManager();
            Path profilePath = gameDir.resolve("fabric-profile.json");

            Files.deleteIfExists(profilePath);
            downloadManager.downloadFile("https://meta.fabricmc.net/v2/versions/loader/" + minecraftVersion + "/" + modloaderProfile.loaderVersion() + "/profile/json", profilePath);

            JsonObject profileJson = JsonParser.parseString(Files.readString(profilePath)).getAsJsonObject();
            JsonArray libraries = profileJson.getAsJsonArray("libraries");

            new ModLibraryProcessor().processLibraries(libraries, gameDir.resolve("libraries"));
        }
        catch (Exception e)
        {
            throw new ModLoaderException("Failed to install Fabric dependencies", e);
        }
    }
}