package gregtech.content.worldgen;

import gregtech.content.block.DenseOreBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.RockBlock;
import gregtech.content.block.RockOreBlock;
import gregtech.content.block.plant.BedrockFlowerBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.worldgen.StoneLayerField.Province;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.platform.Mc;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;

/** A vertical section of stored blocks, recorded alongside the seed's planned layers. */
final class WorldgenProfile {
    record Section(int x, int z, Axis axis, int width, int minY, int maxY) {
        int xAt(int column) {
            return axis == Axis.X ? x + column : x;
        }

        int zAt(int column) {
            return axis == Axis.Z ? z + column : z;
        }

        int height() {
            return maxY - minY + 1;
        }
    }

    record Export(Path path, int ores, int markers) {}

    private record RockLayer(String material, String color) {}

    private record StoredBlock(String id, String rock, String host, String ore, String form, String color) {}

    private record Marker(String kind, String material, String item) {
        private static final Marker NONE = new Marker("", "", "");
        private static final Marker FLOWER = new Marker("bedrock_flower", "", "");
    }

    private record Sample(int layer, Province province, StoredBlock block, Marker marker) {}

    private static final String CSV_HEADER =
            "x,z,min_y,max_y,province,planned_layer,block,rock_material,ore_host,ore_material,ore_form,marker,marker_material,marker_item\n";

    private final Section section;
    private final StoneLayerField field;
    private final RockLayer[] rockLayers;
    private final int[] layers;
    private final Province[] provinces;
    private final Map<BlockState, StoredBlock> blockInfo = new HashMap<>();
    private final Map<Block, Marker> plants;
    private int ores;
    private int markers;

    private WorldgenProfile(ServerLevel level, Section section, boolean planned) {
        this.section = section;
        field = planned ? new StoneLayerField(level.getSeed()) : null;
        rockLayers = planned
                ? StoneLayerField.layers().stream()
                        .map(layer -> new RockLayer(layer.material(), rockColor(layer.material())))
                        .toArray(RockLayer[]::new)
                : new RockLayer[0];
        layers = new int[section.height()];
        provinces = new Province[section.height()];
        plants = plantMarkers(level.dimension().location().toString());
    }

    static Export write(ServerLevel level, Section section, LevelChunk[] columns, boolean planned) throws IOException {
        Path save = level.getServer().getWorldPath(LevelResource.ROOT);
        Path reports = Files.createDirectories(save.resolve("gt6ce-worldgen"));
        Path report = Files.createTempDirectory(reports, "profile-");
        var profile = new WorldgenProfile(level, section, planned);
        try (var csv = Files.newBufferedWriter(report.resolve("profile.csv"));
                var graphic = Files.newBufferedWriter(report.resolve("profile.svg"))) {
            csv.write(CSV_HEADER);
            var svg = new WorldgenProfileSvg(graphic, section, planned);
            for (int col = 0; col < section.width(); col++) profile.writeColumn(col, columns[col], csv, svg);
            svg.finish();
        }
        var manifest = new Properties();
        manifest.setProperty("dimension", level.dimension().location().toString());
        manifest.setProperty("seed", Long.toString(level.getSeed()));
        manifest.setProperty("axis", section.axis().getSerializedName());
        manifest.setProperty("origin_x", Integer.toString(section.x()));
        manifest.setProperty("origin_z", Integer.toString(section.z()));
        manifest.setProperty("width", Integer.toString(section.width()));
        manifest.setProperty("min_y", Integer.toString(section.minY()));
        manifest.setProperty("max_y", Integer.toString(section.maxY()));
        manifest.setProperty("planned_layers", Boolean.toString(planned));
        Path unpublished = report.resolve("profile.properties.tmp");
        try (var writer = Files.newBufferedWriter(unpublished)) {
            manifest.store(writer, "GT6CE loaded world profile");
        }
        Files.move(unpublished, report.resolve("profile.properties"));
        return new Export(save.relativize(report), profile.ores, profile.markers);
    }

    private void writeColumn(int col, LevelChunk chunk, BufferedWriter csv, WorldgenProfileSvg svg) throws IOException {
        int x = section.xAt(col);
        int z = section.zAt(col);
        if (field != null) field.column(x, z).fill(section.minY(), layers, provinces);
        var pos = new BlockPos.MutableBlockPos(x, section.minY(), z);
        Sample previous = null;
        int start = section.minY();
        for (int i = 0; i < section.height(); i++) {
            int y = section.minY() + i;
            pos.setY(y);
            var state = chunk.getBlockState(pos);
            var block = blockInfo.computeIfAbsent(state, WorldgenProfile::storedBlock);
            var marker = marker(chunk, pos, state.getBlock());
            if (!block.ore().isEmpty()) ores++;
            if (!marker.kind().isEmpty()) markers++;
            var next = new Sample(field == null ? -1 : layers[i], provinces[i], block, marker);
            if (!next.equals(previous)) {
                if (previous != null) writeRun(col, start, y - 1, previous, csv, svg);
                previous = next;
                start = y;
            }
        }
        writeRun(col, start, section.maxY(), previous, csv, svg);
    }

