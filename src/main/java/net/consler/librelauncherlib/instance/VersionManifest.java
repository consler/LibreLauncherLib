package net.consler.librelauncherlib.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.consler.librelauncherlib.launch.LaunchException;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.util.DownloadTask;
import net.consler.librelauncherlib.util.MavenHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static net.consler.librelauncherlib.util.SystemHelper.getArchitecture;
import static net.consler.librelauncherlib.util.SystemHelper.getOS;

/**
 * Version JSON
 */
public final class VersionManifest
{
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<String> KNOWN_ARCHES = Set.of("x86", "x86_64", "arm64");
    private static final String DEFAULT_LIBRARY_URL = "https://libraries.minecraft.net/";

    /**
     * A library jar the classpath needs.
     *
     * @param path path below the libraries directory
     * @param serverOnly old Forge profiles flag server-only libraries ("clientreq": false), which may be absent
     */
    public record ClasspathEntry(String path, boolean serverOnly) {}

    /**
     * A library file that has to be downloaded.
     *
     * @param path path below the libraries directory
     * @param url where to download the file from
     * @param sha1 the expected SHA-1 hash, or null if unknown
     * @param size the expected size in bytes, or -1 if unknown
     * @param nativeArchive whether the file holds native libraries that have to be extracted
     */
    public record LibraryFile(String path, String url, String sha1, long size, boolean nativeArchive)
    {
        /**
         * Converts this file to a download task.
         *
         * @param librariesDir the libraries directory the path is relative to
         */
        public DownloadTask toDownloadTask(Path librariesDir)
        {
            return new DownloadTask(url, librariesDir.resolve(path), sha1, size);
        }
    }

    private final JsonObject versionDetails;
    private final JsonObject loaderDetails;
    private final Set<String> enabledFeatures;
    private final String osName;

    private final String assetIndex;
    private final String mainClass;
    private final String jarVersion;
    private final int requiredJavaMajor;
    private final JsonArray libraries;
    private final List<JsonElement> jvmTemplates = new ArrayList<>();
    private final List<JsonElement> gameTemplates = new ArrayList<>();

    /**
     * @param version the version id the launch profile asked for
     * @param versionDetails the parsed vanilla version JSON
     * @param loaderDetails the parsed loader profile, or null for vanilla
     * @param loaderId the loader id, used in error messages
     * @param enabledFeatures the launcher features "features" rules are checked against
     * @param osName {@code windows}, {@code osx} or {@code linux}
     * @param followOsRules whether to follow OS-specific rules
     * @param followArchRules whether to follow architecture-specific rules
     * @param followOsVersionRules whether to follow OS version-specific rules
     * @throws LaunchException when neither JSON names a main class
     */
    public VersionManifest(String version, JsonObject versionDetails, JsonObject loaderDetails, String loaderId,
                           Set<String> enabledFeatures, String osName, boolean followOsRules,
                           boolean followArchRules, boolean followOsVersionRules) throws LaunchException
    {
        this.versionDetails = versionDetails;
        this.loaderDetails = loaderDetails;
        this.enabledFeatures = enabledFeatures;
        this.osName = osName;

        String assetsKey = string(versionDetails, "assets");
        String indexId = string(obj(versionDetails, "assetIndex"), "id");
        assetIndex = indexId != null ? indexId : assetsKey != null ? assetsKey : "legacy";

        String loaderMain = string(loaderDetails, "mainClass");
        mainClass = loaderMain != null ? loaderMain : string(versionDetails, "mainClass");
        if (mainClass == null) throw new LaunchException("No mainClass found in the version JSON or the " + loaderId + " profile.");

        String inherits = string(versionDetails, "inheritsFrom");
        String loaderJar = string(loaderDetails, "jar");
        String vanillaJar = string(versionDetails, "jar");
        jarVersion = loaderJar != null ?
                loaderJar :
                vanillaJar != null ?
                        vanillaJar :
                        inherits != null ?
                                inherits :
                                version;

        int javaMajor = Math.max(javaMajorOf(versionDetails), loaderDetails == null ? 0 : javaMajorOf(loaderDetails));
        requiredJavaMajor = javaMajor <= 0 ? 8 : javaMajor;

        libraries = mergeLibraries();
        collectArguments(versionDetails);
        if (loaderDetails != null)
        {
            if (loaderDetails.has("minecraftArguments") && !hasGameArguments(loaderDetails)) gameTemplates.clear();
            collectArguments(loaderDetails);
        }
    }

