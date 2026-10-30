package net.consler.librelauncherlib.modloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModLoaderUninstallerTest
{
    @TempDir
    Path gameDir;

    @Test
    void uninstallersRemoveProfilesLibrariesAndLoaderVersions() throws IOException
    {
        assertUninstall("fabric-profile.json", "fabric-loader-0.1-test", FabricInstaller::uninstall, false);
        assertUninstall("quilt-profile.json", "quilt-loader-0.1-test", QuiltInstaller::uninstall, false);
        assertUninstall("forge-profile.json", "1.20.1-forge-1.0", ForgeInstaller::uninstall, true);
        assertUninstall("neoforge-profile.json", "1.20.1-neoforge-1.0", NeoforgeInstaller::uninstall, true);
    }

    private void assertUninstall(String profileName, String versionId, Uninstaller uninstaller, boolean hasInstallerFiles) throws IOException
    {
        String loaderArtifact = profileName.substring(0, profileName.indexOf('-'));
        String libraryPath = "example/" + loaderArtifact + "/1.0/" + loaderArtifact + "-1.0.jar";
        Path library = gameDir.resolve("libraries").resolve(libraryPath);
        Path versionFile = gameDir.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        Path profile = gameDir.resolve(profileName);
        Path unrelatedLibrary = gameDir.resolve("libraries/example/other/1.0/other-1.0.jar");

        Files.createDirectories(library.getParent());
        Files.createDirectories(versionFile.getParent());
        Files.createDirectories(unrelatedLibrary.getParent());
        Files.writeString(library, "loader");
        Files.writeString(versionFile, "{}");
        Files.writeString(unrelatedLibrary, "keep");
        Files.writeString(profile, """
                {
                  "id": "%s",
                  "libraries": [{"name": "example:%s:1.0"}]
                }
                """.formatted(versionId, loaderArtifact));

        if (hasInstallerFiles)
        {
            Files.writeString(gameDir.resolve(loaderArtifact + "-installer.jar"), "installer");
            if (loaderArtifact.equals("forge")) Files.writeString(gameDir.resolve("forge-installer.log"), "log");
        }

        uninstaller.uninstall(gameDir);
        uninstaller.uninstall(gameDir);

        assertFalse(Files.exists(profile));
        assertFalse(Files.exists(library));
        assertFalse(Files.exists(versionFile.getParent()));
        assertTrue(Files.exists(unrelatedLibrary));
        if (hasInstallerFiles)
        {
            assertFalse(Files.exists(gameDir.resolve(loaderArtifact + "-installer.jar")));
            if (loaderArtifact.equals("forge")) assertFalse(Files.exists(gameDir.resolve("forge-installer.log")));
        }
    }

    @FunctionalInterface
    private interface Uninstaller
    {
        void uninstall(Path gameDir);
    }
}
