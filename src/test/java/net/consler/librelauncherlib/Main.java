package net.consler.librelauncherlib;

import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.download.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.versions.Forge;

import java.nio.file.Path;

public class Main
{
    private static final String version = "26.2";
    private static final Path gameDir = Path.of("/home/consler/TEST");

    static void main(String[] args)
    {
        switch (args[0])
        {
            case "download" -> download();
            case "run" -> run();
            case "list" -> listVersions();
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

        new MinecraftLauncher().launch(launchProfile, authProfile);
    }

    private static void listVersions()
    {
        System.out.println(Forge.getVersionsCompatibleWith("26.4"));
    }
}
