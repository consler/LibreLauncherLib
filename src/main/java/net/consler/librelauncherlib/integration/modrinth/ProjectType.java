package net.consler.librelauncherlib.integration.modrinth;

/**
 * Supported project types on Modrinth.
 */
public enum ProjectType
{
    MOD("mod"),
    MODPACK("modpack"),
    RESOURCE_PACK("resourcepack"),
    SHADER("shader"),
    PLUGIN("plugin"),
    DATAPACK("datapack");

    private final String id;

    ProjectType(String id)
    {
        this.id = id;
    }

    /**
     * @return The Modrinth API string identifier.
     */
    public String getId()
    {
        return id;
    }
}