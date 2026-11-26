package gregtech.tools.worldgen;

import gregtech.api.mod.ModData;
import gregtech.content.worldgen.OreSourceCatalog;
import gregtech.content.worldgen.StoneLayerField;
import gregtech.engine.material.GTMaterialRegistry;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/** Plots the Overworld rock field in three planes through the selected coordinates. */
public final class WorldgenSampler {
    static final int BOTTOM = -64;
    static final int TOP = 319;

    record Layer(String material, boolean noDeep, boolean hasOre) {}

    record Area(int centerX, int centerZ, int width, int sliceY) {
        int minX() {
            return centerX - width / 2;
        }

        int minZ() {
            return centerZ - width / 2;
        }

        int maxX() {
            return minX() + width - 1;
        }

        int maxZ() {
            return minZ() + width - 1;
        }
    }

    enum Axis {
        X("vertical"),
        Z("vertical-zy");

        final String file;

        Axis(String file) {
            this.file = file;
        }

        int x(Area area, int offset) {
            return this == X ? area.minX() + offset : area.centerX();
        }

        int z(Area area, int offset) {
            return this == Z ? area.minZ() + offset : area.centerZ();
        }
    }

    private WorldgenSampler() {}

    private static final class MaterialCatalog {
        private static final OreSourceCatalog CATALOG = load();

        private static OreSourceCatalog load() {
            ModData.bindPresence(id -> false);
            GTMaterialRegistry.init();
            return OreSourceCatalog.current();
        }
    }