    /**
     * Creates a manifest for installing files instead of launching the game. Uses this system's OS and enables no launcher features.
     *
     * @param version the Minecraft version
     * @param versionDetails the parsed vanilla version JSON, or null if only the libraries of a loader profile are needed
     * @param loaderDetails the parsed loader profile, or null for vanilla
     * @param loaderId the loader id, used in error messages
     * @throws LaunchException when neither JSON names a main class
     */
    public static VersionManifest forInstall(String version, JsonObject versionDetails, JsonObject loaderDetails, String loaderId) throws LaunchException
    {
        return new VersionManifest(version, versionDetails != null ? versionDetails : new JsonObject(), loaderDetails, loaderId, Set.of(), getOS(), true, true, true);
    }

    public JsonObject versionDetails()
    {
        return versionDetails;
    }

    public JsonObject loaderDetails()
    {
        return loaderDetails;
    }

    public String assetIndex()
    {
        return assetIndex;
    }

    public String mainClass()
    {
        return mainClass;
    }

    public String jarVersion()
    {
        return jarVersion;
    }

    public int requiredJavaMajor()
    {
        return requiredJavaMajor;
    }

    public String versionType()
    {
        String type = string(versionDetails, "type");
        return type != null ? type : "release";
    }

    public String loggingArgument()
    {
        return string(obj(obj(versionDetails, "logging"), "client"), "argument");
    }

    public String loggingConfigId()
    {
        return string(obj(obj(obj(versionDetails, "logging"), "client"), "file"), "id");
    }

    /**
     * Returns the download entry (id, url, sha1, size) of the client's logging config, or null if the version has none.
     */
    public JsonObject loggingFile()
    {
        return obj(obj(obj(versionDetails, "logging"), "client"), "file");
    }

    public Path gameAssets(Path gameDir, Path assetsDir)
    {
        Path index = assetsDir.resolve("indexes").resolve(assetIndex + ".json");
        if (Files.exists(index))
        {
            try
            {
                JsonObject parsed = readJson(index);
                if (bool(parsed, "map_to_resources")) return gameDir.resolve("resources");
                if (bool(parsed, "virtual")) return assetsDir.resolve("virtual").resolve(assetIndex);
            }
            catch (IOException | RuntimeException ignored)
            {
            }
        }
        return assetsDir;
    }

    public JsonArray libraries()
    {
        return libraries;
    }

    public List<ClasspathEntry> classpathEntries()
    {
        List<ClasspathEntry> entries = new ArrayList<>();
        for (JsonElement elem : libraries)
        {
            JsonObject lib = elem.getAsJsonObject();

            String path = libraryPath(lib);
            if (path != null) entries.add(new ClasspathEntry(path, isServerOnly(lib)));
        }
        return entries;
    }

    /**
     * Returns every file the allowed, client-side libraries need. Works for vanilla libraries (with a "downloads" section)
     * as well as for loader profiles, which only list maven coordinates and an optional repository "url".
     * Libraries without a download URL are skipped, since something else (e.g. the Forge installer) provides them.
     */
    public List<LibraryFile> libraryFiles()
    {
        List<LibraryFile> files = new ArrayList<>();
        for (JsonElement elem : libraries)
        {
            JsonObject lib = elem.getAsJsonObject();
            if (isServerOnly(lib)) continue;

            JsonObject downloads = obj(lib, "downloads");
            JsonObject artifact = obj(downloads, "artifact");

            if (artifact != null)
            {
                // 1.19+ ships natives as separate libraries with a "natives-" classifier in their name
                addFile(files, string(artifact, "path"), artifact, isNativeClassifier(lib));
            }
            else
            {
                String path = libraryPath(lib);
                if (path != null)
                {
                    String base = string(lib, "url");
                    if (base == null || base.isEmpty()) base = DEFAULT_LIBRARY_URL;
                    if (!base.endsWith("/")) base += "/";
                    files.add(new LibraryFile(path, base + path, string(lib, "sha1"), sizeOf(lib), false));
                }
            }

            // 1.18- lists natives as classifiers of the library
            String classifier = string(obj(lib, "natives"), osName);
            if (classifier != null)
            {
                String resolved = classifier.replace("${arch}", getArchitecture().contains("64") ? "64" : "32");
                JsonObject nativeArtifact = obj(obj(downloads, "classifiers"), resolved);
                if (nativeArtifact != null) addFile(files, string(nativeArtifact, "path"), nativeArtifact, true);
            }
        }
        return files;
    }

    /**
     * Returns download tasks for {@link #libraryFiles()}.
     *
     * @param librariesDir the libraries directory the files are saved to
     */
    public List<DownloadTask> libraryTasks(Path librariesDir)
    {
        List<DownloadTask> tasks = new ArrayList<>();
        for (LibraryFile file : libraryFiles()) tasks.add(file.toDownloadTask(librariesDir));
        return tasks;
    }

    private static void addFile(List<LibraryFile> files, String path, JsonObject download, boolean nativeArchive)
    {
        String url = string(download, "url");
        if (path == null || url == null || url.isEmpty()) return;

        files.add(new LibraryFile(path, url, string(download, "sha1"), sizeOf(download), nativeArchive));
    }

