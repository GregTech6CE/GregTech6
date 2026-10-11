package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.BumbleBreeding;
import com.gregtech.gregtech.content.bumble.GTBumbleMutations;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GT6's bumblebee mutation, combination and breeding:
 * {@code MultiItemBumbles.bumbleMutateChance:459-467}, {@code bumbleMutate:470-478},
 * {@code bumbleCombine:370-437} and the breeding step of
 * {@code MultiTileEntityBumbliary.onTick2:197-268}.
 *
 * <p>Every assertion is an invariant of GT6's tables, never "this seed happens to roll a certain
 * way": the same-species branch rolls {@code mutateChance}, so a child may be the parent's species or
 * its mutation, and the cross branch rolls {@code rng(4)}, so a child may be either parent or the
 * combination. Counts, types, genomes, the queen and the drone cost are pinned exactly.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleBreedingTests {

    private static final long SEED = 20260917L;

    /**
     * GT6's combination table, the nested switch of {@code bumbleCombine}
     * ({@code MultiItemBumbles:378-435}), as {@code {speciesA, speciesB, result}} - written out
     * independently of {@link GTBumbleMutations} so a wrong table value cannot pass unnoticed.
     */
    private static final int[][] COMBINATIONS = {
            {30, 130, 10100},    // Normal + Water = Sticky
            {30, 530, 10000},    // Normal + Rocky = Clay
            {30, 930, 10200},    // Normal + Sandy = Royal
            {130, 30, 10100},
            {130, 530, 10400},   // Rock + Water = Amnesic / Lubricant
            {330, 430, 10300},   // Nether + End = Satanic
            {330, 10530, 20000}, // Nether + Military = Pyro
            {430, 330, 10300},
            {430, 10530, 20200}, // End + Military = Aero
            {530, 30, 10000},
            {530, 130, 10400},
            {530, 10530, 20300}, // Rocky + Military = Tera
            {630, 930, 10500},   // Jungle + Sandy = Soldier
            {730, 10530, 20100}, // Frosty + Military = Cryo
            {930, 30, 10200},
            {930, 630, 10500},
            {10530, 330, 20000},
            {10530, 430, 20200},
            {10530, 530, 20300},
            {10530, 730, 20100}
    };

    /** A genome whose genes are easy to tell apart, active by day and outside for both parents. */
    private static CompoundTag genes(long work, long aggro, long life, long offspring) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setWorkForce(genes, work);
        BumbleBeeGenes.setAggressiveness(genes, aggro);
        BumbleBeeGenes.setLifeSpan(genes, life);
        BumbleBeeGenes.setOffspring(genes, offspring);
        BumbleBeeGenes.setDayActive(genes, true);
        BumbleBeeGenes.setNightActive(genes, false);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setInsideActive(genes, false);
        return genes;
    }

    private static ItemStack bee(GameTestHelper h, int speciesId, BumbleBeeType type, CompoundTag genes, int count) {
        GTBumbleSpecies.Species species = GTBumbleSpecies.byId(speciesId);
        h.assertTrue(species != null, "species " + speciesId + " exists in the port");
        ItemStack stack = BumbleBeeType.stack(species, type, genes, count);
        h.assertTrue(!stack.isEmpty(), "the bee " + speciesId + " " + type.suffix() + " is registered");
        return stack;
    }

    private static int speciesId(ItemStack stack) {
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(stack);
        return species == null ? -1 : species.id();
    }

    /** GT6's {@code bumbleMutateChance} switch ({@code MultiItemBumbles:460-466}). */
    private static int expectedMutateChance(int speciesId) {
        return switch ((speciesId / 10) % 10) {
            case 0, 1 -> 500;
            case 2 -> 250;
            case 3 -> 25;
            default -> 0;
        };
    }

    /** GT6's {@code bumbleMutate} switch ({@code MultiItemBumbles:471-477}): where a level may go. */
    private static boolean isMutationTarget(int speciesId, int mutatedId) {
        return switch ((speciesId / 10) % 10) {
            case 0 -> mutatedId == speciesId + 10;
            case 1, 2 -> mutatedId == speciesId - 10 || mutatedId == speciesId + 10;
            case 3 -> mutatedId == speciesId - 10;
            default -> mutatedId == speciesId;
        };
    }

    /** GT6's breeding step of a princess and a drone of the same species ({@code :222-231, :252-261}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void sameSpeciesBroodFollowsGt6(GameTestHelper h) {
        CompoundTag mother = genes(1, 100, 1200, 5);
        CompoundTag father = genes(9999, 9000, 140000, 5);
        ItemStack princess = bee(h, 10000, BumbleBeeType.PRINCESS, mother, 1);
        ItemStack drone = bee(h, 10000, BumbleBeeType.DRONE, father, 1);

        BumbleBreeding.Result result = BumbleBreeding.breed(princess, drone, RandomSource.create(SEED));
        h.assertTrue(result != null, "a princess and a drone of the same species breed");

        int princessCount = result.princessCount();
        h.assertTrue(princessCount >= 1 && princessCount <= 3,
                "GT6's 1 + rng(5) / 2 lands in 1..3, got " + princessCount);
        h.assertTrue(result.offspring().length == 5 + princessCount,
                "the brood is offspring(5) + princessCount, got " + result.offspring().length);

        for (int i = 0; i < result.offspring().length; i++) {
            ItemStack child = result.offspring()[i];
            BumbleBeeType type = BumbleBeeType.of(child);
            h.assertTrue(type == (i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE),
                    "offspring " + i + " must be " + (i < princessCount ? "a princess" : "a drone") + ", got " + type);
            // The same-species branch rolls GT6's mutateChance per child and GT6's mutate only steps one
            // level inside the tier, so 10000 -> 10010 is the only species besides the parents'.
            int id = speciesId(child);
            h.assertTrue(id == 10000 || id == 10010,
                    "offspring " + i + " is the parent species or its mutant, got " + id);
            h.assertTrue(BumbleBeeGenes.peek(child) != null, "offspring " + i + " carries a genome");
        }

        h.assertTrue(BumbleBeeType.of(result.queen()) == BumbleBeeType.QUEEN
                        && speciesId(result.queen()) == 10000 && result.queen().getCount() == 1,
                "the queen is one queen of the princess's species, got " + result.queen());
        h.assertTrue(mother.equals(BumbleBeeGenes.peek(result.queen())),
                "GT6's bumbleCrown keeps the princess's genome (:261)");
        h.assertTrue(result.lifeSpan() == 1200, "the lifespan is the princess's gene (:254)");
        h.assertTrue(result.droneCost() == 1 && result.droneDead().getCount() == 1,
                "a single drone costs one drone (:256)");
        h.assertTrue(BumbleBeeType.of(result.droneDead()) == BumbleBeeType.DEAD
                        && speciesId(result.droneDead()) == 10000,
                "the consumed drone dies as a dead bee of the drone's species");
        h.assertTrue(father.equals(BumbleBeeGenes.peek(result.droneDead())),
                "GT6's bumbleKill keeps the drone's genome");
        h.assertTrue(princess.getCount() == 1 && drone.getCount() == 1,
                "the breeding step itself consumes nothing: the machine decrements the slots (:259)");
        h.succeed();
    }

    /** GT6's per-gene inheritance ({@code IItemBumbleBee.Util.getBumbleGenes:109-128}) per child. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void offspringGenesComeFromTheParents(GameTestHelper h) {
        CompoundTag mother = genes(1, 100, 1200, 6);
        CompoundTag father = genes(9999, 9000, 140000, 6);
        // A level 3 species, so its mutation can only step down to the level 2 neighbour.
        ItemStack princess = bee(h, 630, BumbleBeeType.PRINCESS, mother, 1);
        ItemStack drone = bee(h, 630, BumbleBeeType.DRONE, father, 1);

        BumbleBreeding.Result result = BumbleBreeding.breed(princess, drone, RandomSource.create(SEED));
        h.assertTrue(result != null, "the same-species pair breeds");
        h.assertTrue(result.offspring().length == 6 + result.princessCount(),
                "the brood size follows the princess's offspring gene");

        for (ItemStack child : result.offspring()) {
            int id = speciesId(child);
            h.assertTrue(id == 630 || id == 620, "a level 3 species can only mutate down, got " + id);
            CompoundTag childGenes = BumbleBeeGenes.peek(child);
            h.assertTrue(childGenes != null, "every child carries a genome");
            long work = BumbleBeeGenes.workForce(childGenes);
            h.assertTrue(work == 1 || work == 9999, "work force comes from one parent, got " + work);
            long aggro = BumbleBeeGenes.aggressiveness(childGenes);
            h.assertTrue(aggro == 100 || aggro == 9000, "aggressiveness comes from one parent, got " + aggro);
            long life = BumbleBeeGenes.lifeSpan(childGenes);
            h.assertTrue(life == 1200 || life == 140000, "lifespan comes from one parent, got " + life);
            h.assertTrue(BumbleBeeGenes.dayActive(childGenes) && !BumbleBeeGenes.nightActive(childGenes),
                    "the day/night genes follow the parents (GT6 ORs the pair so a bee is never idle)");
        }

        CompoundTag queenGenes = BumbleBeeGenes.peek(result.queen());
        h.assertTrue(queenGenes != null && BumbleBeeGenes.workForce(queenGenes) == 1
                        && BumbleBeeGenes.aggressiveness(queenGenes) == 100
                        && BumbleBeeGenes.lifeSpan(queenGenes) == 1200
                        && BumbleBeeGenes.offspring(queenGenes) == 6,
                "the queen carries the princess's genome, not the brood's");
        h.succeed();
    }

    /** GT6's cross-species brood ({@code MultiTileEntityBumbliary:232-250}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void crossSpeciesBroodUsesTheCombinationTable(GameTestHelper h) {
        // Cultivated x Subnautic is GT6's "Normal + Water = Sticky"; Jungle x Surfing is a pair the table
        // does not list, so only the two parents may appear; Demonic x Nihilistic is "Nether + End =
        // Satanic"; General Bumblemond x Bumble Claus is "Frosty + Military = Cryo".
        int[][] pairs = {{30, 130}, {600, 100}, {330, 430}, {10530, 730}};
        for (int[] pair : pairs) {
            GTBumbleSpecies.Species first = GTBumbleSpecies.byId(pair[0]);
            GTBumbleSpecies.Species second = GTBumbleSpecies.byId(pair[1]);
            Set<Integer> allowed = new LinkedHashSet<>();
            allowed.add(pair[0]);
            allowed.add(pair[1]);
            GTBumbleSpecies.Species forward = GTBumbleMutations.combineSpecies(first, second);
            GTBumbleSpecies.Species reverse = GTBumbleMutations.combineSpecies(second, first);
            if (forward != null) allowed.add(forward.id());
            if (reverse != null) allowed.add(reverse.id());

            ItemStack princess = bee(h, pair[0], BumbleBeeType.PRINCESS, genes(1, 100, 1200, 4), 1);
            ItemStack drone = bee(h, pair[1], BumbleBeeType.DRONE, genes(9999, 9000, 140000, 4), 1);
            h.assertTrue(!BumbleBreeding.sameSpecies(princess, drone),
                    pair[0] + " and " + pair[1] + " are different species");

            BumbleBreeding.Result result = BumbleBreeding.breed(princess, drone, RandomSource.create(SEED));
            h.assertTrue(result != null, pair[0] + " x " + pair[1] + " breeds");
            h.assertTrue(result.offspring().length == 4 + result.princessCount(),
                    "the brood is the princess's offspring gene plus the princess count");
            int princessCount = result.princessCount();
            for (int i = 0; i < result.offspring().length; i++) {
                ItemStack child = result.offspring()[i];
                h.assertTrue(allowed.contains(speciesId(child)),
                        "offspring " + i + " of " + pair[0] + " x " + pair[1] + " must be one of " + allowed
                                + ", got " + speciesId(child));
                h.assertTrue(BumbleBeeType.of(child) == (i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE),
                        "the first princessCount children are princesses, the rest drones (:234-247)");
                h.assertTrue(BumbleBeeGenes.peek(child) != null, "cross children inherit a genome too (:252)");
            }
            h.assertTrue(speciesId(result.queen()) == pair[0] && BumbleBeeType.of(result.queen()) == BumbleBeeType.QUEEN,
                    "the queen stays the princess's species, got " + speciesId(result.queen()));
            h.assertTrue(result.lifeSpan() == 1200, "the lifespan is the princess's gene");
        }
        h.succeed();
    }

    /** GT6's combination table, entry for entry, plus its fallback for an unlisted pair. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void combinationTableMatchesGt6(GameTestHelper h) {
        h.assertTrue(COMBINATIONS.length == 20, "GT6's switch has 20 reachable combinations");
        for (int[] entry : COMBINATIONS) {
            GTBumbleSpecies.Species a = GTBumbleSpecies.byId(entry[0]);
            GTBumbleSpecies.Species b = GTBumbleSpecies.byId(entry[1]);
            h.assertTrue(a != null && b != null && GTBumbleSpecies.byId(entry[2]) != null,
                    "the port has every species of " + entry[0] + " + " + entry[1] + " = " + entry[2]);
            GTBumbleSpecies.Species combined = GTBumbleMutations.combineSpecies(a, b);
            h.assertTrue(combined != null && combined.id() == entry[2],
                    "GT6's " + entry[0] + " + " + entry[1] + " = " + entry[2] + ", got "
                            + (combined == null ? "null" : combined.id()));
            for (BumbleBeeType type : new BumbleBeeType[]{BumbleBeeType.DRONE, BumbleBeeType.PRINCESS}) {
                ItemStack stack = GTBumbleMutations.combine(
                        BumbleBeeType.stack(a, BumbleBeeType.PRINCESS, null, 1),
                        BumbleBeeType.stack(b, BumbleBeeType.DRONE, null, 1), type, RandomSource.create(SEED));
                h.assertTrue(!stack.isEmpty() && BumbleBeeType.of(stack) == type && speciesId(stack) == entry[2]
                                && stack.getCount() == 1,
                        "combine(" + entry[0] + ", " + entry[1] + ", " + type + ") is one " + entry[2]);
                h.assertTrue(BumbleBeeGenes.peek(stack) == null,
                        "GT6's bumbleCombine answers without a genome (IItemBumbleBee:67)");
            }
        }
        // GT6's fallback for a pair the table does not list: the first parent (:436).
        GTBumbleSpecies.Species jungle = GTBumbleSpecies.byId(600);
        GTBumbleSpecies.Species surfing = GTBumbleSpecies.byId(100);
        h.assertTrue(GTBumbleMutations.combineSpecies(jungle, surfing) == jungle
                        && GTBumbleMutations.combineSpecies(surfing, jungle) == surfing,
                "an unlisted pair answers with its first parent, which is how the switch becomes a 50/50");
        h.succeed();
    }

    /** GT6's mutation for every one of the port's 80 species, and for non-bees. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void mutationFollowsGt6ForEverySpecies(GameTestHelper h) {
        for (GTBumbleSpecies.Species species : GTBumbleSpecies.SPECIES) {
            int id = species.id();
            for (BumbleBeeType type : new BumbleBeeType[]{BumbleBeeType.DRONE, BumbleBeeType.PRINCESS}) {
                CompoundTag genome = genes(500, 500, 3000, 2);
                ItemStack stack = BumbleBeeType.stack(species, type, genome, 3);
                h.assertTrue(!stack.isEmpty(), "the bee " + id + " " + type.suffix() + " is registered");
                h.assertTrue(GTBumbleMutations.mutateChance(stack) == expectedMutateChance(id),
                        "species " + id + " has GT6's chance " + expectedMutateChance(id)
                                + ", got " + GTBumbleMutations.mutateChance(stack));

                ItemStack mutated = GTBumbleMutations.mutate(stack, RandomSource.create(SEED + id));
                h.assertTrue(!mutated.isEmpty() && BumbleBeeType.of(mutated) == type,
                        "mutate keeps the bee and its type: " + id + " " + type.suffix());
                h.assertTrue(isMutationTarget(id, speciesId(mutated)),
                        "species " + id + " mutates inside its own tier, got " + speciesId(mutated));
                h.assertTrue(mutated.getCount() == 3 && genome.equals(BumbleBeeGenes.peek(mutated)),
                        "mutate keeps the stack size and the genome (GT6's ST.copyMeta)");
            }
        }

        // GT6's level 0 is the one unconditional case: the mutation always steps up one level.
        ItemStack base = bee(h, 10100, BumbleBeeType.DRONE, genes(1, 100, 1200, 1), 1);
        h.assertTrue(speciesId(GTBumbleMutations.mutate(base, RandomSource.create(SEED))) == 10110,
                "a level 0 species always mutates up, whatever the seed says");
        ItemStack top = bee(h, 10230, BumbleBeeType.PRINCESS, genes(1, 100, 1200, 1), 1);
        h.assertTrue(speciesId(GTBumbleMutations.mutate(top, RandomSource.create(SEED))) == 10220,
                "a level 3 species always mutates down");

        // A stack that is not a bee has no species, so no table can answer with a non-bee item.
        ItemStack notABee = new ItemStack(Items.HONEYCOMB);
        h.assertTrue(GTBumbleMutations.mutateChance(notABee) == 0, "a non-bee never mutates");
        h.assertTrue(GTBumbleMutations.mutate(notABee, RandomSource.create(SEED)).is(Items.HONEYCOMB),
                "mutate hands a non-bee back unchanged");
        h.assertTrue(GTBumbleMutations.combine(notABee, notABee, BumbleBeeType.PRINCESS,
                RandomSource.create(SEED)).isEmpty(), "combine has no answer for non-bees");
        h.assertTrue(GTBumbleMutations.combine(base, base, null, RandomSource.create(SEED)).isEmpty(),
                "combine needs a type");
        h.succeed();
    }

    /** GT6's drone pick ({@code MultiTileEntityBumbliary:200-213}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void findDroneSlotFollowsGt6(GameTestHelper h) {
        ItemStack princess = bee(h, 100, BumbleBeeType.PRINCESS, genes(1, 100, 1200, 1), 1);
        ItemStack same = bee(h, 100, BumbleBeeType.DRONE, genes(1, 100, 1200, 1), 1);
        ItemStack other = bee(h, 200, BumbleBeeType.DRONE, genes(1, 100, 1200, 1), 1);
        ItemStack third = bee(h, 300, BumbleBeeType.DRONE, genes(1, 100, 1200, 1), 1);
        ItemStack otherPrincess = bee(h, 100, BumbleBeeType.PRINCESS, genes(1, 100, 1200, 1), 1);

        // GT6 :202-203: the main drone slot wins whenever it holds a drone.
        h.assertTrue(BumbleBreeding.findDroneSlot(princess, new ItemStack[]{same, other}) == 0,
                "the main drone slot is used when it holds a drone");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess, new ItemStack[]{ItemStack.EMPTY, same}) == 1,
                "an empty main slot falls through to the spare slots");
        // GT6 :205-212: the scan overwrites its choice with every later drone, but stops at a drone of
        // the princess's species.
        h.assertTrue(BumbleBreeding.findDroneSlot(princess,
                new ItemStack[]{ItemStack.EMPTY, other, third, same, third}) == 3,
                "the equal-species drone wins over the drones behind it");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess,
                new ItemStack[]{ItemStack.EMPTY, other, third}) == 2,
                "without an equal species the last drone of the scan is picked");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess,
                new ItemStack[]{ItemStack.EMPTY, otherPrincess, third}) == 2,
                "a princess in a spare slot is not a drone (GT6's % 5 == 0)");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess, new ItemStack[5]) == -1, "no drones at all");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess, new ItemStack[0]) == -1, "an empty slot list");
        h.assertTrue(BumbleBreeding.findDroneSlot(princess, null) == -1, "a null slot list");
        h.succeed();
    }

    /** GT6 only breeds a princess with a drone ({@code :197, :207}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void breedRefusesAnythingButAPrincessAndADrone(GameTestHelper h) {
        CompoundTag genome = genes(1, 100, 1200, 2);
        ItemStack princess = bee(h, 100, BumbleBeeType.PRINCESS, genome, 1);
        ItemStack drone = bee(h, 100, BumbleBeeType.DRONE, genome, 1);
        ItemStack queen = bee(h, 100, BumbleBeeType.QUEEN, genome, 1);
        ItemStack dead = bee(h, 100, BumbleBeeType.DEAD, genome, 1);

        h.assertTrue(BumbleBreeding.breed(null, drone, RandomSource.create(SEED)) == null,
                "a drone without a princess waits");
        h.assertTrue(BumbleBreeding.breed(princess, null, RandomSource.create(SEED)) == null,
                "a princess without a drone waits");
        h.assertTrue(BumbleBreeding.breed(princess, ItemStack.EMPTY, RandomSource.create(SEED)) == null,
                "an empty drone slot does not breed");
        h.assertTrue(BumbleBreeding.breed(princess, dead, RandomSource.create(SEED)) == null,
                "a dead bee is no drone");
        h.assertTrue(BumbleBreeding.breed(dead, drone, RandomSource.create(SEED)) == null,
                "a dead bee is no princess");
        h.assertTrue(BumbleBreeding.breed(drone, drone, RandomSource.create(SEED)) == null,
                "a drone is no princess (GT6's % 5 == 1)");
        h.assertTrue(BumbleBreeding.breed(princess, princess, RandomSource.create(SEED)) == null,
                "a princess is no drone (GT6's % 5 == 0)");
        h.assertTrue(BumbleBreeding.breed(queen, drone, RandomSource.create(SEED)) == null,
                "a queen runs GT6's queen branch, not the breeding one");
        h.assertTrue(BumbleBreeding.breed(new ItemStack(Items.HONEYCOMB), drone, RandomSource.create(SEED)) == null,
                "a non-bee cannot breed");
        h.assertTrue(BumbleBreeding.breed(princess, new ItemStack(Items.HONEYCOMB), RandomSource.create(SEED)) == null,
                "a non-bee is no drone");
        h.succeed();
    }

    /** GT6's drone cost ({@code MultiTileEntityBumbliary:256-259}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void droneCostFollowsTheStackAndTheSlot(GameTestHelper h) {
        CompoundTag genome = genes(1, 100, 1200, 2);
        ItemStack princess = bee(h, 10100, BumbleBeeType.PRINCESS, genome, 1);
        ItemStack single = bee(h, 10100, BumbleBeeType.DRONE, genome, 1);
        ItemStack stack = bee(h, 10100, BumbleBeeType.DRONE, genome, 5);

        BumbleBreeding.Result one = BumbleBreeding.breed(princess, single, RandomSource.create(SEED));
        h.assertTrue(one != null && one.droneCost() == 1 && one.droneDead().getCount() == 1,
                "one drone costs one drone");
        BumbleBreeding.Result many = BumbleBreeding.breed(princess, stack, RandomSource.create(SEED));
        h.assertTrue(many != null && many.droneCost() == 2 && many.droneDead().getCount() == 2,
                "a drone stack of more than one costs two drones (:256)");
        h.assertTrue(speciesId(many.droneDead()) == 10100
                        && BumbleBeeType.of(many.droneDead()) == BumbleBeeType.DEAD,
                "both dead drones are the drone's species");
        // GT6 charges the second drone only when the drone sat in the main drone slot.
        BumbleBreeding.Result spare = BumbleBreeding.breed(princess, stack, false, RandomSource.create(SEED));
        h.assertTrue(spare != null && spare.droneCost() == 1 && spare.droneDead().getCount() == 1,
                "a spare drone slot only loses the one drone (:256)");
        h.assertTrue(stack.getCount() == 5, "the breeding step leaves the drone stack to the machine");
        h.succeed();
    }

    /** GT6's scan state: a scanned pair breeds, but only the crown and the kill keep the scan. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void scannedBeesKeepTheirScanState(GameTestHelper h) {
        CompoundTag genome = genes(1, 100, 1200, 2);
        ItemStack princess = bee(h, 100, BumbleBeeType.SCANNED_PRINCESS, genome, 1);
        ItemStack drone = bee(h, 100, BumbleBeeType.SCANNED_DRONE, genome, 1);

        BumbleBreeding.Result result = BumbleBreeding.breed(princess, drone, RandomSource.create(SEED));
        h.assertTrue(result != null,
                "a scanned princess and a scanned drone breed (GT6's % 5 == 1 and % 5 == 0)");
        h.assertTrue(BumbleBeeType.of(result.queen()) == BumbleBeeType.SCANNED_QUEEN,
                "GT6's bumbleCrown keeps the scan state: (meta / 5) * 5 + 2");
        h.assertTrue(BumbleBeeType.of(result.droneDead()) == BumbleBeeType.SCANNED_DEAD,
                "GT6's bumbleKill keeps the scan state: (meta / 5) * 5 + 4");
        int princessCount = result.princessCount();
        for (int i = 0; i < result.offspring().length; i++) {
            // GT6 builds the brood with the plain types (bumblePrincess = +1, bumbleDrone = +0, :570-571),
            // so the scan state is never inherited by a child.
            BumbleBeeType type = BumbleBeeType.of(result.offspring()[i]);
            h.assertTrue(type == (i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE),
                    "a child of a scanned pair is a plain "
                            + (i < princessCount ? "princess" : "drone") + ", got " + type);
        }
        // A scanned queen still runs GT6's queen branch (:118), not the breeding one.
        h.assertTrue(BumbleBreeding.breed(bee(h, 100, BumbleBeeType.SCANNED_QUEEN, genome, 1), drone,
                RandomSource.create(SEED)) == null, "a scanned queen does not breed");
        h.succeed();
    }

    /** The same seed has to reproduce the whole brood, including the inherited genomes. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void theSameSeedBreedsTheSameBrood(GameTestHelper h) {
        String crossFirst = signature(h, BumbleBreeding.breed(
                bee(h, 330, BumbleBeeType.PRINCESS, genes(11, 111, 2222, 8), 1),
                bee(h, 430, BumbleBeeType.DRONE, genes(22, 222, 4444, 8), 1), RandomSource.create(SEED)));
        String crossSecond = signature(h, BumbleBreeding.breed(
                bee(h, 330, BumbleBeeType.PRINCESS, genes(11, 111, 2222, 8), 1),
                bee(h, 430, BumbleBeeType.DRONE, genes(22, 222, 4444, 8), 1), RandomSource.create(SEED)));
        h.assertTrue(crossFirst.equals(crossSecond),
                "the same seed gives the same cross brood: " + crossFirst + " vs " + crossSecond);

        String sameFirst = signature(h, BumbleBreeding.breed(
                bee(h, 10500, BumbleBeeType.PRINCESS, genes(5, 500, 5000, 8), 1),
                bee(h, 10500, BumbleBeeType.DRONE, genes(50, 5000, 500, 8), 1), RandomSource.create(SEED)));
        String sameSecond = signature(h, BumbleBreeding.breed(
                bee(h, 10500, BumbleBeeType.PRINCESS, genes(5, 500, 5000, 8), 1),
                bee(h, 10500, BumbleBeeType.DRONE, genes(50, 5000, 500, 8), 1), RandomSource.create(SEED)));
        h.assertTrue(sameFirst.equals(sameSecond),
                "the same seed gives the same same-species brood: " + sameFirst + " vs " + sameSecond);
        h.succeed();
    }

    /** Everything a brood is made of, as text - for the determinism check. */
    private static String signature(GameTestHelper h, BumbleBreeding.Result result) {
        h.assertTrue(result != null, "the pair breeds");
        StringBuilder text = new StringBuilder();
        text.append(result.princessCount()).append('|').append(result.droneCost()).append('|')
                .append(result.lifeSpan()).append('|').append(speciesId(result.queen())).append('|')
                .append(speciesId(result.droneDead())).append('|');
        for (ItemStack child : result.offspring()) {
            CompoundTag genes = BumbleBeeGenes.peek(child);
            text.append(speciesId(child)).append(':').append(BumbleBeeType.of(child)).append(':')
                    .append(genes == null ? "no-genome" : BumbleBeeGenes.workForce(genes) + "/"
                            + BumbleBeeGenes.aggressiveness(genes) + "/" + BumbleBeeGenes.lifeSpan(genes)
                            + "/" + BumbleBeeGenes.offspring(genes) + "/" + BumbleBeeGenes.dayActive(genes)
                            + "/" + BumbleBeeGenes.nightActive(genes))
                    .append(';');
        }
        return text.toString();
    }
}
