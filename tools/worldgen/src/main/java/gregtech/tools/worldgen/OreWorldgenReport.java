package gregtech.tools.worldgen;

import static gregtech.tools.worldgen.WorldgenSampler.BOTTOM;
import static gregtech.tools.worldgen.WorldgenSampler.TOP;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.GTOreVeins;
import gregtech.content.worldgen.MineralWorldgenRules;
import gregtech.content.worldgen.OreSourceCatalog;
import gregtech.content.worldgen.OreVeinLayout;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.tools.worldgen.WorldgenSampler.Area;
import gregtech.tools.worldgen.WorldgenSampler.Axis;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/** Overworld mineral plans sampled without loading Minecraft or writing any world files. */
final class OreWorldgenReport {
    private static final String DIMENSION = OreVeinDeclaration.OVERWORLD;
    private static final int NO_MINERAL = 0x292A32;

    private OreWorldgenReport() {}

    static void write(
            Path output,
            long seed,
            Area area,
            BufferedImage horizontal,
            BufferedImage xy,
            BufferedImage zy,
            String selectedName)
            throws IOException {
        var catalog = WorldgenSampler.materialCatalog();
        GTMaterial mineral = selectedName.isEmpty()
                ? null
                : GTMaterialRegistry.get(selectedName).resolve();
        if (mineral != null && !mineral.isValid())
            throw new IllegalArgumentException("Unknown material: " + selectedName);
        var projection = mineral == null ? null : new MineralProjection(mineral, area);
        var plans = catalog.regionalPlans(DIMENSION);
        var centers = new LinkedHashMap<String, List<OreVeinLayout.Center>>();
        var pointCsv = new StringBuilder("vein,shape,x,y,z,regional\n");
        int minX = area.minX(), maxX = area.maxX();
        int minZ = area.minZ(), maxZ = area.maxZ();
        long start = System.nanoTime();
        for (var plan : plans) {
            int region = plan.cells() * OreVeinLayout.CELL_SIZE;
            for (int rx = Math.floorDiv(minX - region * 2, region);
                    rx <= Math.floorDiv(maxX + region * 2, region);
                    rx++) {
                for (int rz = Math.floorDiv(minZ - region * 2, region);
                        rz <= Math.floorDiv(maxZ + region * 2, region);
                        rz++) {
                    for (var center : plan.centers(seed, rx, rz)) {
                        var vein = center.vein();
                        centers.computeIfAbsent(vein.name(), key -> new ArrayList<>())
                                .add(center);
                        if (center.x() < minX - OreVeinLayout.MAX_RADIUS
                                || center.x() > maxX + OreVeinLayout.MAX_RADIUS
                                || center.z() < minZ - OreVeinLayout.MAX_RADIUS
                                || center.z() > maxZ + OreVeinLayout.MAX_RADIUS) continue;
                        var heights = vein.heights(DIMENSION);
                        var body = center.body(heights.intersect(new GTOreVeins.HeightRange(BOTTOM, TOP)));
                        if (body == null) continue;
                        pointCsv.append("%s,%s,%d,%d,%d,%s%n"
                                .formatted(
                                        vein.name(),
                                        body.shape(),
                                        center.x(),
                                        body.centerY(),
                                        center.z(),
                                        center.guaranteed()));
                        overlayHorizontal(body, area, horizontal);
                        overlayVertical(body, area, Axis.X, xy);
                        overlayVertical(body, area, Axis.Z, zy);
                        if (projection != null) projection.add(body);
                    }
                }
            }
        }
        Files.writeString(output.resolve("vein-centers.csv"), pointCsv);
        Files.writeString(output.resolve("vein-distances.csv"), distances(centers, minX, minZ, area.width()));
        ImageIO.write(horizontal, "png", output.resolve("ore-horizontal.png").toFile());
        ImageIO.write(xy, "png", output.resolve("ore-vertical.png").toFile());
        ImageIO.write(zy, "png", output.resolve("ore-vertical-zy.png").toFile());
        if (projection != null) projection.write(output);
        int[] regions = plans.stream()
                .mapToInt(plan -> plan.cells() * OreVeinLayout.CELL_SIZE)
                .distinct()
                .toArray();
        int chunks = writePlan(output, seed, area, regions, horizontal, centers);
        System.out.printf(
                Locale.ROOT,
                "Mineral plans in %.0f ms, %d chunks sampled for bedrock ores%n",
                (System.nanoTime() - start) / 1_000_000.0,
                chunks);
    }

