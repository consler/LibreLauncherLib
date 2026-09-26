package net.consler.librelauncherlib.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.ModReadException;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for handling mods.
 */
public class Mod
{
    private String modId = "unknown";
    private String name;
    private String description = "No description provided.";
    private String version = "Unknown";
    private String loaderType = "Unknown";
    private String license = "Unknown";
    private final List<String> authors = new ArrayList<>();
    private String iconPath = null;
    private BufferedImage icon = null;

    public Mod(File modFile)
    {
        this.name = modFile.getName();

        try (JarFile jar = new JarFile(modFile))
        {
            if (jar.getJarEntry("quilt.mod.json") != null)
            {
                this.loaderType = ModloaderProfile.QUILT_ID;
                parseFabricOrQuilt(jar, "quilt.mod.json", true);
            }
            else if (jar.getJarEntry("fabric.mod.json") != null)
            {
                this.loaderType = ModloaderProfile.FABRIC_ID;
                parseFabricOrQuilt(jar, "fabric.mod.json", false);
            }
            else if (jar.getJarEntry("META-INF/neoforge.mods.toml") != null)
            {
                this.loaderType = ModloaderProfile.NEOFORGE_ID;
                parseForge(jar, "META-INF/neoforge.mods.toml");
            }
            else if (jar.getJarEntry("META-INF/mods.toml") != null)
            {
                this.loaderType = ModloaderProfile.FORGE_ID;
                parseForge(jar, "META-INF/mods.toml");
            }
            else if (jar.getJarEntry("mcmod.info") != null)
            {
                this.loaderType = ModloaderProfile.FORGE_ID;
                parseLegacyForge(jar);
            }

            if (this.version == null || this.version.startsWith("${") || this.version.equals("Unknown"))
            {
                String manifestVersion = extractVersionFromManifest(jar);
                if (manifestVersion != null)
                {
                    this.version = manifestVersion;
                }
            }

            loadIcon(jar);
        }
        catch (Exception e)
        {
            System.err.println("Failed to read mod file: " + modFile.getName());
            throw new ModReadException(e.getMessage());
        }
    }

    private void parseFabricOrQuilt(JarFile jar, String entryName, boolean isQuilt) throws IOException
    {
        String content = readEntry(jar, entryName);
        if (content == null) return;

        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        JsonObject meta = root;

        if (isQuilt && root.has("quilt_loader") && root.getAsJsonObject("quilt_loader").has("metadata"))
        {
            meta = root.getAsJsonObject("quilt_loader").getAsJsonObject("metadata");

            if (root.getAsJsonObject("quilt_loader").has("id"))
            {
                this.modId = root.getAsJsonObject("quilt_loader").get("id").getAsString();
            }
        }

        if (meta.has("id")) this.modId = meta.get("id").getAsString();
        if (meta.has("name")) this.name = meta.get("name").getAsString();
        if (meta.has("version")) this.version = meta.get("version").getAsString();
        if (meta.has("description")) this.description = meta.get("description").getAsString();

        if (meta.has("license"))
        {
            JsonElement licElement = meta.get("license");
            if (licElement.isJsonPrimitive()) this.license = licElement.getAsString();
            else if (licElement.isJsonArray() && !licElement.getAsJsonArray().isEmpty())
            {
                this.license = licElement.getAsJsonArray().get(0).getAsString();
            }
        }

        if (meta.has("authors"))
        {
            JsonElement authElement = meta.get("authors");
            if (authElement.isJsonArray())
            {
                for (JsonElement e : authElement.getAsJsonArray())
                {
                    if (e.isJsonPrimitive()) this.authors.add(e.getAsString());
                    else if (e.isJsonObject() && e.getAsJsonObject().has("name"))
                    {
                        this.authors.add(e.getAsJsonObject().get("name").getAsString());
                    }
                }
            }
        }

        if (meta.has("icon"))
        {
            JsonElement iconElement = meta.get("icon");
            if (iconElement.isJsonPrimitive())
            {
                this.iconPath = iconElement.getAsString();
            }
            else if (iconElement.isJsonObject() && !iconElement.getAsJsonObject().keySet().isEmpty())
            {
                String firstKey = iconElement.getAsJsonObject().keySet().iterator().next();
                this.iconPath = iconElement.getAsJsonObject().get(firstKey).getAsString();
            }
        }
    }

