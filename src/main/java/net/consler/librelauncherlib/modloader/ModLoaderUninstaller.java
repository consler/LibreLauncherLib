package net.consler.librelauncherlib.modloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.util.MavenHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class ModLoaderUninstaller
{
    private ModLoaderUninstaller()
    {
    }

    static void uninstall(Path gameDir, String profileFile, String loaderId, String... temporaryFiles)
    {
        Path root = gameDir.toAbsolutePath().normalize();
        Path profilePath = root.resolve(profileFile);

        try
        {
            Set<Path> libraryFiles = new LinkedHashSet<>();
            String versionId = null;
            if (Files.exists(profilePath))
            {
                JsonObject profile = JsonParser.parseString(Files.readString(profilePath)).getAsJsonObject();
                collectLibraries(profile, root.resolve("libraries"), libraryFiles);

                if (profile.has("install") && profile.get("install").isJsonObject())
                {
                    JsonObject install = profile.getAsJsonObject("install");
                    if (install.has("path")) addLibraryPath(install.get("path").getAsString(), root.resolve("libraries"), libraryFiles);
                }

                versionId = readVersionId(profile);
            }

            for (Path libraryFile : libraryFiles) Files.deleteIfExists(libraryFile);
            deleteEmptyLibraryDirectories(root.resolve("libraries"), libraryFiles);

            if (versionId != null && versionId.toLowerCase(Locale.ROOT).contains(loaderId))
            {
                Path versionsDir = root.resolve("versions").normalize();
                Path versionDir = versionsDir.resolve(versionId).normalize();
                if (versionDir.startsWith(versionsDir) && !versionDir.equals(versionsDir)) deleteTree(versionDir);
            }

            Files.deleteIfExists(profilePath);
            for (String file : temporaryFiles) Files.deleteIfExists(root.resolve(file).normalize());
        }
        catch (Exception e)
        {
            throw new ModLoaderException("Failed to uninstall " + loaderId + " from " + gameDir, e);
        }
    }

    private static void collectLibraries(JsonObject profile, Path librariesDir, Set<Path> libraryFiles)
    {
        if (profile.has("libraries") && profile.get("libraries").isJsonArray())
        {
            collectLibraries(profile.getAsJsonArray("libraries"), librariesDir, libraryFiles);
        }
        if (profile.has("versionInfo") && profile.get("versionInfo").isJsonObject())
        {
            JsonObject versionInfo = profile.getAsJsonObject("versionInfo");
            if (versionInfo.has("libraries") && versionInfo.get("libraries").isJsonArray())
            {
                collectLibraries(versionInfo.getAsJsonArray("libraries"), librariesDir, libraryFiles);
            }
        }
    }

    private static void collectLibraries(JsonArray libraries, Path librariesDir, Set<Path> libraryFiles)
    {
        for (JsonElement element : libraries)
        {
            if (element.isJsonObject() && element.getAsJsonObject().has("name"))
            {
                addLibraryPath(element.getAsJsonObject().get("name").getAsString(), librariesDir, libraryFiles);
            }
        }
    }

    private static void addLibraryPath(String coordinates, Path librariesDir, Set<Path> libraryFiles)
    {
        Path base = librariesDir.toAbsolutePath().normalize();
        Path target = base.resolve(MavenHelper.toJarPath(coordinates)).normalize();
        if (!target.startsWith(base)) throw new IllegalArgumentException("Library path escapes libraries directory: " + coordinates);
        libraryFiles.add(target);
    }

    private static String readVersionId(JsonObject profile)
    {
        if (profile.has("id") && profile.get("id").isJsonPrimitive()) return profile.get("id").getAsString();
        if (profile.has("versionInfo") && profile.get("versionInfo").isJsonObject())
        {
            JsonObject versionInfo = profile.getAsJsonObject("versionInfo");
            if (versionInfo.has("id") && versionInfo.get("id").isJsonPrimitive()) return versionInfo.get("id").getAsString();
        }
        return null;
    }

    private static void deleteEmptyLibraryDirectories(Path librariesDir, Set<Path> libraryFiles) throws IOException
    {
        Path root = librariesDir.toAbsolutePath().normalize();
        for (Path file : libraryFiles)
        {
            Path directory = file.getParent();
            while (directory != null && directory.startsWith(root) && !directory.equals(root))
            {
                try
                {
                    Files.delete(directory);
                }
                catch (java.nio.file.DirectoryNotEmptyException ignored)
                {
                    break;
                }
                directory = directory.getParent();
            }
        }
    }

    private static void deleteTree(Path path) throws IOException
    {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException
            {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
