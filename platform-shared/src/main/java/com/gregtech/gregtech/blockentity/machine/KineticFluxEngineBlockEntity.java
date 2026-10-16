package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.FluxEngineSpec;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** RF → KU engine. RF is size-irrelevant, so packets always use size=1. */
public class KineticFluxEngineBlockEntity extends PistonEngineBlockEntity {
    FluxEngineSpec spec;

    public KineticFluxEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSpec(FluxEngineSpec spec) {
        this.spec = spec;
        this.faceConfig = defaultFaceConfig();
    }

    public FluxEngineSpec spec() { return spec; }

    @Override protected long outputRate() { return spec != null ? spec.outputRate() : 0; }
    /** Rated RF consumption; native RF packets may still have size one. */
    @Override protected long inputRate() { return spec != null ? spec.inputRate() : 0; }
    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.RF; }

    public static <T extends KineticFluxEngineBlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, T be) {
        if (be.spec == null) return;
        be.tickPiston();
    }
}
