package net.consler.librelauncherlib.instance;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModTest
{
    public ModTest() throws URISyntaxException {}

    public Path resourcesDir = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI());

    @Test
    public void testFabricMod()
    {
        Mod mod = new Mod(resourcesDir.resolve("mods", "geckolib-fabric-26.3-5.5.7.jar"));

        assertEquals("geckolib", mod.getModId());
        assertEquals("GeckoLib 5", mod.getName());
        assertEquals("5.5.7", mod.getVersion());
        assertEquals("GeckoLib is an animation engine for Minecraft Mods, with support for complex 3D keyframe-based animations, 30+ easings, concurrent animation support, sound and particle keyframes, event keyframes, and more.", mod.getDescription());
        assertEquals("MIT", mod.getLicense());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("Tslat", mod.getAuthors().getFirst());
    }

    @Test
    public void testForgeMod()
    {
        Mod mod = new Mod(resourcesDir.resolve("mods", "geckolib-forge-26.3-5.5.7.jar"));

        assertEquals("geckolib", mod.getModId());
        assertEquals("GeckoLib 5", mod.getName());
        assertEquals("5.5.7", mod.getVersion());
        assertEquals("GeckoLib is an animation engine for Minecraft Mods, with support for complex 3D keyframe-based animations, 30+ easings, concurrent animation support, sound and particle keyframes, event keyframes, and more.", mod.getDescription());
        assertEquals("MIT", mod.getLicense());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("Tslat", mod.getAuthors().getFirst());
    }

    @Test
    public void testLegacyForgeMod()
    {
        Mod mod = new Mod(resourcesDir.resolve("mods", "geckolib-forge-1.12.2-3.0.31.jar"));

        assertEquals("geckolib3", mod.getModId());
        assertEquals("GeckoLib Example Mod", mod.getName());
        assertEquals("3.0.31", mod.getVersion());
        assertEquals("GeckoLib is an animation engine for modders. It supports forward-kinematic animations and renders bedrock entity geo models.", mod.getDescription());
        assertEquals("Unknown", mod.getLicense());
        assertEquals(2, mod.getAuthors().size());
        assertEquals("Gecko", mod.getAuthors().getFirst());
    }

    @Test
    public void testNeoforgeMod()
    {
        Mod mod = new Mod(resourcesDir.resolve("mods", "geckolib-neoforge-26.3-5.5.7.jar"));

        assertEquals("geckolib", mod.getModId());
        assertEquals("GeckoLib 5", mod.getName());
        assertEquals("5.5.7", mod.getVersion());
        assertEquals("GeckoLib is an animation engine for Minecraft Mods, with support for complex 3D keyframe-based animations, 30+ easings, concurrent animation support, sound and particle keyframes, event keyframes, and more.", mod.getDescription());
        assertEquals("MIT", mod.getLicense());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("Tslat", mod.getAuthors().getFirst());
    }

    @Test
    public void testQuiltFabricMod()
    {
        Mod mod = new Mod(resourcesDir.resolve("mods", "geckolib-fabric-1.20.1-4.2.3.jar"));

        assertEquals("geckolib", mod.getModId());
        assertEquals("Geckolib", mod.getName());
        assertEquals("4.2.3", mod.getVersion());
        assertEquals("GeckoLib is an animation engine for Minecraft Mods, with support for complex 3D keyframe-based animations, 30+ easings, concurrent animation support, sound and particle keyframes, event keyframes, and more.", mod.getDescription());
        assertEquals("MIT", mod.getLicense());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("Gecko, Eliot, AzureDoom, Hydos, DerToaster, Tslat, KyoSleep, Witixin", mod.getAuthors().getFirst());
    }


}
