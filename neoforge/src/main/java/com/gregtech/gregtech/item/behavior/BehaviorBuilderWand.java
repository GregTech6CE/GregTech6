package com.gregtech.gregtech.item.behavior;

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

/**
 * GT6 Builder's Wand ({@code gregtech.items.behaviors.Behavior_Builderwand}, hung on the
 * {@code TOOL_builderwand} tool by {@code GT_Tool_Builderwand:62}).
 *
 * <p>GT6's click handler ({@code Behavior_Builderwand:82-135}) takes the clicked block, then walks the
 * <b>plane perpendicular to the clicked face</b> through that block — the axis of the clicked face
 * stays at offset 0, the other two axes span {@code -tDist..+tDist}
 * ({@code :89-91}) — and for every position that already holds <em>the same block and metadata</em>
 * ({@code :92}) it takes a matching block out of the player's inventory ({@code :97-117}) and places
 * it with {@code tryPlaceItemIntoWorld(..., SIDE_TOP, 0.5F, 0.25F, 0.5F)} ({@code :123}).</p>
 *
 * <h2>The two details that decide the port's behaviour</h2>
 *
 * <ul>
 *   <li><b>The placement side is hardcoded {@code SIDE_TOP}</b> and GT6's {@code SIDE_TOP} is
 *       {@code 1} ({@code CS.java:517}), which is vanilla's {@code Direction.UP}. Vanilla's
 *       {@code ItemBlock.onItemUse} moves an occupied position by the side's offset, so the wand
 *       places its copy <em>on top of</em> each matching block — the plane it walks is the support
 *       layer, the copy lands one block higher. (GT6's own permission double-check at {@code :95} uses
 *       {@code OFF[aSide]}, the clicked face, so the original is inconsistent with itself here; the
 *       <em>effective</em> behaviour is the one implemented, because that is what the placement call
 *       does.)</li>
 *   <li><b>The reach is {@code getPrimaryMaterial(stack).mToolQuality + 1}</b> ({@code :86}). The
 *       port registers one item per tool kind with no material ({@code registry/GTToolItems:24}), so
 *       the number cannot be derived from a stack; {@link #REACH} fixes it to the value the wand's own
 *       default head material produces — {@code GT_Tool_Builderwand:52} falls back to
 *       {@code MT.Heliodor}, and GT6's gem family carries {@code qual(3, …)} ({@code MT.java:210}) —
 *       i.e. {@code 3 + 1 = 4}, a 9×9 plane.</li>
 * </ul>
 *
 * <p><b>Not ported:</b> the Thaumcraft branch ({@code :49-79}) that builds a glass jar around a
 * {@code thaumcraft.api.nodes.INode} — the port has no Thaumcraft.</p>
 */
public final class BehaviorBuilderWand {

    /**
     * Half-width of the plane the wand walks, in blocks: GT6 {@code :86-91}
     * ({@code primaryMaterial.mToolQuality + 1}). See the class javadoc for why it is a constant here.
     */
    public static final int REACH = 4;

    private BehaviorBuilderWand() {}

