package gregtech.compat.jade;

import gregtech.content.block.inventory.DrawerQuadBlock;
import gregtech.content.blockentity.inventory.DrawerQuadBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
//?} else {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/** Uses Jade's native storage display, restricted to the compartment under the crosshair. */
public enum DrawerQuadJadeProvider implements
//? if >=1.21.1 {
        /*IServerExtensionProvider<ItemStack>,
*///?} else {
        IServerExtensionProvider<DrawerQuadBlockEntity, ItemStack>,
//?}
        IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "drawer_quad_storage");
    }

    @Override
    public int getDefaultPriority() {
        return -100;
    }

    @Override
//? if >=1.21.1 {
    /*public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)
                || !(block.getBlockEntity() instanceof DrawerQuadBlockEntity drawer)) return null;
        return groups(drawer, block.getHitResult());
*///?} else {
    public List<ViewGroup<ItemStack>> getGroups(
            ServerPlayer player, ServerLevel world, DrawerQuadBlockEntity drawer, boolean showDetails) {
        if (player == null || player.serverLevel() != world || drawer.getLevel() != world) return List.of();
        // Jade 11 does not expose the request's hit result to storage extensions.
        // Recast the authoritative player sight line using Jade's own viewing range.
        HitResult hit = player.pick(Math.sqrt(snownee.jade.Jade.MAX_DISTANCE_SQR), 1.0F, false);
        return hit instanceof BlockHitResult blockHit ? groups(drawer, blockHit) : List.of();
//?}
    }

    private static List<ViewGroup<ItemStack>> groups(DrawerQuadBlockEntity drawer, BlockHitResult hit) {
        var level = drawer.getLevel();
        BlockPos pos = drawer.getBlockPos();
        if (level == null || drawer.isRemoved() || level.getBlockEntity(pos) != drawer) return List.of();
        BlockState state = drawer.getBlockState();
        int page = selectedPage(state, pos, hit);
        // A non-null empty result stops Jade falling back to all 144 inventory slots.
        if (page < 0) return List.of();

        List<ItemStack> stacks = new ArrayList<>();
        int first = page * DrawerQuadBlockEntity.PAGE_SIZE;
        for (int slot = first; slot < first + DrawerQuadBlockEntity.PAGE_SIZE; slot++) {
            ItemStack stack = drawer.items().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ItemStack match = null;
            for (ItemStack existing : stacks) {
//? if >=1.21.1 {
                /*if (ItemStack.isSameItemSameComponents(existing, stack)) {
*///?} else {
                if (ItemStack.isSameItemSameTags(existing, stack)) {
//?}
                    match = existing;
                    break;
                }
            }
            if (match == null) stacks.add(stack.copy());
            else match.grow(stack.getCount());
        }

        ViewGroup<ItemStack> group = new ViewGroup<>(stacks);
        CompoundTag context = group.getExtraData();
        context.putLong("Position", pos.asLong());
        context.putString("Dimension", level.dimension().location().toString());
        context.putString("Block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        context.putInt("Facing", state.getValue(DrawerQuadBlock.FACING).ordinal());
        context.putInt("Page", page);
        return List.of(group);
    }

    private static int selectedPage(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (!(state.getBlock() instanceof DrawerQuadBlock) || hit == null
                || hit.getType() != HitResult.Type.BLOCK || !pos.equals(hit.getBlockPos())
                || hit.getDirection() != state.getValue(DrawerQuadBlock.FACING)) return -1;
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || x < -0.0001 || x > 1.0001 || y < -0.0001 || y > 1.0001 || z < -0.0001 || z > 1.0001) return -1;
        return DrawerQuadBlock.quadrant(state.getValue(DrawerQuadBlock.FACING), pos, hit.getLocation());
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        if (!(accessor instanceof BlockAccessor block) || groups == null) return List.of();
        BlockState state = block.getBlockState();
        BlockPos pos = block.getPosition();
        int page = selectedPage(state, pos, block.getHitResult());
        if (page < 0) return List.of();
        String dimension = block.getLevel().dimension().location().toString();
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int facing = state.getValue(DrawerQuadBlock.FACING).ordinal();
        List<ViewGroup<ItemStack>> current = new ArrayList<>();
        for (ViewGroup<ItemStack> group : groups) {
            CompoundTag context = group.getExtraData();
            if (context.contains("Position") && context.contains("Facing") && context.contains("Page")
                    && context.getLong("Position") == pos.asLong()
                    && context.getString("Dimension").equals(dimension)
                    && context.getString("Block").equals(blockId)
                    && context.getInt("Facing") == facing && context.getInt("Page") == page) current.add(group);
        }
        return ClientViewGroup.map(current, ItemView::new, null);
    }
}