    private static long sizeOf(JsonObject o)
    {
        Integer size = integer(o, "size");
        return size == null ? -1 : size;
    }

    private static boolean isNativeClassifier(JsonObject lib)
    {
        String name = string(lib, "name");
        if (name == null) return false;

        String[] parts = name.split(":");
        return parts.length > 3 && parts[3].startsWith("native");
    }

    private JsonArray mergeLibraries()
    {
        Map<String, JsonObject> merged = new LinkedHashMap<>();
        int anonymous = 0;

        for (JsonObject source : new JsonObject[] { versionDetails, loaderDetails })
        {
            JsonArray entries = arr(source, "libraries");
            if (entries == null) continue;

            for (JsonElement elem : entries)
            {
                if (!elem.isJsonObject()) continue;
                JsonObject lib = elem.getAsJsonObject();

                if (lib.has("rules") && !isAllowed(arr(lib, "rules"))) continue;

                String key = libraryKey(string(lib, "name"));
                if (key == null) key = "#" + (anonymous++);
                merged.put(key, lib);
            }
        }

        JsonArray result = new JsonArray();
        for (JsonObject lib : merged.values()) result.add(lib);
        return result;
    }

    public static String libraryKey(String name)
    {
        if (name == null) return null;

        String ext = "";
        int at = name.indexOf('@');
        if (at >= 0)
        {
            ext = name.substring(at);
            name = name.substring(0, at);
        }

        String[] parts = name.split(":");
        if (parts.length < 3) return name + ext;

        StringBuilder key = new StringBuilder(parts[0]).append(':').append(parts[1]);
        if (parts.length > 3) key.append(':').append(parts[3]);
        return key.append(ext).toString();
    }

