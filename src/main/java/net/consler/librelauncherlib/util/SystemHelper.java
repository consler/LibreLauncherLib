package net.consler.librelauncherlib.util;

import java.awt.*;

/**
 * Provides helper methods for system-related operations.
 */
public class SystemHelper
{
    public static String getOS()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    public static String getArchitecture()
    {
        return System.getProperty("os.arch");
    }

    public static String getManifestArchitecture()
    {
        String arch = getArchitecture().toLowerCase();
        return switch (arch)
        {
            case "aarch64", "arm64" -> "arm64";
            case "x86", "i386", "i686" -> "x86";
            case "amd64", "x86_64" -> "x86_64";
            default -> arch;
        };
    }
}