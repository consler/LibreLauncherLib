package net.consler.librelauncherlib.install;

/**
 * Listeners for MinecraftInstaller
 */
public interface InstallerListener
{
    /**
     * Called when the installation starts.
     */
    default void onStart() {}

    /**
     * Called when the installation finishes.
     */
    default void onFinish() {}

    /**
     * Called when a new percentage is available.
     */
    default void onNewPercentage(int percentage) {}

    /**
     * Called when a new file is being downloaded.
     */
    default void onNewFile(String file) {}

    /**
     * Called when the modloader installation starts.
     */
    default void onModloaderInstallation(String modloader) {}
}
