package gregtech.content.registry;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CoinItem;
import gregtech.content.item.EnergyCrystalItem;
import gregtech.content.item.MaterialArrowItem;
import gregtech.content.recipe.MaterialArrowRules;
import gregtech.engine.material.MaterialItemDefinitions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.GregTechNeoForge;
import gregtech.platform.neoforge.registry.NeoContentRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import gregtech.GregTech;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** Material item handles over the single shared definition stream, bound by prefix and material. */
public final class GTItems {
//? if >=1.21.1 {
    /*public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GregTechNeoForge.NAMESPACE);
    private static final java.util.Map<String, DeferredRegister.Items> ADDON_ITEMS = new java.util.LinkedHashMap<>();
    public static DeferredRegister.Items items(String namespace) {
        if (GregTechNeoForge.NAMESPACE.equals(namespace)) return ITEMS;
        return ADDON_ITEMS.computeIfAbsent(namespace, DeferredRegister::createItems);
    }
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

    public static java.util.Collection<RegisteredItem<Item>> addonEntries() {
        var entries = new java.util.ArrayList<RegisteredItem<Item>>();
        for (DeferredRegister<Item> register : ADDON_ITEMS.values())
            register.getEntries().forEach(holder -> entries.add(new RegisteredItem<>(holder.getId(), holder)));
        return entries;
    }
//?}
    private static final Logger LOGGER = LogUtils.getLogger();
    public record CreativeEntry(GTMaterial material, RegisteredItem<Item> item) {}
    private static final Map<String, NavigableMap<String, CreativeEntry>> BY_PREFIX = new HashMap<>();
    private static final List<RegisteredItem<Item>> ALL = new ArrayList<>();

    private GTItems() {}

    /** Queues every material item through the platform registrar, in definition order. */
    public static int registerAll(ContentRegistrar registrar) {
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            MaterialPrefix prefix = definition.prefix();
            GTMaterial material = definition.material();
            if (definition.hasIdSuffix()) {
                LOGGER.warn(
                        "Duplicate item id '{}' for material {} (id={}); using '{}'",
                        definition.baseItemId(),
                        material.getName(),
                        material.getId(),
                        definition.itemId());
            }
            RegisteredItem<Item> registered = registrar.item(
                    definition.namespace(),
                    definition.itemId(),
                    () -> prefix == MaterialPrefix.coin
                            ? new CoinItem(new Item.Properties().stacksTo(64), material)
                            : MaterialArrowRules.isArrow(prefix, material)
                                    ? new MaterialArrowItem(new Item.Properties().stacksTo(64), prefix, material)
                                    : EnergyCrystalItem.createOrMaterial(prefix, material));
            bind(prefix, material, registered);
            ALL.add(registered);
        }
        return ALL.size();
    }

//? if >=1.21.1 {
    /*// Called after linked catalog/role setup and before the platform RegisterEvents.
    public static int register(IEventBus modEventBus) {
        if (initialized) throw new IllegalStateException("Neo material items already queued");
        int total = registerAll(new NeoContentRegistrar());
        ITEMS.register(modEventBus);
        initialized = true;
        return total;
    }

    // Addon namespaces keep being created during bootstrap, so they are submitted by the entry point last.
    public static void registerAddonItems(IEventBus modEventBus) {
        for (DeferredRegister.Items register : ADDON_ITEMS.values()) register.register(modEventBus);
    }
*///?}
    public static void bind(MaterialPrefix prefix, GTMaterial material, RegisteredItem<Item> item) {
        BY_PREFIX.computeIfAbsent(prefix.getName(), ignored -> new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())))
                .put(material.getName(), new CreativeEntry(material, item));
    }

    public static String key(MaterialPrefix prefix, GTMaterial material) {
        return prefix.getName() + "/" + material.getName();
    }

    public static RegisteredItem<Item> getObject(MaterialPrefix prefix, GTMaterial material) {
        var entries = BY_PREFIX.get(prefix.getName());
        var entry = entries == null ? null : entries.get(material.getName());
        return entry == null ? null : entry.item();
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material, int count) {
        RegisteredItem<Item> item = getObject(prefix, material);
        return item == null
                ? ItemStack.EMPTY
                : gregtech.engine.material.MaterialUnification.canonical(new ItemStack(item.get(), count));
    }

    /** Creative tabs require exactly one item per entry. */
    public static ItemStack getCreativeStack(MaterialPrefix prefix, GTMaterial material) {
        var item = getObject(prefix, material);
        // Visibility checks need the registered GT identity, not its preferred recipe replacement.
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    public static Collection<RegisteredItem<Item>> allEntries() {
        return Collections.unmodifiableList(ALL);
    }

    public static boolean hasBoundItems(MaterialPrefix prefix) {
        var entries = BY_PREFIX.get(prefix.getName());
        return entries != null && !entries.isEmpty();
    }

    /** Sorted registry bindings, not a second cache of tens of thousands of ItemStacks. */
    public static Collection<CreativeEntry> creativeEntries(MaterialPrefix prefix) {
        var entries = BY_PREFIX.get(prefix.getName());
        return entries == null ? List.of() : Collections.unmodifiableCollection(entries.values());
    }
}
