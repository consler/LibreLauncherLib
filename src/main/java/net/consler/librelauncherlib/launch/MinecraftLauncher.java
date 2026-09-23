package net.consler.librelauncherlib.launch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.exception.LaunchException;
import net.consler.librelauncherlib.exception.LibraryException;
import net.consler.librelauncherlib.exception.VersionJsonMissingException;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.utill.MavenHelper;
import net.consler.librelauncherlib.utill.SystemHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.consler.librelauncherlib.utill.SystemHelper.getArchitecture;

public class MinecraftLauncher
{
    /**
     * Launches a Minecraft game process for the supplied profile.
     *
     * @param launchProfile launch configuration
     * @return the started game process
     */
    public Process launch(LaunchProfile launchProfile, AuthProfile authProfile, ModloaderProfile modloaderProfile)
    {
        try
        {
            Path gameDir = launchProfile.gameDir();
            String version = launchProfile.version();

            Path versionJsonPath = resolveFile(gameDir, version, ".json");
            if (!Files.exists(versionJsonPath))
            {
                throw new VersionJsonMissingException("Version JSON missing for " + version + ". Run the installer first.");
            }

            Path nativesDir = gameDir.resolve("natives");
            Path assetsDir = gameDir.resolve("assets");

            JsonObject versionDetails = JsonParser.parseString(Files.readString(versionJsonPath)).getAsJsonObject();

            String assetIndex = versionDetails.has("assetIndex") && versionDetails.getAsJsonObject("assetIndex").has("id")
                    ? versionDetails.getAsJsonObject("assetIndex").get("id").getAsString()
                    : version;

            JsonArray allLibraries = versionDetails.has("libraries") ? versionDetails.getAsJsonArray("libraries") : new JsonArray();
            String mainClass;

            String loaderId = modloaderProfile.loaderId() != null ? modloaderProfile.loaderId().toLowerCase() : "vanilla";
            JsonObject loaderDetails = null;

            switch (loaderId)
            {
                case "fabric" ->
                {
                    Path fabricJsonPath = gameDir.resolve("fabric-profile.json");
                    if (Files.exists(fabricJsonPath))
                    {
                        loaderDetails = JsonParser.parseString(Files.readString(fabricJsonPath)).getAsJsonObject();
                    }
                    mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
                }
                case "quilt" ->
                {
                    Path quiltJsonPath = gameDir.resolve("quilt-profile.json");
                    if (Files.exists(quiltJsonPath))
                    {
                        loaderDetails = JsonParser.parseString(Files.readString(quiltJsonPath)).getAsJsonObject();
                    }
                    mainClass = "org.quiltmc.loader.impl.launch.knot.KnotClient";
                }
                case "forge" ->
                {
                    Path forgeJsonPath = gameDir.resolve("forge-profile.json");
                    if (Files.exists(forgeJsonPath))
                    {
                        loaderDetails = JsonParser.parseString(Files.readString(forgeJsonPath)).getAsJsonObject();
                    }

                    if (loaderDetails != null && loaderDetails.has("mainClass"))
                    {
                        mainClass = loaderDetails.get("mainClass").getAsString();
                    }
                    else
                    {
                        mainClass = versionDetails.has("mainClass") ? versionDetails.get("mainClass").getAsString() : "cpw.mods.bootstraplauncher.BootstrapLauncher";
                    }
                }
                case "neoforge" ->
                {
                    Path neoforgeJsonPath = gameDir.resolve("neoforge-profile.json");
                    if (Files.exists(neoforgeJsonPath))
                    {
                        loaderDetails = JsonParser.parseString(Files.readString(neoforgeJsonPath)).getAsJsonObject();
                    }

                    if (loaderDetails != null && loaderDetails.has("mainClass"))
                    {
                        mainClass = loaderDetails.get("mainClass").getAsString();
                    }
                    else
                    {
                        mainClass = versionDetails.has("mainClass") ? versionDetails.get("mainClass").getAsString() : "net.neoforged.neoforge.bootstrap.NeoForgeBootstrap";
                    }
                }
                default -> mainClass = versionDetails.get("mainClass").getAsString();
            }

            if (loaderDetails != null)
            {
                if (loaderDetails.has("libraries"))
                {
                    for (JsonElement lib : loaderDetails.getAsJsonArray("libraries"))
                    {
                        allLibraries.add(lib);
                    }
                }

                if (loaderDetails.has("arguments"))
                {
                    JsonObject loaderArgs = loaderDetails.getAsJsonObject("arguments");
                    if (!versionDetails.has("arguments"))
                    {
                        versionDetails.add("arguments", new JsonObject());
                    }
                    JsonObject baseArgs = versionDetails.getAsJsonObject("arguments");

                    if (loaderArgs.has("jvm"))
                    {
                        if (!baseArgs.has("jvm")) baseArgs.add("jvm", new JsonArray());
                        baseArgs.getAsJsonArray("jvm").addAll(loaderArgs.getAsJsonArray("jvm"));
                    }
                    if (loaderArgs.has("game"))
                    {
                        if (!baseArgs.has("game")) baseArgs.add("game", new JsonArray());
                        baseArgs.getAsJsonArray("game").addAll(loaderArgs.getAsJsonArray("game"));
                    }
                }
                else if (loaderDetails.has("minecraftArguments"))
                {
                    versionDetails.add("minecraftArguments", loaderDetails.get("minecraftArguments"));
                }
            }

            String jarVersion = versionDetails.has("inheritsFrom")
                    ? versionDetails.get("inheritsFrom").getAsString()
                    : version;

            Path clientJarPath = resolveFile(gameDir, jarVersion, ".jar");
            if (!Files.exists(clientJarPath)) throw new FileNotFoundException("Minecraft client JAR missing for version " + jarVersion + " at resolved path.");

            String classPath = buildClassPath(allLibraries, gameDir, clientJarPath);
            Path librariesDir = gameDir.resolve("libraries");

            Map<String, String> args = new HashMap<>();
            args.put("${auth_player_name}", authProfile.username());
            args.put("${version_name}", version);
            args.put("${game_directory}", gameDir.toAbsolutePath().toString());
            args.put("${assets_root}", assetsDir.toAbsolutePath().toString());
            args.put("${assets_index_name}", assetIndex);
            args.put("${auth_uuid}", authProfile.uuid());
            args.put("${auth_access_token}", authProfile.accessToken());
            args.put("${user_properties}", "{}");
            args.put("${user_type}", "msa");
            args.put("${version_type}", "release");
            args.put("${natives_directory}", nativesDir.toAbsolutePath().toString());
            args.put("${library_directory}", librariesDir.toAbsolutePath().toString());
            args.put("${classpath_separator}", File.pathSeparator);
            args.put("${launcher_name}", launchProfile.launcherName());
            args.put("${launcher_version}", launchProfile.launcherVersion());
            args.put("${classpath}", classPath);

            List<String> command = new ArrayList<>();
            command.add(launchProfile.javaPath().toAbsolutePath().toString());
            command.add("-Xmx" + launchProfile.ramMb() + "M");
            command.add("-Xms" + launchProfile.ramMb() + "M");
            command.addAll(parseArguments(versionDetails, "jvm", args));
            command.add(mainClass);
            command.addAll(parseArguments(versionDetails, "game", args));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(gameDir.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            return pb.redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        }
        catch (Exception e)
        {
            if (e instanceof LibraryException) throw (LibraryException) e;
            throw new LaunchException("Failed to launch Minecraft for version " + launchProfile.version(), e);
        }
    }

    private Path resolveFile(Path gameDir, String version, String extension)
    {
        Path mojangPath = gameDir.resolve("versions").resolve(version).resolve(version + extension);
        if (Files.exists(mojangPath)) return mojangPath;

        Path flatPath = gameDir.resolve(version + extension);
        if (Files.exists(flatPath)) return flatPath;

        return mojangPath;
    }

    private String buildClassPath(JsonArray libraries, Path gameDir, Path clientJarPath)
    {
        StringBuilder sb = new StringBuilder();
        Path librariesDir = gameDir.resolve("libraries");
        String osName = SystemHelper.getOS();

        for (JsonElement elem : libraries)
        {
            JsonObject lib = elem.getAsJsonObject();

            if (lib.has("rules") && !isAllowedByRules(lib.getAsJsonArray("rules"), osName)) continue;

            if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("artifact"))
            {
                String path = lib.getAsJsonObject("downloads").getAsJsonObject("artifact").get("path").getAsString();
                Path libPath = librariesDir.resolve(path);
                if (Files.exists(libPath))
                {
                    sb.append(libPath.toAbsolutePath()).append(File.pathSeparator);
                }
            }
            else if (lib.has("name"))
            {
                String mavenName = lib.get("name").getAsString();
                String path = MavenHelper.toJarPath(mavenName);
                Path libPath = librariesDir.resolve(path);
                if (Files.exists(libPath))
                {
                    sb.append(libPath.toAbsolutePath()).append(File.pathSeparator);
                }
            }
        }

        sb.append(clientJarPath.toAbsolutePath());
        return sb.toString();
    }

