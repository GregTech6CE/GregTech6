package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.block.tool.BumbliaryBlock;
import com.gregtech.gregtech.blockentity.machine.SmelteryBlockEntityHelper;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.BumbleBreeding;
import com.gregtech.gregtech.content.bumble.BumbleWorkplace;
import com.gregtech.gregtech.content.bumble.GTBumbleProducts;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's bumbliary ({@code MultiTileEntityBumbliary}, {@code gregtech/tileentity/tools}) and its
 * advanced variant ({@code MultiTileEntityBumbliaryAdvanced}, GT6 id 32007): one princess and one
 * drone go in, a queen works for a while, combs and a brood come out.
 *
 * <p>The machine itself is nothing but GT6's {@code onTick2} ({@code :105-279}, the advanced one
 * {@code :105-279} as well) wrapped around an inventory, which is why every step below names the
 * original line. The bee maths lives in {@link BumbleBreeding}, {@link BumbleBeeGenes},
 * {@link GTBumbleSpecies}, {@link GTBumbleMutations} and {@link GTBumbleProducts}.</p>
 *
 * <h2>Inventory ({@code :333-343}, advanced {@code :333-344})</h2>
 * <p>36 slots with GT6's six groups: {@link #SLOT_ROYAL} holds the breeding queen or the waiting
 * princess, {@link #SLOT_DRONE} the preferred drone, {@link #SLOTS_COMBS} the combs a working queen
 * produces, {@link #SLOTS_DRONE} the spare drone slots the brood and the promoted princess come from,
 * and {@link #SLOTS_DEAD} the dead bees. The advanced machine uses 20 slots instead
 * ({@link #ADVANCED_LAYOUT}).</p>
 *
 * <p>The GUI rules are GT6's: only a living princess may be put into the royal slot
 * ({@code isItemValidForSlotGUI:357-364}) and only a drone into the main drone slot, nothing may be
 * put into the comb, spare-drone and dead slots, the spare drone slots cannot be emptied by the
 * player at all ({@code MultiTileEntityBumbliary:399-421}, {@code setCanTake(F)}), the royal slot can
 * only be emptied while it does not hold a live queen ({@code canTakeOutOfSlotGUI:367-369}) and the
 * royal slot takes exactly one item ({@code getInventoryStackLimitGUI:341}).</p>
 *
 * <p>Automation sees the dead slots only, on every side: {@code getAccessibleSlotsFromSide2:340}
 * returns {@link #SLOTS_DEAD}, {@code canInsertItem2:342} is false and {@code canExtractItem2:343}
 * allows the extraction of {@code aSlot >= 27} - the dead bees. The advanced machine exposes
 * {@link #ADV_SLOTS_AUTO} instead ({@code MultiTileEntityBumbliaryAdvanced:341-344}), i.e. its combs
 * and its dead bees.</p>
 *
 * <h2>What runs per tick ({@code :105-279})</h2>
 * <ol>
 *   <li>Every 1200 ticks - and once on load - the environment is refreshed: the sky flag comes from
 *       GT6's {@code getRainAtSide} over the five non-bottom sides ({@code :97, :109}), the
 *       temperature from {@link SmelteryBlockEntityHelper#environmentTemperature} (GT6's
 *       {@code WD.envTemp:404-406}) and the humidity from the biome's rainfall ({@code :99, :111}).</li>
 *   <li>A queen with {@code life > 0} in the royal slot breeds nothing but works: the countdown is
 *       pinned to 1200 ({@code :119}), and while {@link #checkEnvironment} accepts the place the
 *       machine counts the queen's life down one tick at a time ({@code :121}), hands out a comb every
 *       {@code life % 1200 == 600} ({@code :168-186}) and attacks everything in a 9x9x9 box every
 *       {@code life % 300 == 150} ({@code :165-167}, see the port differences). A queen whose genome
 *       does not fit the bumbliary dies immediately ({@code :188-192}).</li>
 *   <li>When the queen's life reaches zero the machine kills the queen and every bee in the spare drone
 *       slots ({@code :124-133}), moves the stored brood into the drone slots - or kills a child whose
 *       genome no longer fits the place ({@code :135-143}) - and promotes the most aggressive princess
 *       of the spare drone slots into the royal slot ({@code :147-163}).</li>
 *   <li>A princess waits for its countdown ({@code :198-199}), then picks a drone
 *       ({@code BumbleBreeding.findDroneSlot}), breeds ({@code :214-266}), stores the brood
 *       ({@code mOffSpring}, {@code :220}), sets its life to the princess's lifespan gene
 *       ({@code :254}), eats the drone ({@code :256-259}), crowns the queen ({@code :261}) and kills
 *       every non-drone bee the spare slots may still hold ({@code :263-266}).</li>
 *   <li>An empty royal slot resets life, brood and countdown ({@code :273-277}).</li>
 * </ol>
 *
 * <h2>Port differences to GT6</h2>
 * <ul>
 *   <li><b>Stings use the shared species rules.</b> The normal/advanced attack boxes are
 *       9x9x9/5x5x5; insect protection and GT6 species immunity are checked by
 *       {@link com.gregtech.gregtech.content.bumble.BumbleSting}.</li>
 *   <li><b>The working-place scan is implemented for registered blocks.</b> GT6 calls
 *       {@code bumbleCanProduce} ({@code MultiItemBumbles:216-349}) before it rolls a product. The
 *       port checks the same species categories within three blocks (one for the advanced machine),
 *       via {@link BumbleWorkplace}. GT6's optional-mod plants without a modern registered counterpart
 *       are not fabricated. {@code bumbleCanProduct} ({@code :497-499}) always answers the machine's
 *       own position, so it needs no additional product-specific check.</li>
 *   <li><b>Scoop access is a synced menu mode.</b> A scoop or creative player opens
 *       GT6's alternate take permissions; slot insertion and live-queen restrictions remain.</li>
 *   <li><b>The cover and paint system has no equivalent</b> ({@code allowCovers:344},
 *       {@code getTexture2:327}), and neither has {@code ITileEntityRunningSuccessfully}
 *       ({@code :351-354}); the port keeps GT6's four states as {@link #life()},
 *       {@link #countdown()} and {@link #endedQueen()} for the GUI and the tests.</li>
 *   <li><b>GT6's {@code rng(..)} is one global RNG</b> ({@code RNGSUS}); the port draws from
 *       {@link Level#random}, the closest single source a block entity can reach, and hands the same
 *       source to {@link BumbleBreeding} (GT6 mixes its tile entity RNG with {@code RNGSUS} there).</li>
 *   <li><b>The NBT keys are GT6's own</b> ({@code CS:1175,1230,1251}): {@code gt.progress} is
 *       {@code mLife}, {@code gt.cooldown} is {@code mBreedingCountDown} and {@code gt.invout} is the
 *       brood list with {@code gt.invout.<i>} entries ({@code :66-85}). The inventory is stored under
 *       {@code inventory} like every other port container.</li>
 *   <li><b>The sky flag latches.</b> GT6 only ever sets {@code mSky} to true ({@code :97, :109}) and
 *       never clears it, so a bumbliary that has seen rain from above once keeps working as an
 *       "outside" machine; the port keeps that behaviour on purpose.</li>
 * </ul>
 */
public class BumbliaryBlockEntity extends BlockEntity implements MenuProvider,
        com.gregtech.gregtech.api.inventory.BlockContents {

    // ── GT6's NBT keys (CS.java:1175, 1230, 1251) ───────────────────────────────────────────────
    /** GT6's {@code NBT_PROGRESS}: the queen's remaining life. */
    public static final String NBT_PROGRESS = "gt.progress";
    /** GT6's {@code NBT_COOLDOWN}: the breeding countdown. */
    public static final String NBT_COOLDOWN = "gt.cooldown";
    /** GT6's {@code NBT_INV_OUT}: the pending brood, one {@code gt.invout.<i>} per child. */
    public static final String NBT_INV_OUT = "gt.invout";
    /** The port's own key for the container (every port block entity stores its inventory here). */
    public static final String NBT_INVENTORY = "inventory";

    /** GT6's countdown of a working breeder and of a machine without a royal bee ({@code :119, :270, :276}). */
    public static final int COUNTDOWN = 1200;
    /** GT6's countdown while a princess has no drone to pair with ({@code :199}). */
    public static final int COUNTDOWN_PAIRING = 600;
    /** GT6's penalty for opening the machine ({@code :289, :307}): five minutes without breeding. */
    public static final int COUNTDOWN_OPEN_PENALTY = 6000;

    /** GT6's {@code onTick2:108} / {@code onTickFirst2:95}: how often the environment is re-read. */
    public static final int ENVIRONMENT_INTERVAL = 1200;

    private static final ItemStack[] NO_BROOD = new ItemStack[0];
    private static final Direction[] SIDES_BUT_BOTTOM = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * GT6's slot groups, so the standard and the advanced machine share one implementation
     * ({@code MultiTileEntityBumbliary:333-337} and {@code MultiTileEntityBumbliaryAdvanced:333-337}).
     */
    public static final class Layout {
        private final boolean advanced;
        private final int slots, royal, drone, productRoll;
        private final int[] combs, drones, dead, accessible;

        public Layout(boolean advanced, int slots, int royal, int drone, int[] combs, int[] drones,
                      int[] dead, int[] accessible, int productRoll) {
            this.advanced = advanced;
            this.slots = slots;
            this.royal = royal;
            this.drone = drone;
            this.combs = combs;
            this.drones = drones;
            this.dead = dead;
            this.accessible = accessible;
            this.productRoll = productRoll;
        }

        /** The advanced machine (GT6 id 32007) or the standard one (GT6 id 32741). */
        public boolean advanced() { return advanced; }
        /** Total inventory size ({@code getDefaultInventory:339}, advanced {@code :340}). */
        public int slots() { return slots; }
        /** GT6's {@code SLOT_ROYAL}. */
        public int royal() { return royal; }
        /** GT6's {@code SLOT_DRONE}, the preferred drone slot. */
        public int drone() { return drone; }
        /** GT6's {@code SLOTS_COMBS}. */
        public int[] combs() { return combs.clone(); }
        /** GT6's {@code SLOTS_DRONE}, the spare drone slots. */
        public int[] drones() { return drones.clone(); }
        /** GT6's {@code SLOTS_DEAD}. */
        public int[] dead() { return dead.clone(); }
        /** What automation may reach: {@code getAccessibleSlotsFromSide2}. */
        public int[] accessible() { return accessible.clone(); }
        /** The product roll bound: {@code rng(10000)} or the advanced {@code rng(20000)}. */
        public int productRoll() { return productRoll; }
    }

    /** GT6's {@code SLOT_ROYAL}: one princess goes in, one queen comes out. */
    public static final int SLOT_ROYAL = 13;
    /** GT6's {@code SLOT_DRONE}: the drone the princess prefers over the spare slots. */
    public static final int SLOT_DRONE = 22;
    /** GT6's {@code SLOTS_COMBS}: what a working queen produces into ({@code :334}). */
    public static final int[] SLOTS_COMBS = {0, 1, 2, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 24, 25, 26};
    /** GT6's {@code SLOTS_DRONE}: the spare drone slots, which keep the brood and the princesses. */
    public static final int[] SLOTS_DRONE = {3, 4, 5, 12, 14, 21, 23};
    /** GT6's {@code SLOTS_DEAD}: the output of every dead bee, and the only slots automation sees. */
    public static final int[] SLOTS_DEAD = {27, 28, 29, 30, 31, 32, 33, 34, 35};

    /** GT6's standard bumbliary ({@code MultiTileEntityBumbliary:333-343}). */
    public static final Layout LAYOUT = new Layout(false, 36, SLOT_ROYAL, SLOT_DRONE,
            SLOTS_COMBS, SLOTS_DRONE, SLOTS_DEAD, SLOTS_DEAD, GTBumbleProducts.ROLL);

    /** GT6's {@code MultiTileEntityBumbliaryAdvanced} slot groups ({@code :333-337}). */
    public static final int ADV_SLOT_ROYAL = 7, ADV_SLOT_DRONE = 12;
    public static final int[] ADV_SLOTS_COMBS = {0, 4, 5, 9, 10, 11, 14};
    public static final int[] ADV_SLOTS_DRONE = {1, 2, 3, 6, 8, 11, 13};
    public static final int[] ADV_SLOTS_DEAD = {15, 16, 17, 18, 19};
    public static final int[] ADV_SLOTS_AUTO = {0, 4, 5, 9, 10, 11, 14, 15, 16, 17, 18, 19};

    /**
     * GT6's advanced bumbliary ({@code MultiTileEntityBumbliaryAdvanced:333-344}). Note that GT6 lists
     * slot 11 in both {@code SLOTS_COMBS} and {@code SLOTS_DRONE}; the port keeps that overlap exactly
     * as the original has it.
     */
    public static final Layout ADVANCED_LAYOUT = new Layout(true, 20, ADV_SLOT_ROYAL, ADV_SLOT_DRONE,
            ADV_SLOTS_COMBS, ADV_SLOTS_DRONE, ADV_SLOTS_DEAD, ADV_SLOTS_AUTO, GTBumbleProducts.ADVANCED_ROLL);

    private final Layout layout;
    private final ItemStackHandler inventory;
    private LazyOptional<IItemHandler> automation;

    /** GT6's {@code mOffSpring}: the brood the dead queen hands out ({@code :63, :220, :145}). */
    private ItemStack[] brood = NO_BROOD;
    /** GT6's {@code mLife}: the queen's remaining life, counted down one tick at a time ({@code :61}). */
    private long life;
    /** GT6's {@code mBreedingCountDown} ({@code :61}). */
    private long countdown = COUNTDOWN;
    /** GT6's {@code mSky}: the machine has been rained on from above ({@code :60}). */
    private boolean sky;
    /** GT6's {@code mTemperature} ({@code :61}), seeded with GT6's default environment temperature. */
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    /** GT6's {@code mHumidity} ({@code :62}). */
    private float humidity = 1.0F;
    /** GT6's {@code mEndedQueen}, true for the one tick a queen dies on ({@code :60, :122}). */
    private boolean endedQueen;
    /** GT6 refreshes the environment on the first tick ({@code onTickFirst2:95-101}). */
    private boolean environmentKnown;

    public BumbliaryBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.BUMBLIARY.get(), pos, state);
        this.layout = state.getBlock() instanceof BumbliaryBlock block && block.advanced()
                ? ADVANCED_LAYOUT : LAYOUT;
        this.inventory = new ItemStackHandler(layout.slots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BumbliaryBlockEntity.this.isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                // GT6's getInventoryStackLimitGUI (:341): the royal slot holds one bee.
                return slot == layout.royal() ? 1 : 64;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.automation = LazyOptional.of(() -> new AutomationHandler());
    }

    /** The slot groups this machine uses (GT6 decides by its class, the port by its block). */
    public Layout layout() { return layout; }

    /** The inventory, 36 slots (or 20 for the advanced machine). */
    public ItemStackHandler inventory() { return inventory; }

    public long life() { return life; }
    public long countdown() { return countdown; }
    public boolean endedQueen() { return endedQueen; }
    public boolean sky() { return sky; }
    public long temperature() { return temperature; }
    public float humidity() { return humidity; }
    /** The pending brood, a copy so a caller cannot change what the machine still owes ({@code :63}). */
    public ItemStack[] brood() { return brood.clone(); }

    // ── environment (GT6 onTickFirst2:95-101 and the 1200-tick refresh :108-112) ────────────────

    /**
     * GT6's {@code onTick2} ({@code :106-279}): the whole machine, server side only.
     *
     * <p>This is the entry point for the block ticker; the game tests drive {@link #tickLogic()}
     * directly so they do not have to wait for game ticks.</p>
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (!environmentKnown) {
            refreshEnvironment();
            environmentKnown = true;
        } else if (level.getGameTime() % ENVIRONMENT_INTERVAL == 0) {
            refreshEnvironment();
        }
        tickLogic();
    }

    /**
     * Re-reads the place the machine stands in ({@code onTickFirst2:95-101}, {@code :108-112}).
     *
     * <p>Port difference: GT6 latches {@code mSky} - it only ever sets it to true ({@code :97, :109})
     * and never clears it - so the flag means "this bumbliary has seen the sky rain on it", not "it is
     * raining right now". The port keeps that.</p>
     */
    public void refreshEnvironment() {
        if (level == null) return;
        for (Direction side : SIDES_BUT_BOTTOM) {
            if (rainAtSide(side)) {
                sky = true;
                break;
            }
        }
        temperature = SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
        humidity = level.getBiome(worldPosition).value().getModifiedClimateSettings().downfall();
    }

    /**
     * GT6's {@code getRain(x, y, z)} ({@code TileEntityBase01Root:266-271}) as the sides of the
     * machine see it: no sky in the dimension means no rain, otherwise the neighbour is "in the rain"
     * when it sits at or above the height the precipitation stops at.
     */
    private boolean rainAtSide(Direction side) {
        if (level == null) return true;                                     // GT6 :267
        if (!level.dimensionType().hasSkyLight()) return false;             // GT6 :268 provider.hasNoSky
        BlockPos neighbour = worldPosition.relative(side);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, neighbour.getX(), neighbour.getZ())
                <= neighbour.getY();
    }

    // ── the machine (GT6 onTick2:105-279) ───────────────────────────────────────────────────────

    /** GT6's {@code onTick2} body ({@code :107-277}), one tick of the bumbliary. */
    public void tickLogic() {
        if (level == null || level.isClientSide) return;
        endedQueen = false;                                                     // GT6 :107

        ItemStack royal = inventory.getStackInSlot(layout.royal());
        BumbleBeeType royalType = BumbleBeeType.of(royal);
        if (!royal.isEmpty() && royalType != null) {                            // GT6 :113
            // GT6 :116 `Util.getBumbleTag(tRoyalStack)`: a bee without a genome gets one here.
            CompoundTag royalGenes = BumbleBeeGenes.of(royal, level.random);
            if (life > 0 && royalType.aliveVariant() == BumbleBeeType.QUEEN) {   // GT6 :118, `% 5 == 2`
                if (layout.advanced()) {
                    if (countdown < COUNTDOWN) countdown = COUNTDOWN;           // ADV :119
                } else {
                    countdown = COUNTDOWN;                                      // GT6 :119
                }
                if (checkEnvironment(royalGenes)) {                             // GT6 :120
                    if (--life <= 0) {                                          // GT6 :121
                        endQueen();
                    } else {
                        if (life % 300 == 150 && level.random.nextInt(10000)
                                < BumbleBeeGenes.aggressiveness(royalGenes)) {
                            int radius = layout.advanced() ? 2 : 4;
                            var area = new net.minecraft.world.phys.AABB(worldPosition).inflate(radius);
                            for (var target : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
                                attackEntity(target);
                            }
                        }
                        produceComb(royal, royalGenes);                         // GT6 :168-186
                    }
                } else {
                    // GT6 :188-192: a queen whose genome does not fit this place dies right away.
                    ItemStack dead = kill(royal);
                    inventory.setStackInSlot(layout.royal(), ItemStack.EMPTY);
                    addToDeadSlots(dead);
                }
            } else {
                // GT6 :193-272: no working queen - a princess waits for a drone, everything else idles.
                life = 0;                                                       // GT6 :194
                brood = NO_BROOD;                                               // GT6 :195
                if (royalType.aliveVariant() == BumbleBeeType.PRINCESS) {        // GT6 :197, `% 5 == 1`
                    if (--countdown <= 0) {                                     // GT6 :198
                        countdown = COUNTDOWN_PAIRING;                          // GT6 :199
                        tryBreed(royal);
                    }
                } else {
                    countdown = COUNTDOWN;                                      // GT6 :270
                }
            }
        } else {
            life = 0;                                                           // GT6 :274
            brood = NO_BROOD;                                                   // GT6 :275
            countdown = COUNTDOWN;                                              // GT6 :276
        }
    }

    /**
     * GT6's breeding step ({@code :200-266}) with {@link BumbleBreeding} doing the maths.
     *
     * <p>The drone slots are handed over in GT6's own order: index 0 is the main drone slot
     * ({@code :202-203}) and the rest are {@link #SLOTS_DRONE} in order ({@code :205-212}), which is
     * exactly what {@link BumbleBreeding#findDroneSlot} expects.</p>
     */
    private void tryBreed(ItemStack royal) {
        int[] spare = layout.drones();
        ItemStack[] droneSlots = new ItemStack[spare.length + 1];
        droneSlots[0] = inventory.getStackInSlot(layout.drone());
        for (int i = 0; i < spare.length; i++) droneSlots[i + 1] = inventory.getStackInSlot(spare[i]);

        int index = BumbleBreeding.findDroneSlot(royal, droneSlots);            // GT6 :200-213
        if (index < 0) return;
        int slot = index == 0 ? layout.drone() : spare[index - 1];

        BumbleBreeding.Result result = BumbleBreeding.breed(royal, droneSlots[index],
                index == 0, level.random);                                      // GT6 :214-252
        if (result == null) return;                                             // the scan proved it is a drone

        countdown = COUNTDOWN;                                                  // GT6 :215
        brood = result.offspring();                                             // GT6 :220 mOffSpring
        life = result.lifeSpan();                                               // GT6 :254
        addToDeadSlots(result.droneDead());                                     // GT6 :257-258
        shrink(slot, result.droneCost());                                       // GT6 :259 decrStackSize
        inventory.setStackInSlot(layout.royal(), single(result.queen()));       // GT6 :261 ST.amount(1, crown)

        for (int droneSlot : spare) {                                           // GT6 :263-266
            ItemStack found = inventory.getStackInSlot(droneSlot);
            if (found.isEmpty() || BumbleBeeType.of(found) == null) continue;
            if (BumbleBeeType.of(found).aliveVariant() == BumbleBeeType.DRONE) continue;
            inventory.setStackInSlot(droneSlot, ItemStack.EMPTY);
            addToDeadSlots(kill(found));
        }
    }

    /**
     * GT6's queen death ({@code :121-163}): the queen, every bee of the spare drone slots and the
     * children that no longer fit the environment die, the brood moves into the drone slots and the
     * most aggressive princess becomes the new queen.
     *
     * <p>Note that GT6 only ever looks at {@link #SLOTS_DRONE} here - never at the main drone slot -
     * so a drone parked in {@link #SLOT_DRONE} survives a queen's death and cannot be promoted.</p>
     */
    private void endQueen() {
        endedQueen = true;                                                      // GT6 :122

        for (int slot : layout.dead()) {                                        // GT6 :124
            ItemStack found = inventory.getStackInSlot(slot);
            if (BumbleBeeType.of(found) != null) inventory.setStackInSlot(slot, kill(found));
        }

        ItemStack queen = inventory.getStackInSlot(layout.royal());             // GT6 :126-128
        inventory.setStackInSlot(layout.royal(), ItemStack.EMPTY);
        addToDeadSlots(kill(queen));

        for (int slot : layout.drones()) {                                      // GT6 :130-133
            ItemStack found = inventory.getStackInSlot(slot);
            if (found.isEmpty() || BumbleBeeType.of(found) == null) continue;
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            addToDeadSlots(kill(found));
        }

        for (ItemStack child : brood) {                                         // GT6 :135-143
            if (child == null || child.isEmpty() || BumbleBeeType.of(child) == null) continue;
            if (checkEnvironment(BumbleBeeGenes.of(child, level.random))) {
                if (!addStackToSlot(layout.drone(), child)) {
                    for (int slot : layout.drones()) if (addStackToSlot(slot, child)) break;
                }
            } else {
                addToDeadSlots(kill(child));
            }
        }

        brood = NO_BROOD;                                                       // GT6 :145

        int princessSlot = -1;                                                  // GT6 :147-163
        for (int slot : layout.drones()) {
            ItemStack candidate = inventory.getStackInSlot(slot);
            BumbleBeeType type = BumbleBeeType.of(candidate);
            if (type == null || type.aliveVariant() != BumbleBeeType.PRINCESS) continue;
            if (princessSlot < 0 || aggressiveness(candidate) > aggressiveness(inventory.getStackInSlot(princessSlot))) {
                princessSlot = slot;
            }
        }
        if (princessSlot >= 0) {
            inventory.setStackInSlot(layout.royal(), single(inventory.getStackInSlot(princessSlot))); // :161
            shrink(princessSlot, 1);                                            // GT6 :162
        }
    }

    /** GT6's comb production ({@code :168-186}). */
    private void produceComb(ItemStack royal, CompoundTag royalGenes) {
        if (life % 1200 != 600) return;                                         // GT6 :168
        if (level.random.nextInt(GTBumbleProducts.ROLL) >= (int) BumbleBeeGenes.workForce(royalGenes)) return;
        if (!checkWork(royalGenes)) return;
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(royal);
        if (species == null) return;
        if (!BumbleWorkplace.hasWorkplace(level, worldPosition, species.id(), layout.advanced())) return;

        for (int i = 0, products = GTBumbleProducts.count(); i < products; i++) {   // GT6 :170
            if (level.random.nextInt(layout.productRoll())
                    >= GTBumbleProducts.chance(species.id())) continue;         // GT6 :171
            ItemStack product = GTBumbleProducts.stack(species.id(), 1);        // GT6 :173
            if (!product.isEmpty()) addProductToCombSlots(product);              // GT6 :174-181
        }
    }

    /**
     * GT6's comb placement ({@code :174-181}): a comb slot that already holds the same product is
     * filled first, otherwise the first comb slot that takes it wins; a product that fits nowhere is
     * lost, exactly like in GT6.
     */
    private void addProductToCombSlots(ItemStack product) {
        for (int slot : layout.combs()) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, product)) continue;
            if (addStackToSlot(slot, product)) return;
        }
        for (int slot : layout.combs()) if (addStackToSlot(slot, product)) return;
    }

    // ── GT6's environment checks (:375-385) ─────────────────────────────────────────────────────

    /**
     * GT6's {@code checkEnvironment} ({@code :375-377}): the genome has to accept the temperature, the
     * humidity and the inside/outside side of the machine.
     */
    private boolean checkEnvironment(CompoundTag genes) {
        return BumbleBeeGenes.temperatureMatches(genes, temperature)
                && BumbleBeeGenes.humidityMatches(genes, humidity)
                && (sky ? BumbleBeeGenes.outsideActive(genes) : BumbleBeeGenes.insideActive(genes));
    }

    /**
     * GT6's {@code checkWork} ({@code :379-385}): a machine in the rain needs a rainproof bee, one in
     * a thunderstorm a stormproof bee, and the time of day has to match the bee's activity.
     */
    private boolean checkWork(CompoundTag genes) {
        if (sky) {
            if (level.isThundering() && !BumbleBeeGenes.stormproof(genes)) return false;
            if (level.isRaining() && humidity > 0 && !BumbleBeeGenes.rainproof(genes)) return false;
        }
        return level.isDay() ? BumbleBeeGenes.dayActive(genes) : BumbleBeeGenes.nightActive(genes);
    }

    // ── slot rules (GT6 :340-369) ───────────────────────────────────────────────────────────────

    /** GT6's {@code isItemValidForSlotGUI} ({@code :357-364}): princesses and drones, nothing else. */
    private boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        BumbleBeeType type = BumbleBeeType.of(stack);
        if (type == null) return false;                                         // GT6 :358 IItemBumbleBee
        if (slot == layout.drone()) return type.aliveVariant() == BumbleBeeType.DRONE;      // :360 `% 5 == 0`
        if (slot == layout.royal()) return type.aliveVariant() == BumbleBeeType.PRINCESS;   // :361 `% 5 == 1`
        return false;                                                           // :362 everything else
    }

    /**
     * GT6's {@code canTakeOutOfSlotGUI} ({@code :367-369}) for one slot of this machine: everything may
     * leave except a royal bee that is still alive - the queen has to die first.
     */
    public boolean canTakeOutOfSlot(int slot) {
        return slot != layout.royal() || canLeaveRoyalSlot(inventory.getStackInSlot(slot));
    }

    /**
     * The stack half of GT6's {@code canTakeOutOfSlotGUI} ({@code :367-369}): only a live queen may not
     * leave the royal slot ({@code bumbleType % 5 == 2}). The client copy of the container has no
     * machine to ask, so it derives the same answer from the synced stack through this method.
     */
    public static boolean canLeaveRoyalSlot(ItemStack stack) {
        BumbleBeeType type = BumbleBeeType.of(stack);
        return type == null || type.aliveVariant() != BumbleBeeType.QUEEN;
    }

    // ── GT6's inventory helpers ─────────────────────────────────────────────────────────────────

    /**
     * GT6's {@code addStackToSlot} ({@code TileEntityBase05Inventories:173-188}): all or nothing into
     * one slot - an empty slot takes the whole stack, an equal one takes it while it fits.
     */
    private boolean addStackToSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;                      // GT6 :174
        if (slot < 0 || slot >= inventory.getSlots()) return false;             // GT6 :175
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {                                               // GT6 :177-180
            inventory.setStackInSlot(slot, stack.copy());
            return true;
        }
        int limit = Math.min(Math.max(1, existing.getMaxStackSize()), inventory.getSlotLimit(slot));
        if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() + stack.getCount() <= limit) {
            inventory.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + stack.getCount()));
            return true;
        }
        return false;                                                           // GT6 :187
    }

    /** GT6's dead-slot loop: the first slot that takes the stack wins, otherwise the stack is lost. */
    private void addToDeadSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (int slot : layout.dead()) if (addStackToSlot(slot, stack)) return;
    }

    /** GT6's {@code decrStackSize} ({@code :259, :162}). */
    private void shrink(int slot, int amount) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || amount <= 0) return;
        int left = stack.getCount() - amount;
        inventory.setStackInSlot(slot, left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left));
    }

    /** One item of a stack, GT6's {@code ST.amount(1, stack)} ({@code :161, :261}). */
    private static ItemStack single(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    /** GT6's {@code Util.getAggressiveness(Util.getBumbleTag(stack))} ({@code :154}). */
    private long aggressiveness(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return BumbleBeeGenes.aggressiveness(BumbleBeeGenes.of(stack, level.random));
    }

    /**
     * GT6's {@code bumbleKill} ({@code MultiItemBumbles:565}): {@code (meta / 5) * 5 + 4}, i.e. the dead
     * bee of the same species that keeps the scan state, the stack size and the genome.
     */
    private ItemStack kill(ItemStack bee) {
        BumbleBeeType type = BumbleBeeType.of(bee);
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(bee);
        if (type == null || species == null) return bee.copy();
        ItemStack dead = BumbleBeeType.stack(species, type.deadVariant(), BumbleBeeGenes.peek(bee), bee.getCount());
        return dead.isEmpty() ? bee.copy() : dead;
    }

    // ── automation (GT6 getAccessibleSlotsFromSide2:340-343) ────────────────────────────────────

    /**
     * GT6's sided inventory: {@code getAccessibleSlotsFromSide2} lists {@link #SLOTS_DEAD} and
     * {@code canExtractItem2} allows {@code aSlot >= 27} while {@code canInsertItem2} is false, so
     * automation may only take the dead bees out.
     */
    private final class AutomationHandler implements IItemHandler {
        @Override public int getSlots() { return layout.accessible().length; }
        @Override public ItemStack getStackInSlot(int slot) {
            int index = mapped(slot);
            return index < 0 ? ItemStack.EMPTY : inventory.getStackInSlot(index);
        }
        @Override public int getSlotLimit(int slot) {
            int index = mapped(slot);
            return index < 0 ? 0 : inventory.getSlotLimit(index);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }   // GT6 :342
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;                                                       // GT6 :342 canInsertItem2
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int index = mapped(slot);
            if (index < 0) return ItemStack.EMPTY;
            return inventory.extractItem(index, amount, simulate);              // GT6 :343 canExtractItem2
        }

        private int mapped(int slot) {
            int[] accessible = layout.accessible();
            return slot < 0 || slot >= accessible.length ? -1 : accessible[slot];
        }
    }

    // ── NBT (GT6 readFromNBT2:66-85) ────────────────────────────────────────────────────────────

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_INVENTORY)) inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        if (tag.contains(NBT_PROGRESS)) life = tag.getLong(NBT_PROGRESS);
        if (tag.contains(NBT_COOLDOWN)) countdown = tag.getLong(NBT_COOLDOWN);
        if (tag.contains(NBT_INV_OUT)) {
            int size = tag.getInt(NBT_INV_OUT);
            brood = new ItemStack[Math.max(0, size)];
            for (int i = 0; i < brood.length; i++) {
                brood[i] = tag.contains(NBT_INV_OUT + "." + i)
                        ? ItemStack.of(tag.getCompound(NBT_INV_OUT + "." + i)) : ItemStack.EMPTY;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putLong(NBT_PROGRESS, life);
        tag.putLong(NBT_COOLDOWN, countdown);
        if (brood.length > 0) {                                                 // GT6 :81-84
            tag.putInt(NBT_INV_OUT, brood.length);
            for (int i = 0; i < brood.length; i++) {
                if (brood[i] != null && !brood[i].isEmpty()) {
                    tag.put(NBT_INV_OUT + "." + i, brood[i].save(new CompoundTag()));
                }
            }
        }
    }

    // ── contents, break and menu ────────────────────────────────────────────────────────────────

    /**
     * GT6's {@code breakDrop} ({@code :346}): while the machine is running ({@code mLife > 0}) every
     * bee it holds dies as it is handed out, otherwise the bees drop as they are.
     */
    public List<ItemStack> breakDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {               // GT6 canDrop:345
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            drops.add(life > 0 ? kill(stack) : stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return drops;
    }

    /**
     * GT6's {@code breakBlock} ({@code TileEntityBase05Inventories:152-171}): the machine hands its
     * whole inventory out when it is destroyed, whatever destroys it.
     */
    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : breakDrops()) com.gregtech.gregtech.api.inventory.BlockContents.drop(this, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(layout.advanced()
                ? "block.gregtech.advanced_bumbliary" : "block.gregtech.bumbliary");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.gregtech.gregtech.client.gui.BumbliaryContainerMenu(containerId, playerInventory, this, player.isCreative());
    }

    /** GT6 :371-372: only a living, currently working queen defends the machine. */
    public boolean attackEntity(net.minecraft.world.entity.LivingEntity target) {
        ItemStack royal = inventory.getStackInSlot(layout.royal());
        BumbleBeeType type = BumbleBeeType.of(royal);
        return life > 0 && type != null && type.aliveVariant() == BumbleBeeType.QUEEN
                && com.gregtech.gregtech.content.bumble.BumbleSting.attack(royal, target);
    }

    /** GT6's {@code onBlockActivated3:289} / {@code onToolClick2:307}: opening costs five minutes. */
    public void onOpened() {
        countdown = COUNTDOWN_OPEN_PENALTY;
        setChanged();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automation.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        automation = LazyOptional.of(() -> new AutomationHandler());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return automation.cast();
        return super.getCapability(capability, side);
    }
}
