package net.consler.librelauncherlib.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.util.DownloadTask;
import net.consler.librelauncherlib.util.SystemHelper;

import java.nio.file.Path;
import java.util.List;

class LibraryProcessor
{

    private final String osName;
    private final String archName;
    private final String archBits;

    public LibraryProcessor()
    {
        this.osName = SystemHelper.getOS();
        this.archName = SystemHelper.getManifestArchitecture();
        this.archBits = SystemHelper.getArchitecture().contains("64") ? "64" : "32";
    }

    public void processLibraries(JsonArray libraries, Path librariesDir, List<DownloadTask> tasks, List<Path> nativeZips)
    {
        for (JsonElement elem : libraries)
        {
            JsonObject lib = elem.getAsJsonObject();
            if (!isAllowedByRules(lib.getAsJsonArray("rules"))) continue;

            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads == null) continue;

            if (downloads.has("artifact"))
            {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                Path target = librariesDir.resolve(artifact.get("path").getAsString());

                tasks.add(DownloadTask.of(artifact, target));

                if (isNativeLibrary(lib)) nativeZips.add(target);
            }

            if (lib.has("natives") && lib.getAsJsonObject("natives").has(this.osName))
            {
                String classifier = lib.getAsJsonObject("natives").get(this.osName).getAsString().replace("${arch}", this.archBits);
                if (downloads.has("classifiers") && downloads.getAsJsonObject("classifiers").has(classifier))
                {
                    JsonObject nativeArtifact = downloads.getAsJsonObject("classifiers").getAsJsonObject(classifier);
                    Path target = librariesDir.resolve(nativeArtifact.get("path").getAsString());

                    tasks.add(DownloadTask.of(nativeArtifact, target));
                    nativeZips.add(target);
                }
            }
        }
    }

    private boolean isNativeLibrary(JsonObject lib)
    {
        String[] coordinates = lib.get("name").getAsString().split(":");
        return coordinates.length >= 4 && coordinates[3].startsWith("native");
    }

    private boolean isAllowedByRules(JsonArray rules)
    {
        if (rules == null || rules.isEmpty()) return true;

        boolean allowed = false;

        for (JsonElement elem : rules)
        {
            JsonObject rule = elem.getAsJsonObject();
            String action = rule.get("action").getAsString();
            boolean match = true;

            if (rule.has("os"))
            {
                JsonObject osObj = rule.getAsJsonObject("os");

                if (osObj.has("name"))
                {
                    String ruleOs = osObj.get("name").getAsString();
                    if (!ruleOs.equalsIgnoreCase(this.osName)) match = false;
                }

                if (osObj.has("arch"))
                {
                    String ruleArch = osObj.get("arch").getAsString();
                    if (!ruleArch.equalsIgnoreCase(this.archName)) match = false;
                }
            }

            if (rule.has("features")) match = false;

            if (match) allowed = action.equals("allow");
        }

        return allowed;
    }
}