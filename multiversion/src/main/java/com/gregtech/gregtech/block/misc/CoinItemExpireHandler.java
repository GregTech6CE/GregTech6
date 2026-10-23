package com.gregtech.gregtech.block.misc;


//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.registry.GTCoinPiles;
*///?} else {
import com.gregtech.gregtech.registry.GTDecorBlocks;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** GT6 coin items settle into a nearby matching pile or form a new pile every 200 ticks. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
public final class CoinItemExpireHandler {
    /** GT6's ALL_SIDES_MIDDLE_DOWN: own position, below, north, south, west, east, above. */
    private static final BlockPos[] OFFSETS = {
            BlockPos.ZERO, new BlockPos(0, -1, 0), new BlockPos(0, 0, -1),
            new BlockPos(0, 0, 1), new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
            new BlockPos(0, 1, 0)
    };

    private CoinItemExpireHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || entity.isRemoved()) return;
        ItemStack coin = entity.getItem();
        if (!CoinPileBlock.isCoin(coin)) return;

        // GT6's MultiTileEntityCoin.onDespawn always grants another 200 ticks, even when the item
        // is still airborne or no position can accept it yet. The merge itself requires onGround.
        event.setExtraLife(200);

//? if >=1.21.1 {
//?} else {
        event.setCanceled(true);
//?}
        if (!entity.onGround()) return;

        BlockPos origin = entity.blockPosition();
        int remaining = coin.getCount();
        // Existing matching piles win before any new block is placed. Each has 16 cells of 16.
        for (BlockPos offset : OFFSETS) {
            if (remaining <= 0) break;
            BlockPos target = origin.offset(offset);
            if (!level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)) continue;
            if (level.getBlockEntity(target) instanceof CoinPileBlockEntity pile)
                remaining -= pile.absorbDroppedCoins(coin.copyWithCount(remaining));
        }

        // GT6 next checks the same seven positions for a free place to make a fresh pile.
//? if >=1.21.1 {
        /*BlockState placed = GTCoinPiles.COIN_PILE.get().defaultBlockState();
*///?} else {
        BlockState placed = GTDecorBlocks.COIN_PILE.get().defaultBlockState();
//?}
        for (BlockPos offset : OFFSETS) {
            if (remaining <= 0) break;
            BlockPos target = origin.offset(offset);
            if (!level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                    || !PilePlacementChecks.canReplaceForExpiry(level.getBlockState(target)) || !placed.canSurvive(level, target)
                    || !level.setBlock(target, placed, Block.UPDATE_ALL)) continue;
            if (level.getBlockEntity(target) instanceof CoinPileBlockEntity pile) {
                int moved = pile.absorbDroppedCoins(coin.copyWithCount(remaining));
                if (moved > 0) {
                    remaining -= moved;
                    continue;
                }
            }
            level.removeBlock(target, false);
        }

        if (remaining == 0) entity.discard();
        else if (remaining < coin.getCount()) entity.setItem(coin.copyWithCount(remaining));
    }


}
