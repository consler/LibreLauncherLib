package net.consler.librelauncherlib.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.ListVersionsFailureException;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class Vanilla
{
    private static final String METADATA_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static JsonObject cachedVersionManifest;

    private static JsonObject getVersionManifest()
    {
        if(cachedVersionManifest != null) return cachedVersionManifest;

        try
        {
            URL url = URI.create(METADATA_URL).toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            cachedVersionManifest = json;
            return json;
        }
        catch (Exception e)
        {
            throw new ListVersionsFailureException(e.getMessage());
        }
    }
    /**
     * Returns a list of all available Minecraft versions.
     *
     * @return List of Minecraft versions
     */

    public static List<String> getVersions()
    {
        return new ArrayList<>(getVersionsWithType().keySet());
    }

    private static Map<String, String> getVersionsWithType()
    {

        Map<String, String> versionsMap = new LinkedHashMap<>();
        JsonArray versions = getVersionManifest().getAsJsonArray("versions");

        for (JsonElement version : versions)
        {
            JsonObject obj = version.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String type = obj.get("type").getAsString();
            versionsMap.put(id, type);
        }

        return versionsMap;
    }

    /**
     * Returns a list of Minecraft versions filtered by type.
     *
     * @param type Type of versions to include (e.g. release, snapshot, old_beta, old_alpha)
     * @return List of filtered Minecraft versions
     */
    public static List<String> getVersionsFiltered(String type)
    {
        List<String> filteredVersions = new ArrayList<>();

        for (String version : getVersionsWithType().keySet())
        {
            if (getVersionsWithType().get(version).equals(type))
            {
                filteredVersions.add(version);
            }
        }

        return filteredVersions;
    }
    /**
     * Clears cached versions. Useful if the version list needs to be refreshed (e.g. a new version came out).
     */

    public static void clearCachedVersions()
    {
        cachedVersionManifest = null;
    }
}
