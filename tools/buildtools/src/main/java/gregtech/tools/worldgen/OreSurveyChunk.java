package gregtech.tools.worldgen;

import static gregtech.tools.worldgen.WorldgenSampler.BOTTOM;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.worldgen.BoundaryGemOres;
import gregtech.content.worldgen.GTBedrockOres.BedrockOre;
import gregtech.content.worldgen.GTOreVeins.SmallOre;
import gregtech.content.worldgen.MineralWorldgenRules;
import gregtech.content.worldgen.OreVeinLayout.Body;
import gregtech.content.worldgen.SpecialOreDeposits;
import gregtech.content.worldgen.StoneLayerField;
import gregtech.content.worldgen.StratumOreRules;
import gregtech.content.worldgen.WorldgenHash;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntUnaryOperator;

/** One synthetic Overworld column of rock, reused for each chunk in the survey. */
final class OreSurveyChunk implements StratumOreRules.Placer, MineralWorldgenRules.VeinSink {
    static final String DIMENSION = OreVeinDeclaration.OVERWORLD;
    static final byte NORMAL = 1, POOR = 2, SEAM = 3, BEDROCK = 4, BEDROCK_SMALL = 5;
    private final long seed;
    private final int top;
    private final int height;
    private final long bedrockSeed;
    private final MineralWorldgenRules.Point coltan;
    private final StoneLayerField.Contacts contacts = new StoneLayerField.Contacts();
    private final GTMaterial[] seamMaterials;
    final GTMaterial[] minerals;
    final byte[] forms;
    private int cursor;
    private GTMaterial bedrockMaterial;

    OreSurveyChunk(long seed, int top, GTMaterial[] seamMaterials) {
        this.seed = seed;
        this.top = top;
        this.seamMaterials = seamMaterials;
        height = top - BOTTOM + 1;
        minerals = new GTMaterial[256 * height];
        forms = new byte[minerals.length];
        bedrockSeed = MineralWorldgenRules.bedrockSeed(seed, DIMENSION);
        coltan = MineralWorldgenRules.coltanCentre(seed);
    }

    void generate(
            int chunkX,
            int chunkZ,
            StoneLayerField.Samples field,
            WorldgenSurvey.LayerVolumes layerVolumes,
            List<Body> veins,
            List<SmallOre> smallOres,
            List<BedrockOre> bedrock) {
        Arrays.fill(minerals, null);
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        strata(field, minX, minZ, layerVolumes);
        for (var body : veins) vein(body, minX, minZ);
        scatteredOres(smallOres, random(chunkX, chunkZ, 2));
        coltan(minX, minZ, random(chunkX, chunkZ, 3));
        var random = random(chunkX, chunkZ, 4);
        for (var ore : bedrock) {
            if (MineralWorldgenRules.bedrockHit(bedrockSeed, chunkX, chunkZ, ore.name(), ore.chance(), 100))
                basalVein(minX, minZ, ore.material(), random);
        }
    }

    private IntUnaryOperator random(int chunkX, int chunkZ, int phase) {
        return new SplittableRandom(WorldgenHash.at(seed, chunkX, phase, chunkZ))::nextInt;
    }

