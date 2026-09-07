package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.utill.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class FabricInstaller
{
    public static void install(ModloaderProfile profile, Path gameDir, String minecraftVersion)
    {
        try
        {
            DownloadManager downloadManager = new DownloadManager();
            Path profilePath = gameDir.resolve("fabric-profile.json");

            downloadManager.downloadFile("https://meta.fabricmc.net/v2/versions/loader/" + minecraftVersion + "/" + profile.loaderVersion() + "/profile/json", profilePath);

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