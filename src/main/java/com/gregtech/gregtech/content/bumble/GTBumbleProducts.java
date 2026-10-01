package com.gregtech.gregtech.content.bumble;

import com.gregtech.gregtech.GregTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * GT6's bumblebee products: the three tables of {@code MultiItemBumbles} a working queen is rolled
 * against.
 *
 * <p>GT6 keeps them on the bumblebee <em>item</em> because every bee of the mod is one item with a
 * meta; the port registers one item per (species, type), so the same tables live here, keyed by GT6's
 * species id ({@link GTBumbleSpecies}).</p>
 *
 * <table>
 *   <caption>GT6's product tables</caption>
 *   <tr><td>{@code bumbleProductCount} ({@code MultiItemBumbles:492-494})</td>
 *       <td>always 1: a species has exactly one product, its comb</td></tr>
 *   <tr><td>{@code bumbleProductStack} ({@code :189-213})</td>
 *       <td>{@code meta / 100} picks the comb row: the honey comb for every tier 0 species, the water
 *       comb for tier 1, and so on up to the tera comb of tier 203</td></tr>
 *   <tr><td>{@code bumbleProductChance} ({@code :481-489})</td>
 *       <td>the species <em>level</em> ({@code (meta / 10) % 10}) sets the chance of every product:
 *       2500, 5000, 7500 and 10000 per 10000</td></tr>
 * </table>
 *
 * <p>Port differences to GT6:</p>
 * <ul>
 *   <li>GT6's {@code bumbleProductChance} is a per-product-index lookup; with
 *       {@code bumbleProductCount == 1} only index 0 can ever be asked for, so {@link #chance(int)}
 *       answers for the one product of a species. The index is therefore not part of the port's
 *       signature.</li>
 *   <li>The comb is a plain item in the port (the port's 20 {@code *_comb} food items,
 *       {@code GTMultiItemsGen}), looked up by the id {@link GTBumbleSpecies.Species#comb()}. GT6's
 *       fallback for a meta outside its table ({@code default: return IL.Comb_Honey}) is kept: an
 *       unknown species answers with the honey comb.</li>
 *   <li>GT6's product list holds nothing but the comb - a species has no secondary item, and
 *       {@code bumbleProductStack} never returns a stack larger than one. Addons can raise both
 *       (the interface allows several products per bee), but GregTech 6 itself never does, so the port
 *       has no secondary-product path to port.</li>
 * </ul>
 */
public final class GTBumbleProducts {

    /** GT6's {@code rng(10000)} bound for the product roll ({@code MultiTileEntityBumbliary:171}, {@code :168}). */
    public static final int ROLL = 10000;

    /** GT6's roll bound of the advanced bumbliary ({@code MultiTileEntityBumbliaryAdvanced:171}). */
    public static final int ADVANCED_ROLL = 20000;

    private GTBumbleProducts() {}

    /**
     * GT6's species level, {@code (meta / 10) % 10} ({@code MultiItemBumbles:482,460}): the position of
     * a species inside its comb tier, 0..3.
     */
    public static int level(int speciesId) {
        return (speciesId / 10) % 10;
    }

    /**
     * GT6's {@code bumbleProductChance} ({@code MultiItemBumbles:481-489}): 2500, 5000, 7500 and 10000
     * per 10000 for the levels 0..3, and 10000 for anything outside the table.
     */
    public static int chance(int speciesId) {
        return switch (level(speciesId)) {
            case 0 -> 2500;
            case 1 -> 5000;
            case 2 -> 7500;
            default -> 10000;
        };
    }

    /** GT6's {@code bumbleProductCount} ({@code MultiItemBumbles:492-494}): one product per species. */
    public static int count() {
        return 1;
    }

    /**
     * GT6's {@code bumbleProductStack} ({@code MultiItemBumbles:189-213}) for the one product of a
     * species: its comb, or an empty stack when the port does not register that comb.
     */
    public static ItemStack stack(int speciesId, int count) {
        String comb = GTBumbleSpecies.combOf(speciesId);
        if (comb == null) comb = "honey_comb";                      // GT6 :211 `default: Comb_Honey`
        Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath(GregTech.MODID, comb));
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, count));
    }
}
