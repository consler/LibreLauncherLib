package net.consler.librelauncherlib.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Datapack
{
    private final String name;
    private JsonElement description = null;
    private int packFormat = -1;
    private int[] minFormat = null;
    private int[] maxFormat = null;
    private SupportedFormats supportedFormats = null;
    private BufferedImage icon = null;

    public record SupportedFormats(int[] minInclusive, int[] maxInclusive)
    {
    }

    /**
     * A datapack (.zip file)
     * @param packFile the .zip file
     */
    public Datapack(Path packFile)
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
            System.err.println("Failed to read datapack file: " + packFile.getFileName());
            throw new RuntimeException("Failed to read datapack: " + e.getMessage(), e);
        }
    }

    private void parseMetadata(byte[] mcmetaBytes)
    {
        String content = new String(mcmetaBytes, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        if (root.has("pack") && root.get("pack").isJsonObject())
        {
            JsonObject packMeta = root.getAsJsonObject("pack");

            if (packMeta.has("description")) this.description = packMeta.get("description");

            if (packMeta.has("pack_format")) this.packFormat = packMeta.get("pack_format").getAsInt();

            if (packMeta.has("min_format")) this.minFormat = parseFormatVersion(packMeta.get("min_format"), false);

            if (packMeta.has("max_format")) this.maxFormat = parseFormatVersion(packMeta.get("max_format"), true);

            if (packMeta.has("supported_formats")) this.supportedFormats = parseSupportedFormats(packMeta.get("supported_formats"));
        }
    }

    private int[] parseFormatVersion(JsonElement element, boolean isMax)
    {
        if (element == null || element.isJsonNull()) return null;

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            int major = element.getAsInt();
            return new int[]{major, isMax ? Integer.MAX_VALUE : 0};
        }

        if (element.isJsonArray())
        {
            JsonArray arr = element.getAsJsonArray();
            if (arr.size() == 1)
            {
                int major = arr.get(0).getAsInt();
                return new int[]{major, isMax ? Integer.MAX_VALUE : 0};
            }
            else if (arr.size() >= 2)
            {
                return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()};
            }
        }

        return null;
    }

    private SupportedFormats parseSupportedFormats(JsonElement element)
    {
        if (element == null || element.isJsonNull()) return null;

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            int val = element.getAsInt();
            return new SupportedFormats(new int[]{val, 0}, new int[]{val, Integer.MAX_VALUE});
        }

        if (element.isJsonArray())
        {
            JsonArray arr = element.getAsJsonArray();
            if (arr.size() == 1)
            {
                int[] min = parseFormatVersion(arr.get(0), false);
                int[] max = parseFormatVersion(arr.get(0), true);
                return new SupportedFormats(min, max);
            }
            else if (arr.size() >= 2)
            {
                int[] min = parseFormatVersion(arr.get(0), false);
                int[] max = parseFormatVersion(arr.get(1), true);
                return new SupportedFormats(min, max);
            }
        }

        if (element.isJsonObject())
        {
            JsonObject obj = element.getAsJsonObject();
            int[] min = obj.has("min_inclusive") ? parseFormatVersion(obj.get("min_inclusive"), false) : null;
            int[] max = obj.has("max_inclusive") ? parseFormatVersion(obj.get("max_inclusive"), true) : null;
            return new SupportedFormats(min, max);
        }

        return null;
    }

    private void loadIcon(byte[] iconBytes)
    {
        try
        {
            this.icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to parse datapack icon image", e);
        }
    }

    public String getName()
    {
        return name;
    }

    public JsonElement getDescription()
    {
        return description;
    }

    public String getDescriptionAsString()
    {
        if (description == null) return "";
        return description.isJsonPrimitive() ? description.getAsString() : description.toString();
    }

    public int getPackFormat()
    {
        return packFormat;
    }

    public int[] getMinFormat()
    {
        return minFormat;
    }

    public int[] getMaxFormat()
    {
        return maxFormat;
    }

    public SupportedFormats getSupportedFormats()
    {
        return supportedFormats;
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