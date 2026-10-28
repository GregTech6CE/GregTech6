package gregtech.content.cover;

import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.shapes.*;

public final class CoverWorldInteraction {
    private CoverWorldInteraction() {}

    private static final ThreadLocal<Boolean> UNDERLYING_OUTLINE = ThreadLocal.withInitial(() -> false);
    /** Raw host outline: preserve attachment-art decisions even after covers join the public outline. */
    public static VoxelShape underlyingOutline(net.minecraft.world.level.block.entity.BlockEntity owner) {
        boolean previous = UNDERLYING_OUTLINE.get();
        UNDERLYING_OUTLINE.set(true);
        try {
            return owner.getBlockState().getShape(owner.getLevel(), owner.getBlockPos(), CollisionContext.empty());
        } finally {
            if (previous) UNDERLYING_OUTLINE.set(true);
            else UNDERLYING_OUTLINE.remove();
        }
    }

    public static VoxelShape outline(BlockGetter level, BlockPos pos, VoxelShape shape) {
        return UNDERLYING_OUTLINE.get() ? shape : collision(level, pos, shape);
    }
    /** Original AbstractCoverDefault.isSolid; torch/repeater and pressure valve override it false. */
    public static boolean solidCover(BlockGetter level, BlockPos pos, Direction side) {
        // Shape queries are also issued with a null level while foreign mods build collision tables.
        if (level == null) return false;
        if (!(level.getBlockEntity(pos) instanceof PanelCoverHost host)
                || !ComponentCoverFallback.ownClass(host.coverOwner())) return false;
        var stack = host.getCover(side);
        if (stack.isEmpty() && host.coverOwner() instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
            stack = logistics.logisticsCovers().get(side);
        var id = CoverItems.behavior(stack);
        return id != null
                && !CoverItems.REDSTONE_TORCH.equals(id)
                && !CoverItems.REDSTONE_REPEATER.equals(id)
                && !CoverUtilityBehaviors.PRESSURE_VALVE.equals(id);
    }

    public static VoxelShape support(BlockGetter level, BlockPos pos, VoxelShape shape) {
        if (level == null) return shape;
        for (var side : Direction.values())
            if (solidCover(level, pos, side)) {
                var box = CoverPlateGeometry.plate(side.ordinal());
                shape = Shapes.or(
                        shape, Shapes.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
            }
        return shape;
    }
    /** Whether the underlying host already supplies a full face at the block boundary. */
    public static boolean isCoverSurface(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        var level = owner.getLevel();
        return level != null && net.minecraft.world.level.block.Block.isFaceFull(underlyingOutline(owner), side);
    }
    /** Cover storage changes do not replace the block state, so vanilla needs an explicit shape update.
     * Let neighboring blocks decide their own survival and drops; full-size hosts still support them.
     */
    public static void removed(net.minecraft.world.level.block.entity.BlockEntity owner) {
        var level = owner.getLevel();
        var pos = owner.getBlockPos();
        if (level == null || level.isClientSide || level.getBlockEntity(pos) != owner) return;
        var state = owner.getBlockState();
        state.updateNeighbourShapes(level, pos, net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
    }
    /** GT6 onAfterCrowbar: item break, except decorative panels with their own dig sound. */
    public static void removalSound(
            net.minecraft.world.level.block.entity.BlockEntity owner, net.minecraft.world.item.ItemStack stack) {
        var level = owner.getLevel();
        if (level == null || level.isClientSide || stack.isEmpty()) return;
        var sound = net.minecraft.sounds.SoundEvents.ITEM_BREAK;
        if (stack.getItem() instanceof gregtech.content.item.PanelItemView panel)
            sound = panel.panelSpec().kind().equals("wood")
                    ? net.minecraft.sounds.SoundEvents.WOOD_BREAK
                    : net.minecraft.sounds.SoundEvents.STONE_BREAK;
        level.playSound(
                null,
                owner.getBlockPos(),
                sound,
                net.minecraft.sounds.SoundSource.BLOCKS,
                1F,
                .9F + level.random.nextFloat() * .2F);
    }

    public static void walk(Level level, BlockPos pos, Entity entity) {
        if (!(level.getBlockEntity(pos) instanceof PanelCoverHost host)
                || !ComponentCoverFallback.ownClass(host.coverOwner())) return;
        var id = CoverItems.behavior(host.getCover(Direction.UP));
        if (CoverUtilityBehaviors.ASPHALT_PANEL.equals(id)) CoverUtilityBehaviors.walkOverAsphalt(entity);
        if (CoverAttachmentBehaviors.DRAIN.equals(id) && !host.panels().stopped())
            CoverAttachmentBehaviors.walkOverDrain(level, pos, host.componentFluids(Direction.UP), entity);
    }
    /** Original ICover.BOXES_COVERS occupies two pixels inside the block boundary; art stays flush. */
    public static VoxelShape collision(BlockGetter level, BlockPos pos, VoxelShape shape) {
        // Never dereference a null level: static initialisers in other mods query shapes without a world.
        if (level == null) return shape;
        if (!(level.getBlockEntity(pos) instanceof PanelCoverHost host)
                || !ComponentCoverFallback.ownClass(host.coverOwner())) return shape;
        for (var side : Direction.values()) {
            var stack = host.getCover(side);
            if (stack.isEmpty() && host.coverOwner() instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
                stack = logistics.logisticsCovers().get(side);
            if (stack.isEmpty()) continue;
            var id = CoverItems.behavior(stack);
            var box = CoverPlateGeometry.attachment(id, side.ordinal());
            shape = Shapes.or(
                    shape, Shapes.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }
        return shape;
    }
}
