package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.util.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles the installation of NeoForge.
 */
public class NeoforgeInstaller
{
    /**
     * Removes NeoForge's profile, installed version, and libraries listed by the profile.
     *
     * @param gameDir the game directory containing the NeoForge installation
     */
    public static void uninstall(Path gameDir)
    {
        ModLoaderUninstaller.uninstall(gameDir, "neoforge-profile.json", "neoforge", "neoforge-installer.jar", "neoforge-installer.jar.part");
    }

    /**
     * Installs NeoForge into the provided game directory.
     *
     * @param modloaderProfile The version of NeoForge to install (e.g. "26.2.0.88")
     * @param gameDir The directory where NeoForge will be installed.
     * @param minecraftVersion The version of Minecraft in the provided directory.
     */
    public static void install(ModloaderProfile modloaderProfile, Path gameDir, String minecraftVersion, Path javaBin)
    {
        try
        {
            String neoForgeVersion = modloaderProfile.loaderVersion();
            String installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + neoForgeVersion + "/neoforge-" + neoForgeVersion + "-installer.jar";
            Path installerPath = gameDir.resolve("neoforge-installer.jar");

            Files.deleteIfExists(installerPath);
            new DownloadManager().downloadFile(installerUrl, installerPath);

            Path launcherProfiles = gameDir.resolve("launcher_profiles.json");
            boolean hadLauncherProfiles = Files.exists(launcherProfiles);
            byte[] originalLauncherProfiles = hadLauncherProfiles ? Files.readAllBytes(launcherProfiles) : null;
            try
            {
                Files.writeString(launcherProfiles, "{\"profiles\":{}}");

                ProcessBuilder pb = new ProcessBuilder(javaBin.toAbsolutePath().toString(), "-Djava.awt.headless=true", "-jar", installerPath.toAbsolutePath().toString(), "--installClient", gameDir.toAbsolutePath().toString());
                pb.directory(gameDir.toFile());
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) throw new RuntimeException("NeoForge installer process failed with exit code: " + exitCode);

                JsonObject profilesObj = JsonParser.parseString(Files.readString(launcherProfiles)).getAsJsonObject().getAsJsonObject("profiles");
                String installedVersionId = null;
                for (String key : profilesObj.keySet())
                {
                    JsonObject prof = profilesObj.getAsJsonObject(key);
                    if (prof.has("lastVersionId"))
                    {
                        installedVersionId = prof.get("lastVersionId").getAsString();
                        break;
                    }
                }
                if (installedVersionId == null) throw new ModLoaderException("NeoForge installer did not report an installed version in launcher_profiles.json");

                Path installedJson = gameDir.resolve("versions").resolve(installedVersionId).resolve(installedVersionId + ".json");
                if (!Files.exists(installedJson)) throw new ModLoaderException("Expected NeoForge version json not found at " + installedJson);

                Files.copy(installedJson, gameDir.resolve("neoforge-profile.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            finally
            {
                if (hadLauncherProfiles) Files.write(launcherProfiles, originalLauncherProfiles);
                else Files.deleteIfExists(launcherProfiles);
            }

            Files.deleteIfExists(installerPath);
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ModLoaderException("Failed to install NeoForge for version " + minecraftVersion, e);
        }
    }
}