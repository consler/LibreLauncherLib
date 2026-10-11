package net.consler.librelauncherlib.launch;

import net.consler.librelauncherlib.instance.World;
import net.consler.librelauncherlib.util.SystemHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a launch profile for starting Minecraft instances.
 * <p>
 * Everything after {@code javaPath} is optional. Use {@link Builder}, which fills in the defaults.
 *
 * @param ramMb maximum heap in megabytes ({@code -Xmx}); 0 leaves it to the JVM
 * @param version the Minecraft version to start
 * @param launcherName launcher name reported to the game
 * @param launcherVersion launcher version reported to the game
 * @param gameDir the instance directory
 * @param extraJvmArgs raw JVM arguments, added after the manifest's own (so they win on duplicates)
 * @param javaPath the Java executable to start the game with
 * @param initialRamMb initial heap in megabytes ({@code -Xms}); 0 picks min(ramMb, 512)
 * @param extraGameArgs raw game arguments, added at the end
 * @param width window width, 0 for the game's default; must be set together with {@code height}
 * @param height window height, 0 for the game's default; must be set together with {@code width}
 * @param fullscreen start in fullscreen
 * @param demo start in demo mode
 * @param quickPlay join a world or server straight after start, or null
 * @param xuid Xbox user id passed to the game, or null
 * @param clientId Microsoft client id passed to the game, or null
 * @param wrapperCommand command placed in front of Java, for example {@code gamemoderun}
 * @param environment extra environment variables for the game process
 * @param outputLog file receiving the game's stdout and stderr, or null for {@code <gameDir>/logs/launcher-output.log}
 * @param checkJavaVersion refuse to start when {@code javaPath} is older than the version needs
 * @param followOsRules refuse to start when a disapproved OS tries to launch
 * @param followArchRules refuse to start when a disapproved architecture tries to launch
 */
