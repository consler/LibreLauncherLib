package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record ModrinthProject(
        @SerializedName(value = "id", alternate = {"project_id"})
        String id,
        String slug,
        String title,
        String description,
        String body,
        @SerializedName("project_type")
        String projectType,
        @SerializedName("client_side")
        String clientSide,
        @SerializedName("server_side")
        String serverSide,
        @SerializedName("icon_url")
        String iconUrl,
        String author,
        long downloads,
        long followers,
        List<String> categories,
        @SerializedName("game_versions")
        List<String> gameVersions,
        List<String> loaders,
        List<String> versions
) {}