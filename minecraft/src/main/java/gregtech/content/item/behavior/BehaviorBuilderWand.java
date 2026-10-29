package gregtech.content.item.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Ordinary GT6 builder plane: material-quality reach and placement on the clicked face.
 * Multiblock building is dispatched by GTToolItem before this behavior.
 */
public final class BehaviorBuilderWand {

    private BehaviorBuilderWand() {}

    /**
     * One right-click with the wand — GT6 {@code Behavior_Builderwand.onItemUse}.
     *
     * @param side the clicked face; its axis is the one the plane does <em>not</em> walk
     * @return {@code acted} when at least one copy was placed, with the (possibly damaged) wand
     */
//? if >=1.21.1 {
    /*public static ManualToolBehaviorAccess.Outcome useOn(Level level, BlockPos pos, Direction side,
*///?} else {
    public static ItemBehaviors.Outcome useOn(Level level, BlockPos pos, Direction side,
//?}
                                              @Nullable Player player, ItemStack wand,
                                              float hitX, float hitY, float hitZ) {
//? if >=1.21.1 {
        /*if (level.isClientSide() || player == null) return ManualToolBehaviorAccess.Outcome.refused(wand);
        if (!level.hasChunkAt(pos) || !ManualToolBehaviorAccess.mayEdit(level, player, pos)) {
            return ManualToolBehaviorAccess.Outcome.refused(wand);
*///?} else {
        if (level.isClientSide() || player == null) return ItemBehaviors.Outcome.refused(wand);
        if (!level.hasChunkAt(pos) || !ItemBehaviors.mayEdit(level, player, pos)) {
            return ItemBehaviors.Outcome.refused(wand);
//?}
        }
        BlockState clicked = level.getBlockState(pos);
//? if >=1.21.1 {
        /*if (clicked.isAir() || !gregtech.engine.tool.GTToolHelper.isUsable(wand)) return ManualToolBehaviorAccess.Outcome.refused(wand);
*///?} else {
        if (clicked.isAir() || !gregtech.engine.tool.GTToolHelper.isUsable(wand)) return ItemBehaviors.Outcome.refused(wand);
//?}

        int reach = Math.max(0, gregtech.engine.tool.GTToolHelper.getHead(wand).getToolQuality() + 1);
        boolean placedAny = false;
        placementLoop:
        for (int dx = step(side, Direction.Axis.X, -reach); dx <= step(side, Direction.Axis.X, reach); dx++) {
            for (int dy = step(side, Direction.Axis.Y, -reach); dy <= step(side, Direction.Axis.Y, reach); dy++) {
                for (int dz = step(side, Direction.Axis.Z, -reach); dz <= step(side, Direction.Axis.Z, reach); dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (!level.hasChunkAt(target)) continue;
                    // - only positions already holding the very same block (and meta) count.
                    if (!level.getBlockState(target).equals(clicked)) continue;
                    // - the position itself and the one the face points at must both be editable.
//? if >=1.21.1 {
                    /*if (!ManualToolBehaviorAccess.mayEdit(level, player, target)) continue;
                    if (!ManualToolBehaviorAccess.mayEdit(level, player, target.relative(side))) continue;
*///?} else {
                    if (!ItemBehaviors.mayEdit(level, player, target)) continue;
                    if (!ItemBehaviors.mayEdit(level, player, target.relative(side))) continue;
//?}

                    if (!gregtech.engine.tool.GTToolHelper.isUsable(wand)) break placementLoop;
                    int sourceSlot = findSourceBlock(player, clicked);
                    if (sourceSlot < 0) break;   // GT6 runs out of matching blocks and stops the plane
                    if (!place(level, player, target, side, sourceSlot, wand, hitX, hitY, hitZ)) continue;
//? if >=1.21.1 {
                    /*if (!ManualToolBehaviorAccess.creative(player)) {
*///?} else {
                    if (!ItemBehaviors.creative(player)) {
//?}
                        gregtech.engine.tool.GTToolHelper.damageForUse(wand, 1, player,
                                player.getOffhandItem() == wand ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
                                        : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                    placedAny = true;
                }
            }
        }
//? if >=1.21.1 {
        /*return placedAny ? ManualToolBehaviorAccess.Outcome.acted(wand) : ManualToolBehaviorAccess.Outcome.refused(wand);
*///?} else {
        return placedAny ? ItemBehaviors.Outcome.acted(wand) : ItemBehaviors.Outcome.refused(wand);
//?}
    }

    /** The loop bound for one axis: the clicked face's own axis never moves. */
    private static int step(Direction side, Direction.Axis axis, int bound) {
        return side.getAxis() == axis ? 0 : bound;
    }

    /**
     * The stack the wand copies from the player's inventory: the last one whose block is the
     * clicked block.
     *
     * <p>GT6 scans {@code player.inventory.mainInventory} from the end backwards, which in 1.7.10
     * means the main grid first and the hotbar last; 1.20.1 puts the hotbar at 0-8 and the main grid at
     * 9-35, so the same order is {@code 35 → 0}. A stack qualifies when its block is the clicked block
     * ({@code ST.block(tStack) == aBlock}). Ore host/broken identity is encoded in item state
     * rather than separate source blocks, so compare those identity properties explicitly. Placement
     * orientation remains the item's responsibility; do not compare arbitrary placement states.</p>
     */
    private static int findSourceBlock(Player player, BlockState clicked) {
        for (int slot = 35; slot >= 0; slot--) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty()) continue;
            if (!(candidate.getItem() instanceof BlockItem blockItem)) continue;
            if (blockItem.getBlock() != clicked.getBlock()) continue;
            if (clicked.getBlock() instanceof gregtech.content.block.OreBlock
                    && (gregtech.content.block.OreBlock.stoneOfStack(candidate)
                            != gregtech.content.block.OreBlock.stoneOf(clicked)
                    || gregtech.content.block.OreBlock.isBrokenStack(candidate)
                            != gregtech.content.block.OreBlock.isBroken(clicked))) continue;
            return slot;
        }
        return -1;
    }

