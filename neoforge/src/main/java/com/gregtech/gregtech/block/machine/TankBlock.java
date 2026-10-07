package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.client.TankTooltips;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions;

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
        if (type != FluidTransportRegistries.TANK.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<TankBlockEntity>) TankBlockEntity::serverTick;
    }

    private InteractionResult useOriginal(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TankBlockEntity tank)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Wrench / Monkey Wrench: toggle automatic output
        if (!player.isShiftKeyDown() && NeoToolBindings.isMachineWrench(held)) {
            tank.toggleAutoOutput();
            level.playSound(null, pos, FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            NeoToolBindings.damageForUse(held, 1, player);
            return InteractionResult.CONSUME;
        }

        // Soft Hammer: toggle states
        if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"soft_hammer")) {
            tank.toggleSoftHammerState();
            level.playSound(null, pos, FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            NeoToolBindings.damageForUse(held, 1, player);
            return InteractionResult.CONSUME;
        }

        // Magnifying Glass: show details
        if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"magnifying_glass")) {
            TankTooltips.sendTankInfo(tank, player);
            return InteractionResult.CONSUME;
        }

        return tank.handleUse(player, hand);
    }

    @Override
    public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new ItemStack(this);
        if (builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof TankBlockEntity tank) {
            var data = tank.saveItemData(builder.getLevel().registryAccess());
            if (!data.isEmpty()) stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(data));
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

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var result=useOriginal(state,level,pos,player,hand,hit);
        return result==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:
                result==InteractionResult.FAIL?net.minecraft.world.ItemInteractionResult.FAIL:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return useOriginal(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
}