    private static final class MineralProjection {
        private final GTMaterial material;
        private final Area area;
        private final int[] columns;

        private MineralProjection(GTMaterial material, Area area) {
            this.material = material;
            this.area = area;
            columns = new int[area.width() * area.width()];
        }

        private void add(OreVeinLayout.Body body) {
            var center = body.center();
            var vein = center.vein();
            if (vein.top().resolve() != material
                    && vein.bottom().resolve() != material
                    && vein.between().resolve() != material
                    && vein.spread().resolve() != material) return;
            int r = body.radius();
            for (int x = Math.max(area.minX(), center.x() - r); x <= Math.min(area.maxX(), center.x() + r); x++) {
                for (int z = Math.max(area.minZ(), center.z() - r); z <= Math.min(area.maxZ(), center.z() + r); z++) {
                    var column = body.column(x, z);
                    if (column == null) continue;
                    for (int y = body.minY(); y <= body.maxY(); y++) {
                        var sample = column.sample(y);
                        if (sample != null && sample.role().material(vein).resolve() == material)
                            columns[x - area.minX() + (z - area.minZ()) * area.width()]++;
                    }
                }
            }
        }

        private void write(Path output) throws IOException {
            int max = Math.max(1, Arrays.stream(columns).max().orElse(0));
            int width = area.width();
            var map = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
            for (int z = 0; z < width; z++) {
                for (int x = 0; x < width; x++) {
                    int count = columns[x + z * width];
                    int color =
                            count == 0 ? NO_MINERAL : java.awt.Color.HSBtoRGB(0.61f - 0.57f * count / max, 0.9f, 0.86f);
                    map.setRGB(x, z, color);
                }
            }
            ImageIO.write(
                    map,
                    "png",
                    output.resolve("mineral-" + material.getName().toLowerCase(Locale.ROOT) + ".png")
                            .toFile());
        }
    }

    private static void overlayHorizontal(OreVeinLayout.Body body, Area area, BufferedImage image) {
        int y = area.sliceY();
        if (y < body.minY() || y > body.maxY()) return;
        var center = body.center();
        int r = body.radius();
        for (int x = Math.max(area.minX(), center.x() - r); x <= Math.min(area.maxX(), center.x() + r); x++) {
            for (int z = Math.max(area.minZ(), center.z() - r); z <= Math.min(area.maxZ(), center.z() + r); z++) {
                var column = body.column(x, z);
                var sample = column == null ? null : column.sample(y);
                if (sample != null) image.setRGB(x - area.minX(), z - area.minZ(), oreColor(body, sample));
            }
        }
    }

    private static void overlayVertical(OreVeinLayout.Body body, Area area, Axis axis, BufferedImage image) {
        var center = body.center();
        int middle = axis == Axis.X ? center.x() - area.minX() : center.z() - area.minZ();
        int r = body.radius();
        for (int offset = Math.max(0, middle - r); offset <= Math.min(area.width() - 1, middle + r); offset++) {
            var column = body.column(axis.x(area, offset), axis.z(area, offset));
            if (column == null) continue;
            for (int y = body.minY(); y <= body.maxY(); y++) {
                var sample = column.sample(y);
                if (sample != null) image.setRGB(offset, TOP - y, oreColor(body, sample));
            }
        }
    }

    private static int oreColor(OreVeinLayout.Body body, OreVeinLayout.Sample sample) {
        var vein = body.center().vein();
        var material = sample.role().material(vein);
        int color = java.awt.Color.HSBtoRGB(
                Integer.toUnsignedLong(material.getName().hashCode()) % 359 / 359f, 0.8f, sample.small() ? 0.57f : 1f);
        return color & 0xFFFFFF;
    }