    private void parseForge(JarFile jar, String entryName) throws IOException
    {
        String content = readEntry(jar, entryName);
        if (content == null) return;

        String parsedId = extractRegex(content, "(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"");
        if (parsedId != null) this.modId = parsedId;

        String parsedName = extractRegex(content, "(?m)^\\s*displayName\\s*=\\s*\"([^\"]+)\"");
        if (parsedName != null) this.name = parsedName;

        String parsedVersion = extractRegex(content, "(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"");
        if (parsedVersion != null) this.version = parsedVersion;

        String parsedDesc = extractRegex(content, "(?m)^\\s*description\\s*=\\s*'''(.*?)'''");
        if (parsedDesc == null)
        {
            parsedDesc = extractRegex(content, "(?m)^\\s*description\\s*=\\s*\"([^\"]+)\"");
        }
        if (parsedDesc != null) this.description = parsedDesc;

        String parsedLicense = extractRegex(content, "(?m)^\\s*license\\s*=\\s*\"([^\"]+)\"");
        if (parsedLicense != null) this.license = parsedLicense;

        String parsedAuthors = extractRegex(content, "(?m)^\\s*authors\\s*=\\s*\"([^\"]+)\"");
        if (parsedAuthors != null)
        {
            for (String author : parsedAuthors.split(","))
            {
                this.authors.add(author.trim());
            }
        }

        String parsedIcon = extractRegex(content, "(?m)^\\s*logoFile\\s*=\\s*\"([^\"]+)\"");
        if (parsedIcon != null) this.iconPath = parsedIcon;
    }

    private void parseLegacyForge(JarFile jar) throws IOException
    {
        String content = readEntry(jar, "mcmod.info");
        if (content == null) return;

        JsonElement root = JsonParser.parseString(content);
        JsonObject modMeta = null;

        if (root.isJsonArray())
        {
            JsonArray array = root.getAsJsonArray();
            if (!array.isEmpty() && array.get(0).isJsonObject()) modMeta = array.get(0).getAsJsonObject();
        }
        else if (root.isJsonObject() && root.getAsJsonObject().has("modList"))
        {
            JsonArray array = root.getAsJsonObject().getAsJsonArray("modList");
            if (!array.isEmpty() && array.get(0).isJsonObject()) modMeta = array.get(0).getAsJsonObject();
        }

        if (modMeta != null)
        {
            if (modMeta.has("modid")) this.modId = modMeta.get("modid").getAsString();
            if (modMeta.has("name")) this.name = modMeta.get("name").getAsString();
            if (modMeta.has("version")) this.version = modMeta.get("version").getAsString();
            if (modMeta.has("description")) this.description = modMeta.get("description").getAsString();
            if (modMeta.has("logoFile")) this.iconPath = modMeta.get("logoFile").getAsString();

            if (modMeta.has("authorList"))
            {
                JsonArray authorArray = modMeta.getAsJsonArray("authorList");
                for (JsonElement e : authorArray)
                {
                    this.authors.add(e.getAsString());
                }
            }
        }
    }

    private String extractVersionFromManifest(JarFile jar)
    {
        try
        {
            Manifest manifest = jar.getManifest();
            if (manifest != null)
            {
                Attributes attr = manifest.getMainAttributes();
                String implVer = attr.getValue("Implementation-Version");
                if (implVer != null && !implVer.isEmpty()) return implVer;

                String specVer = attr.getValue("Specification-Version");
                if (specVer != null && !specVer.isEmpty()) return specVer;
            }
        }
        catch (IOException ignored) {}
        return null;
    }

    private void loadIcon(JarFile jar)
    {
        if (this.iconPath == null || this.iconPath.isEmpty()) return;

        String normalizedPath = this.iconPath.startsWith("/") ? this.iconPath.substring(1) : this.iconPath;
        JarEntry iconEntry = jar.getJarEntry(normalizedPath);

        if (iconEntry != null)
        {
            try (InputStream is = jar.getInputStream(iconEntry))
            {
                this.icon = ImageIO.read(is);
            }
            catch (IOException e)
            {
                System.err.println("Failed to parse icon image: " + normalizedPath);
            }
        }
    }

    private String readEntry(JarFile jar, String entryName) throws IOException
    {
        JarEntry entry = jar.getJarEntry(entryName);
        if (entry == null) return null;
        try (InputStream is = jar.getInputStream(entry))
        {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractRegex(String content, String regex)
    {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    public String getModId()
    {
        return modId;
    }

    public String getName()
    {
        return name != null ? name : "Unknown";
    }

    public String getDescription()
    {
        return description != null ? description : "";
    }

    public String getVersion()
    {
        return version != null ? version : "Unknown";
    }

    public String getLoaderType()
    {
        return loaderType;
    }

    public String getLicense()
    {
        return license;
    }

    public List<String> getAuthors()
    {
        return authors;
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