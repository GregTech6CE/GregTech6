package com.gregtech.gregtech.api.multiblock;

import com.gregtech.gregtech.data.GregTechTags;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Controllers retain storage; formed parts expose only the role assigned by their definition. */
public interface MultiblockPortOwner extends StructureController {
    boolean isStructureOk();
    default net.minecraftforge.items.IItemHandler portItems(MultiblockLayout.Role role) { return null; }
    IFluidHandler portFluids(MultiblockLayout.Role role);
    default Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) { return List.of(); }
    default boolean emitsPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type) { return false; }
    default long portEnergyOutputSize(MultiblockLayout.Role role,GregTechTags.Tag type) { return 0; }
    default long portEnergyInputRecommended(MultiblockLayout.Role role,GregTechTags.Tag type) { return 512; }
    default long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type) { return Math.max(1,portEnergyInputRecommended(role,type)/2); }
    default long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type) { return portEnergyInputRecommended(role,type)*2; }
    default long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) { return 0; }
    default long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) { return 0; }
    default long portEnergyDemanded(MultiblockLayout.Role role,GregTechTags.Tag type,long size) {
        return size>0?Math.max(0,portEnergyCapacity(role,type)-portEnergyStored(role,type))/size:0;
    }
    default long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type, long size, long amount, boolean execute) { return 0; }
}
