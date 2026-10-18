package com.gregtech.gregtech.blockentity.behavior;

import com.gregtech.gregtech.data.GregTechTags;

import java.util.List;

/** Input/output rate limits (GT6 {@code TE_Behavior_Energy_Stats}). */
public final class EnergyStatsBehavior {
    public final GregTechTags.Tag type;
    public long min;
    public long recommended;
    public long max;
    public boolean overloaded;

    private final EnergyCapacitorBehavior storage;

    public EnergyStatsBehavior(GregTechTags.Tag type, EnergyCapacitorBehavior storage,
                               long sizeMin, long sizeRec, long sizeMax) {
        this.type = type;
        this.storage = storage;
        this.min = Math.abs(sizeMin);
        this.recommended = Math.abs(sizeRec);
        this.max = Math.abs(sizeMax);
    }

    public boolean isType(GregTechTags.Tag energyType) {
        return type == energyType;
    }

    public long sizeMin(GregTechTags.Tag energyType) {
        return isType(energyType) ? min : 0;
    }

    public long sizeRec(GregTechTags.Tag energyType) {
        return isType(energyType) ? recommended : 0;
    }

    public long sizeMax(GregTechTags.Tag energyType) {
        return isType(energyType) ? max : 0;
    }

    public long doInject(long size, long amount, boolean doInject) {
        size = Math.abs(size);
        if (size > max) {
            if (doInject) {
                overloaded = true;
            }
            return amount;
        }
        if (storage == null || storage.isFull()) {
            return 0;
        }
        long room = storage.room();
        long packetEnergy = size * amount;
        long input = Math.min(room, packetEnergy);
        long consumed = Math.min(amount, input / size + (input % size != 0 ? 1 : 0));
        if (doInject) {
            storage.energy += consumed * size;
        }
        return consumed;
    }

    public void addTooltips(List<String> list, boolean emitting) {
        String role = emitting ? "Output" : "Input";
        list.add(role + ": " + recommended + " " + type.getShortName() + "/t (up to " + max + ")");
    }
}