    /**
     * One right-click with the wand — GT6 {@code Behavior_Builderwand.onItemUse} ({@code :44-136}).
     *
     * @param side the clicked face; its axis is the one the plane does <em>not</em> walk
     * @return {@code acted} when at least one copy was placed, with the (possibly damaged) wand
     */
    public static ManualToolBehaviorAccess.Outcome useOn(Level level, BlockPos pos, Direction side,
                                              @Nullable Player player, ItemStack wand,
                                              float hitX, float hitY, float hitZ) {
        if (level.isClientSide() || player == null) return ManualToolBehaviorAccess.Outcome.refused(wand);
        if (!level.hasChunkAt(pos) || !ManualToolBehaviorAccess.mayEdit(level, player, pos)) {
            return ManualToolBehaviorAccess.Outcome.refused(wand);
        }
        BlockState clicked = level.getBlockState(pos);
        if (clicked.isAir()) return ManualToolBehaviorAccess.Outcome.refused(wand);

        boolean placedAny = false;
        for (int dx = step(side, Direction.Axis.X, -REACH); dx <= step(side, Direction.Axis.X, REACH); dx++) {
            for (int dy = step(side, Direction.Axis.Y, -REACH); dy <= step(side, Direction.Axis.Y, REACH); dy++) {
                for (int dz = step(side, Direction.Axis.Z, -REACH); dz <= step(side, Direction.Axis.Z, REACH); dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (!level.hasChunkAt(target)) continue;
                    // GT6 :92 - only positions already holding the very same block (and meta) count.
                    if (!level.getBlockState(target).equals(clicked)) continue;
                    // GT6 :94-95 - the position itself and the one the face points at must both be editable.
                    if (!ManualToolBehaviorAccess.mayEdit(level, player, target)) continue;
                    if (!ManualToolBehaviorAccess.mayEdit(level, player, target.relative(side))) continue;

                    ItemStack source = findSourceBlock(player, clicked);
                    if (source.isEmpty()) break;   // GT6 runs out of matching blocks and stops the plane
                    if (!placeOnTop(level, player, target, source)) continue;
                    if (!ManualToolBehaviorAccess.creative(player)) {
                        // The placement already consumed one block; GT6 :124-129 only skips the damage
                        // and restores the count in creative (`UT.Entities.hasInfiniteItems`).
                        wand.hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                    placedAny = true;
                }
            }
        }
        return placedAny ? ManualToolBehaviorAccess.Outcome.acted(wand) : ManualToolBehaviorAccess.Outcome.refused(wand);
    }

    /** The loop bound for one axis: the clicked face's own axis never moves ({@code :89-91}). */
    private static int step(Direction side, Direction.Axis axis, int bound) {
        return side.getAxis() == axis ? 0 : bound;
    }

    /**
     * The block the wand will copy from the player's inventory — GT6 {@code :97-117}.
     *
     * <p>GT6 scans {@code player.inventory.mainInventory} from the end backwards, which in 1.7.10
     * means the main grid first and the hotbar last; 1.20.1 puts the hotbar at 0-8 and the main grid at
     * 9-35, so the same order is {@code 35 → 0}. A stack qualifies when its block is the clicked block
     * ({@code ST.block(tStack) == aBlock}); GT6 additionally demands metadata equality, which in the
     * port is the block's own state — state-rich blocks such as the ores carry their variant in the
     * item's {@code BlockStateTag} and {@code BlockItem.place} reads it back, so the block test is the
     * one applied here.</p>
     */
    private static ItemStack findSourceBlock(Player player, BlockState clicked) {
        for (int slot = 35; slot >= 0; slot--) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty()) continue;
            if (!(candidate.getItem() instanceof BlockItem blockItem)) continue;
            if (blockItem.getBlock() == clicked.getBlock()) return candidate;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Places a copy of {@code source} on top of {@code target}, the position GT6 reaches by passing
     * {@code SIDE_TOP} to {@code tryPlaceItemIntoWorld} ({@code :123}).
     *
     * <p>1.20.1's equivalent of that call is the item's own {@code BlockItem.place} with a
     * {@link BlockPlaceContext} built the same way vanilla builds it from a
     * {@code BlockHitResult} — that context resolves "clicked position + face" into the final
     * placement position, keeps block-specific placement rules (facing, waterlogging, …) and consumes
     * the stack itself, so the caller must not shrink it again.</p>
     */
    private static boolean placeOnTop(Level level, Player player, BlockPos target, ItemStack source) {
        BlockHitResult hit = new BlockHitResult(
                new Vec3(target.getX() + 0.5D, target.getY() + 0.25D, target.getZ() + 0.5D),
                Direction.UP, target, false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, source, hit);
        InteractionResult result = ((BlockItem) source.getItem()).place(context);
        return result.consumesAction();
    }
}
