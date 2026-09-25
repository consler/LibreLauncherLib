package net.consler.librelauncherlib.instance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.ResourcePackReadException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ResourcePack
{
    private final String name;
    private String description = "No description provided.";
    private int packFormat = -1;
    private BufferedImage icon = null;

    /**
     * A resource pack (.zip file)
     * @param packFile the .zip file. It's a path because on some systems File breaks with some characters
     */
    public ResourcePack(Path packFile)
    {
        this.name = packFile.getFileName().toString().replaceFirst("[.][^.]+$", "");

        try (InputStream in = Files.newInputStream(packFile);
             ZipInputStream zin = new ZipInputStream(in))
        {
            ZipEntry entry;
            byte[] mcmeta = null;
            byte[] iconBytes = null;

            while ((entry = zin.getNextEntry()) != null)
            {
                if (entry.getName().equals("pack.mcmeta")) mcmeta = zin.readAllBytes();
                else if (entry.getName().equals("pack.png")) iconBytes = zin.readAllBytes();
            }

            if (mcmeta != null) parseMetadata(mcmeta);
            if (iconBytes != null) loadIcon(iconBytes);
        }
        catch (Exception e)
        {
            System.err.println("Failed to read resource pack file: " + packFile.getFileName());
            throw new RuntimeException("Failed to read resource pack: " + e.getMessage(), e);
        }
    }

    private void parseMetadata(byte[] mcmetaBytes)
    {
        String content = new String(mcmetaBytes, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        if (root.has("pack") && root.get("pack").isJsonObject())
        {
            JsonObject packMeta = root.getAsJsonObject("pack");
            if (packMeta.has("pack_format")) this.packFormat = packMeta.get("pack_format").getAsInt();
            if (packMeta.has("description"))
            {
                JsonElement descElement = packMeta.get("description");
                this.description = descElement.isJsonPrimitive() ? descElement.getAsString() : descElement.toString();
            }
        }
    }

    private void loadIcon(byte[] iconBytes)
    {
        try
        {
            this.icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
        }
        catch (IOException e)
        {
            throw new ResourcePackReadException("Failed to parse resource pack icon image");
        }
    }


    private String readEntry(ZipFile zip) throws IOException
    {
        ZipEntry entry = zip.getEntry("pack.mcmeta");
        if (entry == null) return null;
        try (InputStream is = zip.getInputStream(entry))
        {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public int getPackFormat()
    {
        return packFormat;
    }

    public BufferedImage getIcon()
    {
        return icon;
    }

    public boolean hasIcon()
    {
        return icon != null;
    }
}