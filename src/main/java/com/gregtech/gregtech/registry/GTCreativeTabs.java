package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.item.FluidItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GTCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GregTech.NAMESPACE);

    private static final Map<MaterialPrefix, RegistryObject<CreativeModeTab>> BY_PREFIX = new HashMap<>();
    private static final Map<BlockMaterialPrefix, RegistryObject<CreativeModeTab>> BY_BLOCK_PREFIX = new HashMap<>();
    public static RegistryObject<CreativeModeTab> TOOLS_TAB;
    public static RegistryObject<CreativeModeTab> STONES_TAB;
    public static RegistryObject<CreativeModeTab> CRUCIBLES_TAB;
    public static RegistryObject<CreativeModeTab> BURNING_BOXES_TAB;
    public static RegistryObject<CreativeModeTab> BASIC_MACHINES_TAB;
    public static RegistryObject<CreativeModeTab> FLUIDS_TAB;
    public static RegistryObject<CreativeModeTab> PIPES_TAB;
    public static RegistryObject<CreativeModeTab> ITEM_PIPES_TAB;
    public static RegistryObject<CreativeModeTab> FLUID_CONTAINERS_TAB;
    public static RegistryObject<CreativeModeTab> HOPPERS_TAB;
    public static RegistryObject<CreativeModeTab> WIRES_TAB;
    public static RegistryObject<CreativeModeTab> ENGINES_TAB;
    public static RegistryObject<CreativeModeTab> AXLES_TAB;
    public static RegistryObject<CreativeModeTab> TECHNOLOGY_TAB;
    public static RegistryObject<CreativeModeTab> ENERGY_NODES_TAB;
    public static RegistryObject<CreativeModeTab> STORAGE_TAB;
    public static RegistryObject<CreativeModeTab> MULTIBLOCKS_TAB;
    public static RegistryObject<CreativeModeTab> ICONSETS_TAB;
    public static RegistryObject<CreativeModeTab> MULTI_ITEMS_TAB;
    private static boolean registered = false;


    private static final Map<String,RegistryObject<CreativeModeTab>> SOURCE_PAGES=new java.util.LinkedHashMap<>();
    private static RegistryObject<CreativeModeTab> registerFamily(String family) {
        if(!com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES.contains(family))return null;
        if(SOURCE_PAGES.isEmpty())for(String id:com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES)
            SOURCE_PAGES.put(id,TABS.register(id,()->CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title(id)))
                .withSearchBar().icon(()->com.gregtech.gregtech.loaders.b.OriginCreativeContents.icon(id))
                .displayItems((params,output)->{}).build()));
        return SOURCE_PAGES.get(family);
    }