    private static boolean isNativeEmptyBucket(ItemStack stack) {
        if (stack.getCount() != 1) return false;
        var bucket = new ItemStack(net.minecraft.world.item.Items.BUCKET);
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(stack, bucket);
*///?} else {
        return ItemStack.isSameItemSameTags(stack, bucket);
//?}
    }

    /** The copied source owns the loader transaction, not the held wand. */
    private static boolean place(Level level, Player player, BlockPos support, Direction side,
                                 int sourceSlot, ItemStack wand, float hitX, float hitY, float hitZ) {
        ItemStack source = player.getInventory().getItem(sourceSlot);
        InteractionHand hand = player.getOffhandItem() == wand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack copy = source.copy();
        BlockHitResult hit = new BlockHitResult(new Vec3(support.getX() + hitX,
                support.getY() + hitY, support.getZ() + hitZ), side, support, false);
        var use = new net.minecraft.world.item.context.UseOnContext(level, player, hand, copy, hit);
        BlockPos destination = new BlockPlaceContext(use).getClickedPos();
        if (!level.hasChunkAt(destination) || !ManualToolBehaviorAccess.mayEdit(level, player, destination)) return false;
        // ItemStack.useOn captures snapshots and returns only after cancellation/rollback.
        // Calling BlockItem.place directly would skip that boundary in onItemUseFirst.
        // Vanilla SolidBucketItem writes its container to the real context hand, even though
        // the placement stack is a copy. Neo also writes the copy there in creative mode.
        // Do not pre-swap hands: all native interaction/protection callbacks still run normally.
        boolean solidBucket = source.is(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET)
                && source.getItem().getClass() == net.minecraft.world.item.SolidBucketItem.class;
        ItemStack returnedContainer = ItemStack.EMPTY;
        InteractionResult result;
        try {
            result = copy.useOn(use);
        } finally {
            ItemStack after = player.getItemInHand(hand);
            if (solidBucket && after != wand) {
                if (after == copy) {
                    player.setItemInHand(hand, wand); // Native Neo creative replacement.
                } else if (isNativeEmptyBucket(after)) {
                    returnedContainer = after;
                    player.setItemInHand(hand, wand);
                }
                // Unknown listener/mod replacements are not ours to overwrite or reinterpret.
            }
        }
        if (!result.consumesAction()) return false; // Canceled native container is discarded too.
        if (!player.getAbilities().instabuild) {
            source.shrink(Math.max(0, source.getCount() - copy.getCount()));
            if (!returnedContainer.isEmpty()) {
                if (source.isEmpty() && player.getInventory().getItem(sourceSlot).isEmpty()) {
                    player.getInventory().setItem(sourceSlot, returnedContainer);
                } else if (!player.addItem(returnedContainer)) {
                    player.drop(returnedContainer, false);
                }
            }
        }
        player.getInventory().setChanged();
        return true;
    }
}