    private static String libraryPath(JsonObject lib)
    {
        JsonObject downloads = obj(lib, "downloads");
        JsonObject artifact = obj(downloads, "artifact");
        String artifactPath = string(artifact, "path");
        if (artifactPath != null) return artifactPath;

        if (lib.has("natives") || (downloads != null && downloads.has("classifiers"))) return null;

        String name = string(lib, "name");
        if (name == null) return null;

        try
        {
            return MavenHelper.toJarPath(name);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    private static boolean isServerOnly(JsonObject lib)
    {
        return lib.has("clientreq") && !bool(lib, "clientreq");
    }

    public List<String> jvmArguments(Map<String, String> placeholders)
    {
        return resolve(jvmTemplates, placeholders);
    }

    public List<String> gameArguments(Map<String, String> placeholders)
    {
        return resolve(gameTemplates, placeholders);
    }

    private void collectArguments(JsonObject details)
    {
        JsonObject arguments = obj(details, "arguments");
        if (arguments != null)
        {
            addTemplates(jvmTemplates, arr(arguments, "jvm"));
            addTemplates(gameTemplates, arr(arguments, "game"));
        }

        String legacy = string(details, "minecraftArguments");
        if (legacy != null && !hasGameArguments(details))
        {
            for (String token : legacy.trim().split("\\s+"))
            {
                if (!token.isEmpty()) gameTemplates.add(new JsonPrimitive(token));
            }
        }
    }

    private static boolean hasGameArguments(JsonObject details)
    {
        return arr(obj(details, "arguments"), "game") != null;
    }

    private static void addTemplates(List<JsonElement> target, JsonArray source)
    {
        if (source == null) return;
        for (JsonElement elem : source) target.add(elem);
    }

    private static int javaMajorOf(JsonObject details)
    {
        Integer major = integer(obj(details, "javaVersion"), "majorVersion");
        return major == null ? 0 : major;
    }

    private List<String> resolve(List<JsonElement> templates, Map<String, String> placeholders)
    {
        List<String> out = new ArrayList<>();

        for (JsonElement elem : templates)
        {
            if (elem.isJsonPrimitive())
            {
                addResolved(out, elem.getAsString(), placeholders);
            }
            else if (elem.isJsonObject())
            {
                JsonObject argObj = elem.getAsJsonObject();
                if (argObj.has("rules") && !isAllowed(arr(argObj, "rules"))) continue;

                JsonElement value = argObj.get("value");
                if (value == null) continue;

                if (value.isJsonArray())
                {
                    for (JsonElement v : value.getAsJsonArray()) addResolved(out, v.getAsString(), placeholders);
                }
                else
                {
                    addResolved(out, value.getAsString(), placeholders);
                }
            }
        }
        return out;
    }

    private static void addResolved(List<String> out, String template, Map<String, String> placeholders)
    {
        String value = replacePlaceholders(template, placeholders);
        if (value != null)
        {
            out.add(value);
            return;
        }

        if (!out.isEmpty() && !template.startsWith("-"))
        {
            String previous = out.getLast();
            if (previous.startsWith("-") && !previous.contains("=")) out.removeLast();
        }
    }

    private static String replacePlaceholders(String template, Map<String, String> placeholders)
    {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find())
        {
            String value = placeholders.get(m.group(1));
            if (value == null) return null;
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Set<String> featuresOf(LaunchProfile profile)
    {
        Set<String> features = new HashSet<>();
        if (profile.hasCustomResolution()) features.add("has_custom_resolution");
        if (profile.demo()) features.add("is_demo_user");

        LaunchProfile.QuickPlay quickPlay = profile.quickPlay();
        if (quickPlay != null)
        {
            features.add("has_quick_plays_support");
            features.add(switch (quickPlay.type())
            {
                case SINGLEPLAYER -> "is_quick_play_singleplayer";
                case MULTIPLAYER -> "is_quick_play_multiplayer";
                case REALMS -> "is_quick_play_realms";
            });
        }
        return Set.copyOf(features);
    }


    private boolean isAllowed(JsonArray rules)
    {
        if (rules == null || rules.isEmpty()) return true;

        boolean allowed = false;

        for (JsonElement elem : rules)
        {
            JsonObject rule = elem.getAsJsonObject();
            String action = string(rule, "action");
            boolean match = true;

            JsonObject os = obj(rule, "os");
            if (os != null)
            {
                String name = string(os, "name");
                if (name != null) match = name.equalsIgnoreCase(osName);

                String arch = string(os, "arch");
                if (arch != null) match = match && normalizeArch(arch).equals(currentArch());

                String osVersion = string(os, "version");
                if (osVersion != null) match = match && osVersionMatches(osVersion);

                JsonObject range = obj(os, "versionRange");
                if (range != null) match = match && osVersionInRange(range);
            }

            JsonObject features = obj(rule, "features");
            if (features != null)
            {
                for (Map.Entry<String, JsonElement> feature : features.entrySet())
                {
                    JsonElement wanted = feature.getValue();
                    boolean required = wanted.isJsonPrimitive() && wanted.getAsBoolean();
                    if (enabledFeatures.contains(feature.getKey()) != required) match = false;
                }
            }

            if (match) allowed = "allow".equals(action);
        }
        return allowed;
    }

    static String normalizeArch(String arch)
    {
        String a = arch.trim().toLowerCase(Locale.ROOT);
        return switch (a)
        {
            case "x86", "i386", "i486", "i586", "i686" -> "x86";
            case "x86_64", "x86-64", "amd64", "x64" -> "x86_64";
            case "arm64", "aarch64" -> "arm64";
            default -> a;
        };
    }

    private static String currentArch()
    {
        String arch = null;
        try
        {
            arch = getArchitecture();
        }
        catch (RuntimeException ignored)
        {
        }

        String normalized = arch == null ? "" : normalizeArch(arch);
        if (!KNOWN_ARCHES.contains(normalized)) normalized = normalizeArch(System.getProperty("os.arch", ""));
        return normalized;
    }

    private static boolean osVersionMatches(String regex)
    {
        try
        {
            return Pattern.compile(regex).matcher(System.getProperty("os.version", "")).find();
        }
        catch (PatternSyntaxException e)
        {
            return false;
        }
    }

    private static boolean osVersionInRange(JsonObject range)
    {
        int[] current = parseVersion(System.getProperty("os.version", ""));
        String min = string(range, "min");
        String max = string(range, "max");
        if (min != null && compareVersions(current, parseVersion(min)) < 0) return false;
        return max == null || compareVersions(current, parseVersion(max)) < 0;
    }

    private static int[] parseVersion(String v)
    {
        String[] parts = v.split("[^0-9]+");
        List<Integer> numbers = new ArrayList<>();
        for (String p : parts)
        {
            if (p.isEmpty()) continue;
            try
            {
                numbers.add(Integer.parseInt(p));
            }
            catch (NumberFormatException e)
            {
                numbers.add(0);
            }
        }
        return numbers.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int compareVersions(int[] a, int[] b)
    {
        for (int i = 0; i < Math.max(a.length, b.length); i++)
        {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    public static JsonObject readJson(Path path) throws IOException
    {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static JsonObject obj(JsonObject o, String key)
    {
        if (o == null || !o.has(key)) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray arr(JsonObject o, String key)
    {
        if (o == null || !o.has(key)) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static String string(JsonObject o, String key)
    {
        if (o == null || !o.has(key)) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static Integer integer(JsonObject o, String key)
    {
        String s = string(o, key);
        if (s == null) return null;
        try
        {
            return Integer.valueOf(s.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static boolean bool(JsonObject o, String key)
    {
        String s = string(o, key);
        return s != null && Boolean.parseBoolean(s.trim());
    }
}