private GTCreativeTabs() {}

    /** Call once before {@link #TABS#register(net.minecraftforge.eventbus.api.IEventBus)}. */
    public static void registerTabs() {
        if (registered) {
            return;
        }
        registered = true;
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (prefix.isHiddenFromCreative()) {
                continue;
            }
            if (!GTItems.hasBoundItems(prefix)) {
                continue;
            }
            MaterialPrefix bound = prefix;
            RegistryObject<CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GregTech.NAMESPACE + "." + bound.getRegistryName()))
                    .withSearchBar()
                    .icon(() -> iconFor(bound))
                    .displayItems((params, output) -> {
                        // Populated in CreativeTabHandler via BuildCreativeModeTabContentsEvent.
                    })
                    .build());
            BY_PREFIX.put(prefix, tab);
        }

        TOOLS_TAB = registerFamily("tools");

        BlockPrefixRegistry.ensurePrefixesLoaded();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (prefix.isPartialCrate()) continue;
            if (!GTBlocks.hasBoundBlocks(prefix)) {
                continue;
            }
            BlockMaterialPrefix bound = prefix;
            RegistryObject<CreativeModeTab> tab = TABS.register("block_" + bound.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GregTech.NAMESPACE + "." + bound.getRegistryName()))
                    .withSearchBar()
                    .icon(() -> blockIconFor(bound))
                    .displayItems((params, output) -> {})
                    .build());
            BY_BLOCK_PREFIX.put(prefix, tab);
        }

        STONES_TAB = registerFamily("stones");

        CRUCIBLES_TAB = registerFamily("smelting_crucibles");

        BURNING_BOXES_TAB = registerFamily("burning_boxes");

        BASIC_MACHINES_TAB = registerFamily("basic_machines");

        FLUIDS_TAB = registerFamily("fluids");

        PIPES_TAB = registerFamily("pipes");

        ITEM_PIPES_TAB = registerFamily("item_pipes");

        FLUID_CONTAINERS_TAB = registerFamily("fluid_containers");

        HOPPERS_TAB = registerFamily("hoppers");

        WIRES_TAB = registerFamily("wires");

        ENGINES_TAB = registerFamily("engines");

        AXLES_TAB = registerFamily("axles");

        TECHNOLOGY_TAB = registerFamily("technology");

        ICONSETS_TAB = registerFamily("iconsets");

        STORAGE_TAB = registerFamily("storage");
        ENERGY_NODES_TAB = registerFamily("energy_nodes");
        MULTIBLOCKS_TAB = registerFamily("multiblocks");
        MULTI_ITEMS_TAB = registerFamily("multi_items");
            for(String family:com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES)registerFamily(family);
}

    private static net.minecraft.world.item.ItemStack storageStack() {
        var ms = GTStorage.MASS_STORAGE;
        if (ms != null && ms.isPresent()) return new net.minecraft.world.item.ItemStack(ms.get());
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARREL);
    }

    private static net.minecraft.world.item.ItemStack energyNodeStack() {
        for (var entry : GTEnergyNodes.all()) {
            if (entry.isPresent()) return new net.minecraft.world.item.ItemStack(entry.get());
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE_BLOCK);
    }

    private static net.minecraft.world.item.ItemStack multiblockStack() {
        var main = GTMultiblocks.DISTILLATION_TOWER_MAIN;
        if (main != null && main.isPresent()) return new net.minecraft.world.item.ItemStack(main.get());
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FURNACE);
    }

    private static net.minecraft.world.item.ItemStack multiItemsStack() {
        for (var entry : GTMultiItems.all()) {
            if (entry.isPresent()) return new net.minecraft.world.item.ItemStack(entry.get());
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD);
    }

    public static Map<MaterialPrefix, RegistryObject<CreativeModeTab>> byPrefix() {
        return Collections.unmodifiableMap(BY_PREFIX);
    }

    public static RegistryObject<CreativeModeTab> tabFor(MaterialPrefix prefix) {
        return BY_PREFIX.get(prefix);
    }

    public static ResourceKey<CreativeModeTab> keyFor(MaterialPrefix prefix) {
        RegistryObject<CreativeModeTab> tab = tabFor(prefix);
        return tab == null ? null : tab.getKey();
    }

    public static ResourceKey<CreativeModeTab> keyFor(BlockMaterialPrefix prefix) {
        RegistryObject<CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
        return tab == null ? null : tab.getKey();
    }

    public static ResourceKey<CreativeModeTab> stonesTabKey() {
        return STONES_TAB == null ? null : STONES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> cruciblesTabKey() {
        return CRUCIBLES_TAB == null ? null : CRUCIBLES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> burningBoxesTabKey() {
        return BURNING_BOXES_TAB == null ? null : BURNING_BOXES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> basicMachinesTabKey() {
        return BASIC_MACHINES_TAB == null ? null : BASIC_MACHINES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> fluidsTabKey() {
        return FLUIDS_TAB == null ? null : FLUIDS_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> pipesTabKey() {
        return PIPES_TAB == null ? null : PIPES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> itemPipesTabKey() {
        return ITEM_PIPES_TAB == null ? null : ITEM_PIPES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> fluidContainersTabKey() {
        return FLUID_CONTAINERS_TAB == null ? null : FLUID_CONTAINERS_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> hoppersTabKey() {
        return HOPPERS_TAB == null ? null : HOPPERS_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> wiresTabKey() {
        return WIRES_TAB == null ? null : WIRES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> enginesTabKey() {
        return ENGINES_TAB == null ? null : ENGINES_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> technologyTabKey() {
        return TECHNOLOGY_TAB == null ? null : TECHNOLOGY_TAB.getKey();
    }

    public static ResourceKey<CreativeModeTab> iconSetsTabKey() {
        return ICONSETS_TAB == null ? null : ICONSETS_TAB.getKey();
    }

    private static ItemStack wiresStack() {
        // Icon: rubber-coated copper cable (1x)
        for (var entry : GTWires.allCables()) {
            if (entry.isPresent()) {
                ElectricWireBlock block = entry.get();
                if ("copper".equals(block.spec().id()) && block.spec().size() == 1) {
                    return new ItemStack(block);
                }
            }
        }
        // fallback: any cable
        for (var entry : GTWires.allCables()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(net.minecraft.world.item.Items.STICK);
    }

    private static ItemStack cruciblesStack() {
        if (GTMachines.SMELTING_CRUCIBLE_STEEL != null && GTMachines.SMELTING_CRUCIBLE_STEEL.isPresent()) {
            return new ItemStack(GTMachines.SMELTING_CRUCIBLE_STEEL.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
    }

    private static ItemStack burningBoxesStack() {
        if (GTMachines.BURNING_BOX_SOLID_STEEL != null && GTMachines.BURNING_BOX_SOLID_STEEL.isPresent()) {
            return new ItemStack(GTMachines.BURNING_BOX_SOLID_STEEL.get());
        }
        return new ItemStack(Items.FURNACE);
    }

    private static ItemStack basicMachinesStack() {
        List<RegistryObject<BasicMachineBlock>> machines = MachineRegistry.basicMachines();
        if (!machines.isEmpty()) {
            RegistryObject<BasicMachineBlock> first = machines.get(0);
            if (first.isPresent()) return new ItemStack(first.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
    }

    private static ItemStack fluidsStack() {
        FluidItem first = GTFluidItems.getFirst();
        if (first != null) {
            return new ItemStack(first);
        }
        return new ItemStack(Items.WATER_BUCKET);
    }

    private static ItemStack pipesStack() {
        if (GTFluidPipes.PIPE_MEDIUM_STEEL != null && GTFluidPipes.PIPE_MEDIUM_STEEL.isPresent()) {
            return new ItemStack(GTFluidPipes.PIPE_MEDIUM_STEEL.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
    }

    private static ItemStack itemPipesStack() {
        if (GTItemPipes.PIPE_MEDIUM_BRASS != null && GTItemPipes.PIPE_MEDIUM_BRASS.isPresent()) {
            return new ItemStack(GTItemPipes.PIPE_MEDIUM_BRASS.get());
        }
        return new ItemStack(Items.HOPPER);
    }

    private static ItemStack fluidContainersStack() {
        if (GTTanks.DRUM_STEEL != null && GTTanks.DRUM_STEEL.isPresent()) {
            return new ItemStack(GTTanks.DRUM_STEEL.get());
        }
        return new ItemStack(Items.BARREL);
    }

    private static ItemStack technologyStack() {
        for (RegistryObject<Item> entry : GTTechnological.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.REDSTONE);
    }

    private static ItemStack iconSetsStack() {
        for (RegistryObject<Block> entry : GTIconSetBlocks.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.BRICK);
    }

    private static ItemStack hoppersStack() {
        if (GTMachines.HOPPER_STEEL != null && GTMachines.HOPPER_STEEL.isPresent()) {
            return new ItemStack(GTMachines.HOPPER_STEEL.get());
        }
        return new ItemStack(Items.HOPPER);
    }

    private static ItemStack enginesStack() {
        List<RegistryObject<EngineBlock>> engines = MachineRegistry.allEngines();
        if (!engines.isEmpty()) {
            RegistryObject<EngineBlock> first = engines.get(0);
            if (first.isPresent()) return new ItemStack(first.get());
        }
        return new ItemStack(Items.FURNACE);
    }

    private static ItemStack axlesStack() {
        for (var entry : GTAxles.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.STICK);
    }

    private static ItemStack blockIconFor(BlockMaterialPrefix prefix) {
        if (prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall) {
            return GTBlocks.getStack(prefix, com.gregtech.gregtech.content.material.Materials.Zinc);
        }
        ItemStack icon = GTCreativeTabIcons.stackFor(prefix);
        return icon.isEmpty() ? new ItemStack(Items.IRON_BLOCK) : icon;
    }

    private static ItemStack iconFor(MaterialPrefix prefix) {
        ItemStack icon = GTCreativeTabIcons.stackFor(prefix);
        return icon.isEmpty() ? new ItemStack(Items.IRON_INGOT) : icon;
    }
}
