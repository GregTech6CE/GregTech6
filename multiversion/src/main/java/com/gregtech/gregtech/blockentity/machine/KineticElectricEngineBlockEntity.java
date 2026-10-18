package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.ElectricEngineSpec;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** EU → KU engine. */
public class KineticElectricEngineBlockEntity extends PistonEngineBlockEntity {
    ElectricEngineSpec spec;

    public KineticElectricEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSpec(ElectricEngineSpec spec) {
        this.spec = spec;
        this.faceConfig = defaultFaceConfig();
    }

    public ElectricEngineSpec spec() { return spec; }

    @Override protected long outputRate() { return spec != null ? spec.outputRate() : 0; }
    @Override protected long inputRate() { return spec != null ? spec.inputRate() : 0; }
    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.EU; }

    public static <T extends KineticElectricEngineBlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, T be) {
        if (be.spec == null) return;
        be.tickPiston();
    }
}
