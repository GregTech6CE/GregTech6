package com.gregtech.gregtech.content.bumble;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's bumblebee <em>types</em> — the {@code meta % 10} of {@code ItemsGT.BUMBLEBEES}
 * ({@code MultiItemBumbles:564-577}).
 *
 * <table>
 *   <caption>GT6's type metas</caption>
 *   <tr><td>0</td><td>Drone</td></tr>
 *   <tr><td>1</td><td>Princess</td></tr>
 *   <tr><td>2</td><td>Queen</td></tr>
 *   <tr><td>4</td><td>Dead</td></tr>
 *   <tr><td>5 / 6 / 7 / 9</td><td>scanned drone / princess / queen / dead</td></tr>
 * </table>
 *
 * <p>GT6 keeps one item with a meta per type; the port registers one item per (species, type), so the
 * type comes from the item id's suffix instead. All eight types are registered: the Bumblelyzer writes
 * the four scanned metas from a living bee ({@code bumbleScan} = {@code meta + 5}, see
 * {@code content/recipe/GTBumbleBeeRecipes}), and {@link #registered()} reports that their items
 * exist.</p>
 */
public enum BumbleBeeType {
    DRONE("drone", false, true),
    PRINCESS("princess", false, true),
    QUEEN("queen", false, true),
    DEAD("dead", false, true),
    SCANNED_DRONE("scanned_drone", true, true),
    SCANNED_PRINCESS("scanned_princess", true, true),
    SCANNED_QUEEN("scanned_queen", true, true),
    SCANNED_DEAD("scanned_dead", true, true);

    private final String suffix;
    private final boolean scanned;
    private final boolean registered;

    BumbleBeeType(String suffix, boolean scanned, boolean registered) {
        this.suffix = suffix;
        this.scanned = scanned;
        this.registered = registered;
    }

    /** The item id suffix, e.g. {@code princess} (the id is {@code <species>_<suffix>}). */
    public String suffix() { return suffix; }

    /** Whether this is one of GT6's "scanned" variants ({@code meta % 10} 5/6/7/9). */
    public boolean scanned() { return scanned; }

    /** Whether the port registers this type's items. */
    public boolean registered() { return registered; }

    /** GT6's {@code bumbleType} value for this type. */
    public int meta() {
        return switch (this) {
            case DRONE -> 0;
            case PRINCESS -> 1;
            case QUEEN -> 2;
            case DEAD -> 4;
            case SCANNED_DRONE -> 5;
            case SCANNED_PRINCESS -> 6;
            case SCANNED_QUEEN -> 7;
            case SCANNED_DEAD -> 9;
        };
    }

    /** The scanned variant of this bee ({@code bumbleScan}). */
    public BumbleBeeType scannedVariant() {
        return switch (this) {
            case DRONE, SCANNED_DRONE -> SCANNED_DRONE;
            case PRINCESS, SCANNED_PRINCESS -> SCANNED_PRINCESS;
            case QUEEN, SCANNED_QUEEN -> SCANNED_QUEEN;
            case DEAD, SCANNED_DEAD -> SCANNED_DEAD;
        };
    }

    /** The dead variant of this bee ({@code bumbleKill}), keeping the scan state. */
    public BumbleBeeType deadVariant() {
        return scanned ? SCANNED_DEAD : DEAD;
    }

    /** The living variant of a scanned bee, or the bee itself. */
    public BumbleBeeType aliveVariant() {
        return switch (this) {
            case SCANNED_DRONE -> DRONE;
            case SCANNED_PRINCESS -> PRINCESS;
            case SCANNED_QUEEN -> QUEEN;
            case SCANNED_DEAD -> DEAD;
            default -> this;
        };
    }

    /** Whether the type can breed: GT6's queens and princesses are the royal ones. */
    public boolean royal() { return aliveVariant() == PRINCESS || aliveVariant() == QUEEN; }

    /** Whether the bee is alive (GT6's dead types drop out of every breeding path). */
    public boolean alive() { return aliveVariant() != DEAD; }

    /** The type of a port bee item, or null when the stack is not a bee. */
    @Nullable
    public static BumbleBeeType of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return null;
        // Longest suffix first, so "_scanned_drone" is not read as "_drone".
        for (BumbleBeeType type : new BumbleBeeType[]{SCANNED_PRINCESS, SCANNED_DRONE, SCANNED_QUEEN,
                SCANNED_DEAD, PRINCESS, QUEEN, DRONE, DEAD}) {
            if (id.getPath().endsWith("_" + type.suffix())) return type;
        }
        return null;
    }

    /** The species of a port bee item, or null. */
    @Nullable
    public static GTBumbleSpecies.Species speciesOf(ItemStack stack) {
        BumbleBeeType type = of(stack);
        if (type == null) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return null;
        String prefix = id.getPath().substring(0, id.getPath().length() - type.suffix().length() - 1);
        return GTBumbleSpecies.byPrefix(prefix);
    }

    /** The port's item id for a species and type, e.g. {@code wild_bumblebee_princess}. */
    public static String itemId(GTBumbleSpecies.Species species, BumbleBeeType type) {
        return species.beeId(type.suffix());
    }

    /** Whether the port registers the item of this species and type. */
    public static boolean exists(GTBumbleSpecies.Species species, BumbleBeeType type) {
        return item(species, type) != null;
    }

    @Nullable
    private static Item item(GTBumbleSpecies.Species species, BumbleBeeType type) {
        Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", itemId(species, type)));
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    /** A bee stack of this species and type, optionally carrying a genome, or an empty stack. */
    public static ItemStack stack(GTBumbleSpecies.Species species, BumbleBeeType type,
                                 @Nullable CompoundTag genes, int count) {
        Item item = item(species, type);
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        if (genes != null) BumbleBeeGenes.with(stack, genes);
        return stack;
    }
}
