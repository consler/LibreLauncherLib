package net.consler.librelauncherlib;

import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.auth.MicrosoftAuthenticator;
import net.consler.librelauncherlib.auth.WebViewFrame;
import net.consler.librelauncherlib.exception.UserCancelledException;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.versions.ForgeVersions;
import net.consler.librelauncherlib.versions.NeoforgeVersions;
import net.consler.librelauncherlib.versions.QuiltVersions;
import net.consler.librelauncherlib.versions.VanillaVersions;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Main
{

    private static final String version = "1.8.9";
    private static final Path gameDir = Path.of("/home/consler/TEST");
    private static final Path java25Bin = Path.of("/home/consler/.jdks/corretto-1.8.0_452/bin/java");

    static void main(String[] args)
    {
        if (args.length == 0) return;

        switch (args[0]) {
            case "install" -> install();
            case "run" -> run();
            case "list" -> listVersions();
            case "loginWV" -> loginWithWebView().join();
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
        System.out.println(VanillaVersions.getVersionsFiltered(true, true, false, false));
    }

    private static CompletableFuture<Void> loginWithWebView()
    {
        return new MicrosoftAuthenticator().login(new WebViewFrame())
                .thenAccept(profile ->
                {
                    System.out.println("Webview login successful!");
                    System.out.println("Username: " + profile.username());
                    System.out.println("UUID: " + profile.uuid());
                    System.out.println("Refresh Token: " + profile.refreshToken());
                })
                .exceptionally(ex ->
                {
                    if (ex.getCause() instanceof UserCancelledException)
                    {
                        System.out.println("User closed the login frame before completing login.");
                    }
                    else
                    {
                        System.err.println("Authentication failed: " + ex.getMessage());
                    }
                    return null;
                });
    }
}