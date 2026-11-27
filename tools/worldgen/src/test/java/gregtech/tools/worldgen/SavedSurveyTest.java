package gregtech.tools.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedSurveyTest {
    @TempDir
    Path directory;

    @Test
    void distinctOreCountsKeepTheirColumns() {
        var sample = new OreSampleRow(-1, 2, "Chromite", 17, 5, 3, 1);
        assertEquals("-1,2,Chromite,17,5,3,1", sample.encode());
        assertEquals(sample, OreSampleRow.decode(sample.encode()));
    }

    @Test
    void distinguishesUnsavedAndUnreadableRegionEntries() throws IOException {
        try (var region = new RandomAccessFile(directory.resolve("r.-1.0.mca").toFile(), "rw")) {
            region.setLength(8192);
            region.seek(31 * 4);
            region.writeInt(9 << 8 | 1);
        }
        assertNull(AnvilChunks.read(directory, -2, 0));
        assertThrows(IOException.class, () -> AnvilChunks.read(directory, -1, 0));
    }

    @Test
    void rejectsAnInvalidIndexEvenAtTheEndOfASection() {
        var states = ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                .add(CompoundBinaryTag.builder()
                        .putString("Name", "minecraft:air")
                        .build())
                .add(CompoundBinaryTag.builder()
                        .putString("Name", "gt6ce:ore_chromite")
                        .build())
                .build();
        long[] data = new long[256];
        data[0] = 1;
        var valid = CompoundBinaryTag.builder()
                .put("palette", states)
                .putLongArray("data", data)
                .build();
        assertEquals(
                "gt6ce:ore_chromite",
                new WorldgenSaveScanner.Section(valid).at(0).getString("Name"));
        data[255] = 2L << 60;
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorldgenSaveScanner.Section(valid.putLongArray("data", data)));
    }

    @Test
    void legacySliceMatchesHistoricalTableAndHonoursFilledSections() throws IOException {
        var legacy = new LegacyStrata(73, null);
        int barren = 0;
        for (int z = -256; z < 256; z++)
            for (int x = -256; x < 256; x++) {
                int index = legacy.index(x, -35, z);
                if (index < 0 || !legacy.layers().get(index).hasOre()) barren++;
            }
        assertEquals(176617, barren);
        assertTrue(legacy.index(0, 160, 0) >= 0);
        var heights = directory.resolve("heights.csv");
        Files.writeString(heights, "chunk_x,chunk_z,highest_section_y\n0,0,8\n-1,0,12\n");
        var terrain = new LegacyStrata(73, heights);
        assertEquals(-1, terrain.index(0, 145, 0));
        assertTrue(terrain.index(-1, 160, 0) >= 0);
        assertThrows(IllegalArgumentException.class, () -> terrain.index(16, 160, 0));
    }
}
