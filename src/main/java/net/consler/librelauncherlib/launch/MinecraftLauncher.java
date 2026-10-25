package net.consler.librelauncherlib.launch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.instance.VersionManifest;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for launching Minecraft instances.
 * <p>
 * This is the public entry point. It checks the instance on disk, loads the version JSON and loader profile into a
 * {@link VersionManifest}, and lets a {@link LaunchCommandBuilder} turn that into a command line. The merged state is
 * built once, in the constructor, so {@link #launch()} and {@link #buildCommand()} can be called any number of times.
 */
public class MinecraftLauncher
{
    private static final Pattern JAVA_VERSION = Pattern.compile("version \"([^\"]+)\"");

    public LaunchProfile launchProfile;
    public AuthProfile authProfile;
    public ModloaderProfile modloaderProfile;
    public Path gameDir;
    public String version;
    public Path versionJsonPath;
    public Path nativesDir;
    public Path assetsDir;
    public Path librariesDir;
    public JsonObject versionDetails;
    public String assetIndex;
    /** Vanilla and loader libraries, filtered by OS rules and de-duplicated (loader entries win). */
    public JsonArray allLibraries;
    public String loaderId;
    public Path loaderJsonPath;
    public JsonObject loaderDetails;
    public String mainClass;
    public String jarVersion;
    /** Minimum Java major version the manifest asks for. 8 when the manifest does not say. */
    public int requiredJavaMajor;

    private final Path logsDir;
    private final LaunchCommandBuilder commandBuilder;

    /**
     * Creates a MinecraftLauncher instance
     * @param launchProfile the launch profile to use
     * @param authProfile the authentication profile to use
     * @param modloaderProfile the modloader profile to use
     * @throws LaunchException when the instance is corrupted
     * @throws IOException when failed an I/O function
     * @throws CorruptedLaunchException when the instance is corrupted
     */
    public MinecraftLauncher(LaunchProfile launchProfile, AuthProfile authProfile, ModloaderProfile modloaderProfile)
            throws LaunchException, IOException, CorruptedLaunchException
    {
        if (launchProfile == null) throw new LaunchException("Launch profile was not specified.");
        if (authProfile == null) throw new LaunchException("Auth profile was not specified.");
        if (modloaderProfile == null) throw new LaunchException("Modloader profile was not specified.");

        this.launchProfile = launchProfile;
        this.authProfile = authProfile;
        this.modloaderProfile = modloaderProfile;

        gameDir = launchProfile.gameDir();
        if(gameDir == null) throw new CorruptedLaunchException("Game directory was not specified.");

        version = launchProfile.version();
        if(version == null) throw new CorruptedLaunchException("Version was not specified.");

        versionJsonPath = resolveFile(gameDir, version, ".json");
        if (!Files.exists(versionJsonPath)) throw new CorruptedLaunchException("Version JSON missing: " + versionJsonPath);

        nativesDir = gameDir.resolve("natives");

        assetsDir = gameDir.resolve("assets");
        if(!Files.exists(assetsDir)) throw new CorruptedLaunchException("Assets directory missing.");

        librariesDir = gameDir.resolve("libraries");
        if(!Files.exists(librariesDir)) throw new CorruptedLaunchException("Libraries directory missing.");

        logsDir = gameDir.resolve("logs");

        versionDetails = VersionManifest.readJson(versionJsonPath);

        loaderId = modloaderProfile.loaderId();
        if (loaderId == null) loaderId = "vanilla";

        loaderJsonPath = loaderId.equals("vanilla") ?
                null :
                gameDir.resolve(loaderId + "-profile.json");

        if (loaderJsonPath != null && !Files.exists(loaderJsonPath))
            throw new CorruptedLaunchException("Loader profile missing for " + loaderId + " (" + loaderJsonPath + "). Run the installer first.");

        loaderDetails = loaderJsonPath == null ?
                null :
                VersionManifest.readJson(loaderJsonPath);

        VersionManifest manifest = new VersionManifest(version, versionDetails, loaderDetails, loaderId,
                VersionManifest.featuresOf(launchProfile), SystemHelper.getOS(), launchProfile.followOsRules(),
                launchProfile.followArchRules(), launchProfile.followOsVersionRules());

        assetIndex = manifest.assetIndex();
        mainClass = manifest.mainClass();
        jarVersion = manifest.jarVersion();
        requiredJavaMajor = manifest.requiredJavaMajor();
        allLibraries = manifest.libraries();

        commandBuilder = new LaunchCommandBuilder(launchProfile, authProfile, manifest,
                gameDir, nativesDir, assetsDir, librariesDir, logsDir);
    }

    /**
     * Launches a Minecraft game process for the supplied profile.
     *
     * @return the started game process
     */
    public Process launch() throws IOException
    {
        return buildProcess().start();
    }

    /**
     * Prepares the game process (command, working directory, environment, output redirect) without starting it.
     *
     * @return the configured process builder
     */
    public ProcessBuilder buildProcess() throws IOException
    {
        List<String> command = buildCommand();

        Path outputLog = launchProfile.outputLog() != null ?
                launchProfile.outputLog() :
                logsDir.resolve("launcher-output.log");

        Path outputParent = outputLog.toAbsolutePath().getParent();
        if (outputParent != null) Files.createDirectories(outputParent);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir.toAbsolutePath().toFile());
        pb.environment().putAll(launchProfile.environment());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(outputLog.toFile()));
        if(launchProfile.linkProcess()) pb.redirectInput(ProcessBuilder.Redirect.DISCARD);
        return pb;
    }

    /**
     * Builds the full command line without starting anything.
     *
     * @return the command, starting with the wrapper command (if any) and the Java executable
     * @throws FileNotFoundException when Java, the client JAR or any required library is missing
     * @throws IOException when the selected Java is older than the version needs, or another I/O step fails
     */
    public List<String> buildCommand() throws IOException
    {
        Path javaPath = launchProfile.javaPath();
        if (javaPath == null || !Files.isRegularFile(javaPath))
            throw new FileNotFoundException("Java executable not found: " + javaPath);

        int javaMajor = detectJavaMajor(javaPath);
        if (launchProfile.checkJavaVersion() && javaMajor > 0 && javaMajor < requiredJavaMajor)
            throw new IOException("Minecraft " + version + " needs Java " + requiredJavaMajor + " or newer, but " + javaPath + " is Java " + javaMajor + ".");

        Path clientJarPath = resolveFile(gameDir, jarVersion, ".jar");
        if (!Files.exists(clientJarPath)) throw new FileNotFoundException("Minecraft client JAR missing for version " + jarVersion + " at resolved path.");

        return commandBuilder.build(javaPath, javaMajor, clientJarPath);
    }

    /**
     * Finds the major version of a Java executable by running {@code java -version}.
     *
     * @return the major version (8, 16, 17, 21, 25...), or -1 when it could not be determined
     */
    public static int detectJavaMajor(Path javaExecutable)
    {
        try
        {
            Process p = new ProcessBuilder(javaExecutable.toAbsolutePath().toString(), "-version").redirectErrorStream(true).start();

            String output;
            try (InputStream in = p.getInputStream())
            {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!p.waitFor(10, TimeUnit.SECONDS))
            {
                p.destroyForcibly();
                return -1;
            }

            Matcher m = JAVA_VERSION.matcher(output);
            if (!m.find()) return -1;

            String v = m.group(1);
            if (v.startsWith("1.")) v = v.substring(2);
            int end = 0;
            while (end < v.length() && Character.isDigit(v.charAt(end))) end++;
            return end == 0 ? -1 : Integer.parseInt(v.substring(0, end));
        }
        catch (IOException e)
        {
            return -1;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private Path resolveFile(Path gameDir, String version, String extension)
    {
        Path mojangPath = gameDir.resolve("versions").resolve(version).resolve(version + extension);
        if (Files.exists(mojangPath)) return mojangPath;

        Path flatPath = gameDir.resolve(version + extension);
        if (Files.exists(flatPath)) return flatPath;

        return mojangPath;
    }
}