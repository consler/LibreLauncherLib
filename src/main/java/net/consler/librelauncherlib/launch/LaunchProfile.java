package net.consler.librelauncherlib.launch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LaunchProfile(String username, String uuid, String accessToken, int ramMb, String version, String launcherName, String launcherVersion, Path gameDir, List<String> extraJvmArgs, Path javaPath)
{
    public static class Builder
    {
        private final String username;
        private final String version;

        private final Path gameDir;

        private String uuid = String.valueOf(UUID.randomUUID());
        private String accessToken = "0";
        private int ramMb = 2048;
        private String launcherName = "LibreLauncher";
        private String launcherVersion = "1.0";
        private Path javaPath = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        private final List<String> extraJvmArgs = new ArrayList<>();

        public Builder(String username, String version, Path gameDir)
        {
            this.username = username;
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
         * Overrides the generated player UUID used by the game launcher.
         */
        public Builder withUuid(String uuid)
        {
            this.uuid = uuid;
            return this;
        }

        /**
         * Sets the authenticated access token supplied to the Minecraft JVM arguments.
         */
        public Builder withAccessToken(String accessToken)
        {
            this.accessToken = accessToken;
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
         * Adds an extra, raw JVM argument to the launch command.
         */
        public Builder withExtraJvmArgs(String arg)
        {
            this.extraJvmArgs.add(arg);
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
         * Builds the configured launch profile.
         */
        public LaunchProfile build()
        {
            return new LaunchProfile(username, uuid, accessToken, ramMb, version, launcherName, launcherVersion, gameDir, extraJvmArgs, javaPath);
        }
    }
}