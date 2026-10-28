package gregtech.content.multiblock;

import gregtech.engine.multiblock.MultiblockLayout;
import java.util.List;
import net.minecraft.world.level.block.Block;

/** The five large steam boilers from GT6 Loader_MultiTileEntities 17201–17205. */
public final class OriginalLargeBoilerSpecs {
    /** All volumes are GT6 litres, represented by Forge millibuckets in the port. */
    public record Variant(int originalId, String path, int denseWallId, long steamOutput, float hardness) {
        public Block wall() {
            return LargeMachineParts.block(denseWallId);
        }

        public long heatCapacity() {
            return steamOutput * 10_000L;
        }

        public long steamCapacity() {
            return steamOutput * 10_000L;
        }

        public long heatInputRecommended() {
            return steamOutput / 2;
        }
    }

    private static final List<Variant> VARIANTS = OriginalLargeBoilerParameters.all().stream()
            .map(p -> new Variant(p.originalId(), p.path(), p.denseWallId(), p.steamOutput(), p.hardness()))
            .toList();

    /** Controller at front-centre of the lowest wall layer, one block ahead of the body centre. */
    public static final MultiblockLayout LAYOUT = layout();

    private OriginalLargeBoilerSpecs() {}

    public static List<Variant> all() {
        return VARIANTS;
    }

    public static Variant byOriginalId(int id) {
        return VARIANTS.stream()
                .filter(v -> v.originalId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large boiler " + id));
    }

    private static MultiblockLayout layout() {
        return MultiblockLayout.fromShared(OriginalLargeBoilerParameters.LAYOUT);
    }
}