    private static String distances(Map<String, List<OreVeinLayout.Center>> centers, int minX, int minZ, int width) {
        var csv = new StringBuilder("vein,points,mean_blocks,p95_blocks,p99_blocks,max_blocks\n");
        int side = Math.min(64, Math.max(1, width / 16));
        for (var entry : centers.entrySet()) {
            var distances = new double[side * side];
            int count = 0;
            for (int x = 0; x < side; x++) {
                for (int z = 0; z < side; z++) {
                    double sampleX = minX + (x + 0.5) * width / side;
                    double sampleZ = minZ + (z + 0.5) * width / side;
                    double nearest = Double.POSITIVE_INFINITY;
                    for (var center : entry.getValue()) {
                        double dx = sampleX - center.x();
                        double dz = sampleZ - center.z();
                        nearest = Math.min(nearest, dx * dx + dz * dz);
                    }
                    distances[count++] = Math.sqrt(nearest);
                }
            }
            Arrays.sort(distances);
            csv.append(String.format(
                    Locale.ROOT,
                    "%s,%d,%.1f,%.1f,%.1f,%.1f%n",
                    entry.getKey(),
                    count,
                    Arrays.stream(distances).average().orElseThrow(),
                    percentile(distances, 0.95),
                    percentile(distances, 0.99),
                    distances[count - 1]));
        }
        return csv.toString();
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted[(int) Math.ceil(fraction * sorted.length) - 1];
    }

    private static int writePlan(
            Path output,
            long seed,
            Area area,
            int[] regions,
            BufferedImage ores,
            Map<String, List<OreVeinLayout.Center>> centers)
            throws IOException {
        int side = area.width();
        var plan = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = plan.createGraphics();
        int chunks;
        try {
            graphics.drawImage(ores, 0, 0, null);
            graphics.setColor(new Color(255, 255, 255, 100));
            for (int region : regions) {
                for (int x = Math.floorMod(-(long) area.minX(), region); x < side; x += region)
                    graphics.drawLine(x, 0, x, side - 1);
                for (int z = Math.floorMod(-(long) area.minZ(), region); z < side; z += region)
                    graphics.drawLine(0, z, side - 1, z);
            }
            var guaranteed = new Color(0xC4F690);
            var bonus = new Color(0xFCDD92);
            for (var declarations : centers.values()) {
                for (var center : declarations) {
                    int x = center.x() - area.minX();
                    int z = center.z() - area.minZ();
                    if (x < 0 || z < 0 || x >= side || z >= side) continue;
                    graphics.setColor(center.guaranteed() ? guaranteed : bonus);
                    graphics.fillRect(x - 1, z - 1, 3, 3);
                }
            }
            chunks = bedrock(output, seed, area, graphics);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(plan, "png", output.resolve("plan-horizontal.png").toFile());
        return chunks;
    }

    private static int bedrock(Path output, long seed, Area area, Graphics2D plan) throws IOException {
        int firstX = area.minX() >> 4, firstZ = area.minZ() >> 4;
        int width = (area.maxX() >> 4) - firstX + 1;
        int height = (area.maxZ() >> 4) - firstZ + 1;
        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var points = new StringBuilder("material,chunk_x,chunk_z\n");
        plan.setColor(new Color(0x68DEDF));
        long bedrockSeed = MineralWorldgenRules.bedrockSeed(seed, DIMENSION);
        var deposits = OreSourceCatalog.current().bedrockOres(DIMENSION, OreSourceCatalog.Geology.WITHOUT_VANILLA_ORES);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < height; dz++) {
                int count = 0;
                for (var ore : deposits) {
                    if (!MineralWorldgenRules.bedrockHit(
                            bedrockSeed, firstX + dx, firstZ + dz, ore.name(), ore.chance(), 100)) continue;
                    points.append("%s,%d,%d%n".formatted(ore.name(), firstX + dx, firstZ + dz));
                    count++;
                }
                map.setRGB(dx, dz, count == 0 ? 0x292A32 : count == 1 ? 0x74C9C1 : 0xFFCC72);
                if (count > 0) {
                    int x = ((firstX + dx) << 4) - area.minX();
                    int z = ((firstZ + dz) << 4) - area.minZ();
                    plan.drawRect(x, z, 15, 15);
                }
            }
        }
        ImageIO.write(map, "png", output.resolve("bedrock.png").toFile());
        Files.writeString(output.resolve("bedrock.csv"), points);
        return width * height;
    }
}
