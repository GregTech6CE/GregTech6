package gregtech.content.registry;



//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
import gregtech.engine.machine.MachineRegistry;
//?}
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;




//? if >=1.21.1 {
//?} else {
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.data.RegisteredFluids;
//?}
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.prefix.PrefixRegistry;

//? if >=1.21.1 {
//?} else {
import gregtech.content.item.FluidItem;
//?}
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GTCreativeTabs {
//? if >=1.21.1 {
    /*public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6ce");
*///?} else {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GregTech.NAMESPACE);
//?}

//? if >=1.21.1 {
    /*private static final Map<MaterialPrefix, DeferredHolder<CreativeModeTab,CreativeModeTab>> BY_PREFIX = new HashMap<>();
    private static final Map<BlockMaterialPrefix, DeferredHolder<CreativeModeTab,CreativeModeTab>> BY_BLOCK_PREFIX = new HashMap<>();
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> TOOLS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> STONES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> CRUCIBLES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> BURNING_BOXES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> BASIC_MACHINES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> FLUIDS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> PIPES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> ITEM_PIPES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> FLUID_CONTAINERS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> HOPPERS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> WIRES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> ENGINES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> AXLES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> TECHNOLOGY_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> ENERGY_NODES_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> STORAGE_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> MULTIBLOCKS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> ICONSETS_TAB;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> MULTI_ITEMS_TAB;
*///?} else {
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
//?}
    private static boolean registered = false;


//? if >=1.21.1 {
    /*private static final Map<String,DeferredHolder<CreativeModeTab,CreativeModeTab>> SOURCE_PAGES=new java.util.LinkedHashMap<>();
    private static DeferredHolder<CreativeModeTab,CreativeModeTab> registerFamily(String family) {
*///?} else {
    private static final Map<String,RegistryObject<CreativeModeTab>> SOURCE_PAGES=new java.util.LinkedHashMap<>();
    private static RegistryObject<CreativeModeTab> registerFamily(String family) {
//?}
        if(!gregtech.content.creative.CreativeTabCatalog.FAMILIES.contains(family))return null;
        if(SOURCE_PAGES.isEmpty())for(String id:gregtech.content.creative.CreativeTabCatalog.FAMILIES)
            SOURCE_PAGES.put(id,TABS.register(id,()->CreativeModeTab.builder()
                .title(Component.translatable(gregtech.content.creative.CreativeTabCatalog.title(id)))
                .withSearchBar().icon(()->gregtech.bootstrap.registration.OriginCreativeContents.icon(id))
                .displayItems((params,output)->{}).build()));
        return SOURCE_PAGES.get(family);
    }
private GTCreativeTabs() {}

    /** Call once before {@link #TABS#register(net.minecraftforge.eventbus.api.IEventBus)}. */

/** Call once before {@link #TABS#register(net.neoforged.neoforge.eventbus.api.IEventBus)}. */
    public static void registerTabs() {
        if (registered) {
            return;
        }
        registered = true;
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (!GTItems.hasBoundItems(prefix)) {
                continue;
            }
            MaterialPrefix bound = prefix;
//? if >=1.21.1 {
            /*DeferredHolder<CreativeModeTab,CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(tabTitle(bound))
*///?} else {
            RegistryObject<CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(tabTitle(bound))
//?}
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
//? if >=1.21.1 {
            /*DeferredHolder<CreativeModeTab,CreativeModeTab> tab = TABS.register("block_" + bound.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(blockTabTitle(bound))
*///?} else {
            RegistryObject<CreativeModeTab> tab = TABS.register("block_" + bound.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(blockTabTitle(bound))
//?}
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
            for(String family:gregtech.content.creative.CreativeTabCatalog.FAMILIES)registerFamily(family);
}

//? if >=1.21.1 {
    /*private static ItemStack storageStack(){return gregtech.bootstrap.registration.NeoCreativeContents.icon("storage");}
*///?} else {
    private static net.minecraft.world.item.ItemStack storageStack() {
        var ms = GTStorage.MASS_STORAGE;
        if (ms != null && ms.isPresent()) return new net.minecraft.world.item.ItemStack(ms.get());
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARREL);
    }
//?}

//? if >=1.21.1 {
    /*private static ItemStack energyNodeStack(){return gregtech.bootstrap.registration.NeoCreativeContents.icon("energy_nodes");}
*///?} else {
    private static net.minecraft.world.item.ItemStack energyNodeStack() {
        for (var entry : GTEnergyNodes.all()) {
            if (entry.isPresent()) return new net.minecraft.world.item.ItemStack(entry.get());
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE_BLOCK);
    }
//?}

//? if >=1.21.1 {
    /*private static ItemStack multiblockStack(){return gregtech.bootstrap.registration.NeoCreativeContents.icon("multiblocks");}
*///?} else {
    private static net.minecraft.world.item.ItemStack multiblockStack() {
        var main = GTMultiblocks.DISTILLATION_TOWER_MAIN;
        if (main != null && main.isPresent()) return new net.minecraft.world.item.ItemStack(main.get());
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FURNACE);
    }
