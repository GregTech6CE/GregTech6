package gregtech.content.worldgen;

import static gregtech.content.worldgen.WorldgenHash.at;
import static gregtech.content.worldgen.WorldgenHash.mix;
import static gregtech.content.worldgen.WorldgenHash.roll;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import java.util.ArrayList;
import java.util.List;

/** Plans centers and ore voxels by world coordinates. Each chunk consumes only its own coordinates. */
public final class OreVeinLayout {
    public static final int CELL_SIZE = 48;
    public static final int MAX_RADIUS = 34;
    public static final int BONUS_CELL_CHANCE = 8;
    private static final int CENTER_MARGIN = 7;
    private static final long ADDON_RESERVE_SALT = 0xCB7053C5ACD59C25L;

    private OreVeinLayout() {}

    public enum Shape {
        VEINED,
        LAYERED,
        DIKE,
        CLUSTER;

        public static Shape forVein(String name) {
            return switch (name) {
                case "iron", "copper", "redstone", "tetrahedrite", "gold" -> VEINED;
                case "titanium", "tungstate", "nickel", "pitchblende", "monazite", "tantalum" -> DIKE;
                case "platinum", "molybdenum", "diamond", "garnet", "peridot", "sapphire", "sapphire2", "trinium" ->
                    CLUSTER;
                default -> LAYERED;
            };
        }
    }

    public static int regionCells(List<OreVein> veins) {
        int subregional = 0;
        int shared = 0;
        for (OreVein vein : veins) {
            if (vein.subregional()) subregional++;
            if (!vein.subregional() || vein.weight() > 0) shared++;
        }
        int region = (int) Math.ceil(Math.sqrt(Math.max(2.0 * shared, veins.size() + 3.0 * subregional)));
        int quadrants = 2 * (int) Math.ceil(Math.sqrt(subregional));
        return Math.max(8, Math.max(region, quadrants));
    }

    public static final class RegionalPlan {
        private final List<OreVein> primary;
        private final List<OreVein> additions;
        private final List<OreVein> weighted;
        private final int totalWeight;
        private final long salt;
        private final int cells;

        private RegionalPlan(
                List<OreVein> primary, List<OreVein> additions, List<OreVein> weighted, int totalWeight, long salt) {
            this.primary = List.copyOf(primary);
            this.additions = List.copyOf(additions);
            this.weighted = List.copyOf(weighted);
            this.totalWeight = totalWeight;
            this.salt = salt;
            cells = regionCells(primary);
        }

        public int cells() {
            return cells;
        }

        public boolean includes(String name) {
            return primary.stream().anyMatch(vein -> vein.name().equals(name))
                    || additions.stream().anyMatch(vein -> vein.name().equals(name));
        }

        public List<Center> centers(long worldSeed, int regionX, int regionZ) {
            return regionalCenters(
                    worldSeed ^ salt, regionX, regionZ, cells, primary, additions, weighted, totalWeight);
        }
    }

    static List<RegionalPlan> regionalPlans(String dimension, List<OreVein> veins, List<OreVein> addons) {
        if (OreVeinDeclaration.END.equals(dimension)) {
            return addons.isEmpty() ? List.of() : List.of(new RegionalPlan(addons, List.of(), List.of(), 0, 0));
        }
        int totalWeight = GTOreVeins.selectionWeight(dimension, veins);
        if (!OreVeinDeclaration.OVERWORLD.equals(dimension) || addons.isEmpty()) {
            return List.of(new RegionalPlan(veins, List.of(), veins, totalWeight, 0));
        }

        var builtins = veins.subList(0, veins.size() - addons.size());
        int side = regionCells(builtins);
        int reserved =
                builtins.stream().mapToInt(vein -> vein.subregional() ? 4 : 1).sum();
        // Leave an empty slot for the ordinary weighted draw even when addons fill most of the grid.
        int fit = Math.min(addons.size(), Math.max(0, side * side - reserved - 1));
        var primary = new RegionalPlan(builtins, addons.subList(0, fit), veins, totalWeight, 0);
        if (fit == addons.size()) return List.of(primary);
        var overflow =
                new RegionalPlan(addons.subList(fit, addons.size()), List.of(), List.of(), 0, ADDON_RESERVE_SALT);
        return List.of(primary, overflow);
    }

