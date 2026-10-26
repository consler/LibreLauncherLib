package net.consler.librelauncherlib;

import javafx.application.Platform;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.auth.MicrosoftAuthenticator;
import net.consler.librelauncherlib.auth.UserCancelledException;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.instance.*;
import net.consler.librelauncherlib.integration.modrinth.Modrinth;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;
import net.consler.librelauncherlib.integration.modrinth.SearchQuery;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.nbt.NBT;
import net.consler.librelauncherlib.versions.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Main
{

    private static final String version = "26.2";
    private static final Path gameDir = Path.of("/home/consler/TEST");
    private static final Path java25Bin = Path.of("/home/consler/.jdks/jbr-25.0.4.1-2/bin/java");

    static void main(String[] args)
    {
        if (args.length == 0) return;

        switch (args[0])
        {
            case "install" -> install();
            case "run" -> run();
            case "list" -> listVersions();
            case "loginWV" -> loginWithWebView().join();
            case "loginJFX" -> loginWithJavaFXWebView();
            case "mod" -> modInfo();
            case "respack" -> resourcePackInfo();
            case "nbt" -> nbtParser();
            case "datapack" -> dataPackInfo();
            case "world" -> worldInfo();
            case "modrinth" -> modrinth();
            case "mrpack" -> mrpack();
        }
    }

    private static void install()
    {
        new MinecraftInstaller().install(version, gameDir, new ModloaderProfile(ModloaderProfile.FABRIC_ID, "0.20.1"));
    }

    private static void run()
    {
        LaunchProfile launchProfile;
        launchProfile = new LaunchProfile.Builder(version, gameDir)
                .withJavaPath(java25Bin)
                .build();
        AuthProfile authProfile = AuthProfile.Offline("Consler");

        try
        {
            new MinecraftLauncher(launchProfile, authProfile, new ModloaderProfile(ModloaderProfile.QUILT_ID, "0.20.0-beta.9")).launch();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void listVersions()
    {
        System.out.println(QuiltVersions.getVersionsCompatibleWith(version));
        System.out.println(FabricVersions.getVersionsCompatibleWith(version));
        System.out.println(ForgeVersions.getVersionsCompatibleWith(version));
        System.out.println(NeoforgeVersions.getVersionsCompatibleWith(version));
        System.out.println(VanillaVersions.getVersionsFiltered(false, false, true, true));
    }

    private static CompletableFuture<Void> loginWithWebView()
    {
        return new MicrosoftAuthenticator().loginWithNativeWebView()
                .thenAccept(profile ->
                {
                    System.out.println("Webview login successful!");
                    System.out.println("Username: " + profile.username());
                    System.out.println("UUID: " + profile.uuid());
                    System.out.println("Refresh Token: " + profile.refreshToken());
                    System.exit(0);
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

    private static void loginWithJavaFXWebView()
    {
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        Platform.startup(() ->
                new MicrosoftAuthenticator().loginWithJavaFXWebView()
                        .thenAccept(profile ->
                        {
                            System.out.println("Username: " + profile.username());
                            System.out.println("UUID: " + profile.uuid());
                            System.out.println("Refresh Token: " + profile.refreshToken());
                            completionFuture.complete(null);
                        })
                        .exceptionally(ex ->
                        {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            if (cause instanceof UserCancelledException)
                            {
                                System.out.println("User closed the login frame before completing login.");
                            }
                            else
                            {
                                System.err.println("Authentication failed: " + cause.getMessage());
                            }
                            completionFuture.complete(null);
                            return null;
                        }));

        completionFuture.join();
    }

    private static void modInfo()
    {
        Mod mod = new Mod(new File("/home/consler/Downloads/worldedit-mod-7.2.14.jar"));
        System.out.println("Mod Name: " + mod.getName());
        System.out.println("Mod Version: " + mod.getVersion());
        System.out.println("Mod Description: " + mod.getDescription());
        System.out.println("Mod Loader Type: " + mod.getLoaderType());
        System.out.println("Mod Authors: " + String.join(", ", mod.getAuthors()));
        System.out.println("Mod License: " + mod.getLicense());
    }

    private static void resourcePackInfo()
    {
        ResourcePack resourcePack = new ResourcePack(Path.of("/home/consler/Downloads/LowOnFire v26.2?8.zip"));

        System.out.println("Resource Pack Name: " + resourcePack.getName());
        System.out.println("Resource Pack Description: " + resourcePack.getDescription());
        System.out.println("Resource Pack Format: " + resourcePack.getPackFormat());
    }
    private static void nbtParser()
    {
        try
        {
            NBT level = new NBT(new File("/home/consler/.local/share/LibreLauncher/26.2/saves/cart/level.dat"));
            System.out.println(level);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void dataPackInfo()
    {
        Datapack datapack = new Datapack(new File("/home/consler/Downloads/Nullscape_v2.0.0+26.3.zip").toPath());
        System.out.println("Data Pack Name: " + datapack.getName());
        System.out.println("Data Pack Description: " + datapack.getDescription());
        System.out.println("Data Pack Format: " + datapack.getPackFormat());
        System.out.println("Data Pack has Icon: " + datapack.hasIcon());
    }

    private static void worldInfo()
    {
        try
        {
            World world = new World(Path.of("/home/consler/.local/share/LibreLauncher/26.2/saves/cart"));

            System.out.println(world);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void modrinth()
    {
        String ua = "consler/LibreLauncher/1.0";

        Modrinth.SearchResult page = Modrinth.search(new SearchQuery("sodium")
                        .projectType(ProjectType.MOD)
                        .offset(10)
                        .limit(20),
                ua).join();

        page.hits().forEach(System.out::println);
    }

    private static void mrpack()
    {
        Mrpack mrpack = new Mrpack(Path.of("/home/consler/Downloads/1.21.11-4.1.mrpack"));

        MrpackInstaller installer = new MrpackInstaller(mrpack);
        try
        {
            installer.installTo(gameDir, MrpackInstaller.Side.CLIENT);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}