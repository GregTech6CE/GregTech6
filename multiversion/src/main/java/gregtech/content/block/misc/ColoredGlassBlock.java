package gregtech.content.block.misc;

import gregtech.content.material.Materials;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.minecraft.world.level.block.TransparentBlock;
*///?} else {
import net.minecraft.world.level.block.GlassBlock;
//?}
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** GT6's {@code BlockGlassClear}/{@code BlockGlassGlow} coloured metadata family. */
//? if >=1.21.1 {
/*public class ColoredGlassBlock extends TransparentBlock {
    @Override protected com.mojang.serialization.MapCodec<? extends TransparentBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
*///?} else {
public class ColoredGlassBlock extends GlassBlock {
//?}
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private final boolean glow;

    public ColoredGlassBlock(boolean glow, Properties properties) {
        super(properties);
        this.glow = glow;
        registerDefaultState(stateDefinition.any().hasProperty(COLOR)
                ? stateDefinition.any().setValue(COLOR, DyeColor.LIGHT_GRAY) : stateDefinition.any());
    }

    public boolean glow() { return glow; }

    @Override public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction side) {
        if (adjacent.getBlock() instanceof ColoredGlassBlock glass && glass.glow() == glow())
            return FixedConstructionColor.colorOf(state) == FixedConstructionColor.colorOf(adjacent);
        return super.skipRendering(state, adjacent, side);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (!(this instanceof FixedConstructionColor)) builder.add(COLOR);
    }

    public static ItemStack coloredItem(Block block, DyeColor color) {
        Block variant = gregtech.content.registry.GTConstructionVariants.coloredBlock(block, color);
        if (variant != null) return new ItemStack(variant);
        ItemStack stack = new ItemStack(block);
//? if >=1.21.1 {
        /*stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("color",color.getName())));
*///?} else {
        CompoundTag stateTag = new CompoundTag();
        stateTag.putString("color", color.getName());
        stack.addTagElement("BlockStateTag", stateTag);
//?}
        return stack;
    }

    public static DyeColor itemColor(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof FixedConstructionColor fixed) return fixed.color();
//? if >=1.21.1 {
        /*var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        return data==null?DyeColor.LIGHT_GRAY:DyeColor.byName(data.properties().getOrDefault("color","light_gray"),DyeColor.LIGHT_GRAY);
*///?} else {
        CompoundTag tag = stack.getTagElement("BlockStateTag");
        return tag == null ? DyeColor.LIGHT_GRAY : DyeColor.byName(tag.getString("color"), DyeColor.LIGHT_GRAY);
//?}
    }

//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        return coloredItem(this, FixedConstructionColor.colorOf(state));
    }

    /** GT6: breaking either full glass block returns 80 glass scraps, not the pane itself. */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack full = GTItems.getStack(MaterialPrefix.scrapGt, Materials.Glass, 64);
        ItemStack rest = GTItems.getStack(MaterialPrefix.scrapGt, Materials.Glass, 16);
        if (full.isEmpty() || rest.isEmpty()) return List.of();
        return List.of(full, rest);
    }

    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }
}