//?}

//? if >=1.21.1 {
    /*private static ItemStack multiItemsStack(){return gregtech.bootstrap.registration.NeoCreativeContents.icon("multi_items");}
*///?} else {
    private static net.minecraft.world.item.ItemStack multiItemsStack() {
        for (var entry : GTMultiItems.all()) {
            if (entry.isPresent()) return new net.minecraft.world.item.ItemStack(entry.get());
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD);
    }
//?}

//? if >=1.21.1 {
    /*public static Map<MaterialPrefix, DeferredHolder<CreativeModeTab,CreativeModeTab>> byPrefix() {
*///?} else {
    public static Map<MaterialPrefix, RegistryObject<CreativeModeTab>> byPrefix() {
//?}
        return Collections.unmodifiableMap(BY_PREFIX);
    }

//? if >=1.21.1 {
    /*public static DeferredHolder<CreativeModeTab,CreativeModeTab> tabFor(MaterialPrefix prefix) {
*///?} else {
    public static RegistryObject<CreativeModeTab> tabFor(MaterialPrefix prefix) {
//?}
        return BY_PREFIX.get(prefix);
    }

    public static ResourceKey<CreativeModeTab> keyFor(MaterialPrefix prefix) {
//? if >=1.21.1 {
        /*DeferredHolder<CreativeModeTab,CreativeModeTab> tab = tabFor(prefix);
*///?} else {
        RegistryObject<CreativeModeTab> tab = tabFor(prefix);
//?}
        return tab == null ? null : tab.getKey();
    }

    public static ResourceKey<CreativeModeTab> keyFor(BlockMaterialPrefix prefix) {
//? if >=1.21.1 {
        /*DeferredHolder<CreativeModeTab,CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
*///?} else {
        RegistryObject<CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
//?}
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
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("wires");
*///?} else {
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
//?}
    }

    private static ItemStack cruciblesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("smelting_crucibles");
*///?} else {
        if (GTMachines.SMELTING_CRUCIBLE_STEEL != null && GTMachines.SMELTING_CRUCIBLE_STEEL.isPresent()) {
            return new ItemStack(GTMachines.SMELTING_CRUCIBLE_STEEL.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
//?}
    }

    private static ItemStack burningBoxesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("burning_boxes");
*///?} else {
        if (GTMachines.BURNING_BOX_SOLID_STEEL != null && GTMachines.BURNING_BOX_SOLID_STEEL.isPresent()) {
            return new ItemStack(GTMachines.BURNING_BOX_SOLID_STEEL.get());
        }
        return new ItemStack(Items.FURNACE);
//?}
    }

    private static ItemStack basicMachinesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("basic_machines");
*///?} else {
        List<RegistryObject<BasicMachineBlock>> machines = MachineRegistry.basicMachines();
        if (!machines.isEmpty()) {
            RegistryObject<BasicMachineBlock> first = machines.get(0);
            if (first.isPresent()) return new ItemStack(first.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
//?}
    }

    private static ItemStack fluidsStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("fluids");
*///?} else {
        FluidItem first = GTFluidItems.getFirst();
        if (first != null) {
            return new ItemStack(first);
        }
        return new ItemStack(Items.WATER_BUCKET);
//?}
    }

    private static ItemStack pipesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("pipes");
