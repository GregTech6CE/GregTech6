package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.content.tool.DynamiteSubstrates;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 Behavior_Place_Dynamite: reverse inventory search, sunk charge, first usable hotbar remote. */
public final class BehaviorPlaceDynamite {
    private BehaviorPlaceDynamite() {}

    public static InteractionResult use(ElectricToolItem tool, ItemStack drill, UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var support = context.getClickedPos();
        var target = support.relative(context.getClickedFace());
        if (level.isClientSide || player == null || !level.hasChunkAt(target)
                || !ItemBehaviors.mayEdit(level, player, support) || !ItemBehaviors.mayEdit(level, player, target)
                || !DynamiteSubstrates.canDrill(level, support)
                || !player.isCreative() && !tool.hasEnergyForUse(drill)) return InteractionResult.PASS;
        for (int slot = player.getInventory().items.size() - 1; slot >= 0; slot--) {
            var inventoryStack = player.getInventory().items.get(slot);
            if (!(inventoryStack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof DynamiteBlock)) continue;
            // Use a copy: neither failed placement nor the temporary SUNK tag can alter inventory NBT.
            var placed = inventoryStack.copy();
            var stateTag = placed.getOrCreateTag().getCompound("BlockStateTag");
            stateTag.putString("sunk", "true");
            placed.getOrCreateTag().put("BlockStateTag", stateTag);
            var hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), support, context.isInside());
            var placement = new UseOnContext(level, player, context.getHand(), placed, hit);
            // ItemStack.useOn retains Forge block-place snapshots and protection-event rollback.
            var result = placed.useOn(placement);
            if (!result.consumesAction() || !level.getBlockState(target).is(item.getBlock())) continue;
            if (!player.isCreative()) {
                inventoryStack.shrink(1);
                tool.consumeEnergy(drill, player);
            }
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                var remote = player.getInventory().items.get(hotbar);
                if (net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "remote_activator")
                        .equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(remote.getItem()))
                        && BehaviorRemote.addCoords(remote, level, target)) break;
            }
            player.getInventory().setChanged();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
