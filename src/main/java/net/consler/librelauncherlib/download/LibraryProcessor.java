package net.consler.librelauncherlib.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.utill.SystemHelper;

import java.nio.file.Path;
import java.util.List;

class LibraryProcessor
{

    private final String osName;

    public LibraryProcessor()
    {
        this.osName = SystemHelper.getOS();
    }

    public void processLibraries(JsonArray libraries, Path librariesDir, List<DownloadTask> tasks, List<Path> nativeZips)
    {
        for (JsonElement elem : libraries)
        {
            JsonObject lib = elem.getAsJsonObject();
            if (!isAllowedByRules(lib.getAsJsonArray("rules"))) continue;

            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads == null) continue;

            // 1.19+
            if (downloads.has("artifact"))
            {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                String pathStr = artifact.get("path").getAsString();
                Path target = librariesDir.resolve(pathStr);

                tasks.add(new DownloadTask(artifact.get("url").getAsString(), target));

                if (pathStr.contains("natives-" + this.osName) || pathStr.contains("native-" + this.osName)) nativeZips.add(target);
            }

            // 1.18-
            if (lib.has("natives") && lib.getAsJsonObject("natives").has(this.osName))
            {
                String classifier = lib.getAsJsonObject("natives").get(this.osName).getAsString();
                if (downloads.has("classifiers") && downloads.getAsJsonObject("classifiers").has(classifier))
                {
                    JsonObject nativeArtifact = downloads.getAsJsonObject("classifiers").getAsJsonObject(classifier);
                    Path target = librariesDir.resolve(nativeArtifact.get("path").getAsString());

                    tasks.add(new DownloadTask(nativeArtifact.get("url").getAsString(), target));
                    nativeZips.add(target);
                }
            }
        }
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
                    if (!ruleArch.equalsIgnoreCase(System.getProperty("os.arch"))) match = false;
                }
            }

            if (rule.has("features")) match = false;

            if (match) allowed = action.equals("allow");
        }

        return allowed;
    }
}