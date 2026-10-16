package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.block.misc.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum ExtenderJadeProvider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid(){return ResourceLocation.fromNamespaceAndPath("gregtech","extender");}
    @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor){
        if(!(accessor.getBlockEntity() instanceof ExtenderBlockEntity relay)||!relay.universal())return;
        var block=(SourceExtenderBlock)relay.getBlockState().getBlock();
        var control=relay.machineControl(block.spec().bridge?accessor.getHitResult().getDirection():null);
        var data=new CompoundTag();data.putBoolean("available",control!=null&&control.available());
        if(control!=null&&control.available()){
            data.putBoolean("enabled",control.enabled());data.putBoolean("running",control.running());data.putBoolean("active",control.active());
            if(control.supportsMode())data.putInt("mode",control.mode());
            if(control.supportsProgress()){data.putLong("progress",control.progress());data.putLong("maximum",control.progressMax());}
        }
        tag.put("gt.extender.control",data);
    }
    @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
        if(!accessor.getServerData().contains("gt.extender.control"))return;
        var data=accessor.getServerData().getCompound("gt.extender.control");
        if(!data.getBoolean("available")){tooltip.add(Component.translatable("message.gregtech.extender.unavailable"));return;}
        tooltip.add(Component.translatable("message.gregtech.extender."+(data.getBoolean("enabled")?"enabled":"disabled")));
        tooltip.add(Component.translatable("jade.gregtech.extender."+(data.getBoolean("active")?"active":data.getBoolean("running")?"running":"idle")));
        if(data.contains("mode"))tooltip.add(Component.translatable("message.gregtech.extender.mode",data.getInt("mode")));
        if(data.contains("maximum")&&data.getLong("maximum")>0)tooltip.add(Component.translatable("jade.gregtech.extender.progress",data.getLong("progress"),data.getLong("maximum")));
    }
}
