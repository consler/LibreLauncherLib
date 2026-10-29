package net.consler.librelauncherlib.instance;

import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VersionManifestTest
{
    public VersionManifestTest() throws URISyntaxException {}
    public Path resourcesDir = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI());

    @Test
    public void testFabricAsInstance() throws IOException
    {
        Path instancePath = resourcesDir.resolve("26.2 Fabric 0.20.1");

        new MinecraftInstaller().install("26.2", instancePath, new ModloaderProfile(ModloaderProfile.FABRIC_ID, "0.20.1"));

        Instance instance = new Instance(instancePath);

        assertNotNull(instance);

        assertEquals("26.2 Fabric 0.20.1", instance.getName());
        assertEquals("26.2", instance.getVersion());
        assertEquals(ModloaderProfile.FABRIC_ID, instance.getModloader());
        assertEquals("0.20.1", instance.getLoaderVersion());

        VersionManifest manifest = instance.getVersionManifest();
        assertEquals("net.minecraft.client.main.Main", manifest.mainClass());
    }

    @Test
    public void testLegacyForgeAsInstance() throws IOException
    {

        Path instancePath = resourcesDir.resolve("1.12.2 Forge 14.23.5.2864");

        new MinecraftInstaller().install("1.12.2", instancePath, new ModloaderProfile(ModloaderProfile.FORGE_ID, "1.12.2-14.23.5.2864"));

        Instance instance = new Instance(instancePath);

        assertNotNull(instance);

        assertEquals("1.12.2 Forge 14.23.5.2864", instance.getName());
        assertEquals("1.12.2", instance.getVersion());
        assertEquals(ModloaderProfile.FORGE_ID, instance.getModloader());
        assertEquals("1.12.2-14.23.5.2864", instance.getLoaderVersion());

        VersionManifest manifest = instance.getVersionManifest();
        assertEquals(8, manifest.javaMajorVersion());
        assertEquals("net.minecraft.client.main.Main", manifest.mainClass());
    }
}
