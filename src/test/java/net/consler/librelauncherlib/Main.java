package net.consler.librelauncherlib;

import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.download.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;

import java.nio.file.Path;

public class Main
{
    private static final String version = "26.2";
    private static final Path gameDir = Path.of("/home/consler/TEST");

    public static void main(String[] args)
    {
        switch (args[0])
        {
            case "download" -> download();
            case "run" -> run();
        }
    }
    private static void download()
    {
        MinecraftInstaller installer = new MinecraftInstaller();
        installer.install(version, gameDir);
    }

    private static void run()
    {
        LaunchProfile launchProfile = new LaunchProfile.Builder(version, gameDir).build();
        AuthProfile authProfile = AuthProfile.Offline("Consler");

        MinecraftLauncher launcher = new MinecraftLauncher();
        launcher.launch(launchProfile, authProfile);

    }
}
