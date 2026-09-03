package net.consler.librelauncherlib;

import net.consler.librelauncherlib.download.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;

import java.nio.file.Path;

/**
 * Library to download and run any Minecraft version.
 */
public final class LibreLauncherLib
{
    private LibreLauncherLib()
    {
    }

    /**
     * Installs the supplied Minecraft version into a target directory.
     * @param version The version of Minecraft to install (e.g. "26.2").
     * @param rootDirectory The directory where Minecraft should be installed.
     */
    public static void install(String version, Path rootDirectory)
    {
        new MinecraftInstaller().install(version, rootDirectory);
    }

    /**
     * Launches Minecraft for with the launch profile.
     */
    public static Process launch(LaunchProfile profile)
    {
        return new MinecraftLauncher().launch(profile);
    }
}