    private void writeRun(int col, int from, int to, Sample sample, BufferedWriter csv, WorldgenProfileSvg svg)
            throws IOException {
        int x = section.xAt(col);
        int z = section.zAt(col);
        String province =
                sample.province() == null ? "" : sample.province().name().toLowerCase(Locale.ROOT);
        var block = sample.block();
        var marker = sample.marker();
        String layer = sample.layer() < 0 ? "" : rockLayers[sample.layer()].material();
        csv.write(x + "," + z + "," + from + "," + to + "," + province + "," + quote(layer) + ","
                + block.id() + "," + quote(block.rock()) + "," + block.host() + "," + quote(block.ore()) + ","
                + block.form() + "," + marker.kind() + "," + quote(marker.material()) + "," + quote(marker.item()));
        csv.newLine();
        String point = "X %d | Z %d | Y %d to %d".formatted(x, z, from, to);
        if (sample.layer() >= 0) {
            svg.layer(col, from, to, rockLayers[sample.layer()].color(), point + " | " + province + " | " + layer);
        }
        String actual = point + " | " + block.id();
        if (!block.ore().isEmpty()) actual += " | " + block.ore() + " (" + block.form() + ")";
        if (!block.host().isEmpty()) actual += " | Host " + block.host();
        if (!marker.kind().isEmpty()) {
            actual += " | " + marker.kind() + " " + marker.material() + " " + marker.item();
        }
        svg.block(col, from, to, block.color(), actual, !marker.kind().isEmpty());
    }

    private Marker marker(LevelChunk chunk, BlockPos pos, Block block) {
        if (block instanceof RockBlock && chunk.getBlockEntity(pos) instanceof RockBlockEntity rock) {
            return new Marker(rock.hasRawOre() ? "raw_pebble" : "pebble", rock.getMaterial(), rock.itemId());
        }
        return plants.getOrDefault(block, block instanceof BedrockFlowerBlock ? Marker.FLOWER : Marker.NONE);
    }

    private static Map<Block, Marker> plantMarkers(String dimension) {
        var materials = new HashMap<Block, TreeSet<String>>();
        for (var ore : OreSourceCatalog.current().bedrockOres(dimension, OreSourceCatalog.Geology.DISABLED)) {
            if (ore.flowerId() == null) continue;
            var id = Mc.gregtechId(ore.flowerId());
            if (BuiltInRegistries.BLOCK.get(id) instanceof BedrockFlowerBlock flower) {
                materials
                        .computeIfAbsent(flower, unused -> new TreeSet<>())
                        .add(ore.material().resolve().getName());
            }
        }
        var result = new HashMap<Block, Marker>();
        materials.forEach(
                (block, names) -> result.put(block, new Marker("bedrock_flower", String.join(" / ", names), "")));
        for (int type = 1; type <= 3; type++) {
            Block grass = GTFluidSpringsFeature.indicatorBlock(type);
            if (grass != null) result.put(grass, new Marker("indicator_grass", "", ""));
        }
        return result;
    }

    private static StoredBlock storedBlock(BlockState state) {
        Block block = state.getBlock();
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        var material = GTOreBlockResolver.oreMaterialOf(block);
        if (material != null) {
            var ore = material.resolve();
            String host = block instanceof OreBlock ? OreBlock.stoneOf(state).id() : "";
            String form = oreForm(state);
            int color = ((ore.getColor() & 0xFEFEFE) >>> 1) + 0x7F7F7F;
            return new StoredBlock(id, "", host, ore.getName(), form, rgb(color));
        }
        String rock = "";
        if (block instanceof GTStoneBlock gtStone) {
            rock = gtStone.stoneMaterial().getName();
        } else {
            OreHostStone host = GTOreBlockResolver.stoneOf(state);
            if (host != null && host.smallOreDustMaterial() != null) rock = host.smallOreDustMaterial();
        }
        String color = state.isAir()
                ? "#192432"
                : state.liquid()
                        ? "#376587"
                        : state.is(Blocks.BEDROCK) ? "#333b48" : rock.isEmpty() ? "#788181" : rockColor(rock);
        return new StoredBlock(id, rock, "", "", "", color);
    }

    private static String oreForm(BlockState state) {
        if (state.getBlock() instanceof OreBlock ore) {
            if (OreBlock.isBedrockOre(state)) return ore.isSmall() ? "bedrock_poor" : "bedrock";
            return ore.isSmall() ? "poor" : "normal";
        }
        if (state.getBlock() instanceof RockOreBlock) return "whole_rock";
        if (state.getBlock() instanceof DenseOreBlock) return "dense";
        return "other";
    }

    private static String rockColor(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        return material.isValid() ? rgb(material.getColor()) : "#79818b";
    }

    private static String rgb(int color) {
        return String.format(Locale.ROOT, "#%06x", color & 0xFFFFFF);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
