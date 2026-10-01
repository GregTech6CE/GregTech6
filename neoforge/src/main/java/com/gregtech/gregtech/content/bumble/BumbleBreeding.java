package com.gregtech.gregtech.content.bumble;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The breeding step of GT6's Bumbliary ({@code MultiTileEntityBumbliary.onTick2:197-268}): a princess
 * and a drone turn into a brood, a queen and a dead drone.
 *
 * <p>Only the pure part of that tick lives here - no block, no GUI. Everything GT6 does around it stays
 * with the machine that calls this class: the environment check ({@code :120, :136}), the breeding
 * countdown ({@code :198-199, :215}), the brood that is moved into the drone slots or killed when its
 * genome does not fit the bumbliary ({@code :135-143}), the drones that are killed off ({@code :263-266})
 * and the consumption of the drone stack ({@code :259, decrStackSize}).</p>
 *
 * <p>What is reproduced exactly: GT6's drone pick ({@code :200-213}, {@link #findDroneSlot}), the
 * princess count {@code 1 + rng(5) / 2} ({@code :218}), the brood size {@code offspring + princessCount}
 * ({@code :220}), the same-species branch with its {@code mutateChance}/{@code mutate} roll per child
 * ({@code :222-231}), the cross branch with its {@code switch(rng(4))} over {princess A, princess B,
 * combine(A, B), combine(B, A)} and the drone equivalents ({@code :232-250}), the per-child inherited
 * genome ({@code :252}), the lifespan taken from the princess ({@code :254}), the drone cost
 * ({@code :256}), the killed drone ({@code :257}) and the crowned queen that keeps the princess's genome
 * ({@code :261}).</p>
 *
 * <p>Port differences to GT6, all in one place:</p>
 * <ul>
 *   <li>GT6 works on slots ({@code SLOT_ROYAL = 13}, {@code SLOT_DRONE = 22},
 *       {@code SLOTS_DRONE = {3, 4, 5, 12, 14, 21, 23}}, {@code :333-337}); the port takes the two
 *       parents as stacks and answers with the brood, the queen and the dead drone, so the machine keeps
 *       its own inventory. {@link #findDroneSlot} is the bridge for the drone side: index 0 of its array
 *       stands for GT6's main drone slot, the remaining entries for the spare slots.</li>
 *   <li>GT6 draws from the global {@code RNGSUS} and from {@code rng(..)} on the tile entity; the port
 *       only draws from the {@link RandomSource} it is handed, so one seed reproduces one brood
 *       (including the per-child inheritance).</li>
 *   <li>GT6's {@code tLoss} is 2 only when the drone sat in the main drone slot
 *       ({@code tBreedSlot == SLOT_DRONE && stackSize > 1}, {@code :256}); a stack-only API cannot see
 *       the slot, so {@link #breed(ItemStack, ItemStack, RandomSource)} charges 2 whenever the drone
 *       stack holds more than one item, and {@link #breed(ItemStack, ItemStack, boolean, RandomSource)}
 *       offers GT6's slot condition for a caller that knows where the drone came from.</li>
 *   <li>GT6 lets {@code Util.getBumbleTag} generate and store a missing genome on both parents
 *       ({@code IItemBumbleBee:97-102}, reached from {@code :116} and from the inheritance at
 *       {@code :252}); the port does the same through {@link BumbleBeeGenes#of}, so a genome-less
 *       princess and drone leave this call with a genome, exactly like in GT6.</li>
 *   <li>GT6's {@code bumbleEqual} ({@code MultiItemBumbles:567}) compares the item <em>and</em>
 *       {@code meta / 10}; GT6 has one item for every species and type, so that is "same species,
 *       whatever the type". The port has one item per (species, type), so {@link #sameSpecies} compares
 *       the species only.</li>
 *   <li>GT6's {@code bumbleCrown} and {@code bumbleKill} ({@code MultiItemBumbles:565-566}) keep the
 *       scan state, which the port reproduces through {@link BumbleBeeType#scanned()} and
 *       {@link BumbleBeeType#deadVariant()}: a scanned princess is crowned to a scanned queen and a
 *       scanned drone dies as a scanned dead bee. The offspring itself is never scanned, because GT6
 *       builds it with the plain types ({@code bumblePrincess} = {@code + 1}, {@code bumbleDrone} =
 *       {@code + 0}, {@code MultiItemBumbles:570-571}).</li>
 *   <li>GT6's {@code rng(..)} for the princess count is the tile entity's global
 *       {@code RNGSUS.nextInt(bound)}; the port uses {@link RandomSource#nextInt(int)} with the same
 *       bound.</li>
 * </ul>
 */
public final class BumbleBreeding {

    /**
     * What GT6's breeding step produced ({@code MultiTileEntityBumbliary:218-261}).
     *
     * @param offspring GT6's {@code mOffSpring}: {@code offspring + princessCount} bees, the first
     *                  {@code princessCount} of them princesses and the rest drones, each with its own
     *                  inherited genome
     * @param queen GT6's {@code bumbleCrown(tRoyalStack)} ({@code :261}): a queen of the princess's
     *              species that keeps the princess's genome
     * @param droneDead GT6's {@code bumbleKill} of the consumed drone ({@code :257-258}), killed by the
     *                  same amount as {@code droneCost}
     * @param lifeSpan GT6's {@code mLife = Util.getLifeSpan(tRoyalTag)} ({@code :254}): how long the
     *                 crowned queen may work
     * @param droneCost GT6's {@code tLoss} ({@code :256}): how many drones the machine has to remove
     * @param princessCount GT6's {@code tPrincessCount} ({@code :218}): {@code 1 + rng(5) / 2}, i.e. 1..3
     */
    public record Result(ItemStack[] offspring, ItemStack queen, ItemStack droneDead,
                         long lifeSpan, int droneCost, int princessCount) {}

    private BumbleBreeding() {}

    /**
     * GT6's drone pick ({@code MultiTileEntityBumbliary:200-213}).
     *
     * <p>GT6 first looks at its main drone slot and takes it whenever it holds a breedable drone
     * ({@code :202-203, bumbleType % 5 == 0}); only otherwise does it scan the spare drone slots
     * ({@code :205-212}). That scan keeps overwriting its choice - the last drone of the scan wins - but
     * it leaves the loop as soon as it finds a drone of the princess's species ({@code :210}), so an
     * equal-species drone always beats a later one of another species.</p>
     *
     * <p>Port difference: GT6 reads its seven fixed slots; the port is handed an array whose index 0
     * stands for GT6's main drone slot ({@code SLOT_DRONE}) and whose remaining entries stand for GT6's
     * spare slots ({@code SLOTS_DRONE}), so the answer is an index into that array and -1 means GT6
     * would have found no drone at all.</p>
     *
     * @param princess the royal bee that is about to breed, used for GT6's equal-species preference
     * @param droneSlots index 0 is GT6's main drone slot, the rest are its spare slots
     * @return the index of the drone to breed with, or -1 when no entry of the array holds a drone
     */
    public static int findDroneSlot(ItemStack princess, ItemStack[] droneSlots) {
        if (droneSlots == null || droneSlots.length == 0) return -1;
        if (isDrone(droneSlots[0])) return 0;
        int found = -1;
        for (int slot = 1; slot < droneSlots.length; slot++) {
            if (!isDrone(droneSlots[slot])) continue;
            found = slot;
            if (sameSpecies(princess, droneSlots[slot])) break;
        }
        return found;
    }

    /**
     * GT6's breeding step ({@code MultiTileEntityBumbliary:214-266}) with the drone treated as if it
     * came from the main drone slot, so a drone stack of more than one item costs two drones
     * ({@code :256}).
     *
     * @return null when GT6 would not breed: the first stack has to be a princess
     *         ({@code bumbleType % 5 == 1}, {@code :197}) and the second a drone
     *         ({@code bumbleType % 5 == 0}, {@code :207})
     */
    @Nullable
    public static Result breed(ItemStack princess, ItemStack drone, RandomSource random) {
        return breed(princess, drone, true, random);
    }

    /**
     * GT6's breeding step with GT6's own drone-slot condition for the cost.
     *
     * <p>This overload is a port addition for the machine: GT6 charges two drones only when
     * {@code tBreedSlot == SLOT_DRONE && tBreedStack.stackSize > 1} ({@code :256}), which the
     * stack-only signature of {@link #breed(ItemStack, ItemStack, RandomSource)} cannot express. A
     * caller that got its drone index from {@link #findDroneSlot} passes {@code slot == 0} here.</p>
     *
     * @param droneInMainSlot whether the drone came from GT6's main drone slot (index 0)
     */
    @Nullable
    public static Result breed(ItemStack princess, ItemStack drone, boolean droneInMainSlot, RandomSource random) {
        BumbleBeeType princessType = BumbleBeeType.of(princess);
        BumbleBeeType droneType = BumbleBeeType.of(drone);
        // GT6 :197 `bumbleType(tRoyalStack) % 5 == 1` and :207 `bumbleType(tDroneStack) % 5 == 0`. The
        // port's aliveVariant() folds the scanned metas 6 and 5 into princess and drone, like GT6's % 5.
        if (princessType == null || princessType.aliveVariant() != BumbleBeeType.PRINCESS) return null;
        if (droneType == null || droneType.aliveVariant() != BumbleBeeType.DRONE) return null;
        GTBumbleSpecies.Species princessSpecies = BumbleBeeType.speciesOf(princess);
        GTBumbleSpecies.Species droneSpecies = BumbleBeeType.speciesOf(drone);
        if (princessSpecies == null || droneSpecies == null) return null;

        // GT6 :116 `Util.getBumbleTag(tRoyalStack)` and :252 `getBumbleTag(tBreedStack)`: both parents
        // end up with a genome, generating and storing one when they have none.
        CompoundTag princessGenes = BumbleBeeGenes.of(princess, random);
        CompoundTag droneGenes = BumbleBeeGenes.of(drone, random);

        int princessCount = 1 + random.nextInt(5) / 2;                          // GT6 :218
        ItemStack[] offspring = new ItemStack[(int) BumbleBeeGenes.offspring(princessGenes) + princessCount];

        if (sameSpecies(princess, drone)) {
            // GT6 :222-231: children of the princess's species, with a mutation roll on each of them.
            for (int i = 0; i < offspring.length; i++) {
                ItemStack child = BumbleBeeType.stack(princessSpecies,
                        i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE, null, 1);
                if (!child.isEmpty() && random.nextInt(10000) < GTBumbleMutations.mutateChance(child)) {
                    child = GTBumbleMutations.mutate(child, random);
                }
                offspring[i] = child;
            }
        } else {
            // GT6 :232-250: one switch(rng(4)) per child, princesses and drones alike.
            for (int i = 0; i < offspring.length; i++) {
                BumbleBeeType type = i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE;
                offspring[i] = cross(princess, drone, princessSpecies, droneSpecies, type, random);
            }
        }

        // GT6 :252: every child gets its own genome, inherited gene by gene from both parents.
        for (ItemStack child : offspring) {
            if (child != null && !child.isEmpty()) {
                BumbleBeeGenes.with(child, BumbleBeeGenes.inherit(princess, drone, random));
            }
        }

        long lifeSpan = BumbleBeeGenes.lifeSpan(princessGenes);                 // GT6 :254
        int droneCost = droneInMainSlot && drone.getCount() > 1 ? 2 : 1;        // GT6 :256
        ItemStack droneDead = kill(droneSpecies, droneType, droneGenes, droneCost);
        ItemStack queen = crown(princessSpecies, princessType, princessGenes);
        return new Result(offspring, queen, droneDead, lifeSpan, droneCost, princessCount);
    }

    /**
     * GT6's cross-species child ({@code MultiTileEntityBumbliary:234-248}): a coin over the two parents
     * and GT6's combination table, in the order of GT6's {@code switch(rng(4))}.
     *
     * <p>Note that GT6 passes the princess first for the {@code bumbleCombine} in case 2 and the drone
     * first in case 3; for a pair the table lists, both directions answer the same species, but for an
     * unlisted pair GT6 falls back to the first parent, which is exactly what makes the switch a
     * 50/50 between the two parents there ({@link GTBumbleMutations#combineSpecies}).</p>
     */
    private static ItemStack cross(ItemStack princess, ItemStack drone, GTBumbleSpecies.Species princessSpecies,
                                   GTBumbleSpecies.Species droneSpecies, BumbleBeeType type, RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> BumbleBeeType.stack(princessSpecies, type, null, 1);
            case 1 -> BumbleBeeType.stack(droneSpecies, type, null, 1);
            case 2 -> GTBumbleMutations.combine(princess, drone, type, random);
            default -> GTBumbleMutations.combine(drone, princess, type, random);
        };
    }

    /**
     * GT6's {@code bumbleEqual} ({@code MultiItemBumbles:567, :210, :222}): whether two bees are of the
     * same species, whatever their types are.
     *
     * <p>Port difference: GT6 compares the item <em>and</em> {@code meta / 10}, which is the same species
     * because GT6 has a single item; the port has one item per (species, type), so only the species can
     * be compared here.</p>
     */
    public static boolean sameSpecies(ItemStack a, ItemStack b) {
        GTBumbleSpecies.Species speciesA = BumbleBeeType.speciesOf(a);
        return speciesA != null && speciesA == BumbleBeeType.speciesOf(b);
    }

    /** GT6's {@code bumbleType(stack) % 5 == 0}: a drone, scanned or not. */
    private static boolean isDrone(ItemStack stack) {
        BumbleBeeType type = BumbleBeeType.of(stack);
        return type != null && type.aliveVariant() == BumbleBeeType.DRONE;
    }

    /**
     * GT6's {@code bumbleKill} ({@code MultiItemBumbles:565}, {@code MultiTileEntityBumbliary:257}):
     * {@code (meta / 5) * 5 + 4}, which keeps the scan state - a scanned drone dies as a scanned dead
     * bee, everything else as a plain dead bee.
     *
     * <p>Port difference: an unbuildable scanned variant - a species or type the port does not register -
     * falls back to the plain {@link BumbleBeeType#DEAD} instead of leaving the machine without a dead
     * drone. All 320 scanned bee items are registered, so that fallback never fires today.</p>
     */
    private static ItemStack kill(GTBumbleSpecies.Species species, BumbleBeeType droneType,
                                  CompoundTag genes, int count) {
        ItemStack dead = BumbleBeeType.stack(species, droneType.deadVariant(), genes, count);
        return dead.isEmpty() ? BumbleBeeType.stack(species, BumbleBeeType.DEAD, genes, count) : dead;
    }

    /**
     * GT6's {@code bumbleCrown} ({@code MultiItemBumbles:566}, {@code MultiTileEntityBumbliary:261}):
     * {@code (meta / 5) * 5 + 2}, the queen of the same species that keeps the scan state - a scanned
     * princess is crowned to a scanned queen.
     *
     * <p>Port difference: as in {@link #kill}, an unbuildable scanned queen falls back to the plain
     * {@link BumbleBeeType#QUEEN}, which never fires while all 320 scanned bee items are registered.</p>
     */
    private static ItemStack crown(GTBumbleSpecies.Species species, BumbleBeeType princessType,
                                   CompoundTag genes) {
        ItemStack queen = BumbleBeeType.stack(species,
                princessType.scanned() ? BumbleBeeType.SCANNED_QUEEN : BumbleBeeType.QUEEN, genes, 1);
        return queen.isEmpty() ? BumbleBeeType.stack(species, BumbleBeeType.QUEEN, genes, 1) : queen;
    }
}
