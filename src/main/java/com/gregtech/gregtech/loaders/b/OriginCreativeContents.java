package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.content.creative.*;
import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import java.util.*;

/** Loader boundary: source categories/order with native stacks, colors and charged variants. */
public final class OriginCreativeContents {
    private OriginCreativeContents() {}
    private static Map<String,List<ItemStack>> pages;
    public static String family(Item item) {
        String id=BuiltInRegistries.ITEM.getKey(item).getPath();
        if(item instanceof CanvasItem)return "equipment";
        if(item instanceof ColoredBookItem || id.equals("dusty_guide_book") || id.equals("dusty_material_dictionary"))return "books";
        if(item instanceof GTToolItem) return "tools";
        if(item instanceof ElectricToolItem) return "tools";
        if(item instanceof com.gregtech.gregtech.item.PanelItemView) return "panels";
        if(item instanceof ChemicalBatteryItem) return "batteries";
        if(item instanceof com.gregtech.gregtech.item.FluidItem) return "fluids";
        if(item instanceof BlockItem b) {
            var block=b.getBlock(); String type=block.getClass().getSimpleName();
            if(type.equals("GTStoneBlock")||type.equals("GTStoneSlabBlock")) return "stones";
            if(type.startsWith("IconSet"))return "iconsets";
            if(type.equals("ConcreteBlock")||type.equals("ColoredConstructionBlock")||type.equals("ColoredGlassBlock")||id.startsWith("glass_")||id.startsWith("cfoam_")||id.equals("asphalt")) return "construction";
            if(id.startsWith("safe_")||id.startsWith("key_safe_")||type.equals("SafeBlock"))return "safes";
            if(id.startsWith("advanced_crafting_table")) return "crafting_tables";
            if(id.contains("chest")) return "chests";
            if(id.startsWith("drawer_")||id.startsWith("mass_storage_")||id.startsWith("locker_"))return "storage";
            if(id.startsWith("logistics")||id.contains("storage_logistics")) return "logistics";
            if(id.startsWith("scaffold"))return "scaffolds";
            if(id.startsWith("sensor_"))return "sensors";
            if(id.startsWith("panel_"))return "panels";
            if(id.startsWith("rope"))return "ropes";
            if(id.startsWith("reactor_")||id.startsWith("fuel_rod_")||id.startsWith("reactor_rod_"))return "reactors";
            if(id.startsWith("long_dist_"))return "long_distance_transport";
            if(type.equals("ZPMBlock"))return "zpm";
            if(id.startsWith("battery_box")||id.startsWith("energy_storage_"))return "battery_boxes";
            if(id.startsWith("solar_panel"))return "solar_panels";
            if(id.startsWith("electric_motor")||id.startsWith("flux_motor"))return "motors";
            if(id.startsWith("electric_generator")||id.startsWith("electric_dynamo")||id.startsWith("flux_dynamo")||id.startsWith("dynamo"))return "dynamos";
            if(id.startsWith("electric_heater"))return "heaters";
            if(id.startsWith("electric_cooler"))return "coolers";
            if(id.contains("turbine") && !id.startsWith("large_"))return "turbines";
            if(id.startsWith("boiler_tank"))return "steam_boilers";
            if(id.startsWith("engine_")||id.startsWith("steam_engine_")||id.startsWith("gas_engine_"))return "engines";
            if(id.startsWith("pump_")||id.startsWith("rotational_pump"))return "pumps";
            if(id.startsWith("magnet_"))return "magnets";
            if(id.startsWith("transformer_"))return "transformers";
            if(id.startsWith("axle_")||id.startsWith("gearbox_"))return "axles";
            if(id.startsWith("pipe_")||id.startsWith("fluid_pipe_"))return "pipes";
            if(id.startsWith("item_pipe"))return "item_pipes";
            if(id.startsWith("hopper_")||id.startsWith("queue_hopper_"))return "hoppers";
            if(id.startsWith("basic_machine")||type.equals("BasicMachineBlock"))return "basic_machines";
            if(id.startsWith("usb_")||id.startsWith("hdd_"))return "computing";
        }
        var source=SourceCreativeCatalog.entry(id);
        if(source!=null && CreativeTabCatalog.FAMILIES.contains(source.family()))return source.family();
        if(item instanceof BlockItem b) {
            String idClass=b.getBlock().getClass().getSimpleName();
            if(id.startsWith("burning_box")||idClass.contains("BurningBox"))return "burning_boxes";
            if(id.contains("crucible")&&!id.contains("large"))return "smelting_crucibles";
            if(id.startsWith("mold")||id.contains("casting_basin"))return "molds";
            if(id.contains("faucet")||id.contains("crossing"))return "crucibles_faucets";
            if(id.startsWith("large_")||idClass.contains("Controller")||idClass.equals("MultiblockPortBlock"))return "multiblocks";
            if(idClass.contains("Wood")||idClass.contains("Log")||idClass.contains("Leaves")||id.contains("planks"))return "woods";
            if(idClass.contains("Wire"))return id.contains("redstone")?"redstone_wires":"wires";
            if(idClass.contains("Tank")||idClass.contains("Drum")||idClass.contains("PortableFluid"))return "fluid_containers";
            if(idClass.contains("Tool")||idClass.contains("Crank")||idClass.contains("Processing")||idClass.contains("FluidAttachment"))return "tool_blocks";
            return "untyped";
        }
        return "technology";
    }
    public static synchronized Map<String,List<ItemStack>> pages() {
        if(pages!=null)return pages;
        var result=new LinkedHashMap<String,List<ItemStack>>();
        for(String family:CreativeTabCatalog.FAMILIES)result.put(family,new ArrayList<>());
        var items=new ArrayList<Item>();
        for(var item:BuiltInRegistries.ITEM) {
            if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gregtech")||item instanceof MaterialFormItem||item instanceof CreativeTabIconItem)continue;
            if(item instanceof BlockItem b && b.getBlock() instanceof MaterialBlockLike)continue;
            if(java.util.Set.of("fluid_spring","bumble_hive","tap","fluid_funnel","cap_nozzle","nozzle","cure_all").contains(BuiltInRegistries.ITEM.getKey(item).getPath()))continue;
            if(item instanceof com.gregtech.gregtech.item.FluidItem fluid && fluid.fluidEntry().isHidden())continue;
            if (item instanceof CannedFoodItem can && can.spec().hidden()) continue;
            if (item instanceof ColoredBookItem book && book.variant().hidden()) continue;
            items.add(item);
        }
        // Source registration order within a category; newly ported entries follow by stable ID.
        items.sort(Comparator.comparingInt((Item item)->{
            // Original canvas loop immediately precedes Magic Research Paper (MultiItemRandomTools:443-452).
            if(item instanceof CanvasItem)return SourceCreativeCatalog.entry("magic_research_paper_introduction").order();
            if(item instanceof ColoredBookItem book)return book.variant().originalId();
            if(item instanceof com.gregtech.gregtech.item.PanelItemView panel)return panel.panelSpec().order();
            if(item instanceof GTToolItem tool)return tool.toolType().gt6Id();
            var source=SourceCreativeCatalog.entry(BuiltInRegistries.ITEM.getKey(item).getPath());
            return source==null?Integer.MAX_VALUE:source.order();
        }).thenComparingInt(item->item instanceof CanvasItem canvas?canvas.variant().originalId():Integer.MAX_VALUE).thenComparing(item->BuiltInRegistries.ITEM.getKey(item).getPath()));
        for(var item:items) {
            if(item instanceof com.gregtech.gregtech.item.PanelItemView panel&&!panel.panelSpec().canonical())continue;
            var page=result.get(family(item));
            if(item instanceof GTToolItem tool) {
                var type=tool.toolType(); var head=type==com.gregtech.gregtech.api.tool.GTToolType.GEM_PICK?com.gregtech.gregtech.content.material.Materials.Diamond:com.gregtech.gregtech.content.material.Materials.Steel;
                page.add(GTToolItem.create(type,head,type.requiresHeadAssembly()?com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood:head));
            } else if(item instanceof ElectricToolItem electric) {
                page.add(electric.assembled(com.gregtech.gregtech.content.material.Materials.Steel,electric.getEnergyCapacity(new ItemStack(item),com.gregtech.gregtech.data.GregTechTags.Energy.EU)));
            } else if(item instanceof ChemicalBatteryItem battery) {
                page.add(new ItemStack(item));var charged=new ItemStack(item);battery.setCharge(charged,battery.spec().capacity());page.add(charged);
            } else if(item instanceof BlockItem b && (b.getBlock() instanceof com.gregtech.gregtech.block.misc.ConcreteBlock||b.getBlock() instanceof com.gregtech.gregtech.block.misc.ColoredConstructionBlock)) {
                for(int meta=0;meta<16;meta++)page.add(com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(b.getBlock(),DyeColor.byId(15-meta)));
            } else if(item instanceof BlockItem b && b.getBlock() instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlock) {
                for(int meta=0;meta<16;meta++)page.add(com.gregtech.gregtech.block.misc.ColoredGlassBlock.coloredItem(b.getBlock(),DyeColor.byId(15-meta)));
            } else if(BuiltInRegistries.ITEM.getKey(item).getPath().equals("zpm_module"))page.add(com.gregtech.gregtech.content.energy.ZpmEnergy.charged());
            else page.add(new ItemStack(item));
        }
        result.replaceAll((family,stacks)->List.copyOf(stacks));pages=Collections.unmodifiableMap(result);return pages;
    }
    public static Collection<ItemStack> contents(String family){return pages().getOrDefault(family,List.of());}
    public static ItemStack icon(String family){var stacks=contents(family);return stacks.isEmpty()?new ItemStack(Items.IRON_BLOCK):stacks.iterator().next().copy();}
}
