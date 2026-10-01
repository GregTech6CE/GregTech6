package com.gregtech.gregtech.block.misc;


import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * GT6 {@code GT_API_Proxy.onItemExpireEvent}: a dropped stack of ingots, plates or gem plates
 * becomes its corresponding placeable pile when it would despawn. The original searches its
 * {@code CUBE_3} positions in this exact order and places only in an irrelevant block.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class PileItemExpireHandler {
    private static final BlockPos[] CUBE_3=java.util.Arrays.stream(com.gregtech.gregtech.block.MaterialPileRules.EXPIRE_OFFSETS).map(v->new BlockPos(v[0],v[1],v[2])).toArray(BlockPos[]::new);

    private PileItemExpireHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || entity.isRemoved()) return;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || PileBlockEntity.materialOf(stack) == null) return;

        var prefix = PileBlockEntity.prefixOf(stack);
        PileBlock.Kind kind;
        if (prefix == PileBlock.Kind.INGOT.prefix()) kind = PileBlock.Kind.INGOT;
        else if (prefix == PileBlock.Kind.PLATE.prefix()) kind = PileBlock.Kind.PLATE;
        else if (prefix == PileBlock.Kind.GEM_PLATE.prefix()) kind = PileBlock.Kind.GEM_PLATE;
        else return;

        BlockPos origin = entity.blockPosition();
        for (BlockPos offset : CUBE_3) {
            BlockPos target = origin.offset(offset);
            if (!level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                    || !isIrrelevant(level.getBlockState(target))) continue;
            BlockState placed = kind.block().defaultBlockState();
            if (!placed.canSurvive(level, target) || !level.setBlock(target, placed, Block.UPDATE_ALL)) continue;

            if (!(level.getBlockEntity(target) instanceof PileBlockEntity pile)) {
                level.removeBlock(target, false);
                continue;
            }
            int moved = pile.add(stack);
            if (moved == 0) {
                level.removeBlock(target, false);
                continue;
            }
            if (moved == stack.getCount()) {
                entity.discard();
            } else {
                // Unusual modded stacks larger than GT6's 64-item pile keep their remainder.
                stack.shrink(moved);
                entity.setItem(stack);
                event.setExtraLife(6000);
            }
            // NeoForge expiry uses extra life; completed transfer already discarded the entity.
            return;
        }
    }

    /** GT6 {@code WD.irrelevant}: air, vine, snow layer, fire, grass/fern or water. */
    private static boolean isIrrelevant(BlockState state) {
        return state.isAir() || state.is(Blocks.VINE) || state.is(Blocks.SNOW)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.WATER);
    }
}
