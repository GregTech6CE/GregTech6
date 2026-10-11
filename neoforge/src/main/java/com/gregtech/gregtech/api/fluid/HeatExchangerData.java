/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Source heat exchanger settings and native/legacy tank boundaries. */
package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.content.multiblock.HeatExchangerRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class HeatExchangerData {
    private HeatExchangerData() {}
    public static HeatExchangerRules.Settings settings(ItemStack stack) {
        var stored=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var data=stored==null?null:stored.copyTag();
        return settings(data);
    }
    public static HeatExchangerRules.Settings settings(CompoundTag tag) {
        var d=HeatExchangerRules.DEFAULTS;
        if(tag==null)return d;
        return new HeatExchangerRules.Settings(tag.contains("gt.output")?tag.getLong("gt.output"):d.rate(),
                tag.contains("gt.eff")?tag.getInt("gt.eff"):d.efficiency(),
                tag.contains("gt.fuelmap")?tag.getString("gt.fuelmap"):d.fuelMap(),
                tag.contains("gt.energy.emitted")?tag.getString("gt.energy.emitted"):d.energyId());
    }
    public static void save(CompoundTag tag,HeatExchangerRules.Settings settings) {
        tag.putLong("gt.output",settings.rate());tag.putShort("gt.eff",(short)settings.efficiency());
        tag.putString("gt.fuelmap",settings.fuelMap());tag.putString("gt.energy.emitted",settings.energyId());
    }
    public static void restore(FluidTankGT tank,CompoundTag data,long capacity,net.minecraft.core.HolderLookup.Provider lookup) {
        // Source setCapacity never discards an over-capacity saved amount.
        tank.setEmpty();tank.setCapacity(capacity);
        // An empty long-tank envelope only has Amount/Capacity, not a fluid codec payload.
        if(!data.contains("Fluid")&&!data.contains("FluidName")&&!data.contains("id"))return;
        var fluidTag=data.contains("Fluid")?data.getCompound("Fluid"):data;
        var fluid=FluidStackNbt.read(lookup,fluidTag);
        long amount=data.contains("Fluid")?data.getLong("Amount")
                :data.contains("LAmount")?data.getLong("LAmount"):fluid.getAmount();
        if(!fluid.isEmpty()&&amount>0)tank.setFluid(fluid,amount);
    }
}
