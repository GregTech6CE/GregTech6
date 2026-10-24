package gregtech.content.registry;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.internal.material.GTMaterial;
//? if >=1.21.1 {
/*import gregtech.internal.material.MaterialItemDefinitions;
*///?} else {
//?}
import gregtech.content.data.MaterialPrefix;
//? if >=1.21.1 {
/*import gregtech.content.item.MaterialItem;
import gregtech.platform.neoforge.GregTechNeoForge;
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
    private static final java.util.Map<String, DeferredRegister.Items> ADDON_ITEMS = new java.util.LinkedHashMap<>();
    public static DeferredRegister.Items items(String namespace) {
        if (GregTechNeoForge.NAMESPACE.equals(namespace)) return ITEMS;
        return ADDON_ITEMS.computeIfAbsent(namespace, DeferredRegister::createItems);
    }
    private static final Logger LOGGER = LogUtils.getLogger();
    public record CreativeEntry(GTMaterial material, DeferredHolder<Item, ? extends Item> item) {}
    private static final Map<String, NavigableMap<String, CreativeEntry>> BY_PREFIX = new HashMap<>();
    private static final List<DeferredItem<Item>> ALL = new ArrayList<>();
    private static boolean initialized;
*///?} else {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);
    private static final java.util.Map<String, DeferredRegister<Item>> ADDON_ITEMS = new java.util.LinkedHashMap<>();

    /** Built-in forms stay on {@link #ITEMS}. An addon prefix uses that addon's namespace. */
    public static DeferredRegister<Item> items(String namespace) {
        if (GregTech.NAMESPACE.equals(namespace)) return ITEMS;
        return ADDON_ITEMS.computeIfAbsent(namespace, id -> DeferredRegister.create(ForgeRegistries.ITEMS, id));
    }

    public static void registerAddonItems(net.minecraftforge.eventbus.api.IEventBus bus) {
        for (DeferredRegister<Item> register : ADDON_ITEMS.values()) register.register(bus);
    }

    public static java.util.Collection<RegistryObject<Item>> addonEntries() {
        var entries = new java.util.ArrayList<RegistryObject<Item>>();
        for (DeferredRegister<Item> register : ADDON_ITEMS.values()) entries.addAll(register.getEntries());
        return entries;
    }

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
            DeferredItem<Item> item = items(definition.namespace()).register(definition.itemId(), () ->
                    prefix==MaterialPrefix.coin?new gregtech.content.item.CoinItem(new Item.Properties().stacksTo(64),material):gregtech.content.recipe.MaterialArrowRules.isArrow(prefix, material)
                            ? new gregtech.content.item.MaterialArrowItem(new Item.Properties().stacksTo(64),prefix,material)
                            : gregtech.content.item.EnergyCrystalItem.createOrMaterial(prefix, material));
            bind(prefix, material, item);
            ALL.add(item);
        }
        ITEMS.register(modEventBus);
        for (DeferredRegister.Items register : ADDON_ITEMS.values()) register.register(modEventBus);
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
