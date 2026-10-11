package net.consler.librelauncherlib.util;

/**
 * Provides helper methods for Maven-related operations.
 */
public class MavenHelper
{
    /**
     * Converts Maven coordinates to the relative path of the jar in a Maven repository layout.
     * Understands {@code group:artifact:version}, an optional {@code :classifier} and an optional {@code @extension}.
     *
     * @param coord the coordinates, for example {@code org.lwjgl:lwjgl:3.3.1:natives-linux}
     * @return the relative path, for example {@code org/lwjgl/lwjgl/3.3.1/lwjgl-3.3.1-natives-linux.jar}
     * @throws IllegalArgumentException when the coordinates have fewer than three parts
     */
    public static String toJarPath(String coord)
    {
        String extension = "jar";
        String rest = coord;

        int at = rest.indexOf('@');
        if (at >= 0)
        {
            extension = rest.substring(at + 1);
            rest = rest.substring(0, at);
        }

        String[] parts = rest.split(":");
        if (parts.length < 3) throw new IllegalArgumentException("Not a Maven coordinate: " + coord);

        String groupPath = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";

        return groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + classifier + "." + extension;
    }
}