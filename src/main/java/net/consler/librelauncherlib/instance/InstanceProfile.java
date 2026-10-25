package net.consler.librelauncherlib.instance;

import net.consler.librelauncherlib.modloader.ModloaderProfile;

import java.io.File;
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
}
