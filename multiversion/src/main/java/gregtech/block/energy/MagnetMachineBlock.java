package gregtech.block.energy;

import gregtech.api.energy.EnergyNodeSpec;


//? if >=1.21.1 {
//?} else {
import gregtech.api.tool.GTToolHelper;
import gregtech.api.tool.ToolInteractionSpec;
//?}
import gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 bipolar magnet: four input faces, positive front and negative rear output. */
public final class MagnetMachineBlock extends EnergyNodeBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MagnetMachineBlock(EnergyNodeSpec spec, Properties properties) {
        super(spec, properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
//? if >=1.21.1 {
    /*protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        // Only an ordinary wrench rotates the source bipolar poles.
        return GTToolHelper.isMonkeyWrench(tool) ? null : super.toolInteraction(state, tool);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        ItemStack tool = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node) {
//? if >=1.21.1 {
            /*if (gregtech.platform.neoforge.NeoToolBindings.isSoftHammer(tool)) {
*///?} else {
            if (GTToolHelper.isSoftHammer(tool)) {
//?}
                if (!level.isClientSide) {
                    boolean on = node.toggleMagnetEnabled();
                    player.displayClientMessage(Component.translatable(on
                            ? "message.gt6ce.magnet.enabled" : "message.gt6ce.magnet.disabled"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
//? if >=1.21.1 {
        /*return super.interact(state, level, pos, player, hand, hit);
*///?} else {
        return super.use(state, level, pos, player, hand, hit);
//?}
    }
}