    private void strata(StoneLayerField.Samples field, int minX, int minZ, WorldgenSurvey.LayerVolumes volumes) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                cursor = index(x, BOTTOM + 1, z);
                for (int y = BOTTOM + 1; y <= top; y++, cursor++) {
                    int layer = field.layerIndex(x, y, z);
                    field.contacts(x, y, z, contacts);
                    volumes.record(layer, y, contacts.nearest());
                    long positionSeed = WorldgenHash.at(seed, x, y, z);
                    if (StratumOreRules.place(layer, y, contacts, positionSeed, this) != null) continue;
                    GTMaterial seam = seamMaterials[layer];
                    if (seam != null) {
                        put(cursor, seam, SEAM);
                    } else if (contacts.nearest() == 1) {
                        var gem = BoundaryGemOres.choose(positionSeed, BoundaryGemOres.CHANCE_DIVISOR);
                        if (gem != null) put(cursor, gem, POOR);
                    }
                }
            }
        }
    }

    @Override
    public boolean place(StratumOreRules.Ore ore, boolean small) {
        if (WorldgenSurvey.disabledVanillaOre(ore.blockId())) return false;
        if (ore.blockId().isEmpty()) put(cursor, ore.material(), small ? POOR : NORMAL);
        // Explicit non-ore decoration consumes its roll and remains available as stone.
        return true;
    }

    private void vein(Body body, int minX, int minZ) {
        var center = body.center();
        int radius = body.radius();
        for (int x = Math.max(minX, center.x() - radius); x <= Math.min(minX + 15, center.x() + radius); x++) {
            for (int z = Math.max(minZ, center.z() - radius); z <= Math.min(minZ + 15, center.z() + radius); z++) {
                var column = body.column(x, z);
                if (column == null) continue;
                int position = index(x, body.minY(), z);
                for (int y = body.minY(); y <= body.maxY(); y++, position++) {
                    var ore = column.sample(y);
                    if (ore == null) continue;
                    var material = ore.role().material(center.vein()).resolve();
                    if (minerals[position] == null
                            || !ore.small() && minerals[position] == material && forms[position] == POOR)
                        put(position, material, ore.small() ? POOR : NORMAL);
                }
            }
        }
    }

    private void scatteredOres(List<SmallOre> ores, IntUnaryOperator random) {
        for (var ore : ores) {
            var heights = ore.heights(DIMENSION, BOTTOM + 1, top);
            if (heights.isEmpty()) continue;
            int attempts = MineralWorldgenRules.scatterAttempts(ore.amount(), random);
            for (int i = 0; i < attempts; i++) {
                int x = random.applyAsInt(16);
                int y = ore.distribution().sample(heights.min(), heights.max(), random);
                int z = random.applyAsInt(16);
                scatter(x, y, z, ore.material(), POOR);
            }
        }
    }

    private void coltan(int minX, int minZ, IntUnaryOperator random) {
        if ((coltan.x() & ~15) == minX && (coltan.z() & ~15) == minZ)
            basalVein(minX, minZ, OreMaterials.Coltan, random);
        long distance = MineralWorldgenRules.distanceSquared(coltan.x(), coltan.z(), minX, minZ);
        int radius = SpecialOreDeposits.COLTAN_RADIUS;
        if (distance > radius * radius) return;
        coltanScatter(random, POOR);
        int fullRadius = SpecialOreDeposits.COLTAN_FULL_ORE_RADIUS;
        if (distance <= fullRadius * fullRadius) coltanScatter(random, NORMAL);
    }

    private void coltanScatter(IntUnaryOperator random, byte form) {
        var heights = SpecialOreDeposits.COLTAN_HEIGHTS;
        int low = Math.max(BOTTOM + 1, heights.min());
        int high = Math.min(top, heights.max());
        if (high < low) return;
        var materials = SpecialOreDeposits.COLTAN_ROLLS;
        int attempts = MineralWorldgenRules.scatterAttempts(SpecialOreDeposits.COLTAN_AMOUNT, random);
        for (int i = 0; i < attempts; i++) {
            int x = random.applyAsInt(16);
            int z = random.applyAsInt(16);
            int y = low + random.applyAsInt(high - low + 1);
            scatter(x, y, z, materials.get(random.applyAsInt(materials.size())), form);
        }
    }

    private void basalVein(int minX, int minZ, GTMaterial material, IntUnaryOperator random) {
        bedrockMaterial = material.resolve();
        MineralWorldgenRules.bedrockVein(minX, minZ, BOTTOM, Math.min(63, top + 1), true, random, this);
    }

    @Override
    public boolean isBedrockFloor(int x, int y, int z) {
        return y == BOTTOM;
    }

    @Override
    public void prepareDrillPad(int x, int y, int z) {
        // The synthetic bedrock occupies only the bottom layer.
    }

    @Override
    public void prepareStone(int x, int y, int z) {
        // Unoccupied positions above bedrock already provide a valid host.
    }

    @Override
    public boolean bedrock(int x, int y, int z, boolean small) {
        int position = index(x, y, z);
        if (minerals[position] != null)
            return !small && minerals[position] == bedrockMaterial && forms[position] == BEDROCK;
        put(position, bedrockMaterial, small ? BEDROCK_SMALL : BEDROCK);
        return true;
    }

    @Override
    public boolean ore(int x, int y, int z, boolean small) {
        return y <= top && scatter(x, y, z, bedrockMaterial, small ? POOR : NORMAL);
    }

    private boolean scatter(int x, int y, int z, GTMaterial material, byte form) {
        int position = index(x, y, z);
        if (minerals[position] != null) return false;
        put(position, material, form);
        return true;
    }

    private void put(int position, GTMaterial material, byte form) {
        minerals[position] = material.resolve();
        forms[position] = form;
    }

    private int index(int x, int y, int z) {
        return ((x & 15) * 16 + (z & 15)) * height + y - BOTTOM;
    }
}
