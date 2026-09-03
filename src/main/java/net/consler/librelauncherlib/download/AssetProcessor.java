package net.consler.librelauncherlib.download;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class AssetProcessor
{

    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    private final DownloadManager downloadManager;

    public AssetProcessor(DownloadManager downloadManager)
    {
        this.downloadManager = downloadManager;
    }

    public void processAssets(JsonObject assetIndexElement, Path assetsDir, List<DownloadTask> tasks)
    {
        try
        {
            if (assetIndexElement == null) return;

            String id = assetIndexElement.get("id").getAsString();
            String url = assetIndexElement.get("url").getAsString();

            Path indexesDir = assetsDir.resolve("indexes");
            Path objectsDir = assetsDir.resolve("objects");
            Files.createDirectories(indexesDir);
            Files.createDirectories(objectsDir);

            Path indexFile = indexesDir.resolve(id + ".json");
            downloadManager.downloadFile(url, indexFile);

            JsonObject assetIndex = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
            JsonObject objects = assetIndex.getAsJsonObject("objects");

            for (Map.Entry<String, JsonElement> entry : objects.entrySet())
            {
                String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
                String subHash = hash.substring(0, 2);
                String assetUrl = RESOURCES_URL + subHash + "/" + hash;
                Path target = objectsDir.resolve(subHash).resolve(hash);

                Files.createDirectories(target.getParent());

                tasks.add(new DownloadTask(assetUrl, target));
            }
        }
        catch (Exception e)
        {
            if (e instanceof net.consler.librelauncherlib.exception.LibraryException) throw (net.consler.librelauncherlib.exception.LibraryException) e;
            throw new net.consler.librelauncherlib.exception.LibraryException("Failed to process asset index", e);
        }
    }
}