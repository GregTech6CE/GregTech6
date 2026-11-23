package gregtech.content.registry;

import gregtech.content.item.GTDungeonKeyItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * References to GT6's ten canonical dungeon key items ({@code MultiItemRandomTools}:
 * Iron, Gold, Copper, Tin, Bronze, Brass, Silver, Platinum, Lead and Plastic key, multi-item ids
 * 30000..30009).
 *
 * <p>The canonical ids are {@code <material>_key} in the order of GT6's {@code IL.KEYS}
 * ({@code IL}), which is alphabetical and is the order the dungeon's key id
 * table uses. The port has no {@code key} material prefix, so the keys are registered like every other
 * plain item of the mod instead of extending the material form table - GT6's keys are ore-dictionary
 * entries built from a plate, not a material form either.</p>
 *
 * <p>GTMultiItems owns the registrations; this class resolves them without registering aliases.
 * Each key stacks to 64, GT6's multi-item default
 * ({@code MultiItem.getItemStackLimit}).</p>
 */
public final class GTDungeonKeys {

    /** GT6's {@code IL.KEYS} ({@code IL}), in that exact order. */
    private static final java.util.List<String> MATERIALS = gregtech.content.storage.SafeLockRules.KEY_MATERIALS;

    private static final List<RegisteredItem<GTDungeonKeyItem>> ALL = new ArrayList<>();

    private GTDungeonKeys() {}

    /** Every key, in GT6's {@code IL.KEYS} order. */
    public static List<RegisteredItem<GTDungeonKeyItem>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** GT6's {@code IL.KEYS[aIndex]}, by that array's index. */
    public static GTDungeonKeyItem byIndex(int index) {
        if (index < 0 || index >= ALL.size()) return null;
        RegisteredItem<GTDungeonKeyItem> key = ALL.get(index);
        return key.isBound() ? key.get() : null;
    }

    /**
     * One of GT6's dungeon keys as a stack, carrying the held name and id of GT6's
     * {@code WorldgenDungeonGT}: {@code "Key #" + (aKeyIndex + 1)} and the {@code gt.key} long.
     */
    public static ItemStack stack(int keyType, long keyId, int keyIndex) {
        GTDungeonKeyItem item = byIndex(keyType);
        return item == null ? ItemStack.EMPTY : GTDungeonKeyItem.key(item, keyId, keyIndex);
    }

    public static void registerAll() {
        if (!ALL.isEmpty()) return;
        for (String material : MATERIALS) ALL.add(RegisteredItem.reference(material + "_key"));
    }
}
