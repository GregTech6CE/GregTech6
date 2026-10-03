package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.platform.neoforge.GregTechNeoForge;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Actual Neo item holders over the single shared material definition stream. */
public final class GTItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GregTechNeoForge.NAMESPACE);
    private static final Logger LOGGER = LogUtils.getLogger();
    public record CreativeEntry(GTMaterial material, DeferredHolder<Item, ? extends Item> item) {}
    private static final Map<String, NavigableMap<String, CreativeEntry>> BY_PREFIX = new HashMap<>();
    private static final List<DeferredItem<Item>> ALL = new ArrayList<>();
    private static boolean initialized;
    private GTItems() {}

    /** Called after linked catalog/role setup and before the platform RegisterEvents. */
    public static int register(IEventBus modEventBus) {
        if (initialized) throw new IllegalStateException("Neo material items already queued");
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            MaterialPrefix prefix = definition.prefix();
            GTMaterial material = definition.material();
            if (definition.hasIdSuffix()) {
                LOGGER.warn("Duplicate item id '{}' for material {} (id={}); using '{}'",
                        definition.baseItemId(), material.getName(), material.getId(), definition.itemId());
            }
            DeferredItem<Item> item = ITEMS.register(definition.itemId(), () ->
                    prefix==MaterialPrefix.coin?new com.gregtech.gregtech.item.CoinItem(new Item.Properties().stacksTo(64),material):com.gregtech.gregtech.content.recipe.MaterialArrowRules.isArrow(prefix, material)
                            ? new com.gregtech.gregtech.item.MaterialArrowItem(new Item.Properties().stacksTo(64),prefix,material)
                            : new MaterialItem(new Item.Properties().stacksTo(64), prefix, material));
            bind(prefix, material, item);
            ALL.add(item);
        }
        ITEMS.register(modEventBus);
        initialized = true;
        return ALL.size();
    }

    public static void bind(MaterialPrefix prefix, GTMaterial material, DeferredHolder<Item, ? extends Item> item) {
        BY_PREFIX.computeIfAbsent(prefix.getName(), ignored -> new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())))
                .put(material.getName(), new CreativeEntry(material, item));
    }

    public static String key(MaterialPrefix prefix, GTMaterial material) {
        return prefix.getName() + "/" + material.getName();
    }

    public static DeferredHolder<Item, ? extends Item> getObject(MaterialPrefix prefix, GTMaterial material) {
        var entries = BY_PREFIX.get(prefix.getName());
        var entry = entries == null ? null : entries.get(material.getName());
        return entry == null ? null : entry.item();
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material, int count) {
        DeferredHolder<Item, ? extends Item> item = getObject(prefix, material);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get(), count);
    }

    public static ItemStack getCreativeStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static Collection<DeferredItem<Item>> allEntries() {
        return Collections.unmodifiableList(ALL);
    }

    public static boolean hasBoundItems(MaterialPrefix prefix) {
        var entries = BY_PREFIX.get(prefix.getName());
        return entries != null && !entries.isEmpty();
    }

    public static Collection<CreativeEntry> creativeEntries(MaterialPrefix prefix) {
        var entries = BY_PREFIX.get(prefix.getName());
        return entries == null ? List.of() : Collections.unmodifiableCollection(entries.values());
    }
}
