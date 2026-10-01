package com.gregtech.gregtech.registry;



import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;




import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GTCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gregtech");

    private static final Map<MaterialPrefix, DeferredHolder<CreativeModeTab,CreativeModeTab>> BY_PREFIX = new HashMap<>();
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
    private static boolean registered = false;

    private GTCreativeTabs() {}

    /** Call once before {@link #TABS#register(net.neoforged.neoforge.eventbus.api.IEventBus)}. */
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
            DeferredHolder<CreativeModeTab,CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + "gregtech" + "." + bound.getRegistryName()))
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
            DeferredHolder<CreativeModeTab,CreativeModeTab> tab = TABS.register("block_" + bound.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + "gregtech" + "." + bound.getRegistryName()))
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

    private static ItemStack storageStack(){return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("storage");}

    private static ItemStack energyNodeStack(){return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("energy_nodes");}

    private static ItemStack multiblockStack(){return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("multiblocks");}

    private static ItemStack multiItemsStack(){return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("multi_items");}

    public static Map<MaterialPrefix, DeferredHolder<CreativeModeTab,CreativeModeTab>> byPrefix() {
        return Collections.unmodifiableMap(BY_PREFIX);
    }

    public static DeferredHolder<CreativeModeTab,CreativeModeTab> tabFor(MaterialPrefix prefix) {
        return BY_PREFIX.get(prefix);
    }

    public static ResourceKey<CreativeModeTab> keyFor(MaterialPrefix prefix) {
        DeferredHolder<CreativeModeTab,CreativeModeTab> tab = tabFor(prefix);
        return tab == null ? null : tab.getKey();
    }

    public static ResourceKey<CreativeModeTab> keyFor(BlockMaterialPrefix prefix) {
        DeferredHolder<CreativeModeTab,CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
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
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("wires");
    }

    private static ItemStack cruciblesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("smelting_crucibles");
    }

    private static ItemStack burningBoxesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("burning_boxes");
    }

    private static ItemStack basicMachinesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("basic_machines");
    }

    private static ItemStack fluidsStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("fluids");
    }

    private static ItemStack pipesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("pipes");
    }

    private static ItemStack itemPipesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("item_pipes");
    }

    private static ItemStack fluidContainersStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("fluid_containers");
    }

    private static ItemStack technologyStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("technology");
    }

    private static ItemStack iconSetsStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("iconsets");
    }

    private static ItemStack hoppersStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("hoppers");
    }

    private static ItemStack enginesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("engines");
    }

    private static ItemStack axlesStack() {
        return com.gregtech.gregtech.loaders.b.NeoCreativeContents.icon("axles");
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
