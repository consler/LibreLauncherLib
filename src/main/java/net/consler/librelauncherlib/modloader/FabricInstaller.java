package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.utill.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class FabricInstaller
{
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

            downloadManager.downloadFile("https://meta.fabricmc.net/v2/versions/loader/" + minecraftVersion + "/" + modloaderProfile.loaderVersion() + "/profile/json", profilePath);

            JsonObject profileJson = JsonParser.parseString(Files.readString(profilePath)).getAsJsonObject();
            JsonArray libraries = profileJson.getAsJsonArray("libraries");

            new LibraryProcessor().processLibraries(libraries, gameDir.resolve("libraries"));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to install Fabric dependencies", e);
        }
    }
}