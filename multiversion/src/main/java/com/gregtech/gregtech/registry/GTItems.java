package com.gregtech.gregtech.registry;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.api.material.GTMaterial;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
*///?} else {
//?}
import com.gregtech.gregtech.data.MaterialPrefix;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.platform.neoforge.GregTechNeoForge;
import com.mojang.logging.LogUtils;
*///?} else {
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import java.util.ArrayList;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

//?}
import java.util.Collection;
import java.util.Collections;
//? if >=1.21.1 {
/*import java.util.Comparator;
*///?} else {
//?}
import java.util.HashMap;
//? if >=1.21.1 {
/*import java.util.List;
*///?} else {
//?}
import java.util.Map;
//? if >=1.21.1 {
/*import java.util.NavigableMap;
import java.util.TreeMap;
*///?} else {
//?}


/** Actual Neo item holders over the single shared material definition stream. */
public final class GTItems {
//? if >=1.21.1 {
    /*public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GregTechNeoForge.NAMESPACE);
    private static final Logger LOGGER = LogUtils.getLogger();
    public record CreativeEntry(GTMaterial material, DeferredHolder<Item, ? extends Item> item) {}
    private static final Map<String, NavigableMap<String, CreativeEntry>> BY_PREFIX = new HashMap<>();
    private static final List<DeferredItem<Item>> ALL = new ArrayList<>();
    private static boolean initialized;
*///?} else {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    public record CreativeEntry(GTMaterial material, RegistryObject<Item> item) {}
    private static final Map<String, java.util.NavigableMap<String,CreativeEntry>> BY_PREFIX = new HashMap<>();

//?}
    private GTItems() {}

//? if >=1.21.1 {
    /*/^* Called after linked catalog/role setup and before the platform RegisterEvents. ^/
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
*///?} else {
    public static void bind(MaterialPrefix prefix, GTMaterial material, RegistryObject<Item> item) {
        BY_PREFIX.computeIfAbsent(prefix.getName(), ignored -> new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder())))
                .put(material.getName(),new CreativeEntry(material,item));
//?}
    }

    public static String key(MaterialPrefix prefix, GTMaterial material) {
        return prefix.getName() + "/" + material.getName();
    }

//? if >=1.21.1 {
    /*public static DeferredHolder<Item, ? extends Item> getObject(MaterialPrefix prefix, GTMaterial material) {
        var entries = BY_PREFIX.get(prefix.getName());
        var entry = entries == null ? null : entries.get(material.getName());
        return entry == null ? null : entry.item();
*///?} else {
    public static RegistryObject<Item> getObject(MaterialPrefix prefix, GTMaterial material) {
        var entries=BY_PREFIX.get(prefix.getName());
        var entry=entries==null?null:entries.get(material.getName());
        return entry==null?null:entry.item();
//?}
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material, int count) {
//? if >=1.21.1 {
        /*DeferredHolder<Item, ? extends Item> item = getObject(prefix, material);
*///?} else {
        RegistryObject<Item> item = getObject(prefix, material);
//?}
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get(), count);
    }

    /** Creative tabs require exactly one item per entry. */
    public static ItemStack getCreativeStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

//? if >=1.21.1 {
    /*public static Collection<DeferredItem<Item>> allEntries() {
        return Collections.unmodifiableList(ALL);
*///?} else {
    public static Collection<RegistryObject<Item>> allEntries() {
        return BY_PREFIX.values().stream().flatMap(entries -> entries.values().stream()).map(CreativeEntry::item).toList();
//?}
    }

    public static boolean hasBoundItems(MaterialPrefix prefix) {
        var entries=BY_PREFIX.get(prefix.getName());
        return entries!=null && !entries.isEmpty();
    }

    /** Sorted registry bindings, not a second cache of tens of thousands of ItemStacks. */
    public static Collection<CreativeEntry> creativeEntries(MaterialPrefix prefix) {
//? if >=1.21.1 {
        /*var entries = BY_PREFIX.get(prefix.getName());
        return entries == null ? List.of() : Collections.unmodifiableCollection(entries.values());
*///?} else {
        var entries=BY_PREFIX.get(prefix.getName());
        return entries==null?java.util.List.of():Collections.unmodifiableCollection(entries.values());
//?}
    }
}
