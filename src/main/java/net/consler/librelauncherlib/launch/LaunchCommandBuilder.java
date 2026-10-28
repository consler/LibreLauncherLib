package net.consler.librelauncherlib.launch;

import net.consler.librelauncherlib.auth.AuthProfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns resolved launch metadata plus the launch and auth profiles into the command line that starts the game.
 */
final class LaunchCommandBuilder
{
    private static final long INITIAL_HEAP_MB = 512;

    private static final int ARGFILE_THRESHOLD = 20_000;

    private static final String LOG_CONFIGS_DIR = "log_configs";

    private static final String DEFAULT_SERVER_PORT = "25565";

    private final LaunchProfile launchProfile;
    private final AuthProfile authProfile;
    private final LaunchManifest manifest;
    private final String version;
    private final Path gameDir;
    private final Path nativesDir;
    private final Path assetsDir;
    private final Path librariesDir;
    private final Path logsDir;

    LaunchCommandBuilder(LaunchProfile launchProfile, AuthProfile authProfile, LaunchManifest manifest, String version,
                         Path gameDir, Path nativesDir, Path assetsDir, Path librariesDir, Path logsDir)
    {
        this.launchProfile = launchProfile;
        this.authProfile = authProfile;
        this.manifest = manifest;
        this.version = version;
        this.gameDir = gameDir;
        this.nativesDir = nativesDir;
        this.assetsDir = assetsDir;
        this.librariesDir = librariesDir;
        this.logsDir = logsDir;
    }

    /**
     * Builds the full command line without starting anything.
     *
     * @param javaPath the Java executable
     * @param javaMajor its major version, or -1 when unknown
     * @param clientJarPath the client jar that goes last on the classpath
     * @return the command, starting with the wrapper command (if any) and the Java executable
     * @throws FileNotFoundException when a required library is missing
     */
    List<String> build(Path javaPath, int javaMajor, Path clientJarPath) throws IOException
    {
        String classPath = buildClassPath(clientJarPath);
        Files.createDirectories(nativesDir);
        if (launchProfile.quickPlay() != null) Files.createDirectories(quickPlayLog().toAbsolutePath().getParent());

        Map<String, String> placeholders = buildPlaceholders(classPath);

        List<String> jvm = manifest.jvmArguments(placeholders);
        if (!hasClassPath(jvm))
        {
            jvm.add("-cp");
            jvm.add(classPath);
        }
        if (jvm.stream().noneMatch(a -> a.startsWith("-Djava.library.path=")))
        {
            jvm.add("-Djava.library.path=" + nativesDir.toAbsolutePath());
        }
        addLoggingArguments(jvm);
        createNativeSubdirectories(jvm);
        jvm.addAll(launchProfile.extraJvmArgs());

        List<String> game = manifest.gameArguments(placeholders);
        applyOptionalGameArguments(game);

        List<String> command = new ArrayList<>(launchProfile.wrapperCommand());
        command.add(javaPath.toAbsolutePath().toString());
        addMemoryArguments(command);

        int length = manifest.mainClass().length() + 1;
        for (String s : command) length += s.length() + 1;
        for (String s : jvm) length += s.length() + 3;
        for (String s : game) length += s.length() + 1;

        boolean argFileSupported = javaMajor > 0 ? javaMajor >= 9 : manifest.requiredJavaMajor() >= 9;
        if (argFileSupported && length > ARGFILE_THRESHOLD)
        {
            command.add("@" + writeArgFile(jvm));
        }
        else
        {
            command.addAll(jvm);
        }

        command.add(manifest.mainClass());
        command.addAll(game);
        return command;
    }

    private String buildClassPath(Path clientJarPath) throws FileNotFoundException
    {
        StringBuilder sb = new StringBuilder();
        List<String> missing = new ArrayList<>();

        for (LaunchManifest.ClasspathEntry entry : manifest.classpathEntries())
        {
            Path libPath = librariesDir.resolve(entry.path());
            if (Files.exists(libPath))
            {
                sb.append(libPath.toAbsolutePath()).append(File.pathSeparator);
            }
            else if (!entry.serverOnly())
            {
                missing.add(entry.path());
            }
        }

        if (!missing.isEmpty())
        {
            StringBuilder message = new StringBuilder("Missing ").append(missing.size()).append(" librar")
                    .append(missing.size() == 1 ? "y" : "ies").append(" in ").append(librariesDir).append(": ");
            for (int i = 0; i < Math.min(missing.size(), 10); i++)
            {
                if (i > 0) message.append(", ");
                message.append(missing.get(i));
            }
            if (missing.size() > 10) message.append(", ...");
            message.append(". Run the installer again.");
            throw new FileNotFoundException(message.toString());
        }

        sb.append(clientJarPath.toAbsolutePath());
        return sb.toString();
    }

