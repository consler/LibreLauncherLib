package net.consler.librelauncherlib.instance;

import java.io.File;
import java.nio.file.Path;

/**
 * Record representing minecraft instances.
 *
 * @param gameDir the directory containing the minecraft instance
 */
public record InstanceProfile(Path gameDir)
{
    public Path getFolder()
    {
        return gameDir;
    }

    public Path getModsFolder()
    {
        return new File(gameDir.toFile(), "mods").toPath();
    }

    public Path getResourcePackFolder()
    {
        return new File(gameDir.toFile(), "resourcepacks").toPath();
    }

    public Path getSavesFolder()
    {
        return new File(gameDir.toFile(), "saves").toPath();
    }

    public Path getScreenshotsFolder()
    {
        return new File(gameDir.toFile(), "screenshots").toPath();
    }

    public File getOptionsFile()
    {
        return new File(gameDir.toFile(), "options.txt");
    }

    public Path getLogsFolder()
    {
        return new File(gameDir.toFile(), "logs").toPath();
    }

    public File getLatestLog()
    {
        return new File(gameDir.resolve("logs").toFile(), "latest.log");
    }
}
