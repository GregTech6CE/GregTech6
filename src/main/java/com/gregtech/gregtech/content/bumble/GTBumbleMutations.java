package com.gregtech.gregtech.content.bumble;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's bumblebee mutation and combination tables as pure functions over the port's species ids
 * ({@code MultiItemBumbles.bumbleMutateChance:459-467}, {@code bumbleMutate:470-478} and
 * {@code bumbleCombine:370-437}).
 *
 * <p>GT6 registers one item with a meta per species and type ({@code MultiItemBumbles.make:581-597}),
 * so its tables are written in terms of {@code meta / 10} (the species id) and {@code meta % 10} (the
 * type). The port registers one item per (species, type) pair and the type lives in the item id, so the
 * same tables are keyed by the species ids of {@link GTBumbleSpecies} here, and the type is a
 * {@link BumbleBeeType} argument.</p>
 *
 * <p>Port differences to GT6, all in one place:</p>
 * <ul>
 *   <li>{@code bumbleCombine} first delegates to the other bee's item when one of the two bees comes
 *       from an addon mod ({@code :372-376}); the port has no addon bees, so an input that does not
 *       resolve through {@link BumbleBeeType#speciesOf} makes these methods answer with an empty stack
 *       (or the input, for {@link #mutate}) instead of inventing an item.</li>
 *   <li>GT6 can build a stack for <em>any</em> meta through {@code ST.make(this, 1, meta)}, even one
 *       that has no item; the port can only name species {@link GTBumbleSpecies} knows and whose items
 *       are registered, so a table entry naming a species the port does not have is skipped (see
 *       {@link #COMBINATIONS}) - none had to be skipped.</li>
 *   <li>GT6's {@code Random} parameter of {@code bumbleCombine} is only ever handed to that addon
 *       delegation, so {@link #combine} keeps the parameter for signature parity but never draws from
 *       it; {@link #mutate} draws exactly where GT6 does (one {@code nextBoolean} for the level 1 and
 *       level 2 cases).</li>
 *   <li>GT6 reads the meta straight off the stack; the port resolves the species through
 *       {@link BumbleBeeType#speciesOf}, so a stack that is not a port bee has no species, no mutate
 *       chance and no mutation.</li>
 * </ul>
 */
public final class GTBumbleMutations {

    /**
     * GT6's combination table, the nested switch of {@code bumbleCombine}
     * ({@code MultiItemBumbles:378-435}), as {@code {speciesA, speciesB, result}} triples in GT6's own
     * order. A result is always the base species of a tier (the {@code ...00} one), because GT6 returns
     * {@code result + aBumbleType}, i.e. a drone (0) or a princess (1).
     *
     * <p>GT6's comments next to the species ({@code MultiItemBumbles.addItems:120-185}) name the
     * intended recipes; they are repeated per entry. The reverse direction is listed wherever GT6 lists
     * it, which makes the table symmetric; a pair that is <em>not</em> listed falls back to the first
     * parent ({@code :436}).</p>
     *
     * <p>Skip check: GT6's switch names the species 30, 130, 330, 430, 530, 630, 730, 930, 10000,
     * 10100, 10200, 10300, 10400, 10500, 10530, 20000, 20100, 20200 and 20300, and every one of them
     * exists in {@link GTBumbleSpecies}, so all twenty entries are ported and none was skipped. GT6's
     * {@code addItems:180-185} lists six further ideas it never implemented (Jungle + Water = Swamp,
     * Rocky + Sandy = Red Sand, Rocky + Frozen = Coal, Rocky + Nether = Heavy Metal, Royal + ??? =
     * Heroic, Frosty + Nether = Gloomstone); there is nothing to port for those.</p>
     */
    private static final int[][] COMBINATIONS = {
            // case 3: Cultivated Bumblebee
            {30, 130, 10100},    // Normal + Water = Sticky
            {30, 530, 10000},    // Normal + Rocky = Clay
            {30, 930, 10200},    // Normal + Sandy = Royal
            // case 13: Subnautic Bumblebee
            {130, 30, 10100},    // Normal + Water = Sticky (reverse)
            {130, 530, 10400},   // Rock + Water = Amnesic / Lubricant
            // case 33: Demonic Bumblebee
            {330, 430, 10300},   // Nether + End = Satanic
            {330, 10530, 20000}, // Nether + Military = Pyro
            // case 43: Nihilistic Bumblebee
            {430, 330, 10300},   // Nether + End = Satanic (reverse)
            {430, 10530, 20200}, // End + Military = Aero
            // case 53: Bumbelvis
            {530, 30, 10000},    // Normal + Rocky = Clay (reverse)
            {530, 130, 10400},   // Rock + Water = Amnesic / Lubricant (reverse)
            {530, 10530, 20300}, // Rocky + Military = Tera
            // case 63: Bumblezan
            {630, 930, 10500},   // Jungle + Sandy = Soldier
            // case 73: Bumble Claus
            {730, 10530, 20100}, // Frosty + Military = Cryo
            // case 93: Bumbobee
            {930, 30, 10200},    // Normal + Sandy = Royal (reverse)
            {930, 630, 10500},   // Jungle + Sandy = Soldier (reverse)
            // case 1053: General Bumblemond, the military tier's level 3
            {10530, 330, 20000}, // Nether + Military = Pyro (reverse)
            {10530, 430, 20200}, // End + Military = Aero (reverse)
            {10530, 530, 20300}, // Rocky + Military = Tera (reverse)
            {10530, 730, 20100}  // Frosty + Military = Cryo (reverse)
    };

    private GTBumbleMutations() {}

    /**
     * GT6's {@code bumbleMutateChance} ({@code MultiItemBumbles:459-467}), in 1/10000.
     *
     * <p>GT6 switches on {@code (meta / 10) % 10}, the species' <em>level</em> digit inside its tier of
     * four: levels 0 and 1 mutate with 5%, level 2 with 2.5%, level 3 with 0.25%, and any other level
     * never mutates ({@code default: return 0}). The type never matters, because {@code meta} is
     * {@code species + type} with a type below 10.</p>
     *
     * @param bee a port bee stack; anything else has no species and answers 0
     */
    public static int mutateChance(ItemStack bee) {
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(bee);
        return species == null ? 0 : mutateChance(level(species));
    }

    /** The chance table of {@code MultiItemBumbles:460-466}, keyed by the level digit. */
    private static int mutateChance(int level) {
        return switch (level) {
            case 0, 1 -> 500;
            case 2 -> 250;
            case 3 -> 25;
            default -> 0;
        };
    }

    /**
     * GT6's {@code bumbleMutate} ({@code MultiItemBumbles:470-478}): the level digit decides the
     * direction of the mutation, and the bee keeps its type and its genome.
     *
     * <table>
     *   <caption>GT6's mutation</caption>
     *   <tr><td>level 0</td><td>{@code meta + 10}, always upwards - no random draw at all</td></tr>
     *   <tr><td>level 1 / 2</td><td>{@code meta +/- 10} on a coin flip</td></tr>
     *   <tr><td>level 3</td><td>{@code meta - 10}, always downwards</td></tr>
     *   <tr><td>anything else</td><td>{@code ST.copy(aBumbleBee)}, unchanged</td></tr>
     * </table>
     *
     * <p>GT6's {@code ST.copyMeta} keeps the stack size and the {@code gt.bumble} tag, so both are kept
     * here too. The port difference: a target species the port does not register (and any input that is
     * not a port bee) answers with a copy of the input, where GT6 would have built a stack for that
     * meta anyway.</p>
     *
     * @param bee the bee to mutate, unchanged by this call
     * @param random GT6's {@code aRandom} - drawn from only for the level 1 and 2 flip
     * @return the mutated bee, or a copy of the input when nothing mutates
     */
    public static ItemStack mutate(ItemStack bee, RandomSource random) {
        if (bee == null || bee.isEmpty()) return ItemStack.EMPTY;
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(bee);
        BumbleBeeType type = BumbleBeeType.of(bee);
        if (species == null || type == null) return bee.copy();

        GTBumbleSpecies.Species mutated;
        switch (level(species)) {
            case 0: mutated = GTBumbleSpecies.byId(species.id() + 10); break;
            case 1:
            case 2: mutated = GTBumbleSpecies.byId(species.id() + (random.nextBoolean() ? 10 : -10)); break;
            case 3: mutated = GTBumbleSpecies.byId(species.id() - 10); break;
            default: return bee.copy();
        }
        if (mutated == null) return bee.copy();

        ItemStack result = BumbleBeeType.stack(mutated, type, BumbleBeeGenes.peek(bee), bee.getCount());
        return result.isEmpty() ? bee.copy() : result;
    }

    /**
     * GT6's {@code bumbleCombine} ({@code MultiItemBumbles:370-437}): the special offspring of two
     * different species.
     *
     * <p>GT6's {@code aBumbleType} is 0 for a drone and 1 for a princess; it is only appended to the
     * result species, so any {@link BumbleBeeType} works here, but GT6's breeding passes
     * {@link BumbleBeeType#DRONE} and {@link BumbleBeeType#PRINCESS} (the Bumbliary does, at
     * {@code MultiTileEntityBumbliary:238-246}).</p>
     *
     * <p>Like GT6's {@code bumbleCombine} - documented as "Stacksize 1 and no BumbleTag"
     * ({@code IItemBumbleBee:67}) - the answer never carries a genome; the breeding step assigns the
     * inherited one afterwards.</p>
     *
     * @param a the first parent (GT6's {@code aBumbleBeeA})
     * @param b the second parent (GT6's {@code aBumbleBeeB})
     * @param type the offspring's type, GT6's {@code aBumbleType}
     * @param random kept for GT6 parity; GT6 only uses it on the addon delegation path
     * @return the combined bee, or an empty stack when the port cannot name GT6's result
     */
    public static ItemStack combine(ItemStack a, ItemStack b, BumbleBeeType type, RandomSource random) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty() || type == null) return ItemStack.EMPTY;
        GTBumbleSpecies.Species speciesA = BumbleBeeType.speciesOf(a);
        GTBumbleSpecies.Species speciesB = BumbleBeeType.speciesOf(b);
        if (speciesA == null || speciesB == null) return ItemStack.EMPTY;
        GTBumbleSpecies.Species result = combineSpecies(speciesA, speciesB);
        return result == null ? ItemStack.EMPTY : BumbleBeeType.stack(result, type, null, 1);
    }

    /**
     * The species GT6's combination switch returns for a pair, including GT6's fallback of the first
     * parent ({@code MultiItemBumbles:436}, {@code ST.make(this, 1, (aMetaDataA / 10) * 10 + type)}) for
     * a pair the table does not list.
     *
     * @return the port species, or null when either parent is not a known species
     */
    @Nullable
    public static GTBumbleSpecies.Species combineSpecies(GTBumbleSpecies.Species a, GTBumbleSpecies.Species b) {
        if (a == null || b == null) return null;
        for (int[] entry : COMBINATIONS) {
            if (entry[0] != a.id() || entry[1] != b.id()) continue;
            GTBumbleSpecies.Species result = GTBumbleSpecies.byId(entry[2]);
            if (result != null) return result;
            // Port rule: a result species the port does not have skips the entry, so the pair falls
            // through to GT6's first-parent fallback (unreachable while all 80 species are registered).
        }
        return a;
    }

    /** GT6's level digit: {@code (meta / 10) % 10}, the species' position inside its tier of four. */
    private static int level(GTBumbleSpecies.Species species) {
        return (species.id() / 10) % 10;
    }
}
