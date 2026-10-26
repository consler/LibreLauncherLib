package net.consler.librelauncherlib.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Record for easier Java version management
 * @param executable The executable path
 * @param home The Java home directory
 * @param major The major version (e.g. 8, 16, 17, 21, 25)
 * @param fullVersion The full version string
 * @param vendor The vendor name
 * @param arch The architecture
 * @param jdk Is a JDK
 */
public record JavaVersion(
        Path executable,
        Path home,
        int major,
        String fullVersion,
        String vendor,
        String arch,
        boolean jdk
) implements Comparable<JavaVersion>
{
    private static final Pattern MAJOR_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?");
    private static final Pattern PROPERTY_LINE = Pattern.compile("^\\s{4}(\\S+) = (.*)$");
    private static final Pattern REG_VALUE_LINE = Pattern.compile("^\\s+(\\S+)\\s+REG_(?:EXPAND_)?SZ\\s+(.+)$");

    public static ArrayList<JavaVersion> autoDetect()
    {
        String os = System.getProperty("os.name").toLowerCase();
        boolean windows = os.contains("win");
        boolean mac = os.contains("mac");
        String exe = windows ? "java.exe" : "java";
        String home = System.getProperty("user.home");

        Set<Path> found = new LinkedHashSet<>();

        Consumer<Path> addExe = candidate ->
        {
            try
            {
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                    found.add(candidate.toRealPath());
            }
            catch (IOException | SecurityException ignored)
            {
            }
        };

        addExe.accept(Paths.get(System.getProperty("java.home"), "bin", exe));

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank())
        {
            try
            {
                addExe.accept(Paths.get(javaHome, "bin", exe));
            }
            catch (InvalidPathException ignored)
            {
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null)
        {
            for (String dir : pathEnv.split(File.pathSeparator))
            {
                if (dir.isBlank()) continue;
                try
                {
                    addExe.accept(Paths.get(dir, exe));
                }
                catch (InvalidPathException ignored)
                {
                }
            }
        }

        if (windows)
        {
            for (Path javaHomeDir : registryJavaHomes())
                addExe.accept(javaHomeDir.resolve("bin").resolve(exe));
        }

        List<String> roots = new ArrayList<>();
        if (windows)
        {
            for (String envVar : new String[]{"ProgramFiles", "ProgramFiles(x86)"})
            {
                String pf = System.getenv(envVar);
                if (pf == null) continue;
                for (String vendor : new String[]{"Java", "Eclipse Adoptium", "Microsoft", "Zulu", "Amazon Corretto", "BellSoft", "Semeru", "Eclipse Foundation"})
                {
                    roots.add(pf + File.separator + vendor);
                }
            }
        }
        else if (mac)
        {
            roots.add("/Library/Java/JavaVirtualMachines");
            roots.add(home + "/Library/Java/JavaVirtualMachines");
            roots.add("/opt/homebrew/opt");
            roots.add("/usr/local/opt");
        }
        else
        {
            roots.add("/usr/lib/jvm");
            roots.add("/usr/java");
            roots.add("/opt/java");
            roots.add("/opt/jdk");
        }
        roots.add(home + "/.sdkman/candidates/java");
        roots.add(home + "/.jdks");

        for (String root : roots)
        {
            Path rootPath = Paths.get(root);
            if (!Files.isDirectory(rootPath)) continue;

            try (Stream<Path> children = Files.list(rootPath))
            {
                children.forEach(child ->
                {
                    addExe.accept(child.resolve("bin").resolve(exe));
                    if (mac) addExe.accept(child.resolve("Contents/Home/bin").resolve(exe));
                });
            }
            catch (IOException | SecurityException ignored)
            {
            }
        }

        ArrayList<JavaVersion> result = new ArrayList<>();
        for (Path p : found)
        {
            fromExecutable(p).ifPresent(result::add);
        }

        result.sort(Collections.reverseOrder());
        return result;
    }

    public static JavaVersion current()
    {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path candidate = Paths.get(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");

        Path exe;
        try
        {
            exe = candidate.toRealPath();
        }
        catch (IOException | SecurityException e)
        {
            exe = candidate.toAbsolutePath();
        }

        Path bin = exe.getParent();
        Path home = bin.getParent();
        Path homeParent = home.getParent();
        boolean homeIsJreFolder = home.getFileName() != null
                && home.getFileName().toString().equals("jre")
                && homeParent != null;

        boolean jdk = hasJavac(bin) || (homeIsJreFolder && hasJavac(homeParent.resolve("bin")));

        return new JavaVersion(
                exe,
                home,
                Runtime.version().feature(),
                System.getProperty("java.version"),
                System.getProperty("java.vendor", "Unknown"),
                System.getProperty("os.arch", "unknown"),
                jdk);
    }


    public static Optional<JavaVersion> fromExecutable(Path executable)
    {
        try
        {
            Path exe = executable.toRealPath();
            Path bin = exe.getParent();
            if (bin == null || bin.getParent() == null) return Optional.empty();
            Path home = bin.getParent();
            Path homeParent = home.getParent();
            boolean homeIsJreFolder = home.getFileName() != null
                    && home.getFileName().toString().equals("jre")
                    && homeParent != null;

            Map<String, String> release = readRelease(home);
            if (release.isEmpty() && homeIsJreFolder)
                release = readRelease(homeParent);

            String version = release.get("JAVA_VERSION");
            String vendor = release.get("IMPLEMENTOR");
            String arch = release.get("OS_ARCH");

            if (version == null || vendor == null || arch == null)
            {
                Map<String, String> props = queryProperties(exe);
                if (props != null)
                {
                    if (version == null) version = props.get("java.version");
                    if (vendor == null) vendor = props.get("java.vendor");
                    if (arch == null) arch = props.get("os.arch");
                }
            }

            if (version == null) return Optional.empty();

            int major = parseMajor(version);
            if (major < 0) return Optional.empty();

            if (vendor == null) vendor = "Unknown";
            if (arch == null) arch = "unknown";

            boolean jdk = hasJavac(bin) || (homeIsJreFolder && hasJavac(homeParent.resolve("bin")));

            return Optional.of(new JavaVersion(exe, home, major, version, vendor, arch, jdk));
        }
        catch (IOException | SecurityException e)
        {
            return Optional.empty();
        }
    }

    private static boolean hasJavac(Path bin)
    {
        return Files.exists(bin.resolve("javac")) || Files.exists(bin.resolve("javac.exe"));
    }

    private static int parseMajor(String version)
    {
        Matcher m = MAJOR_PATTERN.matcher(version);
        if (!m.find()) return -1;
        int first = Integer.parseInt(m.group(1));
        if (first == 1 && m.group(2) != null) return Integer.parseInt(m.group(2));
        return first;
    }

    private static Map<String, String> readRelease(Path home)
    {
        Map<String, String> map = new HashMap<>();
        Path file = home.resolve("release");
        if (!Files.isRegularFile(file)) return map;
        try
        {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8))
            {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
                    value = value.substring(1, value.length() - 1);
                map.put(key, value);
            }
        }
        catch (IOException ignored)
        {
        }
        return map;
    }

    private static Map<String, String> queryProperties(Path exe)
    {
        try
        {
            Process proc = new ProcessBuilder(exe.toString(), "-XshowSettings:properties", "-version")
                    .redirectErrorStream(true).start();

            Map<String, String> props = new HashMap<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    Matcher m = PROPERTY_LINE.matcher(line);
                    if (m.matches()) props.put(m.group(1), m.group(2).trim());
                }
            }

            if (!proc.waitFor(5, TimeUnit.SECONDS))
            {
                proc.destroyForcibly();
                return null;
            }
            return props.isEmpty() ? null : props;
        }
        catch (IOException e)
        {
            return null;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static List<Path> registryJavaHomes()
    {
        String[] keys = {
                "HKLM\\SOFTWARE\\JavaSoft",
                "HKLM\\SOFTWARE\\Eclipse Adoptium",
                "HKLM\\SOFTWARE\\Eclipse Foundation",
                "HKLM\\SOFTWARE\\Microsoft\\JDK",
                "HKLM\\SOFTWARE\\Azul Systems",
                "HKLM\\SOFTWARE\\Amazon Corretto",
                "HKLM\\SOFTWARE\\BellSoft",
                "HKLM\\SOFTWARE\\Semeru",
                "HKLM\\SOFTWARE\\IBM"
        };
        Set<String> valueNames = Set.of("JavaHome", "Path", "InstallationPath", "InstallDir");

        Set<Path> homes = new LinkedHashSet<>();
        for (String view : new String[]{"/reg:64", "/reg:32"})
        {
            for (String key : keys)
            {
                try
                {
                    Process proc = new ProcessBuilder("reg", "query", key, "/s", view)
                            .redirectErrorStream(true).start();

                    try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset())))
                    {
                        String line;
                        while ((line = r.readLine()) != null)
                        {
                            Matcher m = REG_VALUE_LINE.matcher(line);
                            if (!m.matches() || !valueNames.contains(m.group(1))) continue;
                            try
                            {
                                Path dir = Paths.get(m.group(2).trim());
                                if (Files.isRegularFile(dir.resolve("bin").resolve("java.exe")))
                                    homes.add(dir);
                            }
                            catch (InvalidPathException ignored)
                            {
                            }
                        }
                    }
                    if (!proc.waitFor(5, TimeUnit.SECONDS)) proc.destroyForcibly();
                }
                catch (IOException | SecurityException ignored)
                {
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>(homes);
                }
            }
        }
        return new ArrayList<>(homes);
    }

    @Override
    public int compareTo(JavaVersion other)
    {
        return Integer.compare(this.major, other.major);
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof JavaVersion other && executable.equals(other.executable);
    }

    @Override
    public int hashCode()
    {
        return executable.hashCode();
    }

    @Override
    public String toString()
    {
        return "Java " + major + " (" + fullVersion + ", " + vendor + ", " + arch + (jdk ? ", JDK" : ", JRE") + ") @ " + executable;
    }
}