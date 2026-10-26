package net.consler.librelauncherlib.instance;

import net.consler.librelauncherlib.util.DownloadManager;
import net.consler.librelauncherlib.util.DownloadTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Installs the files and overrides from a Modrinth modpack into a game directory.
 */
public final class MrpackInstaller
{
    private final Mrpack pack;

    public MrpackInstaller(Mrpack pack)
    {
        if (pack == null) throw new IllegalArgumentException("pack must not be null");
        this.pack = pack;
    }

    /**
     * Installs applicable indexed files and overrides into the game directory.
     * The Minecraft version and mod loader must be installed separately.
     *
     * @param gameDirectory Minecraft game directory to install into.
     * @param side Environment whose unsupported files should be excluded.
     * @return Paths of installed files and paths of installed files under {@code mods}.
     * @throws IOException If the archive cannot be read or files cannot be installed.
     */
    public InstallationResult installTo(Path gameDirectory, Side side) throws IOException
    {
        if (gameDirectory == null) throw new IllegalArgumentException("gameDirectory must not be null");
        if (side == null) throw new IllegalArgumentException("side must not be null");

        Path root = gameDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path staging = Files.createTempDirectory("mrpack-install-");
        try
        {
            List<StagedFile> downloads = prepareDownloads(root, staging, side);
            List<OverrideFile> overrides = readOverrides(root, side);

            List<DownloadTask> tasks = downloads.stream()
                    .map(file -> new DownloadTask(file.url(), file.stagedPath(), file.sha1(), file.size()))
                    .toList();
            new DownloadManager().downloadBatch(tasks);

            for (StagedFile file : downloads)
            {
                verifyHashes(file);
                Path target = safeTarget(root, file.relativePath());
                createSafeParentDirectories(root, target);
                Files.move(file.stagedPath(), target, StandardCopyOption.REPLACE_EXISTING);
            }

            try (ZipFile archive = new ZipFile(pack.getPackFile().toFile()))
            {
                for (OverrideFile override : overrides)
                {
                    Path target = safeTarget(root, override.relativePath());
                    createSafeParentDirectories(root, target);
                    ZipEntry entry = archive.getEntry(override.entryName());
                    if (entry == null) throw new IOException("Modpack override disappeared: " + override.entryName());
                    try (InputStream input = archive.getInputStream(entry))
                    {
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            List<Path> installed = new ArrayList<>(downloads.size() + overrides.size());
            for (StagedFile file : downloads)
                installed.add(safeTarget(root, file.relativePath()));
            for (OverrideFile file : overrides)
                installed.add(safeTarget(root, file.relativePath()));
            List<Path> mods = installed.stream()
                    .filter(path -> root.relativize(path).getNameCount() > 1)
                    .filter(path -> root.relativize(path).getName(0).toString().equalsIgnoreCase("mods"))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .toList();

            return new InstallationResult(installed, mods);
        }
        finally
        {
            deleteTree(staging);
        }
    }

    private List<StagedFile> prepareDownloads(Path root, Path staging, Side side) throws IOException
    {
        List<StagedFile> files = new ArrayList<>();
        Set<Path> destinations = new HashSet<>();
        for (Mrpack.IndexFile file : pack.getFiles())
        {
            if (isUnsupported(file, side)) continue;
            Path relative = safeRelativePath(file.path());
            if (!destinations.add(relative))
                throw new IOException("Duplicate modpack file path: " + file.path());
            safeTarget(root, relative);

            if (file.downloads().isEmpty())
                throw new IOException("Modpack file has no download URL: " + file.path());
            String url = requireHttpsUrl(file.downloads().getFirst(), file.path());
            Path staged = safeTarget(staging, relative);
            String sha1 = hash(file, "sha1");
            files.add(new StagedFile(relative, staged, url, sha1, file.fileSize(), file.hashes()));
        }
        return files;
    }

    private List<OverrideFile> readOverrides(Path root, Side side) throws IOException
    {
        List<OverrideFile> files = new ArrayList<>();
        try (ZipFile archive = new ZipFile(pack.getPackFile().toFile()))
        {
            List<String> overrideDirectories = new ArrayList<>();
            overrideDirectories.add(pack.getOverrides());
            overrideDirectories.add(side == Side.CLIENT ? "client-overrides" : "server-overrides");
            for (String directory : overrideDirectories)
            {
                Path prefixPath = safeRelativePath(directory);
                String prefix = prefixPath.toString().replace('\\', '/') + "/";
                var entries = archive.entries();
                while (entries.hasMoreElements())
                {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entry.isDirectory() || !entryName.startsWith(prefix)) continue;

                    String relativeName = entryName.substring(prefix.length());
                    Path relative = safeRelativePath(relativeName);
                    safeTarget(root, relative);
                    files.add(new OverrideFile(entryName, relative));
                }
            }
        }
        return files;
    }

    private boolean isUnsupported(Mrpack.IndexFile file, Side side)
    {
        String environment = file.env().get(side == Side.CLIENT ? "client" : "server");
        return "unsupported".equalsIgnoreCase(environment);
    }

    private static String requireHttpsUrl(String url, String path) throws IOException
    {
        try
        {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IOException("Modpack download URL must be HTTPS: " + path);
            return uri.toString();
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("Invalid modpack download URL: " + path, e);
        }
    }

    private static String hash(Mrpack.IndexFile file, String algorithm)
    {
        return file.hashes().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(algorithm))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void verifyHashes(StagedFile file) throws IOException
    {
        for (var expected : file.hashes().entrySet())
        {
            try
            {
                MessageDigest digest = MessageDigest.getInstance(expected.getKey().replace("-", ""));
                try (InputStream input = Files.newInputStream(file.stagedPath()))
                {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                }
                String actual = HexFormat.of().formatHex(digest.digest());
                if (!actual.equalsIgnoreCase(expected.getValue()))
                    throw new IOException("Checksum mismatch for modpack file: " + file.relativePath());
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new IOException("Unsupported hash algorithm in modpack: " + expected.getKey(), e);
            }
        }
    }

    private static Path safeRelativePath(String value) throws IOException
    {
        if (value == null || value.isBlank() || value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0)
            throw new IOException("Invalid modpack path: " + value);
        Path path = Path.of(value);
        if (path.isAbsolute() || path.getNameCount() == 0)
            throw new IOException("Invalid modpack path: " + value);
        for (Path component : path)
        {
            if (component.toString().equals(".") || component.toString().equals(".."))
                throw new IOException("Invalid modpack path: " + value);
        }
        return path.normalize();
    }

    private static Path safeTarget(Path root, Path relative) throws IOException
    {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root))
            throw new IOException("Modpack path escapes target directory: " + relative);
        return target;
    }

    private static void createSafeParentDirectories(Path root, Path target) throws IOException
    {
        Path relativeParent = root.relativize(target.getParent());
        Path current = root;
        for (Path component : relativeParent)
        {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current))
                throw new IOException("Refusing to install through a symbolic link: " + current);
            if (Files.exists(current) && !Files.isDirectory(current))
                throw new IOException("Install path component is not a directory: " + current);
            Files.createDirectories(current);
        }
        if (Files.isSymbolicLink(target))
            throw new IOException("Refusing to replace a symbolic link: " + target);
    }

    private static void deleteTree(Path root) throws IOException
    {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root))
        {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        }
    }

    public enum Side
    {
        CLIENT,
        SERVER
    }

    public record InstallationResult(List<Path> installedFiles, List<Path> installedMods)
    {
        public InstallationResult
        {
            installedFiles = List.copyOf(installedFiles);
            installedMods = List.copyOf(installedMods);
        }
    }

    private record StagedFile(
            Path relativePath,
            Path stagedPath,
            String url,
            String sha1,
            long size,
            java.util.Map<String, String> hashes
    ) {}

    private record OverrideFile(String entryName, Path relativePath) {}
}
