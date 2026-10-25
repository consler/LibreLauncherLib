package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.Gson;
import net.consler.librelauncherlib.instance.Datapack;
import net.consler.librelauncherlib.instance.Mod;
import net.consler.librelauncherlib.instance.Modpack;
import net.consler.librelauncherlib.instance.Plugin;
import net.consler.librelauncherlib.instance.ResourcePack;
import net.consler.librelauncherlib.instance.Shader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModrinthProjectTest
{
    private static final Gson GSON = new Gson();

    @TempDir
    Path tempDir;

    @Test
    void convertsDownloadedFilesToTheMatchingInstanceTypes() throws IOException
    {
        Path jar = createJar();
        Path zip = createZip("pack.zip");
        Path modpackFile = createModpack();

        assertInstanceOf(Mod.class, project(ProjectType.MOD).toMod(jar));
        assertInstanceOf(Datapack.class, project(ProjectType.DATAPACK).toDatapack(zip));
        Modpack modpack = project(ProjectType.MODPACK).toModpack(modpackFile);
        assertEquals("Example Pack", modpack.getName());
        assertEquals("1.21.1", modpack.getDependencies().get("minecraft"));
        assertInstanceOf(ResourcePack.class, project(ProjectType.RESOURCE_PACK).toResourcePack(zip));
        assertInstanceOf(Shader.class, project(ProjectType.SHADER).toShader(zip));
        assertInstanceOf(Plugin.class, project(ProjectType.PLUGIN).toPlugin(jar));
    }

    @Test
    void rejectsConversionToMismatchedInstanceType()
    {
        assertThrows(IllegalStateException.class,
                () -> project(ProjectType.DATAPACK).toMod(tempDir.resolve("anything.jar")));
    }

    private Path createZip(String name) throws IOException
    {
        Path zip = tempDir.resolve(name);
        try (ZipOutputStream ignored = new ZipOutputStream(Files.newOutputStream(zip)))
        {
        }
        return zip;
    }

    private Path createJar() throws IOException
    {
        Path jar = tempDir.resolve("mod.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar)))
        {
            output.putNextEntry(new JarEntry("placeholder"));
            output.closeEntry();
        }
        return jar;
    }

    private Path createModpack() throws IOException
    {
        Path file = tempDir.resolve("pack.mrpack");
        String index = """
                {
                  "formatVersion": 1,
                  "game": "minecraft",
                  "name": "Example Pack",
                  "versionId": "1.0.0",
                  "dependencies": {"minecraft": "1.21.1"}
                }
                """;
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file)))
        {
            output.putNextEntry(new JarEntry("modrinth.index.json"));
            output.write(index.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return file;
    }

    private ModrinthProject project(ProjectType type)
    {
        return GSON.fromJson("{\"project_type\":\"" + type.getId() + "\"}", ModrinthProject.class);
    }
}