*///?} else {
        if (GTFluidPipes.PIPE_MEDIUM_STEEL != null && GTFluidPipes.PIPE_MEDIUM_STEEL.isPresent()) {
            return new ItemStack(GTFluidPipes.PIPE_MEDIUM_STEEL.get());
        }
        return new ItemStack(Items.IRON_BLOCK);
//?}
    }

    private static ItemStack itemPipesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("item_pipes");
*///?} else {
        if (GTItemPipes.PIPE_MEDIUM_BRASS != null && GTItemPipes.PIPE_MEDIUM_BRASS.isPresent()) {
            return new ItemStack(GTItemPipes.PIPE_MEDIUM_BRASS.get());
        }
        return new ItemStack(Items.HOPPER);
//?}
    }

    private static ItemStack fluidContainersStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("fluid_containers");
*///?} else {
        if (GTTanks.DRUM_STEEL != null && GTTanks.DRUM_STEEL.isPresent()) {
            return new ItemStack(GTTanks.DRUM_STEEL.get());
        }
        return new ItemStack(Items.BARREL);
//?}
    }

    private static ItemStack technologyStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("technology");
*///?} else {
        for (RegistryObject<Item> entry : GTTechnological.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.REDSTONE);
//?}
    }

    private static ItemStack iconSetsStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("iconsets");
*///?} else {
        for (RegistryObject<Block> entry : GTIconSetBlocks.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.BRICK);
//?}
    }

    private static ItemStack hoppersStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("hoppers");
*///?} else {
        if (GTMachines.HOPPER_STEEL != null && GTMachines.HOPPER_STEEL.isPresent()) {
            return new ItemStack(GTMachines.HOPPER_STEEL.get());
        }
        return new ItemStack(Items.HOPPER);
//?}
    }

    private static ItemStack enginesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("engines");
*///?} else {
        List<RegistryObject<EngineBlock>> engines = MachineRegistry.allEngines();
        if (!engines.isEmpty()) {
            RegistryObject<EngineBlock> first = engines.get(0);
            if (first.isPresent()) return new ItemStack(first.get());
        }
        return new ItemStack(Items.FURNACE);
//?}
    }

    private static ItemStack axlesStack() {
//? if >=1.21.1 {
        /*return gregtech.bootstrap.registration.NeoCreativeContents.icon("axles");
*///?} else {
        for (var entry : GTAxles.all()) {
            if (entry.isPresent()) return new ItemStack(entry.get());
        }
        return new ItemStack(Items.STICK);
//?}
    }

    private static ItemStack blockIconFor(BlockMaterialPrefix prefix) {
        if (prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall) {
            return GTBlocks.getStack(prefix, gregtech.content.material.Materials.Zinc);
        }
        ItemStack icon = GTCreativeTabIcons.stackFor(prefix);
        return icon.isEmpty() ? new ItemStack(Items.IRON_BLOCK) : icon;
    }

    private static Component blockTabTitle(BlockMaterialPrefix prefix) {
        String key = "itemGroup.gt6ce." + prefix.getRegistryName();
        return prefix.isAddon()
                ? Component.translatableWithFallback(key, prefix.getDisplayName())
                : Component.translatable(key);
    }

    private static Component tabTitle(MaterialPrefix prefix) {
        String key = "itemGroup.gt6ce." + prefix.getRegistryName();
        return prefix.isAddon()
                ? Component.translatableWithFallback(key, prefix.getDisplayName())
                : Component.translatable(key);
    }

    private static ItemStack iconFor(MaterialPrefix prefix) {
        ItemStack icon = GTCreativeTabIcons.stackFor(prefix);
        return icon.isEmpty() ? new ItemStack(Items.IRON_INGOT) : icon;
    }
}
