package net.consler.librelauncherlib.modloader;

public record ModloaderProfile(String loaderId, String loaderVersion)
{
    public static final String FABRIC_ID = "fabric";
    public static final String FORGE_ID = "forge";
    public static final String NEOFORGE_ID = "neoforge";
    public static final String QUILT_ID = "quilt";
    public static final String VANILLA_ID = "vanilla";

    public static ModloaderProfile VANILLA()
    {
        return new ModloaderProfile(VANILLA_ID, null);
    }
}
