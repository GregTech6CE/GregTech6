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


    private static final Map<String,DeferredHolder<CreativeModeTab,CreativeModeTab>> SOURCE_PAGES=new java.util.LinkedHashMap<>();
    private static DeferredHolder<CreativeModeTab,CreativeModeTab> registerFamily(String family) {
        if(!com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES.contains(family))return null;
        if(SOURCE_PAGES.isEmpty())for(String id:com.gregtech.gregtech.content.creative.CreativeTabCatalog.FAMILIES)
            SOURCE_PAGES.put(id,TABS.register(id,()->CreativeModeTab.builder()
                .title(Component.translatable(com.gregtech.gregtech.content.creative.CreativeTabCatalog.title(id)))
                .withSearchBar().icon(()->com.gregtech.gregtech.loaders.b.OriginCreativeContents.icon(id))
                .displayItems((params,output)->{}).build()));
        return SOURCE_PAGES.get(family);
    }
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

        TOOLS_TAB = registerFamily("tools");

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
