package net.consler.librelauncherlib.integration.modrinth;

/**
 * Supported project types on Modrinth.
 */
public enum ModrinthProjectType
{
    MOD("mod"),
    MODPACK("modpack"),
    RESOURCE_PACK("resourcepack"),
    SHADER("shader"),
    PLUGIN("plugin"),
    DATAPACK("datapack");

    private final String id;

    ModrinthProjectType(String id)
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

    /**
     * @return The facet string for filtering searches by this type, e.g. "project_type:mod".
     */
    public String toFacet()
    {
        return "project_type:" + id;
    }

    /**
     * @param id The Modrinth API identifier, e.g. "resourcepack".
     * @return The matching type, or null if unknown.
     */
    public static ModrinthProjectType fromId(String id)
    {
        if (id == null) return null;
        for (ModrinthProjectType type : values())
        {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}