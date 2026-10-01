package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries;
import com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import com.gregtech.gregtech.content.transport.HopperRegistries;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;
import java.util.*;

/** Source family pages over existing registrations, with component-aware per-page de-duplication. */
public final class NeoCreativeContents {
    private static Map<String,Set<ItemStack>> families;
    private NeoCreativeContents(){}
    private static void add(Map<String,Set<ItemStack>> result,String family,ItemLike item){add(result,family,new ItemStack(item));}
    private static void add(Map<String,Set<ItemStack>> result,String family,ItemStack stack){if(!stack.isEmpty())result.computeIfAbsent(family,k->ItemStackLinkedSet.createTypeAndComponentsSet()).add(stack.copyWithCount(1));}
    private static void holders(Map<String,Set<ItemStack>> result,String family,Iterable<? extends java.util.function.Supplier<? extends ItemLike>> entries){for(var entry:entries)add(result,family,entry.get());}
    private static synchronized Map<String,Set<ItemStack>> families(){
        if(families!=null)return families;Map<String,Set<ItemStack>> out=new LinkedHashMap<>();
        for(String family:com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES)out.put(family,ItemStackLinkedSet.createTypeAndComponentsSet());
        holders(out,"tools",GTToolBlocks.ITEMS.getEntries());holders(out,"tools",GTElectricItems.ITEMS.getEntries());
        holders(out,"tools",GTRadiationProtection.SUIT.values());holders(out,"tools",GTWoods.all());holders(out,"tools",GTLasers.all());
        holders(out,"tools",com.gregtech.gregtech.platform.neoforge.energy.LegacyBatteryRegistries.all());
        for(var entry:GTBlocks.BLOCK_ITEMS.getEntries()){
            Item item=entry.get();if(!(item instanceof BlockItem blockItem))continue;Block block=blockItem.getBlock();
            if(block instanceof MaterialBlockLike)continue;
            if(block instanceof GTStoneBlock||block instanceof GTStoneSlabBlock){add(out,"stones",item);continue;}
            if(block instanceof com.gregtech.gregtech.block.IconSetBlock||block instanceof com.gregtech.gregtech.block.IconSetPlantBlock||block instanceof com.gregtech.gregtech.block.IconSetLilyBlock){add(out,"iconsets",item);continue;}
            if(block instanceof com.gregtech.gregtech.block.misc.ConcreteBlock){for(var dye:DyeColor.values())add(out,"tools",com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(block,dye));}
            else if(block instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlock){for(var dye:DyeColor.values())add(out,"tools",com.gregtech.gregtech.block.misc.ColoredGlassBlock.coloredItem(block,dye));}
            else add(out,"tools",item);
        }
        holders(out,"smelting_crucibles",SmelteryRegistries.crucibles());holders(out,"smelting_crucibles",SmelteryRegistries.molds());holders(out,"smelting_crucibles",SmelteryRegistries.basins());holders(out,"smelting_crucibles",SmelteryRegistries.faucets());holders(out,"smelting_crucibles",SmelteryRegistries.crossings());
        holders(out,"engines",GTEngines.all());holders(out,"engines",GTPumps.all());holders(out,"tools",GTMagnets.all());add(out,"fluid_containers",GTLogisticsTank.BLOCK.get());holders(out,"engines",GTBoilers.all());
        holders(out,"burning_boxes",SmelteryRegistries.solidBoxes());holders(out,"burning_boxes",SmelteryRegistries.fuelBoxes());holders(out,"basic_machines",BasicMachineRegistries.all());
        FluidRegistries.ITEMS.getEntries().stream().filter(e->e.get() instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem f&&!f.fluidEntry().isHidden()).sorted(Comparator.comparing(e->e.getId().getPath())).forEach(e->add(out,"fluids",e.get()));
        holders(out,"pipes",FluidTransportRegistries.pipes());holders(out,"item_pipes",GTItemPipes.all());holders(out,"fluid_containers",FluidTransportRegistries.tanks());holders(out,"fluid_containers",GTToolBlocks.PORTABLE);
        var normal=HopperRegistries.hoppers();var queue=HopperRegistries.queues();for(int i=0;i<Math.max(normal.size(),queue.size());i++){if(i<normal.size())add(out,"hoppers",normal.get(i).get());if(i<queue.size())add(out,"hoppers",queue.get(i).get());}
        holders(out,"wires",GTSignalWires.all());holders(out,"wires",GTWires.all());holders(out,"axles",GTAxles.all());holders(out,"axles",GTGearboxes.allGearboxes());holders(out,"axles",GTGearboxes.allTransformers());
        holders(out,"technology",GTTechnological.all());add(out,"technology",GTStorageContainers.USB_SWITCH.get());add(out,"technology",GTStorageContainers.HDD_SWITCH.get());
        holders(out,"storage",GTMetalChests.all());holders(out,"storage",GTStorageContainers.DRAWERS);holders(out,"storage",com.gregtech.gregtech.platform.neoforge.logistics.StorageRegistries.all());holders(out,"storage",GTSensors.all());
        for(var item:BuiltInRegistries.ITEM){if(!(item instanceof BlockItem b))continue;String id=BuiltInRegistries.ITEM.getKey(item).getPath();if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gregtech"))continue;
            if(b.getBlock() instanceof com.gregtech.gregtech.block.inventory.SafeBlock||b.getBlock() instanceof com.gregtech.gregtech.block.inventory.LockerBlock||b.getBlock() instanceof com.gregtech.gregtech.block.inventory.EnderGarbageBlock||b.getBlock() instanceof com.gregtech.gregtech.block.inventory.EnderGarbageDumpBlock)add(out,"storage",item);
            // These source engine/boiler identities become visible as their remaining platform registrations arrive.
            if(id.startsWith("engine_")||id.startsWith("steam_engine_")||id.startsWith("gas_engine_")||id.startsWith("boiler_tank_"))add(out,"engines",item);
        }
        holders(out,"energy_nodes",GTEnergyNodes.all());add(out,"energy_nodes",GTEnergyNodes.REACTOR_CORE_BLOCK.get());add(out,"energy_nodes",GTEnergyNodes.REACTOR_CORE_2X2.get());add(out,"energy_nodes",GTEnergyNodes.REACTOR_CASING.get());
        for(var entry:GTFuelRods.ALL)if(entry.get().definition()!=null)add(out,"energy_nodes",entry.get());
        for(var entry:GTChemicalBatteries.all()){var empty=new ItemStack(entry.get());add(out,"energy_nodes",empty);var charged=empty.copy();var battery=(com.gregtech.gregtech.item.ChemicalBatteryItem)charged.getItem();battery.setCharge(charged,battery.spec().capacity());add(out,"energy_nodes",charged);}
        holders(out,"multiblocks",GTMultiblocks.texturedBlocks());holders(out,"multi_items",GTMultiItems.all());
        for(var type:com.gregtech.gregtech.api.tool.GTToolType.values()){var material=type==com.gregtech.gregtech.api.tool.GTToolType.GEM_PICK?com.gregtech.gregtech.content.material.Materials.Diamond:com.gregtech.gregtech.content.material.Materials.Iron;add(out,"tools",com.gregtech.gregtech.item.GTToolItem.create(type,material,type.requiresHeadAssembly()?com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood:material));}
        add(out,"tools",com.gregtech.gregtech.content.energy.ZpmEnergy.charged());
        // Source registries expose many extra items through overlapping pages. Keep every real
        // non-material item reachable, including portable blocks, keys and newly integrated tracks.
        Set<Item> visible=Collections.newSetFromMap(new IdentityHashMap<>());for(var page:out.values())for(var stack:page)visible.add(stack.getItem());
        for(var item:BuiltInRegistries.ITEM){if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gregtech")||visible.contains(item)||item instanceof MaterialItem||item instanceof com.gregtech.gregtech.item.CreativeTabIconItem||item instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem)continue;
            if(item instanceof BlockItem b&&(b.getBlock() instanceof MaterialBlockLike||b.getBlock() instanceof GTStoneBlock||b.getBlock() instanceof GTStoneSlabBlock))continue;
            add(out,"tools",item);
        }
        for(var entry:out.entrySet())if(entry.getValue().isEmpty())com.mojang.logging.LogUtils.getLogger().warn("[gregtech] Source creative family '{}' currently has no native registered contents",entry.getKey());
        families=Collections.unmodifiableMap(out);return families;
    }
    public static Collection<ItemStack> contents(String family){return families().getOrDefault(family,Set.of());}
    public static ItemStack icon(String family){var items=contents(family);return items.isEmpty()?new ItemStack(Items.IRON_BLOCK):items.iterator().next().copy();}
}
