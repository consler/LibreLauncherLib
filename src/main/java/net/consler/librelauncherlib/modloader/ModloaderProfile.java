package net.consler.librelauncherlib.modloader;

/**
 * Represents a modloader profile with an ID and version.
 * For vanilla, loaderVersion can be null.
 */
public record ModloaderProfile(String loaderId, String loaderVersion)
{
    /**
     * Constant representing ID of the Fabric mod loader.
     */
    public static final String FABRIC_ID = "fabric";
    /**
     * Constant representing ID of the Forge mod loader.
     */
    public static final String FORGE_ID = "forge";
    /**
     * Constant representing ID of the NeoForge mod loader.
     */
    public static final String NEOFORGE_ID = "neoforge";
    /**
     * Constant representing ID of the Quilt mod loader.
     */
    public static final String QUILT_ID = "quilt";
    /**
     * Constant representing Vanilla ID.
     */
    public static final String VANILLA_ID = "vanilla";

    /**
     * Returns a modloader profile for vanilla Minecraft.
     *
     * @return A modloader profile for vanilla Minecraft.
     */
    public static ModloaderProfile VANILLA()
    {
        return new ModloaderProfile(VANILLA_ID, null);
    }
}
