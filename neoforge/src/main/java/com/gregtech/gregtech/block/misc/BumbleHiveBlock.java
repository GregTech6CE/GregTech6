package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's wild bumblebee hive ({@code MultiTileEntityBumbleHive}): a painted block that holds the
 * colony the world generator put in it.
 *
 * <p>GT6 stores the hive's dye colour in its NBT ({@code NBT_COLOR}) and renders a coloured base
 * texture with an overlay on top; the port bakes the sixteen colour combinations into textures and
 * carries the colour as a block state property, so no custom colour handler is needed.</p>
 *
 * <p>Breaking rule ({@code MultiTileEntityBumbleHive:97-99}): only a player's break sets
 * {@code mDroppable}, and only then does the hive hand out its contents — a piston, an explosion or
 * {@code /setblock} destroys the colony with the block.</p>
 */
public class BumbleHiveBlock extends Block implements EntityBlock {

    /** GT6's {@code NBT_COLOR} as a block state, so one block covers all sixteen dye colours. */
    public static final EnumProperty<DyeColor> COLOR =
            EnumProperty.create("color", DyeColor.class);

    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public BumbleHiveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.LIGHT_GRAY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BumbleHiveBlockEntity(pos, state);
    }

    /** GT6's default properties: pumpkin hardness, 300 flammability, solid on every side. */
    public static Properties defaultProperties() {
        return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F, 1.0F)
                .sound(SoundType.WOOD).ignitedByLava();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BumbleHiveBlockEntity hive) {
            for (ItemStack stack : hive.contents()) popResource(level, pos, stack);
            hive.clearContents();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** GT6's thermometer tooltip ({@code addToolTips}), reusing the port's own lang key. */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.crucible.thermometer"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