public record LaunchProfile(
        int ramMb,
        String version,
        String launcherName,
        String launcherVersion,
        Path gameDir,
        List<String> extraJvmArgs,
        Path javaPath,
        int initialRamMb,
        List<String> extraGameArgs,
        int width,
        int height,
        boolean fullscreen,
        boolean demo,
        QuickPlay quickPlay,
        String xuid,
        String clientId,
        List<String> wrapperCommand,
        Map<String, String> environment,
        Path outputLog,
        boolean checkJavaVersion,
        boolean followOsRules,
        boolean followArchRules,
        boolean followOsVersionRules)
{
    /**
     * A world or server to join as soon as the game has started.
     * Singleplayer and multiplayer work from 1.20 (older versions only get multiplayer, through {@code --server}),
     * Realms needs 1.20 or newer.
     *
     * @param type what kind of target this is
     * @param target world folder name, {@code host[:port]}, or Realms id
     */
    public record QuickPlay(Type type, String target)
    {
        public enum Type { SINGLEPLAYER, MULTIPLAYER, REALMS }

        public QuickPlay
        {
            Objects.requireNonNull(type, "type");
            if (target == null || target.isBlank()) throw new IllegalArgumentException("Quick play target must not be blank");
            target = target.trim();
        }

        public static QuickPlay singleplayer(String worldFolder)
        {
            return new QuickPlay(Type.SINGLEPLAYER, worldFolder);
        }

        public static QuickPlay multiplayer(String address)
        {
            return new QuickPlay(Type.MULTIPLAYER, address);
        }

        public static QuickPlay realms(String realmId)
        {
            return new QuickPlay(Type.REALMS, realmId);
        }
    }

    public LaunchProfile
    {
        if (ramMb < 0) throw new IllegalArgumentException("ramMb can not be negative");
        if (initialRamMb < 0) throw new IllegalArgumentException("initialRamMb can not be negative");
        if (width < 0 || height < 0) throw new IllegalArgumentException("width and height can not be negative");
        if ((width == 0) != (height == 0)) throw new IllegalArgumentException("width and height must be set together");

        extraJvmArgs = immutableCopy(extraJvmArgs);
        extraGameArgs = immutableCopy(extraGameArgs);
        wrapperCommand = immutableCopy(wrapperCommand);
        environment = immutableCopy(environment);
    }

    /**
     * @return true when both a window width and height are set
     */
    public boolean hasCustomResolution()
    {
        return width > 0 && height > 0;
    }

    private static List<String> immutableCopy(List<String> list)
    {
        if (list == null) return List.of();
        for (String s : list) Objects.requireNonNull(s, "list can not contain null");
        return List.copyOf(list);
    }

    private static Map<String, String> immutableCopy(Map<String, String> map)
    {
        if (map == null) return Map.of();
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : map.entrySet())
        {
            copy.put(Objects.requireNonNull(e.getKey(), "environment key"), Objects.requireNonNull(e.getValue(), "environment value"));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Builder class for constructing launch profiles.
     */
    public static class Builder
    {
        private final String version;

        private final Path gameDir;
        private int ramMb = 2048;
        private String launcherName = "LibreLauncherLib";
        private String launcherVersion = "1.0";
        private Path javaPath = SystemHelper.getJavaBin();
        private final List<String> extraJvmArgs = new ArrayList<>();

        private int initialRamMb = 0;
        private final List<String> extraGameArgs = new ArrayList<>();
        private int width = 0;
        private int height = 0;
        private boolean fullscreen = false;
        private boolean demo = false;
        private QuickPlay quickPlay = null;
        private String xuid = null;
        private String clientId = null;
        private final List<String> wrapperCommand = new ArrayList<>();
        private final Map<String, String> environment = new LinkedHashMap<>();
        private Path outputLog = null;
        private boolean checkJavaVersion = true;
        private boolean followOsRules = true;
        private boolean followArchRules = true;
        private boolean followOsVersionRules = true;

        public Builder(String version, Path gameDir)
        {
            this.version = version;
            this.gameDir = gameDir;
        }

        /**
         * Sets the allocated RAM in megabytes for the game JVM.
         */
        public Builder withRamMb(int ramMb)
        {
            this.ramMb = ramMb;
            return this;
        }

        /**
         * Sets the launcher name used in the game metadata.
         */
        public Builder withLauncherName(String launcherName)
        {
            this.launcherName = launcherName;
            return this;
        }

        /**
         * Sets the launcher version value injected into the launch arguments.
         */
        public Builder withLauncherVersion(String launcherVersion)
        {
            this.launcherVersion = launcherVersion;
            return this;
        }

        /**
         * Adds extra, raw JVM arguments to the launch command. Can be called several times.
         */
        public Builder withExtraJvmArgs(String... args)
        {
            this.extraJvmArgs.addAll(List.of(args));
            return this;
        }

        /**
         * Overrides the Java binary that should be used to start the game.
         */
        public Builder withJavaPath(Path javaPath)
        {
            this.javaPath = javaPath;
            return this;
        }

        /**
         * Sets the initial heap in megabytes ({@code -Xms}). Without this it is min(ram, 512).
         */
        public Builder withInitialRamMb(int initialRamMb)
        {
            this.initialRamMb = initialRamMb;
            return this;
        }

        /**
         * Adds extra, raw game arguments to the end of the launch command. Can be called several times.
         */
        public Builder withExtraGameArgs(String... args)
        {
            this.extraGameArgs.addAll(List.of(args));
            return this;
        }

        /**
         * Sets the window size.
         */
        public Builder withResolution(int width, int height)
        {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Starts the game in fullscreen.
         */
        public Builder withFullscreen(boolean fullscreen)
        {
            this.fullscreen = fullscreen;
            return this;
        }

        /**
         * Starts the game in demo mode.
         */
        public Builder withDemoMode(boolean demo)
        {
            this.demo = demo;
            return this;
        }

        /**
         * Opens a singleplayer world straight after start (1.20+). Replaces any other quick play target.
         *
         * @param worldFolder the folder name of the world inside {@code saves}
         */
        public Builder withQuickPlaySingleplayer(String worldFolder)
        {
            this.quickPlay = QuickPlay.singleplayer(worldFolder);
            return this;
        }

        /**
         * Opens a singleplayer world straight after start (1.20+). Replaces any other quick play target.
         *
         * @param world the world to play
         */
        public Builder withQuickPlaySingleplayer(World world)
        {
            this.quickPlay = QuickPlay.singleplayer(world.worldDir.toString());
            return this;
        }

        /**
         * Joins a server straight after start. Replaces any other quick play target.
         *
         * @param address {@code host} or {@code host:port}
         */
        public Builder withQuickPlayMultiplayer(String address)
        {
            this.quickPlay = QuickPlay.multiplayer(address);
            return this;
        }

        /**
         * Joins a Realm straight after start (1.20+). Replaces any other quick play target.
         */
        public Builder withQuickPlayRealms(String realmId)
        {
            this.quickPlay = QuickPlay.realms(realmId);
            return this;
        }

        /**
         * Sets the Xbox user id the game reports (used for telemetry and some multiplayer features).
         */
        public Builder withXuid(String xuid)
        {
            this.xuid = xuid;
            return this;
        }

        /**
         * Sets the client id the game reports.
         */
        public Builder withClientId(String clientId)
        {
            this.clientId = clientId;
            return this;
        }

        /**
         * Puts a command in front of Java, for example {@code gamemoderun} or {@code mangohud}.
         */
        public Builder withWrapperCommand(String... command)
        {
            this.wrapperCommand.addAll(List.of(command));
            return this;
        }

        /**
         * Sets an environment variable for the game process.
         */
        public Builder withEnvironment(String key, String value)
        {
            this.environment.put(key, value);
            return this;
        }

        /**
         * Sets where the game's stdout and stderr go. The file is replaced on every launch.
         */
        public Builder withOutputLog(Path outputLog)
        {
            this.outputLog = outputLog;
            return this;
        }

        /**
         * Turns the check off that refuses a Java older than the Minecraft version needs.
         */
        public Builder withJavaVersionCheck(boolean checkJavaVersion)
        {
            this.checkJavaVersion = checkJavaVersion;
            return this;
        }

        /**
         * Turns the check off that refuses a non-approved OS
         */
        public Builder withOsCheck(boolean followOsRules)
        {
            this.followOsRules = followOsRules;
            return this;
        }

        /**
         * Turns the check off that refuses a non-approved architecture.
         */
        public Builder withArchCheck(boolean followArchRules)
        {
            this.followArchRules = followArchRules;
            return this;
        }

        /**
         * Turns the check off that refuses a non-approved OS version.
         */
        public Builder withOsVersionCheck(boolean followOsVersionRules)
        {
            this.followOsVersionRules = followOsVersionRules;
            return this;
        }

        /**
         * Builds the configured launch profile.
         */
        public LaunchProfile build()
        {
            return new LaunchProfile(ramMb, version, launcherName, launcherVersion, gameDir, extraJvmArgs, javaPath,
                    initialRamMb, extraGameArgs, width, height, fullscreen, demo, quickPlay, xuid, clientId,
                    wrapperCommand, environment, outputLog, checkJavaVersion, followOsRules, followArchRules, followOsVersionRules);
        }
    }
}