package com.gregtech.gregtech.content.cover;
import net.minecraft.core.Direction;
import com.gregtech.gregtech.api.machine.MachineControl;
/** Native storage progress capabilities remain separate from machine on/off controls. */
public final class CoverProgress {
    private CoverProgress(){}
    public static MachineControl control(PanelCoverHost host,Direction side){
        var items=host.componentItems(side);var fluids=host.componentFluids(side);
        var energy=host.coverOwner() instanceof com.gregtech.gregtech.api.energy.IEnergyBlock block?block:null;
        var types=energy==null?java.util.List.<com.gregtech.gregtech.data.GregTechTags.Tag>of():energy.getEnergyCapacitorTypes(side);
        if(items==null&&fluids==null&&types.isEmpty())return null;
        return new MachineControl(){
            public boolean available(){return !host.coverOwner().isRemoved();}
            public boolean supportsSwitch(){return false;}
            public boolean enabled(){return available();}
            public boolean setEnabled(boolean value){return enabled();}
            public boolean running(){return false;}
            public boolean active(){return false;}
            public long progress(){
                if(!types.isEmpty())return energy.getEnergyStored(types.iterator().next(),side);
                long amount=0;
                if(fluids!=null)for(int i=0;i<fluids.getTanks();i++)amount+=fluids.getFluidInTank(i).getAmount();
                else if(items!=null)for(int i=0;i<items.getSlots();i++)amount+=items.getStackInSlot(i).getCount();
                return amount;
            }
            public long progressMax(){
                if(!types.isEmpty())return energy.getEnergyCapacity(types.iterator().next(),side);
                long capacity=0;
                if(fluids!=null)for(int i=0;i<fluids.getTanks();i++)capacity+=fluids.getTankCapacity(i);
                else if(items!=null)for(int i=0;i<items.getSlots();i++)capacity+=items.getSlotLimit(i);
                return capacity;
            }
        };
    }
}
