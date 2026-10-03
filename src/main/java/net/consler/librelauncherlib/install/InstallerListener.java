package net.consler.librelauncherlib.install;

/**
 * Listeners for MinecraftInstaller
 */
public interface InstallerListener
{
    /**
     * Called when the installation starts.
     */
    void onStart();

    /**
     * Called when the installation finishes.
     */
    void onFinish();

    /**
     * Called when a new percentage is available.
     */
    void onNewPercentage(int percentage);

    /**
     * Called when a new file is being downloaded.
     */
    void onNewFile(String file);

    /**
     * Called when the modloader installation starts.
     */
    void onModloaderInstallation(String modloader);
}
