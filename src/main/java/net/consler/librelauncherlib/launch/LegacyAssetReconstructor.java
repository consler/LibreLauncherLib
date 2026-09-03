package net.consler.librelauncherlib.launch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

class LegacyAssetReconstructor
{
    static void reconstructIfNeeded(Path gameDir, Path assetsDir, String assetIndexName)
    {
        try
        {
            if (!assetIndexName.equals("legacy") && !assetIndexName.equals("pre-1.6")) return;

            Path indexFile = assetsDir.resolve("indexes").resolve(assetIndexName + ".json");
            if (!Files.exists(indexFile)) return;

            Path resourcesDir = gameDir.resolve("resources");

            try (FileReader reader = new FileReader(indexFile.toFile()))
            {
                JsonObject indexJson = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject objects = indexJson.getAsJsonObject("objects");

                for (Map.Entry<String, JsonElement> entry : objects.entrySet())
                {
                    String originalPath = entry.getKey();
                    JsonObject assetInfo = entry.getValue().getAsJsonObject();
                    String hash = assetInfo.get("hash").getAsString();

                    String hashFolder = hash.substring(0, 2);
                    Path objectFile = assetsDir.resolve("objects").resolve(hashFolder).resolve(hash);

                    if (Files.exists(objectFile))
                    {
                        Path targetFile = resourcesDir.resolve(originalPath);
                        Files.createDirectories(targetFile.getParent());
                        Files.copy(objectFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new net.consler.librelauncherlib.exception.LibraryException("Failed to reconstruct legacy assets", e);
        }
    }
}