package gregtech.engine.multiblock;

import gregtech.content.data.GregTechTags;
import gregtech.api.machine.MachineFace;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}
import java.util.*;

/** Controllers retain storage; formed parts expose only the role assigned by their definition. */
public interface MultiblockPortOwner extends StructureController {
    boolean isStructureOk();
//? if >=1.21.1 {
    /*default net.neoforged.neoforge.items.IItemHandler portItems(MultiblockLayout.Role role) { return null; }
*///?} else {
    default net.minecraftforge.items.IItemHandler portItems(MultiblockLayout.Role role) { return null; }
//?}
    IFluidHandler portFluids(MultiblockLayout.Role role);
    /** Port side is relative to the controller; it need not share the controller block's IO faces. */
    default IItemHandler portItems(MultiblockLayout.Role role, MachineFace face) { return portItems(role); }
    default IFluidHandler portFluids(MultiblockLayout.Role role, MachineFace face) { return portFluids(role); }
    /** One physical tank in the same index space as portFluids; null means it cannot be addressed separately. */
    default IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank) {
        var handler = portFluids(role);
        return tank == 0 && handler != null && handler.getTanks() == 1 ? handler : null;
    }
    default IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank, MachineFace face) {
        return portFluidTank(role, tank);
    }
    default Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) { return List.of(); }
    /** Separate from emitting so a bidirectional role can use the same energy type both ways. */
    default boolean acceptsPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return portEnergyTypes(role).contains(type) && !emitsPortEnergy(role, type);
    }
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
    default long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type, MachineFace face,
                                  long size, long amount, boolean execute) {
        return injectPortEnergy(role, type, size, amount, execute);
    }
}
