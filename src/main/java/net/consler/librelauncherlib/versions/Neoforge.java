package net.consler.librelauncherlib.versions;

import net.consler.librelauncherlib.exception.ListVersionsFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Neoforge
{
    private static final String METADATA_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static List<String> cachedVersions;

    private static List<String> getVersionManifest()
    {
        if (cachedVersions != null) return cachedVersions;

        try
        {
            URL url = URI.create(METADATA_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream is = conn.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            List<String> versions = new ArrayList<>();
            NodeList versionNodes = doc.getElementsByTagName("version");

            for (int i = 0; i < versionNodes.getLength(); i++)
            {
                versions.add(versionNodes.item(i).getTextContent());
            }

            cachedVersions = versions;
            return versions;
        }
        catch (Exception e)
        {
            throw new ListVersionsFailureException(e.getMessage());
        }
    }

    public static List<String> getVersions()
    {
        return getVersionManifest().reversed();
    }

    public static void clearCachedVersions()
    {
        cachedVersions = null;
    }

    public static List<String> getVersionsCompatibleWith(String minecraftVersion)
    {
        List<String> versions = new ArrayList<>();
        String prefix;

        if (minecraftVersion.startsWith("1.")) prefix = minecraftVersion.substring(2);
        else prefix = minecraftVersion;

        try
        {
            for (String version : getVersionManifest())
            {
                if (version.startsWith(prefix)) versions.add(version);
            }
        }
        catch (Exception e)
        {
            throw new ListVersionsFailureException(e.getMessage());
        }

        return versions.reversed();
    }
}