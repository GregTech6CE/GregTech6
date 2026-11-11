package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.bootstrap.registration.OriginCreativeContents;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, REGISTRY_NAMESPACE);

//? if >=1.21.1 {
    /*private static final Map<MaterialPrefix, DeferredHolder<CreativeModeTab, CreativeModeTab>> BY_PREFIX =
            new HashMap<>();
    private static final Map<BlockMaterialPrefix, DeferredHolder<CreativeModeTab, CreativeModeTab>> BY_BLOCK_PREFIX =
            new HashMap<>();
*///?} else {
    private static final Map<MaterialPrefix, RegistryObject<CreativeModeTab>> BY_PREFIX = new HashMap<>();
    private static final Map<BlockMaterialPrefix, RegistryObject<CreativeModeTab>> BY_BLOCK_PREFIX = new HashMap<>();
//?}
    private static boolean registered = false;

//? if >=1.21.1 {
    /*private static final Map<String, DeferredHolder<CreativeModeTab, CreativeModeTab>> SOURCE_PAGES =
            new LinkedHashMap<>();

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> registerFamily(String family) {
*///?} else {
    private static final Map<String, RegistryObject<CreativeModeTab>> SOURCE_PAGES = new LinkedHashMap<>();

    private static RegistryObject<CreativeModeTab> registerFamily(String family) {
//?}
        if (!gregtech.content.creative.CreativeTabCatalog.FAMILIES.contains(family)) return null;
        if (SOURCE_PAGES.isEmpty())
            for (String id : gregtech.content.creative.CreativeTabCatalog.FAMILIES)
                SOURCE_PAGES.put(
                        id,
                        TABS.register(
                                id,
                                () -> CreativeModeTab.builder()
                                        .title(Component.translatable(
                                                gregtech.content.creative.CreativeTabCatalog.title(id)))
                                        .withSearchBar()
                                        .icon(() -> OriginCreativeContents.icon(id))
                                        .displayItems((params, output) -> {})
                                        .build()));
        return SOURCE_PAGES.get(family);
    }

    private GTCreativeTabs() {}

    /** Call once before {@code TABS.register(IEventBus)}. */
    public static void registerTabs() {
        if (registered) return;
        registered = true;
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (!GTItems.hasBoundItems(prefix)) continue;
            MaterialPrefix bound = prefix;
//? if >=1.21.1 {
            /*DeferredHolder<CreativeModeTab, CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(tabTitle(bound))
*///?} else {
            RegistryObject<CreativeModeTab> tab = TABS.register(prefix.getRegistryName(), () -> CreativeModeTab.builder()
                    .title(tabTitle(bound))
//?}
                    .withSearchBar()
                    .icon(() -> iconFor(bound))
                    .displayItems((params, output) -> {})
                    .build());
            BY_PREFIX.put(prefix, tab);
        }

        // Register the family pages before the block-prefix tabs so the tab order matches the source catalog.
        registerFamily("tools");

        BlockPrefixRegistry.ensurePrefixesLoaded();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!GTBlocks.hasBoundBlocks(prefix)) continue;
            BlockMaterialPrefix bound = prefix;
//? if >=1.21.1 {
            /*DeferredHolder<CreativeModeTab, CreativeModeTab> tab = TABS.register("block_" + bound.getRegistryName(), () -> CreativeModeTab.builder()
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

        for (String family : gregtech.content.creative.CreativeTabCatalog.FAMILIES) registerFamily(family);
    }

//? if >=1.21.1 {
    /*private static DeferredHolder<CreativeModeTab, CreativeModeTab> tabFor(MaterialPrefix prefix) {
*///?} else {
    private static RegistryObject<CreativeModeTab> tabFor(MaterialPrefix prefix) {
//?}
        return BY_PREFIX.get(prefix);
    }

    public static ResourceKey<CreativeModeTab> keyFor(MaterialPrefix prefix) {
//? if >=1.21.1 {
        /*DeferredHolder<CreativeModeTab, CreativeModeTab> tab = tabFor(prefix);
*///?} else {
        RegistryObject<CreativeModeTab> tab = tabFor(prefix);
//?}
        return tab == null ? null : tab.getKey();
    }

    public static ResourceKey<CreativeModeTab> keyFor(BlockMaterialPrefix prefix) {
//? if >=1.21.1 {
        /*DeferredHolder<CreativeModeTab, CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
*///?} else {
        RegistryObject<CreativeModeTab> tab = BY_BLOCK_PREFIX.get(prefix);
//?}
        return tab == null ? null : tab.getKey();
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
