package net.consler.librelauncherlib;

import net.consler.librelauncherlib.download.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;

import java.nio.file.Path;

public class Main
{
    private static final String version = "26.2";

    static void main(String[] args) throws Exception
    {
        switch (args[0])
        {
            case "download" -> download();
            case "run" -> run();
        }
    }
    private static void download() throws Exception
    {
        MinecraftInstaller installer = new MinecraftInstaller();
        installer.install(version, Path.of("/home/consler/TEST"));
    }

    private static void run() throws Exception
    {
        Path gameDir = Path.of("/home/consler/TEST");

        LaunchProfile profile = new LaunchProfile.Builder("Consler", version, gameDir).build();
        MinecraftLauncher launcher = new MinecraftLauncher();
        launcher.launch(profile);

    }
}
