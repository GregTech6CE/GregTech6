package gregtech.content.multiblock;

import gregtech.content.block.machine.OriginalLargeBoilerControllerBlock;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.engine.multiblock.tracking.StructurePlan;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Both viewers consume the same compiled cells as structure validation and the building wand. */
public final class MultiblockInfoData {
    private MultiblockInfoData() {}

    public record Info(
            ItemStack controller,
            Map<BlockPos, MultiblockLayout.Role> cells,
            boolean boiler,
            Map<BlockPos, ItemStack> blocks) {}

    public static List<Info> recipes() {
        return recipes(ControllerStructureLayouts.plans());
    }

    public static List<Info> recipes(Map<Block, List<StructurePlan.Cell>> plans) {
        var result = new LinkedHashMap<Item, Info>();
        plans.forEach((block, plan) -> {
            var controller = new ItemStack(block);
            if (controller.isEmpty() || result.containsKey(controller.getItem())) return;
            var roles = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
            var blocks = new LinkedHashMap<BlockPos, ItemStack>();
            roles.put(BlockPos.ZERO, MultiblockLayout.Role.CASING);
            blocks.put(BlockPos.ZERO, new ItemStack(block));
            for (var cell : plan) {
                if (cell.offset().equals(BlockPos.ZERO)) continue;
                roles.put(cell.offset(), cell.specification().role());
                if (cell.specification().role() == MultiblockLayout.Role.AIR) continue;
                var stack = new ItemStack(cell.specification().block());
                blocks.put(cell.offset(), stack);
            }
            result.put(
                    controller.getItem(),
                    new Info(
                            controller,
                            Collections.unmodifiableMap(roles),
                            block instanceof OriginalLargeBoilerControllerBlock,
                            Collections.unmodifiableMap(blocks)));
        });
        return List.copyOf(result.values());
    }

    public static ItemStack expectedPart(Info info, BlockPos pos) {
        return info.blocks().getOrDefault(pos, ItemStack.EMPTY).copy();
    }

    public static Map<BlockPos, ItemStack> previewBlocks(Info info) {
        var copy = new LinkedHashMap<BlockPos, ItemStack>();
        info.blocks().forEach((pos, stack) -> copy.put(pos, stack.copy()));
        return Collections.unmodifiableMap(copy);
    }

    public static List<ItemStack> parts(Info info) {
        var counts = new LinkedHashMap<net.minecraft.world.item.Item, Integer>();
        info.blocks().forEach((pos, stack) -> {
            if (!pos.equals(BlockPos.ZERO) && !stack.isEmpty()) counts.merge(stack.getItem(), 1, Integer::sum);
        });
        return counts.entrySet().stream()
                .map(e -> new ItemStack(e.getKey(), e.getValue()))
                .toList();
    }
}
