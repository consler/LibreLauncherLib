package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.util.DownloadTask;
import net.consler.librelauncherlib.util.DownloadManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Handles the installation of Forge.
 */
public class ForgeInstaller
{
    /**
     * Installs Forge into the provided game directory.
     *
     * @param modloaderProfile The version of Forge to install (e.g. "26.2-65.1.3")
     * @param gameDir The directory where Forge will be installed.
     * @param minecraftVersion The version of Minecraft in the provided directory.
     */
    public static void install(ModloaderProfile modloaderProfile, Path gameDir, String minecraftVersion, Path javaBin) throws InterruptedException, IOException, ModLoaderException
    {
        Path installerPath = gameDir.resolve("forge-installer.jar");

        try
        {
            String loaderVer = modloaderProfile.loaderVersion();
            String forgeVersion = loaderVer.startsWith(minecraftVersion) ? loaderVer : minecraftVersion + "-" + loaderVer;
            String installerUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + forgeVersion + "/forge-" + forgeVersion + "-installer.jar";

            Files.deleteIfExists(installerPath);
            new DownloadManager().downloadFile(installerUrl, installerPath);
            JsonObject profileJson = readInstallProfile(installerPath);
            if (profileJson.has("install")) installLegacy(profileJson, installerPath, gameDir);
            else installModern(installerPath, gameDir, javaBin);
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ModLoaderException("Failed to install Forge for version " + minecraftVersion, e);
        }
        finally
        {
            deleteQuietly(installerPath);
        }
    }

    private static JsonObject readInstallProfile(Path installerPath) throws Exception
    {
        try (ZipFile zip = new ZipFile(installerPath.toFile()))
        {
            ZipEntry entry = zip.getEntry("install_profile.json");
            if (entry == null) throw new RuntimeException("install_profile.json not found inside forge installer jar");

            try (InputStream in = zip.getInputStream(entry))
            {
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
    }

    private static void installLegacy(JsonObject profileJson, Path installerPath, Path gameDir) throws IOException
    {
        Path librariesDir = gameDir.resolve("libraries");

        JsonObject installSection = profileJson.getAsJsonObject("install");
        String mavenCoords = installSection.get("path").getAsString();
        String forgeLibPath = mavenCoordsToPath(mavenCoords);
        Path forgeLibDest = librariesDir.resolve(forgeLibPath);
        Files.createDirectories(forgeLibDest.getParent());

        String filePath = installSection.has("filePath") ? installSection.get("filePath").getAsString() : null;
        if (filePath != null)
        {
            try (ZipFile zip = new ZipFile(installerPath.toFile()))
            {
                ZipEntry entry = zip.getEntry(filePath);
                if (entry == null) throw new RuntimeException("Could not find " + filePath + " inside forge installer jar");

                try (InputStream in = zip.getInputStream(entry))
                {
                    Files.copy(in, forgeLibDest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        JsonObject versionInfo = profileJson.getAsJsonObject("versionInfo");

        if (versionInfo.has("libraries"))
        {
            downloadLegacyLibraries(versionInfo.getAsJsonArray("libraries"), librariesDir);
        }

        Files.writeString(gameDir.resolve("forge-profile.json"), versionInfo.toString());
    }

    private static void downloadLegacyLibraries(JsonArray libraries, Path librariesDir) throws IOException
    {
        List<DownloadTask> tasks = new ArrayList<>();
        for (JsonElement el : libraries)
        {
            JsonObject lib = el.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String relativePath = mavenCoordsToPath(name);
            Path dest = librariesDir.resolve(relativePath);
            if (Files.exists(dest)) continue;
            String baseUrl = (lib.has("url") && !lib.get("url").getAsString().isEmpty()) ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
            if (!baseUrl.endsWith("/")) baseUrl += "/";
            Files.createDirectories(dest.getParent());
            tasks.add(new DownloadTask(baseUrl + relativePath, dest));
        }
        if (!tasks.isEmpty())
        {
            DownloadManager downloadManager = new DownloadManager();
            downloadManager.downloadBatch(tasks);
        }
    }

    private static String mavenCoordsToPath(String coords)
    {
        String extension = "jar";
        if (coords.contains("@"))
        {
            int at = coords.lastIndexOf('@');
            extension = coords.substring(at + 1);
            coords = coords.substring(0, at);
        }
        String[] parts = coords.split(":");
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? parts[3] : null;
        String fileName = artifact + "-" + version + (classifier != null ? "-" + classifier : "") + "." + extension;
        return group + "/" + artifact + "/" + version + "/" + fileName;
    }


    private static void installModern(Path installerPath, Path gameDir, Path javaPath) throws IOException, InterruptedException
    {
        Path launcherProfiles = gameDir.resolve("launcher_profiles.json");
        Path installerLog = gameDir.resolve("forge-installer.log");
        Files.writeString(launcherProfiles, "{\"profiles\":{}}");
        ProcessBuilder pb = new ProcessBuilder(
                javaPath.toAbsolutePath().toString(),
                "-jar",
                installerPath.toAbsolutePath().toString(),
                "--installClient",
                gameDir.toAbsolutePath().toString()
        );
        pb.directory(gameDir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(installerLog.toFile()));
        Process process = pb.start();
        if (!process.waitFor(15, TimeUnit.MINUTES))
        {
            process.destroyForcibly();
            throw new RuntimeException("Forge installer process timed out (see " + installerLog + ")");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) throw new RuntimeException("Forge installer process failed with exit code: " + exitCode + " (see " + installerLog + ")");
        Files.deleteIfExists(installerLog);


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
        if (installedVersionId == null) throw new RuntimeException("Forge installer did not create a launcher profile");

        Path installedJson = gameDir.resolve("versions").resolve(installedVersionId).resolve(installedVersionId + ".json");
        if (!Files.exists(installedJson)) throw new RuntimeException("Forge version json not found: " + installedJson);

        Files.copy(installedJson, gameDir.resolve("forge-profile.json"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (Exception ignored)
        {
        }
    }
}