    private Map<String, String> buildPlaceholders(String classPath)
    {
        String uuid = nvl(authProfile.uuid());

        Map<String, String> args = new HashMap<>();
        args.put("auth_player_name", nvl(authProfile.username()));
        args.put("version_name", version);
        args.put("game_directory", gameDir.toAbsolutePath().toString());
        args.put("assets_root", assetsDir.toAbsolutePath().toString());
        args.put("game_assets", manifest.gameAssets(gameDir, assetsDir).toAbsolutePath().toString());
        args.put("assets_index_name", manifest.assetIndex());
        args.put("auth_uuid", uuid);
        args.put("auth_access_token", nvl(authProfile.accessToken()));
        args.put("auth_session", "token:" + nvl(authProfile.accessToken()) + ":" + uuid.replace("-", ""));
        args.put("user_properties", "{}");
        args.put("user_type", authProfile.isOffline() ? "legacy" : "msa");
        args.put("version_type", manifest.versionType());
        args.put("natives_directory", nativesDir.toAbsolutePath().toString());
        args.put("library_directory", librariesDir.toAbsolutePath().toString());
        args.put("classpath_separator", File.pathSeparator);
        args.put("launcher_name", nvl(launchProfile.launcherName()));
        args.put("launcher_version", nvl(launchProfile.launcherVersion()));
        args.put("classpath", classPath);

        if (notBlank(launchProfile.xuid())) args.put("auth_xuid", launchProfile.xuid());
        if (notBlank(launchProfile.clientId())) args.put("clientid", launchProfile.clientId());

        if (launchProfile.hasCustomResolution())
        {
            args.put("resolution_width", String.valueOf(launchProfile.width()));
            args.put("resolution_height", String.valueOf(launchProfile.height()));
        }

        LaunchProfile.QuickPlay quickPlay = launchProfile.quickPlay();
        if (quickPlay != null)
        {
            args.put("quickPlayPath", quickPlayLog().toAbsolutePath().toString());
            switch (quickPlay.type())
            {
                case SINGLEPLAYER -> args.put("quickPlaySingleplayer", quickPlay.target());
                case MULTIPLAYER -> args.put("quickPlayMultiplayer", quickPlay.target());
                case REALMS -> args.put("quickPlayRealms", quickPlay.target());
            }
        }
        return args;
    }

    private Path quickPlayLog()
    {
        return gameDir.resolve("quickPlay").resolve("java").resolve("launcher.json");
    }

    private static boolean hasClassPath(List<String> jvmArgs)
    {
        for (String a : jvmArgs)
        {
            if (a.equals("-cp") || a.equals("-classpath") || a.equals("--class-path") || a.startsWith("--class-path=")) return true;
        }
        return false;
    }

    private void addMemoryArguments(List<String> command)
    {
        long ramMb = launchProfile.ramMb();
        if (ramMb <= 0) return;

        long initial = launchProfile.initialRamMb() > 0 ? launchProfile.initialRamMb() : INITIAL_HEAP_MB;
        command.add("-Xmx" + ramMb + "M");
        command.add("-Xms" + Math.min(ramMb, initial) + "M");
    }

    private void addLoggingArguments(List<String> jvm)
    {
        String argument = manifest.loggingArgument();
        String fileId = manifest.loggingConfigId();

        if (argument != null && fileId != null)
        {
            Path config = assetsDir.resolve(LOG_CONFIGS_DIR).resolve(fileId);
            if (Files.exists(config)) jvm.add(argument.replace("${path}", config.toAbsolutePath().toString()));
        }

        jvm.add("-Dlog4j2.formatMsgNoLookups=true");
    }

    private void createNativeSubdirectories(List<String> jvm) throws IOException
    {
        Path root = nativesDir.toAbsolutePath().normalize();
        for (String a : jvm)
        {
            int eq = a.indexOf('=');
            if (!a.startsWith("-D") || eq < 0) continue;

            try
            {
                Path p = Paths.get(a.substring(eq + 1)).toAbsolutePath().normalize();
                if (p.startsWith(root)) Files.createDirectories(p);
            }
            catch (InvalidPathException ignored)
            {
            }
        }
    }

    private Path writeArgFile(List<String> args) throws IOException
    {
        Files.createDirectories(logsDir);
        Path file = logsDir.resolve("launcher-jvm.args");

        StringBuilder sb = new StringBuilder();
        for (String a : args)
        {
            sb.append('"').append(a.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"\n");
        }

        Charset charset;
        try
        {
            charset = Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8"));
        }
        catch (RuntimeException e)
        {
            charset = StandardCharsets.UTF_8;
        }

        Files.writeString(file, sb.toString(), charset);
        return file.toAbsolutePath();
    }

    private void applyOptionalGameArguments(List<String> game)
    {
        if (launchProfile.hasCustomResolution() && !game.contains("--width"))
        {
            game.add("--width");
            game.add(String.valueOf(launchProfile.width()));
            game.add("--height");
            game.add(String.valueOf(launchProfile.height()));
        }

        if (launchProfile.fullscreen() && !game.contains("--fullscreen")) game.add("--fullscreen");
        if (launchProfile.demo() && !game.contains("--demo")) game.add("--demo");

        LaunchProfile.QuickPlay quickPlay = launchProfile.quickPlay();
        if (quickPlay != null && quickPlay.type() == LaunchProfile.QuickPlay.Type.MULTIPLAYER && !game.contains("--quickPlayMultiplayer"))
        {
            String[] hostAndPort = splitAddress(quickPlay.target());
            game.add("--server");
            game.add(hostAndPort[0]);
            game.add("--port");
            game.add(hostAndPort[1]);
        }

        game.addAll(launchProfile.extraGameArgs());
    }

    static String[] splitAddress(String address)
    {
        String host = address;
        String port = DEFAULT_SERVER_PORT;

        if (address.startsWith("["))
        {
            int close = address.indexOf(']');
            if (close > 0)
            {
                host = address.substring(1, close);
                if (address.length() > close + 2 && address.charAt(close + 1) == ':') port = address.substring(close + 2);
            }
        }
        else
        {
            int colon = address.lastIndexOf(':');
            if (colon > 0 && address.indexOf(':') == colon)
            {
                host = address.substring(0, colon);
                port = address.substring(colon + 1);
            }
        }

        if (!port.matches("\\d{1,5}")) port = DEFAULT_SERVER_PORT;
        return new String[] { host, port };
    }

    private static boolean notBlank(String s)
    {
        return s != null && !s.isBlank();
    }

    private static String nvl(String s)
    {
        return s == null ? "" : s;
    }
}