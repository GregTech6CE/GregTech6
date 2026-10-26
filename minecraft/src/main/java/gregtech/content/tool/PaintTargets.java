package gregtech.content.tool;

import gregtech.engine.tool.Paintable;
import gregtech.content.block.misc.FixedConstructionColor;
import gregtech.content.registry.GTConstructionVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Spray target policy; color identities and actual block behavior remain owned by their registries. */
public final class PaintTargets {

    private PaintTargets() {
    }
    private static final Map<Block, String> FAMILY = new HashMap<>();
    static {
        for (var color : DyeColor.values())
        for (String suffix : List.of("terracotta", "stained_glass", "stained_glass_pane", "wool", "carpet"))
        FAMILY.put(vanilla(color.getName() + "_" + suffix), suffix);
        FAMILY.put(Blocks.GLASS, "stained_glass");
        FAMILY.put(Blocks.GLASS_PANE, "stained_glass_pane");
        FAMILY.put(Blocks.TERRACOTTA, "terracotta");
    }

    private static Block vanilla(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", path));
    }

    private static boolean grass(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id.getNamespace().equals("gt6ce") && PaintingRules.GRASS_IDS.contains(id.getPath());
    }

    public static Block colorTarget(Block block, int index) {
        Block target;
        String suffix = FAMILY.get(block);
        if (suffix != null) {
            String color = DyeColor.byId(PaintingRules.vanillaDyeId(index)).getName();
            target = vanilla(color + "_" + suffix);
        } else if (block == Blocks.GRASS_BLOCK || grass(block)) {
            String id = PaintingRules.grassId(index);
            if (id == null) return null;
            target = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
            if (target == Blocks.AIR) return null;
        } else return null;
        return target == block ? null : target;
    }

    public static long color(Level level, BlockPos pos, int index) {
        var state = level.getBlockState(pos);
        if (GTConstructionVariants.family(state.getBlock()) != null) {
            var dye = DyeColor.byId(PaintingRules.vanillaDyeId(index));
            return FixedConstructionColor.recolor(level, pos, dye) ? PaintingRules.BLOCK_COST : 0;
        }
        if (level.getBlockEntity(pos) instanceof Paintable target) {
            boolean changed = target.isPainted() ? target.mixPaint(PaintingRules.DYES.get(index).rgb())
                    : target.paint(PaintingRules.DYES.get(index).rgb());
            return changed ? PaintingRules.BLOCK_COST : 0;
        }
        Block target = colorTarget(state.getBlock(), index);
        return target != null && level.setBlock(pos, target.defaultBlockState(), 3) ? PaintingRules.BLOCK_COST : 0;
    }

    public static boolean unpaint(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var family = GTConstructionVariants.family(state.getBlock());
        if (family != null) return FixedConstructionColor.recolor(level, pos, GTConstructionVariants.defaultColor(family));
        if (level.getBlockEntity(pos) instanceof Paintable target && target.unpaint()) return true;
        return grass(state.getBlock()) && level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
    }
}