    private static int[] cellOrder(long seed, int side) {
        int slots = side * side;
        long stride = Math.floorMod((int) seed | 1, slots);
        // A stride coprime to the region area visits each slot once, including in non-power-of-two regions.
        while (gcd(stride, slots) != 1) stride++;
        int[] order = new int[slots];
        for (int cell = 0; cell < slots; cell++) {
            order[Math.floorMod(cell * stride + (int) (seed >>> 32), slots)] = cell;
        }
        return order;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    /** Each regional vein gets a slot. Subregional veins get one slot in each quadrant. */
    private static List<Center> regionalCenters(
            long worldSeed,
            int regionX,
            int regionZ,
            int side,
            List<OreVein> veins,
            List<OreVein> additions,
            List<OreVein> weighted,
            int totalWeight) {
        long regionSeed = at(worldSeed ^ 0xBBE0563303A4615FL, regionX, regionZ);
        var guaranteed = new OreVein[side * side];
        int[] order = cellOrder(regionSeed, side);
        int[] edges = {0, side / 2, side};
        for (OreVein vein : veins) {
            if (!vein.subregional()) continue;
            long seed = WorldgenHash.named(regionSeed, vein.name());
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    int cell = subregionCell(guaranteed, seed, side, edges[x], edges[x + 1], edges[z], edges[z + 1]);
                    guaranteed[cell] = vein;
                }
            }
        }

        int slot = 0;
        for (var group : List.of(veins, additions)) {
            for (OreVein vein : group) {
                if (vein.subregional()) continue;
                while (guaranteed[order[slot]] != null) slot++;
                guaranteed[order[slot++]] = vein;
            }
        }

