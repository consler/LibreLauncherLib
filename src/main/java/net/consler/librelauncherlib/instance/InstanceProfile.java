package net.consler.librelauncherlib.instance;

import com.google.gson.JsonObject;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Record representing minecraft instances.
 *
 * @param gameDir the directory containing the minecraft instance
 */
public record InstanceProfile(Path gameDir)
{
    /** @return The name of the instance (Directory name).*/
    public String getName()
    {
        return gameDir.getFileName().toString();
    }

    /** @return The directory containing the minecraft instance.*/
    public Path getFolder()
    {
        return gameDir;
    }

    /** @return The mod directory of the instance.*/
    public Path getModsFolder()
    {
        return new File(gameDir.toFile(), "mods").toPath();
    }

    /** @return The resource pack directory of the instance.*/
    public Path getResourcePackFolder()
    {
        return new File(gameDir.toFile(), "resourcepacks").toPath();
    }

    /** @return The saves(world) directory of the instance.*/
    public Path getSavesFolder()
    {
        return new File(gameDir.toFile(), "saves").toPath();
    }

    /** @return The screenshots directory of the instance.*/
    public Path getScreenshotsFolder()
    {
        return new File(gameDir.toFile(), "screenshots").toPath();
    }

    /** @return The options file of the instance.*/
    public File getOptionsFile()
    {
        return new File(gameDir.toFile(), "options.txt");
    }

    /** @return The logs directory of the instance.*/
    public Path getLogsFolder()
    {
        return new File(gameDir.toFile(), "logs").toPath();
    }

    /** @return The latest log file of the instance.*/
    public File getLatestLog()
    {
        return new File(gameDir.resolve("logs").toFile(), "latest.log");
    }

    /** @return Servers of the instance.*/
    public Servers getServers()
    {
        return new Servers(gameDir.resolve("servers.dat").toFile());
    }

    /**
     * @return The mod loader used by the instance.
     */
    public String getModloader()
    {
        if (getFolder().resolve("fabric-profile.json").toFile().exists())
            return ModloaderProfile.FABRIC_ID;
        if (getFolder().resolve("quilt-profile.json").toFile().exists())
            return ModloaderProfile.QUILT_ID;
        if (getFolder().resolve("forge-profile.json").toFile().exists())
            return ModloaderProfile.FORGE_ID;
        if (getFolder().resolve("neoforge-profile.json").toFile().exists())
            return ModloaderProfile.NEOFORGE_ID;

        return ModloaderProfile.VANILLA_ID;
    }

    /**
     * @return The version of the modloader.
     */

    public String getLoaderVersion() throws IOException
    {
        switch(getModloader())
        {
            case ModloaderProfile.FABRIC_ID, ModloaderProfile.QUILT_ID ->
            {
                JsonObject profile = VersionManifest.readJson(getFolder().resolve("fabric-profile.json"));
                String input = profile.get("id").getAsString();

                int firstHyphen = input.indexOf('-');
                int secondHyphen = input.indexOf('-', firstHyphen + 1);
                int lastHyphen = input.lastIndexOf('-');

                if (secondHyphen < 0 || lastHyphen <= secondHyphen)
                    throw new IllegalArgumentException("Unexpected format: " + input);

                return input.substring(secondHyphen + 1, lastHyphen);

            }
            case ModloaderProfile.FORGE_ID ->
            {
                JsonObject profile = VersionManifest.readJson(getFolder().resolve("forge-profile.json"));
                String value = profile.get("id").getAsString();
                return value.replace("-forge-", "-");
            }
            case ModloaderProfile.NEOFORGE_ID ->
            {
                JsonObject profile = VersionManifest.readJson(getFolder().resolve("neoforge-profile.json"));
                String value = profile.get("id").getAsString();
                return value.replace("neoforge-", "");
            }
            default ->
            {
                return "0.0.0";
            }
        }
    }

    /**
     * @return The version of the instance.
     */

    public String getVersion() throws IOException
    {
        JsonObject manifest = VersionManifest.readJson(getFolder().resolve("manifest.json"));
        return manifest.get("id").getAsString();
    }
}
