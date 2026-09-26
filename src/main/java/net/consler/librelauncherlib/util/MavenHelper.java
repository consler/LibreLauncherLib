package net.consler.librelauncherlib.util;

public class MavenHelper
{
    public static String toJarPath(String coord)
    {
        String[] parts = coord.split(":");

        String groupPath = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];

        return groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }
}
