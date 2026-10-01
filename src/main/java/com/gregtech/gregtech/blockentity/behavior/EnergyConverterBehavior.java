package com.gregtech.gregtech.blockentity.behavior;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.util.GTEnergySides;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Energy type conversion (GT6 {@code TE_Behavior_Energy_Converter}). */
public final class EnergyConverterBehavior {
    public final EnergyStatsBehavior energyIn;
    public final EnergyStatsBehavior energyOut;
    public final EnergyCapacitorBehavior storage;
    public boolean wasteEnergy;
    public boolean limitConsumption;
    public boolean overloaded;
    public boolean emitsEnergy;
    public boolean canEmitEnergy;
    public long multiplier = 1;

    public EnergyConverterBehavior(EnergyCapacitorBehavior storage,
                                   EnergyStatsBehavior energyIn,
                                   EnergyStatsBehavior energyOut,
                                   long multiplier,
                                   boolean wasteEnergy,
                                   boolean limitConsumption) {
        this.storage = storage;
        this.energyIn = energyIn;
        this.energyOut = energyOut;
        this.multiplier = Math.max(1, multiplier);
        this.wasteEnergy = wasteEnergy;
        this.limitConsumption = limitConsumption || GregTechTags.Energy.ALL_COMSUMPTION_LIMITED.contains(energyIn.type);
    }

    public boolean doConversion(BlockEntity emitter, Direction outputSide) {
        long output = scale(storage.energy, energyIn.recommended, energyOut.recommended);
        canEmitEnergy = output >= energyOut.min;
        emitsEnergy = false;
        if (!canEmitEnergy) {
            return false;
        }
        if (output > energyOut.max) {
            if (limitConsumption) {
                output = energyOut.max;
            } else {
                overloaded = true;
                storage.energy = 0;
                return false;
            }
        }
        long emitted;
        if (GregTechTags.Energy.isSizeIrrelevant(energyOut.type)) {
            emitted = outputSide == null
                    ? EnergyTransfer.emitEnergyToNetwork(energyOut.type, 1, output * multiplier, (IEnergyBlock) emitter)
                    : EnergyTransfer.emitEnergyToSide(energyOut.type, outputSide, 1, output * multiplier, emitter);
        } else {
            emitted = outputSide == null
                    ? EnergyTransfer.emitEnergyToNetwork(energyOut.type, output * multiplier, 1, (IEnergyBlock) emitter)
                    : EnergyTransfer.emitEnergyToSide(energyOut.type, outputSide, output * multiplier, 1, emitter);
        }
        if (emitted > 0) {
            if (!wasteEnergy) {
                storage.energy -= scale(emitted * output, energyOut.recommended * multiplier, energyIn.recommended, true);
            }
            emitsEnergy = true;
        }
        return canEmitEnergy;
    }

    private static long scale(long value, long from, long to) {
        if (from <= 0) {
            return 0;
        }
        return value * to / from;
    }

    private static long scale(long value, long from, long to, boolean ceilRemainder) {
        long result = scale(value, from, to);
        if (ceilRemainder && value * to % from != 0) {
            result++;
        }
        return result;
    }
}
