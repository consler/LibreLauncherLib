package net.consler.librelauncherlib.install;

import net.consler.librelauncherlib.exception.NativesExtractionException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class NativeExtractor
{

    public void extractNatives(List<Path> nativeZips, Path nativesDir)
    {
        try
        {
            if (nativeZips.isEmpty()) throw new NativesExtractionException("No native archives found to extract.");

            for (Path zipPath : nativeZips)
            {
                if (!Files.exists(zipPath)) throw new NativesExtractionException("Native ZIP missing: " + zipPath);

                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath)))
                {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null)
                    {
                        String name = entry.getName();
                        if (!entry.isDirectory() && !name.startsWith("META-INF"))
                        {
                            if (name.endsWith(".so") || name.endsWith(".dll") || name.endsWith(".dylib"))
                            {
                                Path filename = Path.of(name).getFileName();
                                Path target = nativesDir.resolve(filename);

                                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (e instanceof NativesExtractionException) throw (NativesExtractionException) e;
            throw new net.consler.librelauncherlib.exception.LibraryException("Failed to extract native libraries", e);
        }
    }
}