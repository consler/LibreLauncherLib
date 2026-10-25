package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.Gson;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for filters accepted by Modrinth's project versions endpoint.
 */
public final class VersionQuery
{
    private static final Gson GSON = new Gson();

    private final List<String> loaders = new ArrayList<>();
    private final List<String> gameVersions = new ArrayList<>();
    private boolean featuredOnly;

    /**
     * Restricts results to any of the given loaders.
     *
     * @param loaders Loader IDs such as {@code fabric} or {@code quilt}.
     * @return This query.
     */
    public VersionQuery loader(String... loaders)
    {
        addValues(this.loaders, loaders, "loader");
        return this;
    }

    /**
     * Restricts results to any of the given Minecraft versions.
     *
     * @param gameVersions Minecraft version IDs.
     * @return This query.
     */
    public VersionQuery gameVersion(String... gameVersions)
    {
        addValues(this.gameVersions, gameVersions, "game version");
        return this;
    }

    /**
     * Sets whether to include only featured versions.
     *
     * @param featuredOnly True to include featured versions only.
     * @return This query.
     */
    public VersionQuery featuredOnly(boolean featuredOnly)
    {
        this.featuredOnly = featuredOnly;
        return this;
    }

    String toQueryString()
    {
        List<String> params = new ArrayList<>();
        if (!loaders.isEmpty()) params.add("loaders=" + enc(GSON.toJson(loaders)));
        if (!gameVersions.isEmpty()) params.add("game_versions=" + enc(GSON.toJson(gameVersions)));
        if (featuredOnly) params.add("featured=true");
        return String.join("&", params);
    }

    private static void addValues(List<String> target, String[] values, String name)
    {
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("at least one " + name + " is required");

        for (String value : values)
        {
            if (value == null || value.isBlank())
                throw new IllegalArgumentException(name + " must not be empty");
            target.add(value);
        }
    }

    private static String enc(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
