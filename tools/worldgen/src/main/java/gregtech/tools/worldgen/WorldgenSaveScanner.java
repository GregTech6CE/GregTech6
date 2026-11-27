package gregtech.tools.worldgen;

import gregtech.content.block.SpecialOreDefinitions;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.tools.worldgen.WorldgenComparison.Region;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

/** Exports a square of already generated chunks from a world copy. Never opens a region for writing. */
public final class WorldgenSaveScanner {
    private WorldgenSaveScanner() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 6 || args.length > 7)
            throw new IllegalArgumentException("Expected: world output dimension chunkX chunkZ side [mineral]");
        Path save = Path.of(args[0]).toRealPath();
        Path regionDir = AnvilChunks.region(save, args[2]);
        if (!Files.isDirectory(regionDir)) throw new IOException("Missing region directory " + regionDir);
        var region = new Region(
                AnvilChunks.seed(save),
                args[2],
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4]),
                Integer.parseInt(args[5]));
        if (region.side() < 1 || region.side() > 256) throw new IllegalArgumentException("Side must be 1..256 chunks");
        Math.addExact(region.originX(), region.side() - 1);
        Math.addExact(region.originZ(), region.side() - 1);
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new FileAlreadyExistsException(output.toString());
        Path ancestor = output.getParent();
        while (ancestor != null && Files.notExists(ancestor)) ancestor = ancestor.getParent();
        if (ancestor == null) throw new IOException("Cannot resolve output directory " + output);
        output = ancestor.toRealPath().resolve(ancestor.relativize(output));
        if (output.startsWith(save)) throw new IOException("Choose an output directory outside the save");
        Files.createDirectories(output.getParent());
        Files.createDirectory(output);
        scan(regionDir, output, region, args.length == 7 ? args[6] : "*");
    }

    private record Ore(String material, boolean small) {}

    private static final class Blocks {
        final Map<String, Ore> ores = new HashMap<>();
        final Set<String> rock = new HashSet<>(Set.of(
                "minecraft:stone",
                "minecraft:granite",
                "minecraft:diorite",
                "minecraft:andesite",
                "minecraft:tuff",
                "minecraft:deepslate",
                "minecraft:netherrack",
                "minecraft:basalt",
                "minecraft:blackstone",
                "minecraft:end_stone"));

        Blocks() {
            WorldgenSampler.materialCatalog();
            for (var material : GTMaterialRegistry.sortedMaterials()) {
                String slug = material.getName().toLowerCase(Locale.ROOT);
                var name = material.resolve().getName();
                ores.put("gt6ce:ore_" + slug, new Ore(name, false));
                ores.put("gt6ce:ore_small_" + slug, new Ore(name, true));
            }
            SpecialOreDefinitions.rocks()
                    .forEach((icon, spec) -> ores.put("gt6ce:block_" + icon, new Ore(spec.material(), false)));
            for (var spec : SpecialOreDefinitions.vanilla())
                ores.put("gt6ce:block_" + spec.icon(), new Ore(spec.material(), false));
            for (String icon : SpecialOreDefinitions.ORDERED_ICONS) {
                if (!icon.startsWith("crystal_ore_")) continue;
                String slug = icon.substring("crystal_ore_".length());
                var base = ores.get("gt6ce:ore_" + slug);
                if (base != null) ores.put("gt6ce:" + icon, base);
            }
            for (var type : StoneType.values())
                rock.add("gt6ce:" + type.registryId() + "_" + StoneVariant.STONE.registrySuffix());
        }
    }

    private static final class Counts {
        long full, poor, bedrock, raw;
    }

    private static void scan(Path regionDir, Path output, Region region, String mineral) throws IOException {
        var blocks = new Blocks();
        var rocks = new TreeMap<String, Long>();
        var heatmap = new WorldgenHeatmap(region.side() * region.side());
        int issues = 0;
        try (var ores = Files.newBufferedWriter(output.resolve("ores.csv"));
                var markers = Files.newBufferedWriter(output.resolve("markers.csv"));
                var heights = Files.newBufferedWriter(output.resolve("heights.csv"));
                var skipped = Files.newBufferedWriter(output.resolve("skipped.csv"))) {
            ores.write(OreSampleRow.HEADER + "\n");
            markers.write("x,y,z,kind,material,item\n");
            heights.write("chunk_x,chunk_z,highest_section_y\n");
            skipped.write("chunk_x,chunk_z,section_y,reason\n");
            for (int dx = 0; dx < region.side(); dx++) {
                for (int dz = 0; dz < region.side(); dz++) {
                    int x = region.originX() + dx, z = region.originZ() + dz;
                    CompoundBinaryTag chunk;
                    try {
                        chunk = AnvilChunks.read(regionDir, x, z);
                        if (chunk == null) throw new IOException("chunk not saved");
                        String status = chunk.getString("Status");
                        if (!status.equals("full") && !status.equals("minecraft:full"))
                            throw new IOException("chunk not fully generated");
                        if (!(chunk.get("sections") instanceof ListBinaryTag))
                            throw new IOException("missing sections");
                    } catch (IOException | IllegalArgumentException e) {
                        skipped.write(x + "," + z + ",," + quote(e.getMessage()) + "\n");
                        issues++;
                        continue;
                    }
                    var counts = new TreeMap<String, Counts>();
                    var sections = new HashMap<Integer, Section>();
                    int highest = -5;
                    boolean complete = true;
                    for (var element : chunk.getList("sections", BinaryTagTypes.COMPOUND)) {
                        var sectionTag = (CompoundBinaryTag) element;
                        int y = sectionTag.getByte("Y");
                        if (sectionTag.get("block_states") == null) continue;
                        try {
                            var section = new Section(sectionTag.getCompound("block_states"));
                            sections.put(y, section);
                            if (section.count(blocks, counts, rocks)) highest = Math.max(highest, y);
                        } catch (IllegalArgumentException e) {
                            skipped.write(x + "," + z + "," + y + "," + quote(e.getMessage()) + "\n");
                            complete = false;
                            issues++;
                        }
                    }
                    if (complete) heights.write(x + "," + z + "," + highest + "\n");
                    for (var element : chunk.getList("block_entities", BinaryTagTypes.COMPOUND)) {
                        var tag = (CompoundBinaryTag) element;
                        int bx = tag.getInt("x"), by = tag.getInt("y"), bz = tag.getInt("z");
                        var section = sections.get(by >> 4);
                        if (section == null || (bx >> 4) != x || (bz >> 4) != z) continue;
                        var state = section.at((by & 15) * 256 + (bz & 15) * 16 + (bx & 15));
                        if (!state.getString("Name").equals("gt6ce:rock")) continue;
                        String material = tag.getString("material", "Stone");
                        String kind = tag.getBoolean("rawOre")
                                ? "raw_pebble"
                                : state.getCompound("Properties")
                                                .getString("appearance")
                                                .equals("indicator")
                                        ? "indicator"
                                        : "pebble";
                        markers.write(bx + "," + by + "," + bz + "," + kind + "," + quote(material) + ","
                                + quote(tag.getString("item")) + "\n");
                        if (kind.equals("raw_pebble") && material.matches("[A-Za-z0-9_.-]+"))
                            counts.computeIfAbsent(material, ignored -> new Counts()).raw++;
                    }
                    for (var entry : counts.entrySet()) {
                        var c = entry.getValue();
                        var row = new OreSampleRow(x, z, entry.getKey(), c.full, c.poor, c.bedrock, c.raw);
                        ores.write(row.encode() + "\n");
                        if (mineral.equals("*") || mineral.equalsIgnoreCase(row.material()))
                            heatmap.add(
                                    dx * region.side() + dz, row.normal(), row.poor(), row.bedrock(), row.rawPebbles());
                    }
                }
            }
        }
        try (var writer = Files.newBufferedWriter(output.resolve("rocks.csv"))) {
            writer.write("block,volume\n");
            for (var e : rocks.entrySet()) writer.write(e.getKey() + "," + e.getValue() + "\n");
        }
        var manifest = new Properties();
        manifest.setProperty("seed", Long.toString(region.seed()));
        manifest.setProperty("dimension", region.dimension());
        manifest.setProperty("origin_x", Integer.toString(region.originX()));
        manifest.setProperty("origin_z", Integer.toString(region.originZ()));
        manifest.setProperty("side", Integer.toString(region.side()));
        manifest.setProperty("complete", Boolean.toString(issues == 0));
        heatmap.writeSingle(output.resolve("ore-map.svg"), region, mineral, issues);
        try (var writer = Files.newBufferedWriter(output.resolve("sample.properties"))) {
            manifest.store(writer, "GT6CE saved chunks");
        }
        System.out.printf(
                "Scanned %d chunks; %d missing or unreadable entries. %s%n",
                region.side() * region.side(), issues, output);
    }

    private static String quote(String value) {
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    /** Validate every packed index before recording any counts for a section. */
    static final class Section {
        final ListBinaryTag palette;
        final long[] data;
        final int bits, perLong;
        final int[] counts;

        Section(CompoundBinaryTag states) {
            palette = states.getList("palette", BinaryTagTypes.COMPOUND);
            if (palette.size() < 1 || palette.size() > 4096) throw new IllegalArgumentException("Invalid palette size");
            for (int i = 0; i < palette.size(); i++)
                if (palette.getCompound(i).getString("Name").isEmpty())
                    throw new IllegalArgumentException("Missing block ID");
            counts = new int[palette.size()];
            data = states.getLongArray("data");
            bits = storageBits(palette.size(), data.length);
            perLong = 64 / bits;
            if (palette.size() == 1) {
                counts[0] = 4096;
            } else {
                for (int i = 0; i < 4096; i++) {
                    int entry = index(i);
                    if (entry >= counts.length) throw new IllegalArgumentException("Invalid packed index " + entry);
                    counts[entry]++;
                }
            }
        }

        private static int storageBits(int size, int words) {
            int minimum = Math.max(4, 32 - Integer.numberOfLeadingZeros(size - 1));
            if (size == 1 || words == packedLength(minimum)) return minimum;
            // Some writers retain a wider palette. Use it only when the section length identifies it.
            int width = 0;
            for (int bits = minimum + 1; bits <= 12; bits++) {
                if (words != packedLength(bits)) continue;
                if (width != 0) throw new IllegalArgumentException("Ambiguous packed palette width");
                width = bits;
            }
            if (width == 0) throw new IllegalArgumentException("Invalid packed length");
            return width;
        }

        private static int packedLength(int bits) {
            int perLong = 64 / bits;
            return (4096 + perLong - 1) / perLong;
        }

        private int index(int i) {
            return palette.size() == 1 ? 0 : (int) (data[i / perLong] >>> ((i % perLong) * bits)) & ((1 << bits) - 1);
        }

        CompoundBinaryTag at(int i) {
            return palette.getCompound(index(i));
        }

        boolean count(Blocks blocks, Map<String, Counts> ores, Map<String, Long> rocks) {
            boolean filled = false;
            for (int i = 0; i < counts.length; i++) {
                int count = counts[i];
                if (count == 0) continue;
                var state = palette.getCompound(i);
                String id = state.getString("Name");
                if (!id.equals("minecraft:air") && !id.equals("minecraft:cave_air") && !id.equals("minecraft:void_air"))
                    filled = true;
                var ore = blocks.ores.get(id);
                if (ore != null) {
                    var c = ores.computeIfAbsent(ore.material(), ignored -> new Counts());
                    if (state.getCompound("Properties").getString("stone").equals("bedrock")) c.bedrock += count;
                    else if (ore.small()) c.poor += count;
                    else c.full += count;
                } else if (blocks.rock.contains(id)) rocks.merge(id, (long) count, Long::sum);
            }
            return filled;
        }
    }
}
