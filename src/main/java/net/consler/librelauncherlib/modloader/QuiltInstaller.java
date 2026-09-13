package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.utill.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class QuiltInstaller
{
    /**
     * Installs Quilt into the provided game directory.
     *
     * @param modloaderProfile The version of Quilt to install (e.g. "0.25.0")
     * @param gameDir The directory where Quilt will be installed.
     * @param minecraftVersion The version of Minecraft in the provided directory.
     */
    public static void install(ModloaderProfile modloaderProfile, Path gameDir, String minecraftVersion)
    {
        try
        {
            DownloadManager downloadManager = new DownloadManager();
            Path profilePath = gameDir.resolve("quilt-profile.json");

            downloadManager.downloadFile("https://meta.quiltmc.org/v3/versions/loader/" + minecraftVersion + "/" + modloaderProfile.loaderVersion() + "/profile/json", profilePath);

            JsonObject profileJson = JsonParser.parseString(Files.readString(profilePath)).getAsJsonObject();
            JsonArray libraries = profileJson.getAsJsonArray("libraries");

            new LibraryProcessor().processLibraries(libraries, gameDir.resolve("libraries"));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to install Quilt dependencies", e);
        }
    }
}