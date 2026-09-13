package net.consler.librelauncherlib;

import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.versions.ForgeVersions;
import net.consler.librelauncherlib.versions.NeoforgeVersions;
import net.consler.librelauncherlib.versions.QuiltVersions;

import java.nio.file.Path;

public class Main
{
    private static final String version = "26.2";
    private static final Path gameDir = Path.of("/home/consler/TEST");
    private static final Path java8Bin = Path.of("/home/consler/.jdks/corretto-1.8.0_452/bin/java");
    private static final Path java25Bin = Path.of("/home/consler/.jdks/jbr-25.0.4.1/bin/java");

    static void main(String[] args)
    {
        switch (args[0])
        {
            case "install" -> install();
            case "run" -> run();
            case "list" -> listVersions();
        }
    }
    private static void install()
    {
        new MinecraftInstaller().install(version, gameDir, ModloaderProfile.VANILLA());
    }

    private static void run()
    {
        LaunchProfile launchProfile = new LaunchProfile.Builder(version, gameDir).withJavaPath(java25Bin).build();
        AuthProfile authProfile = AuthProfile.Offline("Consler");

        new MinecraftLauncher().launch(launchProfile, authProfile, ModloaderProfile.VANILLA());
    }

    private static void listVersions()
    {
        System.out.println(QuiltVersions.getVersionsCompatibleWith(version));
        System.out.println(ForgeVersions.getVersionsCompatibleWith(version));
        System.out.println(NeoforgeVersions.getVersionsCompatibleWith(version));
    }
}
