package net.consler.librelauncherlib.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.ListVersionsFailureException;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Quilt
{
    private static final String METADATA_URL = "https://meta.quiltmc.org/v3/versions/loader";

    private static JsonArray cachedVersionManifest;

    private static JsonArray getVersionManifest()
    {
        if(cachedVersionManifest != null) return cachedVersionManifest;

        try
        {
            URL url = URI.create(METADATA_URL).toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonArray json = JsonParser.parseReader(reader).getAsJsonArray();
            cachedVersionManifest = json;
            return json;
        }
        catch (Exception e)
        {
            throw new ListVersionsFailureException(e.getMessage());
        }
    }

    public static List<String> getVersions()
    {
        List<String> versions = new ArrayList<>();
        for (JsonElement element : getVersionManifest())
        {
            versions.add(element.getAsJsonObject().get("version").getAsString());
        }
        return versions;
    }

    public static void clearCachedVersions()
    {
        cachedVersionManifest = null;
    }

    public static List<String> getVersionsCompatibleWith(String minecraftVersion)
    {
        String versionSpecificMetadataUrl = METADATA_URL + "/" + minecraftVersion;
        List<String> versions = new ArrayList<>();

        try
        {
            URL url = URI.create(versionSpecificMetadataUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 400) return versions;

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());

            JsonElement root = JsonParser.parseReader(reader);

            JsonArray jsonArray = root.getAsJsonArray();
            for (JsonElement element : jsonArray)
            {
                JsonObject obj = element.getAsJsonObject();
                versions.add(obj.getAsJsonObject("loader").get("version").getAsString());
            }

        }
        catch (Exception e)
        {
            throw new ListVersionsFailureException(e.getMessage());
        }

        return versions;
    }
}
