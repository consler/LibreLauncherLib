package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.util.DownloadManager;
import net.consler.librelauncherlib.util.MavenHelper;

import java.nio.file.Path;

class LibraryProcessor
{
    public void processLibraries(JsonArray libraries, Path librariesDir)
    {
        DownloadManager downloadManager = new DownloadManager();

        for (JsonElement element : libraries)
        {
            JsonObject libObj = element.getAsJsonObject();
            if (!libObj.has("name")) continue;

            String library = libObj.get("name").getAsString();
            String pathStr = MavenHelper.toJarPath(library);
            Path target = librariesDir.resolve(pathStr);

            String baseUrl = libObj.has("url") ? libObj.get("url").getAsString() : "https://maven.quiltmc.org/repository/release/";

            downloadManager.downloadFile(baseUrl + pathStr, target);
        }
    }
}