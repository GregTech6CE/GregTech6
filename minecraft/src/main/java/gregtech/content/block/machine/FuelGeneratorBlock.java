package gregtech.content.block.machine;

import gregtech.api.addon.machine.FuelGenerator;
import gregtech.content.blockentity.machine.FuelGeneratorBlockEntity;
import gregtech.content.blockentity.machine.FuelTurbineBlockEntity;
import gregtech.engine.addon.AddonMachines;
import gregtech.engine.machine.MachineSpec;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
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

/** Addon fuel generator shell. GregTech owns the block; the addon supplies the declaration. */
public class FuelGeneratorBlock extends GTFacingMachineBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private final AddonMachines.AcceptedFuel accepted;
    private Supplier<BlockEntityType<?>> beType = () -> null;

    public FuelGeneratorBlock(AddonMachines.AcceptedFuel accepted, Properties properties) {
        super(spec(accepted), properties);
        this.accepted = accepted;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(RUNNING, false));
    }

    private static MachineSpec spec(AddonMachines.AcceptedFuel accepted) {
        FuelGenerator declaration = accepted.declaration();
        var material = GTMaterialRegistry.get(declaration.casing().material());
        return new MachineSpec(
                declaration.casing().id(),
                material.getLocalName(),
                material.getColor(),
                0,
                0,
                declaration.casing().machineType(),
                5.0F,
                5.0F);
    }

    public AddonMachines.AcceptedFuel accepted() {
        return accepted;
    }

    public FuelGenerator declaration() {
        return accepted.declaration();
    }

    public ResourceLocation modelId(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(
                accepted.addonId(),
                "block/machine/basic/" + accepted.declaration().casing().machineType() + suffix);
    }

    public void setBeType(Supplier<BlockEntityType<?>> supplier) {
        this.beType = supplier;
    }

    public BlockEntity create(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return declaration().multi()
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != beType.get()) return null;
        return (lvl, pos, blockState, be) ->
                FuelGeneratorBlockEntity.serverTick(lvl, pos, blockState, (FuelGeneratorBlockEntity) be);
    }
}