        var centers = new ArrayList<Center>();
        for (int cell = 0; cell < guaranteed.length; cell++) {
            int x = regionX * side + cell / side;
            int z = regionZ * side + cell % side;
            long seed = at(worldSeed ^ 0xDB4F0B9175AE2165L, x, z);
            OreVein vein = guaranteed[cell];
            if (vein != null) centers.add(center(vein, x, z, seed, true));
            else if (totalWeight > 0 && roll(seed, BONUS_CELL_CHANCE) == 0) {
                vein = MineralWorldgenRules.selectVein(weighted, roll(mix(seed), totalWeight));
                if (vein != null) centers.add(center(vein, x, z, seed, false));
            }
        }
        return centers;
    }

    private static int subregionCell(OreVein[] planned, long seed, int side, int minX, int maxX, int minZ, int maxZ) {
        int best = -1;
        long bestScore = Long.MAX_VALUE;
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                int cell = x * side + z;
                if (planned[cell] != null) continue;
                long dx = 2L * x - minX - maxX + 1;
                long dz = 2L * z - minZ - maxZ + 1;
                long score = (dx * dx + dz * dz) * 8 + roll(at(seed, x, z), 32);
                if (score >= bestScore) continue;
                best = cell;
                bestScore = score;
            }
        }
        return best;
    }

    /** Weighted draw for a single cell in dimensions without regional placement. */
    public static Center weightedCenter(long worldSeed, int x, int z, List<OreVein> veins, int totalWeight) {
        if (totalWeight <= 0) return null;
        long seed = at(worldSeed ^ 0xDB4F0B9175AE2165L, x, z);
        OreVein vein = MineralWorldgenRules.selectVein(veins, roll(seed, totalWeight));
        return vein == null ? null : center(vein, x, z, seed, false);
    }

    private static Center center(OreVein vein, int cellX, int cellZ, long seed, boolean guaranteed) {
        int width = CELL_SIZE - CENTER_MARGIN * 2;
        int x = cellX * CELL_SIZE + CENTER_MARGIN + roll(mix(seed ^ 1), width);
        int z = cellZ * CELL_SIZE + CENTER_MARGIN + roll(mix(seed ^ 2), width);
        return new Center(vein, x, z, seed, guaranteed);
    }

    public record Center(OreVein vein, int x, int z, long seed, boolean guaranteed) {
        public int radius() {
            return 5 + Math.min(MAX_RADIUS - 5, (int) (2L * vein.size() / 3));
        }

        /** Largest continuous usable band in a column, clipped to the vein's allowed depths. */
        public Body fit(int x, int z, HeightRange range, List<HeightRange> bands, boolean reserve) {
            if (bands.isEmpty()) return null;
            int surface = bands.get(bands.size() - 1).max();
            var usable = reserve
                    ? GTOreVeins.reservePlacement(range, surface)
                    : new HeightRange(range.min(), Math.min(range.max(), surface - GTOreVeins.SURFACE_MARGIN));
            HeightRange best = null;
            for (var band : bands) {
                var intersection = band.intersect(usable);
                if (!intersection.isEmpty()
                        && (best == null || intersection.max() - intersection.min() > best.max() - best.min()))
                    best = intersection;
            }
            return best == null ? null : new Center(vein, x, z, seed, guaranteed).body(best);
        }

        public Body body(HeightRange band) {
            if (band.isEmpty()) return null;
            int low = band.min(), high = band.max();
            Shape shape = Shape.forVein(vein.name());
            int maxHalfHeight = switch (shape) {
                case VEINED -> 9;
                case LAYERED -> 7;
                case DIKE -> 18;
                case CLUSTER -> 11;
            };
            int halfHeight = Math.min(maxHalfHeight, (high - low) / 2);
            int y = low + halfHeight + roll(mix(seed ^ 3), high - low - 2 * halfHeight + 1);
            double yaw = roll(mix(seed ^ 4), 360) * (Math.PI / 180.0);
            return new Body(this, shape, radius(), y, halfHeight, Math.cos(yaw), Math.sin(yaw));
        }
    }

    public record Body(Center center, Shape shape, int radius, int centerY, int halfHeight, double cos, double sin) {
        public int minY() {
            return centerY - halfHeight;
        }

        public int maxY() {
            return centerY + halfHeight;
        }

        public Column column(int x, int z) {
            double dx = (x - center.x()) / (double) radius;
            double dz = (z - center.z()) / (double) radius;
            if (dx * dx + dz * dz > 1) return null;
            double u = dx * cos + dz * sin;
            double w = dz * cos - dx * sin;
            double primary;
            double secondary = 3;
            double heightShift;
            switch (shape) {
                case VEINED -> {
                    double wiggle = 0.17 * Math.sin(4 * u);
                    primary = sq(u) + sq((w - wiggle) / 0.24);
                    secondary = sq(u / 0.85) + sq((w + wiggle + 0.18 * u) / 0.19);
                    heightShift = 0.32 * Math.sin(3.7 * u);
                }
                case LAYERED -> {
                    primary = sq(u) + sq(w / 0.71);
                    heightShift = 0.42 * u;
                }
                case DIKE -> {
                    primary = sq(u / 0.23) + sq(w / 0.88);
                    heightShift = 0.21 * w;
                }
                case CLUSTER -> {
                    primary = sq((u + 0.34) / 0.57) + sq((w + 0.13) / 0.53);
                    secondary = Math.min(
                            sq((u - 0.28) / 0.59) + sq((w - 0.16) / 0.56), sq(u / 0.47) + sq((w + 0.27) / 0.46));
                    heightShift = 0.18 * u;
                }
                default -> throw new AssertionError(shape);
            }
            // A guaranteed deposit has a compact mixed core in its geometry, even in a one-block band.
            Role core = center.guaranteed() && Math.abs(x - center.x()) <= 1 && Math.abs(z - center.z()) <= 1
                    ? ROLES[((x - center.x()) & 1) + 2 * ((z - center.z()) & 1)]
                    : null;
            if (core == null && Math.min(primary, secondary) > 1) return null;
            return new Column(this, primary, secondary, heightShift, at(center.seed(), x, z), core);
        }
    }

    public enum Role {
        BOTTOM,
        BETWEEN,
        TOP,
        SPREAD;

        public GTMaterial material(OreVein vein) {
            return switch (this) {
                case BOTTOM -> vein.bottom();
                case BETWEEN -> vein.between();
                case TOP -> vein.top();
                case SPREAD -> vein.spread();
            };
        }
    }

    private static final Role[] ROLES = Role.values();

    public record Sample(Role role, boolean small) {}

    private static final Sample[] CORE = samples(false);
    private static final Sample[] FRINGE = samples(true);

    private static Sample[] samples(boolean small) {
        return java.util.Arrays.stream(ROLES)
                .map(role -> new Sample(role, small))
                .toArray(Sample[]::new);
    }

    public record Column(Body body, double primary, double secondary, double heightShift, long seed, Role core) {
        public Sample sample(int y) {
            if (y < body.minY() || y > body.maxY()) return null;
            if (core != null && Math.abs(y - body.centerY()) <= 1) return CORE[core.ordinal()];
            double v = (y - body.centerY()) / (double) Math.max(1, body.halfHeight()) - heightShift;
            double distance = switch (body.shape()) {
                case VEINED -> Math.min(primary + sq(v / 0.62), secondary + sq((v + 0.16) / 0.53));
                case LAYERED -> primary + sq(v / 0.57);
                case DIKE -> primary + sq(v / 0.90);
                case CLUSTER -> Math.min(primary + sq((v + 0.19) / 0.76), secondary + sq((v - 0.24) / 0.68));
            };
            if (distance > 1) return null;
            long random = mix(seed ^ 0x165667B19E3779F9L * y);
            int density = 43 + 6 * Math.min(6, body.center().vein().density()) - (int) (25 * distance);
            if (roll(random, 100) >= density) return null;
            Role role;
            if (body.halfHeight() == 0) role = ROLES[roll(mix(~random), ROLES.length)];
            else if (roll(mix(~random), 7) == 0) role = Role.SPREAD;
            else if (v < -0.18) role = Role.BOTTOM;
            else if (v > 0.18) role = Role.TOP;
            else role = Role.BETWEEN;
            return (distance >= 0.68 ? FRINGE : CORE)[role.ordinal()];
        }
    }

    private static double sq(double value) {
        return value * value;
    }
}
