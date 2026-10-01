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

        TOOLS_TAB = TABS.register("tools", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("tools")))
                .withSearchBar()
                .icon(GTCreativeTabIcons::toolsStack)
                .displayItems((params, output) -> {
                    // Populated in CreativeTabHandler.
                })
                .build());

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

        STONES_TAB = TABS.register("stones", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("stones")))
                .withSearchBar()
                .icon(GTCreativeTabIcons::stonesStack)
                .displayItems((params, output) -> {})
                .build());

        CRUCIBLES_TAB = TABS.register("smelting_crucibles", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("smelting_crucibles")))
                .withSearchBar()
                .icon(GTCreativeTabs::cruciblesStack)
                .displayItems((params, output) -> {})
                .build());

        BURNING_BOXES_TAB = TABS.register("burning_boxes", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("burning_boxes")))
                .withSearchBar()
                .icon(GTCreativeTabs::burningBoxesStack)
                .displayItems((params, output) -> {})
                .build());

        BASIC_MACHINES_TAB = TABS.register("basic_machines", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("basic_machines")))
                .withSearchBar()
                .icon(GTCreativeTabs::basicMachinesStack)
                .displayItems((params, output) -> {})
                .build());

        FLUIDS_TAB = TABS.register("fluids", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("fluids")))
                .withSearchBar()
                .icon(GTCreativeTabs::fluidsStack)
                .displayItems((params, output) -> {})
                .build());

        PIPES_TAB = TABS.register("pipes", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("pipes")))
                .withSearchBar()
                .icon(GTCreativeTabs::pipesStack)
                .displayItems((params, output) -> {})
                .build());

        ITEM_PIPES_TAB = TABS.register("item_pipes", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("item_pipes")))
                .withSearchBar()
                .icon(GTCreativeTabs::itemPipesStack)
                .displayItems((params, output) -> {})
                .build());

        FLUID_CONTAINERS_TAB = TABS.register("fluid_containers", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("fluid_containers")))
                .withSearchBar()
                .icon(GTCreativeTabs::fluidContainersStack)
                .displayItems((params, output) -> {})
                .build());

        HOPPERS_TAB = TABS.register("hoppers", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("hoppers")))
                .withSearchBar()
                .icon(GTCreativeTabs::hoppersStack)
                .displayItems((params, output) -> {})
                .build());

        WIRES_TAB = TABS.register("wires", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("wires")))
                .withSearchBar()
                .icon(GTCreativeTabs::wiresStack)
                .displayItems((params, output) -> {})
                .build());

        ENGINES_TAB = TABS.register("engines", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("engines")))
                .withSearchBar()
                .icon(GTCreativeTabs::enginesStack)
                .displayItems((params, output) -> {})
                .build());

        AXLES_TAB = TABS.register("axles", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("axles")))
                .withSearchBar()
                .icon(GTCreativeTabs::axlesStack)
                .displayItems((params, output) -> {})
                .build());

        TECHNOLOGY_TAB = TABS.register("technology", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("technology")))
                .withSearchBar()
                .icon(GTCreativeTabs::technologyStack)
                .displayItems((params, output) -> {})
                .build());

        ICONSETS_TAB = TABS.register("iconsets", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("iconsets")))
                .withSearchBar()
                .icon(GTCreativeTabs::iconSetsStack)
                .displayItems((params, output) -> {})
                .build());

        STORAGE_TAB = TABS.register("storage", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("storage")))
                .icon(GTCreativeTabs::storageStack)
                .displayItems((params, output) -> {})
                .build());
        ENERGY_NODES_TAB = TABS.register("energy_nodes", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("energy_nodes")))
                .icon(GTCreativeTabs::energyNodeStack)
                .displayItems((params, output) -> {})
                .build());
        MULTIBLOCKS_TAB = TABS.register("multiblocks", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("multiblocks")))
                .icon(GTCreativeTabs::multiblockStack)
                .displayItems((params, output) -> {})
                .build());
        MULTI_ITEMS_TAB = TABS.register("multi_items", () -> CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title("multi_items")))
                .withSearchBar()
                .icon(GTCreativeTabs::multiItemsStack)
                .displayItems((params, output) -> {})
                .build());
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
