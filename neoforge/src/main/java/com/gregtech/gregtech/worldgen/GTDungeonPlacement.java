package com.gregtech.gregtech.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * The placement that makes vanilla's structure system see GT6's dungeon lattice
 * ({@code abs(chunk) % 11 == 5} on both axes: {@code GTDungeonLayout.GRID_PERIOD} and
 * {@code GTDungeonLayout.ANCHOR_OFFSET}, i.e. {@code WorldgenDungeonGT:151-152}).
 *
 * <h2>Why a subclass of {@code RandomSpreadStructurePlacement}</h2>
 * <p>{@code /locate structure gregtech:gt_dungeon} reaches
 * {@code ServerLevel.findNearestMapStructure} ({@code ServerLevel:1121-1134}) and then
 * {@code ChunkGenerator.findNearestMapStructure} ({@code ChunkGenerator:120-186}), which dispatches on the
 * placement's runtime type: {@code ConcentricRingsStructurePlacement} first ({@code :142}) and
 * {@code RandomSpreadStructurePlacement} second ({@code :153-156}). <b>Any other placement type is silently
 * ignored</b> - the candidate list stays empty and locate answers "not found" however correct the placement
 * is. A custom placement <em>type</em> is therefore fine, but it has to be a
 * {@code RandomSpreadStructurePlacement} subclass, which is what this class is.</p>
 *
 * <h2>Why the lattice cannot come from {@code minecraft:random_spread}</h2>
 * <p>Vanilla's own random spread picks a <em>random</em> chunk per region:
 * {@code RandomSpreadStructurePlacement.getPotentialStructureChunk} ({@code :51-60}) seeds a
 * {@code WorldgenRandom} from the level seed, the region index and the salt, then draws
 * {@code spreadType.evaluate(random, spacing - separation)} per axis. No salt can pin that draw to 5 for
 * every level seed, so with {@code minecraft:random_spread} locate would report chunks that hold no
 * dungeon. This subclass keeps the record shape of the vanilla type - the JSON still declares
 * {@code spacing}, {@code separation} and {@code salt}, and the codec refuses anything that is not GT6's
 * lattice - but answers with the lattice itself:</p>
 * <ul>
 *   <li>{@link #getPotentialStructureChunk} snaps the queried chunk to the anchor of its grid region, so
 *       locate's ring search ({@code ChunkGenerator:216-237}, which walks {@code center + spacing * ring}
 *       and asks the placement for that region's chunk) lands on real anchors,</li>
 *   <li>{@link #isPlacementChunk} accepts a chunk exactly when {@link GTDungeonLayout#isAnchor} does, which
 *       is the rule {@code GTDungeonFeature.place} uses for the cells it builds - chunk generation gates
 *       the structure starts on {@code StructurePlacement.isStructureChunk}
 *       ({@code StructurePlacement:62-70}), i.e. on this method.</li>
 * </ul>
 *
 * <p>This placement is GT6's lattice and nothing else: it accepts exactly the anchors
 * {@link GTDungeonLayout#isAnchor} names, so {@code isStructureChunk} answers what
 * {@code GTDungeonFeature.place} uses to pick the cells it builds. GT6's one-in-a-hundred gate
 * ({@code WorldgenDungeonGT:150}) is deliberately <em>not</em> here - it belongs to the dungeon, and both
 * sides roll it from the anchor's seed ({@link GTDungeonLayout#passesProbability}): the feature in
 * {@code GTDungeonFeature.place}, the structure in {@link GTDungeonStructure#findGenerationPoint}. That is
 * what keeps {@code /locate} honest - a structure exists only where a dungeon was really rolled - while the
 * placement stays the plain lattice the task of finding an anchor needs.</p>
 */
public final class GTDungeonPlacement extends RandomSpreadStructurePlacement {

    /** GT6's grid period, {@code mMaxSize + 4} ({@link GTDungeonLayout#GRID_PERIOD}). */
    public static final int SPACING = GTDungeonLayout.GRID_PERIOD;
    /** GT6 aligns every dungeon to the anchor in the middle of its grid, so nothing is separated off. */
    public static final int SEPARATION = 0;
    /** GT6's lattice holds no randomness at all, so the record's salt goes unused; declared as 0. */
    public static final int SALT = 0;

    /**
     * The placement codec, in exactly the shape of vanilla's {@code RandomSpreadStructurePlacement.CODEC}
     * ({@code RandomSpreadStructurePlacement:15-17}): {@code placementCodec(...)} - locate offset, frequency
     * reduction, salt and exclusion zone - plus {@code spacing} and {@code separation}, all wrapped in
     * {@code ExtraCodecs.validate}, so a datapack cannot set a period that would desync this placement from
     * the feature that builds the dungeon.
     *
     * <p><b>Why {@code RecordCodecBuilder.mapCodec} and not {@code RecordCodecBuilder.create}.</b>
     * {@code StructurePlacement.CODEC} dispatches on the placement's type through DFU's
     * {@code KeyDispatchCodec}, whose decode has two shapes: a {@code MapCodecCodec} is decoded with the
     * whole JSON object, everything else falls through to {@code c.decode(ops, input.get("value"))} - a
     * <em>nested</em> {@code "value"} entry no structure set JSON has. {@code RecordCodecBuilder.create(...)}
     * is a {@code MapCodecCodec} ({@code MapCodec.codec()} returns {@code new MapCodecCodec<>(this)}), but
     * {@code ExtraCodecs.validate} <em>on a Codec</em> is a {@code flatXmap} and yields a plain {@code Codec},
     * which would send the dispatch looking for that {@code "value"} entry: the decode then lands in
     * {@code JsonOps.getMap(null)} and fails with {@code "Not a JSON object: null"} - exactly how this file
     * first broke the whole datapack load. {@code mapCodec(...)} plus the {@code MapCodec} overload of
     * {@code validate} keep it a {@code MapCodecCodec}, which is what vanilla's own placement is.</p>
     */
    public static final com.mojang.serialization.MapCodec<GTDungeonPlacement> CODEC =
            RecordCodecBuilder.<GTDungeonPlacement>mapCodec(instance -> placementCodec(instance)
                    .and(instance.group(
                            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(GTDungeonPlacement::spacing),
                            Codec.intRange(0, 4096).fieldOf("separation").forGetter(GTDungeonPlacement::separation)))
                    .apply(instance, GTDungeonPlacement::new)).validate(
            placement -> placement.spacing() == SPACING && placement.separation() == SEPARATION
                    ? DataResult.success(placement)
                    : DataResult.error(() -> "gregtech:gt_dungeon_lattice is GT6's fixed lattice: spacing must be "
                            + SPACING + " and separation " + SEPARATION));

    private GTDungeonPlacement(Vec3i locateOffset, StructurePlacement.FrequencyReductionMethod method,
                               float frequency, int salt, Optional<StructurePlacement.ExclusionZone> exclusionZone,
                               int spacing, int separation) {
        super(locateOffset, method, frequency, salt, exclusionZone, spacing, separation, RandomSpreadType.LINEAR);
    }

    /** GT6's anchor lattice: {@code abs(chunk) % GRID_PERIOD == ANCHOR_OFFSET} on both axes. */
    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return GTDungeonLayout.isAnchor(chunkX) && GTDungeonLayout.isAnchor(chunkZ);
    }

    /**
     * The anchor of the queried chunk's grid region - GT6's lattice point instead of a random draw. Locate
     * asks this for every region on its expanding ring ({@code ChunkGenerator:227}), and every answer has
     * to be a chunk {@link #isPlacementChunk} accepts, which is what {@link GTDungeonLayout#anchorOfRegion}
     * returns.
     */
    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        return new ChunkPos(GTDungeonLayout.anchorOfRegion(GTDungeonLayout.regionOf(chunkX)),
                GTDungeonLayout.anchorOfRegion(GTDungeonLayout.regionOf(chunkZ)));
    }

    /** The registered type, {@code gregtech:gt_dungeon_lattice} (see {@link GTStructures}). */
    @Override
    public StructurePlacementType<?> type() {
        return GTStructures.DUNGEON_LATTICE.get();
    }
}
