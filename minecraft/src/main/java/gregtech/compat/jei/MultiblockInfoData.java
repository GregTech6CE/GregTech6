package gregtech.compat.jei;

import gregtech.content.multiblock.ControllerStructureLayouts;
import gregtech.engine.multiblock.MultiblockLayout;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Both viewers consume the same compiled cells as structure validation and the building wand. */
public class MultiblockInfoData {
    protected MultiblockInfoData() {}

    public record Info(
            ItemStack controller,
            ItemStack wall,
            ItemStack base,
            Map<BlockPos, MultiblockLayout.Role> cells,
            boolean boiler,
            ItemStack extra,
            Map<BlockPos, ItemStack> blocks) {}

    public static List<Info> recipes() {
        var result = new ArrayList<Info>();
        ControllerStructureLayouts.plans().forEach((block, plan) -> {
            var roles = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
            var blocks = new LinkedHashMap<BlockPos, ItemStack>();
            var counts = new LinkedHashMap<net.minecraft.world.item.Item, Integer>();
            roles.put(BlockPos.ZERO, MultiblockLayout.Role.CASING);
            blocks.put(BlockPos.ZERO, new ItemStack(block));
            for (var cell : plan) {
                if (cell.offset().equals(BlockPos.ZERO)) continue;
                roles.put(cell.offset(), cell.specification().role());
                if (cell.specification().role() == MultiblockLayout.Role.AIR) continue;
                var stack = new ItemStack(cell.specification().block());
                blocks.put(cell.offset(), stack);
                if (!stack.isEmpty()) counts.merge(stack.getItem(), 1, Integer::sum);
            }
            var parts = counts.entrySet().stream()
                    .map(e -> new ItemStack(e.getKey(), e.getValue()))
                    .toList();
            result.add(new Info(
                    new ItemStack(block),
                    parts.isEmpty() ? ItemStack.EMPTY : parts.get(0),
                    parts.size() < 2 ? ItemStack.EMPTY : parts.get(1),
                    Collections.unmodifiableMap(roles),
                    block instanceof gregtech.content.block.machine.OriginalLargeBoilerControllerBlock,
                    ItemStack.EMPTY,
                    Collections.unmodifiableMap(blocks)));
        });
        return List.copyOf(result);
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
