package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.blockentity.machine.FuelGeneratorBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FuelTurbineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Addon fuel generator shell. GregTech owns the block; the addon supplies the declaration. */
public class FuelGeneratorBlock extends GTFacingMachineBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private final AddonMachines.AcceptedFuel accepted;
    private Supplier<BlockEntityType<?>> beType = () -> null;

    public FuelGeneratorBlock(AddonMachines.AcceptedFuel accepted, Properties properties) {
        super(spec(accepted), properties);
        this.accepted = accepted;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
                .setValue(LIT, false).setValue(RUNNING, false));
    }

    private static MachineSpec spec(AddonMachines.AcceptedFuel accepted) {
        FuelGeneratorDeclaration declaration = accepted.declaration();
        var material = GTMaterialRegistry.get(declaration.material());
        return new MachineSpec(declaration.id(), material.getLocalName(), material.getColor(),
                0, 0, declaration.machineType(), 5.0F, 5.0F);
    }

    public AddonMachines.AcceptedFuel accepted() { return accepted; }
    public FuelGeneratorDeclaration declaration() { return accepted.declaration(); }

    public ResourceLocation modelId(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(accepted.addonId(),
                "block/machine/basic/" + accepted.declaration().machineType() + suffix);
    }

    public void setBeType(Supplier<BlockEntityType<?>> supplier) { this.beType = supplier; }

    public BlockEntity create(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return declaration().kind() == FuelGeneratorDeclaration.Kind.MULTI
                ? new FuelTurbineBlockEntity(type, pos, state)
                : new FuelGeneratorBlockEntity(type, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, RUNNING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return create(beType.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != beType.get()) return null;
        return (lvl, pos, blockState, be) -> FuelGeneratorBlockEntity.serverTick(lvl, pos, blockState, (FuelGeneratorBlockEntity) be);
    }
}
