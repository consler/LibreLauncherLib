package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.NeoforgeInstallerException;
import net.consler.librelauncherlib.utill.DownloadManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NeoforgeInstaller
{
    public static void install(ModloaderProfile profile, Path gameDir, String minecraftVersion, Path javaBin)
    {
        try
        {
            String neoForgeVersion = profile.loaderVersion();
            String installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + neoForgeVersion + "/neoforge-" + neoForgeVersion + "-installer.jar";
            Path installerPath = gameDir.resolve("neoforge-installer.jar");
            new DownloadManager().downloadFile(installerUrl, installerPath);

            Path launcherProfiles = gameDir.resolve("launcher_profiles.json");
            Files.writeString(launcherProfiles, "{\"profiles\":{}}");

            ProcessBuilder pb = new ProcessBuilder(javaBin.toAbsolutePath().toString(), "-Djava.awt.headless=true", "-jar", installerPath.toAbsolutePath().toString(), "--installClient", gameDir.toAbsolutePath().toString());
            pb.directory(gameDir.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
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
            if (installedVersionId == null) throw new NeoforgeInstallerException("NeoForge installer did not report an installed version in launcher_profiles.json");

            Path installedJson = gameDir.resolve("versions").resolve(installedVersionId).resolve(installedVersionId + ".json");
            if (!Files.exists(installedJson)) throw new NeoforgeInstallerException("Expected NeoForge version json not found at " + installedJson);

            Files.copy(installedJson, gameDir.resolve("neoforge-profile.json"), StandardCopyOption.REPLACE_EXISTING);

            Files.deleteIfExists(installerPath);
        }
        catch (Exception e)
        {
            throw new NeoforgeInstallerException("Failed to install NeoForge for version " + minecraftVersion + "\n" + e.getMessage());
        }
    }
}