    static OreSourceCatalog materialCatalog() {
        return MaterialCatalog.CATALOG;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 6 || args.length > 8) {
            throw new IllegalArgumentException(
                    "Expected: seed output centerX centerZ width sliceY [--legacy [--heights=heights.csv]|--mineral=name]");
        }
        String option = args.length > 6 ? args[6] : "";
        boolean legacy = option.equals("--legacy");
        String mineral = option.startsWith("--mineral=") ? option.substring("--mineral=".length()) : "";
        if (!option.isEmpty() && !legacy && mineral.isBlank())
            throw new IllegalArgumentException("Unknown option " + option);
        if (args.length == 8 && (!legacy || !args[7].startsWith("--heights="))) {
            throw new IllegalArgumentException("The legacy height map must be supplied as --heights=path");
        }
        long seed = Long.parseLong(args[0]);
        Path output = Path.of(args[1]);
        var area = new Area(
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4]),
                Integer.parseInt(args[5]));
        int width = area.width();
        int sliceY = area.sliceY();
        if (width < 1 || width > 4096 || sliceY < BOTTOM || sliceY > TOP) {
            throw new IllegalArgumentException("Width must be 1..4096 and sliceY must be -64..319");
        }

        var field = new StoneLayerField(seed);
        var oldField = legacy ? new LegacyStrata(seed, args.length == 8 ? Path.of(args[7].substring(10)) : null) : null;
        var layers = legacy
                ? oldField.layers()
                : StoneLayerField.layers().stream()
                        .map(layer -> new Layer(
                                layer.material(),
                                layer.noDeep(),
                                layer.isOreSeam() || !layer.ores().isEmpty()))
                        .toList();
        int[] palette = new int[layers.size() + 1];
        palette[0] = color("Host");
        for (int i = 0; i < layers.size(); i++)
            palette[i + 1] = color(layers.get(i).material());

        long start = System.nanoTime();
        Files.createDirectories(output);
        var horizontal = new LayerImage("horizontal", width, width, palette);
        for (int dx = 0; dx < width; dx++) {
            int x = area.minX() + dx;
            for (int dz = 0; dz < width; dz++) {
                int z = area.minZ() + dz;
                int layer = legacy
                        ? oldField.index(x, sliceY, z)
                        : field.column(x, z).layerIndex(sliceY);
                horizontal.put(dx, dz, layer);
            }
        }
        var xy = vertical(output, area, Axis.X, field, oldField, palette);
        var zy = vertical(output, area, Axis.Z, field, oldField, palette);
        writeLayers(output, layers, List.of(horizontal, xy, zy));
        System.out.printf(
                Locale.ROOT,
                "Seed %d, Y=%d, %,d sample points in %.0f ms%n",
                seed,
                sliceY,
                horizontal.size() + xy.size() + zy.size(),
                (System.nanoTime() - start) / 1_000_000.0);
        if (!legacy) {
            OreWorldgenReport.write(output, seed, area, horizontal.image, xy.image, zy.image, mineral);
        }
    }

    private static LayerImage vertical(
            Path output, Area area, Axis axis, StoneLayerField field, LegacyStrata oldField, int[] palette)
            throws IOException {
        int width = area.width();
        var section = new LayerImage(axis.file, width, TOP - BOTTOM + 1, palette);
        var samples = oldField != null
                ? null
                : axis == Axis.X
                        ? field.sample(area.minX(), area.centerZ(), width, 1, BOTTOM, TOP)
                        : field.sample(area.centerX(), area.minZ(), 1, width, BOTTOM, TOP);
        var contactImage =
                samples == null ? null : new BufferedImage(width, TOP - BOTTOM + 1, BufferedImage.TYPE_INT_RGB);
        var contacts = new StoneLayerField.Contacts();
        for (int offset = 0; offset < width; offset++) {
            int x = axis.x(area, offset);
            int z = axis.z(area, offset);
            for (int y = BOTTOM; y <= TOP; y++) {
                int layer = samples == null ? oldField.index(x, y, z) : samples.layerIndex(x, y, z);
                section.put(offset, TOP - y, layer);
                if (samples != null) {
                    samples.contacts(x, y, z, contacts);
                    int distance = contacts.nearest();
                    int color = distance == 1
                            ? 0xFF8A50
                            : distance <= StoneLayerField.CONTACT_DEPTH
                                    ? 0xFFD38A
                                    : (palette[layer + 1] & 0xFEFEFE) >>> 1;
                    contactImage.setRGB(offset, TOP - y, color);
                }
            }
        }
        if (contactImage != null) {
            ImageIO.write(
                    contactImage,
                    "png",
                    output.resolve("contact-" + axis.file + ".png").toFile());
        }
        return section;
    }

    private static void writeLayers(Path output, List<Layer> layers, List<LayerImage> images) throws IOException {
        Map<String, int[]> counts = new TreeMap<>();
        var summary = new StringBuilder("slice,catalog_empty_blocks,total_blocks\n");
        var csv = new StringBuilder("material,rgb");
        for (int plane = 0; plane < images.size(); plane++) {
            var image = images.get(plane);
            ImageIO.write(
                    image.image, "png", output.resolve(image.name + ".png").toFile());
            csv.append(',')
                    .append(image.name.replace('-', '_'))
                    .append("_blocks,")
                    .append(image.name.replace('-', '_'))
                    .append("_fraction");
            int barren = image.counts[0];
            for (int i = 0; i < image.counts.length; i++) {
                int count = image.counts[i];
                if (count == 0) continue;
                String material = i == 0 ? "Host" : layers.get(i - 1).material();
                counts.computeIfAbsent(material, ignored -> new int[images.size()])[plane] += count;
                if (i > 0 && !layers.get(i - 1).hasOre()) barren += count;
            }
            summary.append(image.name)
                    .append(',')
                    .append(barren)
                    .append(',')
                    .append(image.size())
                    .append('\n');
        }
        csv.append('\n');
        counts.forEach((material, count) -> {
            csv.append(String.format(Locale.ROOT, "%s,%06X", material, color(material)));
            for (int plane = 0; plane < images.size(); plane++) {
                csv.append(String.format(
                        Locale.ROOT,
                        ",%d,%.6f",
                        count[plane],
                        count[plane] / (double) images.get(plane).size()));
            }
            csv.append('\n');
        });
        Files.writeString(output.resolve("layers.csv"), csv);
        Files.writeString(output.resolve("layer-summary.csv"), summary);
    }

    private static final class LayerImage {
        final String name;
        final BufferedImage image;
        final int[] counts;
        final int[] palette;

        LayerImage(String name, int width, int height, int[] palette) {
            this.name = name;
            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            this.counts = new int[palette.length];
            this.palette = palette;
        }

        void put(int x, int y, int layer) {
            int index = layer + 1;
            image.setRGB(x, y, palette[index]);
            counts[index]++;
        }

        int size() {
            return image.getWidth() * image.getHeight();
        }
    }

    private static int color(String material) {
        return switch (material) {
            case "Host", "Stone" -> 0x818181;
            case "Deepslate" -> 0x44434C;
            case "GraniteBlack" -> 0x30292E;
            case "Basalt" -> 0x53495A;
            case "Tuff" -> 0x666652;
            case "GraniteRed", "Granite" -> 0x9E5E54;
            case "Slate" -> 0x3C5974;
            case "Shale" -> 0x665950;
            case "Marble" -> 0xDFDAD8;
            case "Limestone" -> 0xBEB490;
            case "Andesite" -> 0x87817D;
            case "Diorite" -> 0xACAAAF;
            default ->
                Color.HSBtoRGB(Integer.toUnsignedLong(material.hashCode()) % 359 / 359f, 0.40f, 0.78f) & 0xFFFFFF;
        };
    }
}
