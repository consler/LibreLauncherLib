package net.consler.librelauncherlib.util;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;

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

    public static void openBrowser(String url)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(URI.create(url));
            }
        }
        catch (Exception e)
        {
            System.out.println("Could not open browser automatically. Please open: " + url);
        }
    }

    public static Path getJavaBin()
    {
        return Path.of(System.getProperty("java.home"), "bin", "java" + (SystemHelper.getOS().equals("windows") ? ".exe" : ""));
    }
}
