package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Bind the actual native block items before material recovery recipes are created. */
public final class TransportMaterialRegistration {
    private TransportMaterialRegistration() {}
    private static final Set<Item> RECOVERY_ITEMS=Collections.newSetFromMap(new IdentityHashMap<>());
    public static Set<Item> recoveryItems(){return Collections.unmodifiableSet(RECOVERY_ITEMS);}
    private static void bind(String id,ItemComposition data){
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+id));
        if(item==Items.AIR)throw new IllegalStateException("Missing native transport material item "+id);
        ItemMaterialRegistry.register(item,data); RECOVERY_ITEMS.add(item);
        if(data.prefix()!=null) com.gregtech.gregtech.registry.GTItems.bind(data.prefix(),data.material(),
                net.minecraftforge.registries.RegistryObject.create(ResourceLocation.parse("gregtech:"+id),net.minecraftforge.registries.ForgeRegistries.ITEMS));
    }
    public static int register(){
        for(var spec:FluidTransportDefinitions.pipes())bind(spec.id(),TransportMaterialRules.pipe(spec));
        for(var mat:ItemPipeCatalog.ITEM_PIPE_MATS)for(var size:ItemPipeSpec.ItemPipeSize.values()){
            String id="item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+mat.idSuffix();
            bind(id,TransportMaterialRules.pipe(ItemPipeSpec.of(id,mat.material(),size,mat.stepSize(),mat.invSize(),true)));
        }
        for(var spec:FluidTransportDefinitions.tanks())TransportMaterialRules.tank(spec).ifPresent(data->bind(spec.id(),data));
        TransportMaterialRules.logisticsComponents().forEach((id, data) -> {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:" + id));
            if (item == Items.AIR) throw new IllegalStateException("Missing logistics component " + id);
            ItemMaterialRegistry.register(item, data);
        });
        var logistics = com.gregtech.gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        bind(logistics.id(), TransportMaterialRules.tank(logistics).orElseThrow());
        return RECOVERY_ITEMS.size();
    }
}
