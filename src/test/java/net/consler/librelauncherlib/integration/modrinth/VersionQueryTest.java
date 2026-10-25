package net.consler.librelauncherlib.integration.modrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionQueryTest
{
    @Test
    void encodesVersionFiltersAsJsonQueryParameters()
    {
        VersionQuery query = new VersionQuery()
                .loader("fabric", "quilt")
                .gameVersion("1.21.1")
                .featuredOnly(true);

        assertEquals(
                "loaders=%5B%22fabric%22%2C%22quilt%22%5D"
                        + "&game_versions=%5B%221.21.1%22%5D&featured=true",
                query.toQueryString()
        );
    }

    @Test
    void rejectsEmptyFilterValues()
    {
        assertThrows(IllegalArgumentException.class, () -> new VersionQuery().loader());
        assertThrows(IllegalArgumentException.class, () -> new VersionQuery().gameVersion(" "));
    }
}