    private List<String> parseArguments(JsonObject versionDetails, String argType, Map<String, String> placeholders)
    {
        List<String> args = new ArrayList<>();

        // 1.12-
        if (argType.equals("game") && versionDetails.has("minecraftArguments"))
        {
            String[] legacyArgs = versionDetails.get("minecraftArguments").getAsString().split(" ");
            for (String arg : legacyArgs)
            {
                args.add(replacePlaceholders(arg, placeholders));
            }
            return args;
        }

        // 1.13+
        if (versionDetails.has("arguments") && versionDetails.getAsJsonObject("arguments").has(argType))
        {
            JsonArray jsonArgs = versionDetails.getAsJsonObject("arguments").getAsJsonArray(argType);
            String osName = SystemHelper.getOS();

            for (JsonElement elem : jsonArgs)
            {
                if (elem.isJsonPrimitive())
                {
                    args.add(replacePlaceholders(elem.getAsString(), placeholders));
                }
                else if (elem.isJsonObject())
                {
                    JsonObject argObj = elem.getAsJsonObject();
                    if (isAllowedByRules(argObj.getAsJsonArray("rules"), osName))
                    {
                        JsonElement value = argObj.get("value");
                        if (value.isJsonArray())
                        {
                            for (JsonElement v : value.getAsJsonArray())
                            {
                                args.add(replacePlaceholders(v.getAsString(), placeholders));
                            }
                        }
                        else
                        {
                            args.add(replacePlaceholders(value.getAsString(), placeholders));
                        }
                    }
                }
            }
        }

        if (argType.equals("jvm") && args.isEmpty())
        {
            args.add("-Djava.library.path=" + placeholders.get("${natives_directory}"));
            args.add("-cp");
            args.add(placeholders.get("${classpath}"));
        }

        return args;
    }

    private String replacePlaceholders(String arg, Map<String, String> placeholders)
    {
        String processed = arg;
        for (Map.Entry<String, String> entry : placeholders.entrySet())
        {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        return processed;
    }

    private boolean isAllowedByRules(JsonArray rules, String osName)
    {
        boolean allowed = false;

        for (JsonElement elem : rules)
        {
            JsonObject rule = elem.getAsJsonObject();
            String action = rule.get("action").getAsString();
            boolean match = true;

            if (rule.has("os"))
            {
                JsonObject os = rule.getAsJsonObject("os");
                if (os.has("name")) match = os.get("name").getAsString().equalsIgnoreCase(osName);
                if (os.has("arch")) match = match && os.get("arch").getAsString().equalsIgnoreCase(getArchitecture());
            }

            if (rule.has("features")) match = false;

            if (match) allowed = action.equals("allow");
        }
        return allowed;
    }
}