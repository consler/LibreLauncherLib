package net.consler.librelauncherlib.integration.modrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModrinthTest
{
    @Test
    public void searchSodiumTest()
    {
        Modrinth.SearchResult page = Modrinth.search(new ModrinthSearchQuery("sodium")
                        .projectType(ModrinthProjectType.MOD)
                        .limit(1),
                "consler/LibreLauncherLib").join();

        assertEquals("AANobbMI", page.hits().getFirst().id());
    }

    @Test
    public void lookupProjectByFileSha1Test()
    {
        ModrinthVersion expectedVersion = Modrinth.getProjectVersions("AANobbMI", "consler/LibreLauncherLib")
                .join().getFirst();
        ModrinthVersion.VersionFile file = expectedVersion.primaryFile();
        assertNotNull(file);

        String sha1 = file.hashes().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("sha1"))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElseThrow();

        ModrinthVersion actualVersion = Modrinth.getVersionBySha1(sha1, "consler/LibreLauncherLib").join();
        assertEquals(expectedVersion.id(), actualVersion.id());

        ModrinthProject project = Modrinth.getProjectBySha1(sha1, "consler/LibreLauncherLib").join();
        assertEquals("AANobbMI", project.id());
    }
}
