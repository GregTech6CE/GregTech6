package gregtech.content.block.machine;

import gregtech.engine.machine.TankSpec;
//? if >=1.21.1 {
//?} else {
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
//?}
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.client.TankTooltips;
//? if >=1.21.1 {
/*import gregtech.content.transport.fluid.FluidTransportRegistries;
import gregtech.platform.neoforge.NeoToolBindings;
import gregtech.platform.neoforge.transport.FluidPipeToolInteractions;

*///?} else {
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTSounds;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** GT6 fluid container block — wood barrel, plastic canister, or metal drum. */
public class TankBlock extends Block implements EntityBlock {
    private final TankSpec spec;

    public TankBlock(TankSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TankSpec spec() { return spec; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
//? if >=1.21.1 {
        /*if (type != FluidTransportRegistries.TANK.get()) return null;
*///?} else {
        if (type != GTBlockEntities.TANK.get()) return null;
//?}
        return (BlockEntityTicker<T>) (BlockEntityTicker<TankBlockEntity>) TankBlockEntity::serverTick;
    }

//? if >=1.21.1 {
    /*private InteractionResult useOriginal(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TankBlockEntity tank)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Wrench / Monkey Wrench: toggle automatic output
//? if >=1.21.1 {
        /*if (!player.isShiftKeyDown() && NeoToolBindings.isMachineWrench(held)) {
*///?} else {
        if (!player.isShiftKeyDown() && GTToolHelper.isMachineWrench(held)) {
//?}
            tank.toggleAutoOutput();
//? if >=1.21.1 {
            /*level.playSound(null, pos, FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            NeoToolBindings.damageForUse(held, 1, player);
*///?} else {
            level.playSound(null, pos, GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            GTToolHelper.damageForUse(held, 1, player);
//?}
            return InteractionResult.CONSUME;
        }

        // Soft Hammer: toggle states
//? if >=1.21.1 {
        /*if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"soft_hammer")) {
*///?} else {
        if (!player.isShiftKeyDown() && GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER)) {
//?}
            tank.toggleSoftHammerState();
//? if >=1.21.1 {
            /*level.playSound(null, pos, FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            NeoToolBindings.damageForUse(held, 1, player);
*///?} else {
            level.playSound(null, pos, GTSounds.WRENCH.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            GTToolHelper.damageForUse(held, 1, player);
//?}
            return InteractionResult.CONSUME;
        }

        // Magnifying Glass: show details
//? if >=1.21.1 {
        /*if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"magnifying_glass")) {
*///?} else {
        if (!player.isShiftKeyDown() && GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
//?}
            TankTooltips.sendTankInfo(tank, player);
            return InteractionResult.CONSUME;
        }

        return tank.handleUse(player, hand);
    }

    @Override
    public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new ItemStack(this);
        if (builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof TankBlockEntity tank) {
//? if >=1.21.1 {
            /*var data = tank.saveItemData(builder.getLevel().registryAccess());
            if (!data.isEmpty()) stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
            var data = tank.saveItemData();
            if (!data.isEmpty()) stack.addTagElement("BlockEntityTag", data);
//?}
        }
        return java.util.List.of(stack);
    }

    public static Properties defaultProperties(TankSpec spec) {
        return Properties.of()
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops();
    }

    public static Properties woodProperties(TankSpec spec) {
        return Properties.of()
                .strength(2.0F, 3.0F);
    }

    public static Properties plasticProperties(TankSpec spec) {
        return Properties.of()
                .strength(3.0F, 5.0F);
    }
//? if >=1.21.1 {

    /*@Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var result=useOriginal(state,level,pos,player,hand,hit);
        return result==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:
                result==InteractionResult.FAIL?net.minecraft.world.ItemInteractionResult.FAIL:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return useOriginal(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
}
