package net.consler.librelauncherlib.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
    private static final String[] FALLBACK_ICONS = { "pack.png", "logo.png", "icon.png" };

    private String modId = "unknown";
    private String name;
    private String description = "No description provided.";
    private String version = "Unknown";
    private String loaderType = "Unknown";
    private String license = "Unknown";
    private final List<String> authors = new ArrayList<>();
    private String iconPath = null;
    private BufferedImage icon = null;
    private final File modFile;

    public Mod(Path modPath)
    {
        this(modPath.toFile());
    }

    public Mod(File modFile)
    {
        this.modFile = modFile;
        this.name = modFile.getName();

        try (JarFile jar = new JarFile(modFile))
        {
            try
            {
                parseMetadata(jar);
            }
            catch (Exception ignored)
            {
            }

            if (!isUsable(this.version) || this.version.equals("Unknown"))
            {
                String manifestVersion = extractVersionFromManifest(jar);
                if (manifestVersion != null)
                {
                    this.version = manifestVersion;
                }
            }
            if (!isUsable(this.version))
            {
                this.version = "Unknown";
            }

            loadIcon(jar);
        }
        catch (Exception e)
        {
            throw new ModReadException("Failed to read mod file: " + modFile.getName(), e);
        }
    }

    private void parseMetadata(JarFile jar) throws IOException
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
    }

    private void parseFabricOrQuilt(JarFile jar, String entryName, boolean isQuilt) throws IOException
    {
        String content = readEntry(jar, entryName);
        if (content == null) return;

        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        JsonObject idSource = root;
        JsonObject meta = root;

        if (isQuilt)
        {
            JsonObject loader = getObject(root, "quilt_loader");
            idSource = loader != null ? loader : new JsonObject();
            JsonObject metadata = getObject(idSource, "metadata");
            meta = metadata != null ? metadata : new JsonObject();
        }

        String value;
        if ((value = getString(idSource, "id")) != null) this.modId = value;
        if ((value = getString(idSource, "version")) != null) this.version = value;
        if ((value = getString(meta, "name")) != null) this.name = value;
        if ((value = getString(meta, "description")) != null) this.description = value;
        if ((value = getLicense(meta.get("license"))) != null) this.license = value;

        JsonElement authElement = meta.get("authors");
        if (authElement != null && authElement.isJsonArray())
        {
            for (JsonElement e : authElement.getAsJsonArray())
            {
                if (e.isJsonPrimitive()) this.authors.add(e.getAsString());
                else if (e.isJsonObject() && getString(e.getAsJsonObject(), "name") != null)
                {
                    this.authors.add(getString(e.getAsJsonObject(), "name"));
                }
            }
        }

        JsonObject contributors = getObject(meta, "contributors");
        if (contributors != null)
        {
            this.authors.addAll(contributors.keySet());
        }

        JsonElement iconElement = meta.get("icon");
        if (iconElement != null)
        {
            if (iconElement.isJsonPrimitive())
            {
                this.iconPath = iconElement.getAsString();
            }
            else if (iconElement.isJsonObject())
            {
                this.iconPath = getLargestIcon(iconElement.getAsJsonObject());
            }
        }
    }

    private static String getLargestIcon(JsonObject icons)
    {
        String best = null;
        int bestSize = -1;

        for (Map.Entry<String, JsonElement> entry : icons.entrySet())
        {
            if (!entry.getValue().isJsonPrimitive()) continue;

            int size;
            try
            {
                size = Integer.parseInt(entry.getKey());
            }
            catch (NumberFormatException e)
            {
                size = 0;
            }

            if (size > bestSize)
            {
                bestSize = size;
                best = entry.getValue().getAsString();
            }
        }
        return best;
    }

    private static String getLicense(JsonElement el)
    {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) return el.getAsString();

        if (el.isJsonObject())
        {
            JsonObject o = el.getAsJsonObject();
            String id = getString(o, "id");
            return id != null ? id : getString(o, "name");
        }

        if (el.isJsonArray() && !el.getAsJsonArray().isEmpty())
        {
            return getLicense(el.getAsJsonArray().get(0));
        }
        return null;
    }

    private void parseForge(JarFile jar, String entryName) throws IOException
    {
        String content = readEntry(jar, entryName);
        if (content == null) return;

        String block = firstModsBlock(content);

        String value;
        if ((value = tomlString(block, "modId")) != null) this.modId = value;
        if ((value = tomlString(block, "displayName")) != null) this.name = value;
        if ((value = tomlString(block, "version")) != null) this.version = value;
        if ((value = tomlString(block, "description")) != null) this.description = value;
        if ((value = tomlString(content, "license")) != null) this.license = value; // top-level key

        if ((value = tomlString(block, "authors")) != null)
        {
            for (String author : value.split(","))
            {
                String trimmed = author.trim();
                if (!trimmed.isEmpty()) this.authors.add(trimmed);
            }
        }

        if ((value = tomlString(block, "logoFile")) != null) this.iconPath = value;
    }

    private static String firstModsBlock(String toml)
    {
        Matcher start = Pattern.compile("(?m)^[ \\t]*\\[\\[[ \\t]*mods[ \\t]*]]").matcher(toml);
        if (!start.find()) return toml;

        int from = start.end();
        Matcher next = Pattern.compile("(?m)^[ \\t]*\\[").matcher(toml);
        return next.find(from) ? toml.substring(from, next.start()) : toml.substring(from);
    }

    private static String tomlString(String content, String key)
    {
        String prefix = "(?m)^[ \\t]*" + Pattern.quote(key) + "[ \\t]*=[ \\t]*";
        String[] patterns = {
                prefix + "'''(.*?)'''",
                prefix + "\"\"\"(.*?)\"\"\"",
                prefix + "\"((?:[^\"\\\\\\r\\n]|\\\\.)*)\"",
                prefix + "'([^'\\r\\n]*)'"
        };
        boolean[] basic = { false, true, true, false };

        for (int i = 0; i < patterns.length; i++)
        {
            Matcher matcher = Pattern.compile(patterns[i], Pattern.DOTALL).matcher(content);
            if (matcher.find())
            {
                String value = matcher.group(1).trim();
                if (basic[i]) value = unescapeToml(value);
                if (!value.isEmpty()) return value;
            }
        }
        return null;
    }

    private static String unescapeToml(String s)
    {
        if (s.indexOf('\\') < 0) return s;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length())
            {
                char n = s.charAt(++i);
                switch (n)
                {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c).append(n);
                }
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void parseLegacyForge(JarFile jar) throws IOException
    {
        String content = readEntry(jar, "mcmod.info");
        if (content == null) return;

        JsonElement root = JsonParser.parseString(content);
        JsonObject modMeta = null;

        if (root.isJsonArray())
        {
            modMeta = firstObject(root.getAsJsonArray());
        }
        else if (root.isJsonObject())
        {
            JsonElement list = root.getAsJsonObject().get("modList");
            if (list != null && list.isJsonArray()) modMeta = firstObject(list.getAsJsonArray());
            else modMeta = root.getAsJsonObject();
        }

        if (modMeta == null) return;

        String value;
        if ((value = getString(modMeta, "modid")) != null) this.modId = value;
        if ((value = getString(modMeta, "name")) != null) this.name = value;
        if ((value = getString(modMeta, "version")) != null) this.version = value;
        if ((value = getString(modMeta, "description")) != null) this.description = value;
        if ((value = getString(modMeta, "logoFile")) != null) this.iconPath = value;

        for (String key : new String[] { "authorList", "authors" })
        {
            JsonElement el = modMeta.get(key);
            if (el != null && el.isJsonArray())
            {
                for (JsonElement e : el.getAsJsonArray())
                {
                    if (e.isJsonPrimitive()) this.authors.add(e.getAsString());
                }
                break;
            }
        }
    }

    private static JsonObject firstObject(JsonArray array)
    {
        if (!array.isEmpty() && array.get(0).isJsonObject()) return array.get(0).getAsJsonObject();
        return null;
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
                if (isUsable(implVer)) return implVer;

                String specVer = attr.getValue("Specification-Version");
                if (isUsable(specVer)) return specVer;
            }
        }
        catch (IOException ignored) {}
        return null;
    }

    private void loadIcon(JarFile jar)
    {
        if (this.iconPath != null && !this.iconPath.isBlank())
        {
            BufferedImage image = readImage(jar, findEntry(jar, this.iconPath, true));
            if (image != null)
            {
                this.icon = image;
                return;
            }
        }

        for (String fallback : FALLBACK_ICONS)
        {
            BufferedImage image = readImage(jar, findEntry(jar, fallback, false));
            if (image != null)
            {
                this.icon = image;
                return;
            }
        }
    }

    private static JarEntry findEntry(JarFile jar, String path, boolean ignoreCase)
    {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./") || normalized.startsWith("/"))
        {
            normalized = normalized.startsWith("./") ? normalized.substring(2) : normalized.substring(1);
        }
        if (normalized.isEmpty()) return null;

        JarEntry exact = jar.getJarEntry(normalized);
        if (exact != null && !exact.isDirectory()) return exact;

        if (ignoreCase)
        {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().equalsIgnoreCase(normalized)) return entry;
            }
        }
        return null;
    }

    private static BufferedImage readImage(JarFile jar, JarEntry entry)
    {
        if (entry == null) return null;

        try (InputStream is = jar.getInputStream(entry))
        {
            return ImageIO.read(is);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String readEntry(JarFile jar, String entryName) throws IOException
    {
        JarEntry entry = jar.getJarEntry(entryName);
        if (entry == null) return null;
        try (InputStream is = jar.getInputStream(entry))
        {
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text; // strip BOM
        }
    }

    private static String getString(JsonObject obj, String key)
    {
        if (obj == null) return null;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return null;
        String s = el.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private static JsonObject getObject(JsonObject obj, String key)
    {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static boolean isUsable(String value)
    {
        return value != null && !value.isBlank() && !value.contains("${");
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

    public File getModFile()
    {
        return modFile;
    }
}