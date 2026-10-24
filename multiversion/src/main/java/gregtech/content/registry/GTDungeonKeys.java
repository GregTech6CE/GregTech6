package gregtech.content.registry;

import gregtech.content.item.GTDungeonKeyItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * References to GT6's ten canonical dungeon key items ({@code gregtech/items/MultiItemRandomTools.java:589-598}:
 * Iron, Gold, Copper, Tin, Bronze, Brass, Silver, Platinum, Lead and Plastic key, multi-item ids
 * 30000..30009).
 *
 * <p>The canonical ids are {@code <material>_key} in the order of GT6's {@code IL.KEYS}
 * ({@code gregapi/data/IL.java:516}), which is alphabetical and is the order the dungeon's key id
 * table uses. The port has no {@code key} material prefix, so the keys are registered like every other
 * plain item of the mod instead of extending the material form table - GT6's keys are ore-dictionary
 * entries built from a plate, not a material form either.</p>
 *
 * <p>GTMultiItems owns the registrations; this class resolves them without registering aliases.
 * Each key stacks to 64, GT6's multi-item default
 * ({@code MultiItem.getItemStackLimit:392-396}).</p>
 */
public final class GTDungeonKeys {

    /** GT6's {@code IL.KEYS} ({@code IL.java:516}), in that exact order. */
    private static final java.util.List<String> MATERIALS = gregtech.content.storage.SafeLockRules.KEY_MATERIALS;

//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Item,GTDungeonKeyItem>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<GTDungeonKeyItem>> ALL = new ArrayList<>();
//?}

    private GTDungeonKeys() {}

    /** Every key, in GT6's {@code IL.KEYS} order. */
//? if >=1.21.1 {
    /*public static List<DeferredHolder<Item,GTDungeonKeyItem>> all() {
*///?} else {
    public static List<RegistryObject<GTDungeonKeyItem>> all() {
//?}
        return Collections.unmodifiableList(ALL);
    }

    /** GT6's {@code IL.KEYS[aIndex]}, by that array's index. */
    public static GTDungeonKeyItem byIndex(int index) {
        if (index < 0 || index >= ALL.size()) return null;
        var original = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", MATERIALS.get(index) + "_key"));
        if (original instanceof GTDungeonKeyItem nativeKey) return nativeKey;
//? if >=1.21.1 {
        /*DeferredHolder<Item,GTDungeonKeyItem> key = ALL.get(index);
        return key.isBound() ? key.get() : null;
*///?} else {
        RegistryObject<GTDungeonKeyItem> key = ALL.get(index);
        return key.isPresent() ? key.get() : null;
//?}
    }

    /**
     * One of GT6's dungeon keys as a stack, carrying the held name and id of GT6's
     * {@code WorldgenDungeonGT:173}: {@code "Key #" + (aKeyIndex + 1)} and the {@code gt.key} long.
     */
    public static ItemStack stack(int keyType, long keyId, int keyIndex) {
        GTDungeonKeyItem item = byIndex(keyType);
        return item == null ? ItemStack.EMPTY : GTDungeonKeyItem.key(item, keyId, keyIndex);
    }

    public static void registerAll() {
        if (!ALL.isEmpty()) return;
        for (String material : MATERIALS) {
//? if >=1.21.1 {
            /*ALL.add(DeferredHolder.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", material + "_key")));
*///?} else {
            ALL.add(RegistryObject.create(new net.minecraft.resources.ResourceLocation("gt6ce", material + "_key"), net.minecraftforge.registries.ForgeRegistries.ITEMS));
//?}
        }
    }
}
