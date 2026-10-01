package com.gregtech.gregtech.integration.jade;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
public enum ReactorJadeProvider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid(){return ResourceLocation.fromNamespaceAndPath("gregtech","reactor");}
    @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor){
        if(!(accessor.getBlockEntity() instanceof ReactorCoreBlockEntity c))return;
        tag.putString("gt_reactor_hot_side",c.outputSide().getSerializedName());tag.putString("gt_reactor_cold_side",c.coldOutputSide().getSerializedName());
        tag.putInt("gt_reactor_rods",c.rodCount());tag.putInt("gt_reactor_slots",c.rods.length);tag.putLong("gt_reactor_neutrons",c.neutronTotal());tag.putLong("gt_reactor_heat",c.lastHeat);tag.putLong("gt_reactor_stored",c.storedHeat);tag.putBoolean("gt_reactor_stopped",c.stopped);tag.putInt("gt_reactor_overflow",c.overflowCount());
        tag.put("gt_reactor_in",c.input.getFluid().writeToNBT(new CompoundTag()));tag.put("gt_reactor_out",c.output.getFluid().writeToNBT(new CompoundTag()));
    }
    @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
        var d=accessor.getServerData();if(!d.contains("gt_reactor_rods"))return;
        tooltip.add(Component.translatable("message.gregtech.reactor.detail",d.getInt("gt_reactor_rods"),d.getInt("gt_reactor_slots"),d.getLong("gt_reactor_neutrons"),d.getLong("gt_reactor_heat"),Component.translatable(d.getBoolean("gt_reactor_stopped")?"message.gregtech.reactor.stopped":"message.gregtech.reactor.running"),d.getInt("gt_reactor_overflow")));
        tooltip.add(Component.translatable("jade.gregtech.reactor.ports",d.getString("gt_reactor_hot_side"),d.getString("gt_reactor_cold_side")));
        tooltip.add(Component.translatable("jade.gregtech.reactor.heat",d.getLong("gt_reactor_stored")));
        for(String key:new String[]{"gt_reactor_in","gt_reactor_out"}){var fluid=net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(d.getCompound(key));if(!fluid.isEmpty())tooltip.add(Component.translatable("jade.gregtech.reactor.fluid",fluid.getDisplayName(),fluid.getAmount()));}
    }
}
