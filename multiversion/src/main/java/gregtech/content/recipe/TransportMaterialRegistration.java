package gregtech.content.recipe;

import gregtech.internal.machine.ItemPipeSpec;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.content.transport.*;
import gregtech.content.transport.fluid.FluidTransportDefinitions;
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
        String key=gregtech.internal.addon.AddonPipes.namespace(id)+":"+id;
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(key));
        if(item==Items.AIR)throw new IllegalStateException("Missing native transport material item "+key);
        ItemMaterialRegistry.register(item,data); RECOVERY_ITEMS.add(item);
        if(data.prefix()!=null) gregtech.content.registry.GTItems.bind(data.prefix(),data.material(),
//? if >=1.21.1 {
                /*net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse(key)));
*///?} else {
                net.minecraftforge.registries.RegistryObject.create(ResourceLocation.parse(key),net.minecraftforge.registries.ForgeRegistries.ITEMS));
//?}
    }
    public static int register(){
        for(var spec:FluidTransportDefinitions.pipes())bind(spec.id(),TransportMaterialRules.pipe(spec));
        for(var mat:ItemPipeCatalog.mats())for(var size:ItemPipeSpec.ItemPipeSize.values()){
            String id="item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+mat.idSuffix();
            bind(id,TransportMaterialRules.pipe(ItemPipeSpec.of(id,mat.material(),size,mat.stepSize(),mat.invSize(),true)));
        }
        for(var spec:FluidTransportDefinitions.tanks())TransportMaterialRules.tank(spec).ifPresent(data->bind(spec.id(),data));
        TransportMaterialRules.logisticsComponents().forEach((id, data) -> {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("gt6ce:" + id));
            if (item == Items.AIR) throw new IllegalStateException("Missing logistics component " + id);
            ItemMaterialRegistry.register(item, data);
        });
        var logistics = gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        bind(logistics.id(), TransportMaterialRules.tank(logistics).orElseThrow());
        return RECOVERY_ITEMS.size();
    }
}
