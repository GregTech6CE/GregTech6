package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import com.gregtech.gregtech.content.tool.OriginalToolShapes;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.EnumMap;

/** Shared outline, collision and wrench rotation for the restored utility meshes. */
public class ShapedToolBlock extends HorizontalDirectionalBlock implements ToolInteractionTarget {
    private final String toolId;
    private final EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    @Override public com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public ShapedToolBlock(String id, Properties properties) {
        super(id.equals("advanced_button") ? properties.noOcclusion().noCollission() : properties.noOcclusion());
        toolId = id;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        VoxelShape shape = Shapes.empty();
        for (int[] b : OriginalToolShapes.bounds(id))
            shape = Shapes.or(shape, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            shapes.put(direction, shape.optimize());
            VoxelShape rotated = Shapes.empty();
            for (var box : shape.toAabbs())
                rotated = Shapes.or(rotated, Shapes.box(1-box.maxZ, box.minY, box.minX, 1-box.minZ, box.maxY, box.maxX));
            shape = rotated;
        }
    }
    public String toolId() { return toolId; }
    public int tintRgb() {
        return switch (toolId) {
            case "bumbliary" -> com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood.getColor();
            case "mixing_bowl", "mixing_bowl_table", "juicer", "plant_pot" -> com.gregtech.gregtech.content.material.Materials.Ceramic.getColor();
            case "bathing_pot_wood", "bathing_pot_table_wood" -> com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood.getColor();
            case "sap_bag" -> com.gregtech.gregtech.content.material.Materials.Rubber.getColor();
            default -> com.gregtech.gregtech.content.material.Materials.Steel.getColor();
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (toolId.equals("advanced_button")) return Shapes.empty();
        if (toolId.equals("scaffold")) return Block.box(0,14,0,16,16,16);
        return getShape(state, level, pos, context);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool) ? ToolInteractionSpec.facing(FACING, MachineRotationType.HORIZONTAL) : null;
    }
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(state,level,pos,player,hand,hit)==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new ItemStack(this));}

}
