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
    @Test
    public void testAsInstance() throws IOException, URISyntaxException
    {
        Path resourcesDir = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI());
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
}
