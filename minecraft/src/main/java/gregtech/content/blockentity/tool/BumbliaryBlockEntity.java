package gregtech.content.blockentity.tool;

import gregtech.content.block.tool.BumbliaryBlock;
import gregtech.content.blockentity.machine.SmelteryBlockEntityHelper;
import gregtech.content.bumble.BumbleBeeGenes;
import gregtech.content.bumble.BumbleBeeType;
import gregtech.content.bumble.BumbleBreeding;
import gregtech.content.bumble.BumbleWorkplace;
import gregtech.content.bumble.GTBumbleProducts;
import gregtech.content.bumble.GTBumbleSpecies;
import gregtech.content.data.GregTechConstants;
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
//? if >=1.21.1 {



/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's bumbliary ({@code MultiTileEntityBumbliary}, {@code gregtech/tileentity/tools}) and its
 * advanced variant ({@code MultiTileEntityBumbliaryAdvanced}, GT6 id 32007): one princess and one
 * drone go in, a queen works for a while, combs and a brood come out.
 *
 * <p>The machine itself is nothing but GT6's {@code onTick2} (the advanced variant has its own
 * copy) wrapped around an inventory. The bee maths lives in {@link BumbleBreeding},
 * {@link BumbleBeeGenes}, {@link GTBumbleSpecies}, {@link GTBumbleMutations} and
 * {@link GTBumbleProducts}.</p>
 *
 * <h2>Inventory</h2>
 * <p>36 slots with GT6's six groups: {@link #SLOT_ROYAL} holds the breeding queen or the waiting
 * princess, {@link #SLOT_DRONE} the preferred drone, {@link #SLOTS_COMBS} the combs a working queen
 * produces, {@link #SLOTS_DRONE} the spare drone slots the brood and the promoted princess come from,
 * and {@link #SLOTS_DEAD} the dead bees. The advanced machine uses 20 slots instead
 * ({@link #ADVANCED_LAYOUT}).</p>
 *
 * <p>The GUI rules are GT6's: only a living princess may be put into the royal slot
 * ({@code isItemValidForSlotGUI}) and only a drone into the main drone slot, nothing may be
 * put into the comb, spare-drone and dead slots, the spare drone slots cannot be emptied by the
 * player at all ({@code MultiTileEntityBumbliary:399-421}, {@code setCanTake(F)}), the royal slot can
 * only be emptied while it does not hold a live queen ({@code canTakeOutOfSlotGUI}) and the
 * royal slot takes exactly one item ({@code getInventoryStackLimitGUI}).</p>
 *
 * <p>Automation sees the dead slots only, on every side: {@code getAccessibleSlotsFromSide2}
 * returns {@link #SLOTS_DEAD}, {@code canInsertItem2} is false and {@code canExtractItem2}
 * allows the extraction of {@code aSlot >= 27} - the dead bees. The advanced machine exposes
 * {@link #ADV_SLOTS_AUTO} instead ({@code MultiTileEntityBumbliaryAdvanced:341-344}), i.e. its combs
 * and its dead bees.</p>
 *
 * <h2>What runs per tick</h2>
 * <ol>
 *   <li>Every 1200 ticks - and once on load - the environment is refreshed: the sky flag comes from
 *       GT6's {@code getRainAtSide} over the five non-bottom sides, the
 *       temperature from {@link SmelteryBlockEntityHelper#environmentTemperature} (GT6's
 *       {@code WD.envTemp}) and the humidity from the biome's rainfall.</li>
 *   <li>A queen with {@code life > 0} in the royal slot breeds nothing but works: the countdown is
 *       pinned to 1200, and while {@link #checkEnvironment} accepts the place the
 *       machine counts the queen's life down one tick at a time, hands out a comb every
 *       {@code life % 1200 == 600} and attacks everything in a 9x9x9 box every
 *       {@code life % 300 == 150} (see the port differences). A queen whose genome
 *       does not fit the bumbliary dies immediately.</li>
 *   <li>When the queen's life reaches zero the machine kills the queen and every bee in the spare drone
 *       slots, moves the stored brood into the drone slots - or kills a child whose
 *       genome no longer fits the place - and promotes the most aggressive princess
 *       of the spare drone slots into the royal slot.</li>
 *   <li>A princess waits for its countdown, then picks a drone
 *       ({@code BumbleBreeding.findDroneSlot}), breeds, stores the brood
 *       ({@code mOffSpring}), sets its life to the princess's lifespan gene,
 *       eats the drone, crowns the queen and kills
 *       every non-drone bee the spare slots may still hold.</li>
 *   <li>An empty royal slot resets life, brood and countdown.</li>
 * </ol>
 *
 * <h2>Port differences to GT6</h2>
 * <ul>
 *   <li><b>Stings use the shared species rules.</b> The normal/advanced attack boxes are
 *       9x9x9/5x5x5; insect protection and GT6 species immunity are checked by
 *       {@link gregtech.content.bumble.BumbleSting}.</li>
 *   <li><b>The working-place scan is implemented for registered blocks.</b> GT6 calls
 *       {@code bumbleCanProduce} ({@code MultiItemBumbles:216-349}) before it rolls a product. The
 *       port checks the same species categories within three blocks (one for the advanced machine),
 *       via {@link BumbleWorkplace}. GT6's optional-mod plants without a modern registered counterpart
 *       are not fabricated. {@code bumbleCanProduct} always answers the machine's
 *       own position, so it needs no additional product-specific check.</li>
 *   <li><b>Scoop access is a synced menu mode.</b> A scoop or creative player opens
 *       GT6's alternate take permissions; slot insertion and live-queen restrictions remain.</li>
 *   <li><b>The cover and paint system has no equivalent</b> ({@code allowCovers},
 *       {@code getTexture2}), and neither has {@code ITileEntityRunningSuccessfully};
 *       the port keeps GT6's four states as {@link #life()},
 *       {@link #countdown()} and {@link #endedQueen()} for the GUI and the tests.</li>
 *   <li><b>GT6's {@code rng(..)} is one global RNG</b> ({@code RNGSUS}); the port draws from
 *       {@link Level#random}, the closest single source a block entity can reach, and hands the same
 *       source to {@link BumbleBreeding} (GT6 mixes its tile entity RNG with {@code RNGSUS} there).</li>
 *   <li><b>The NBT keys are GT6's own</b> ({@code CS:1175,1230,1251}): {@code gt.progress} is
 *       {@code mLife}, {@code gt.cooldown} is {@code mBreedingCountDown} and {@code gt.invout} is the
 *       brood list with {@code gt.invout.<i>} entries. The inventory is stored under
 *       {@code inventory} like every other port container.</li>
 *   <li><b>The sky flag latches.</b> GT6 only ever sets {@code mSky} to true and
 *       never clears it, so a bumbliary that has seen rain from above once keeps working as an
 *       "outside" machine; the port keeps that behaviour on purpose.</li>
 * </ul>
 */
public class BumbliaryBlockEntity extends BlockEntity implements MenuProvider,
        gregtech.engine.inventory.BlockContents {

    // ── GT6's NBT keys (CS.java:1175, 1230, 1251) ───────────────────────────────────────────────
    /** GT6's {@code NBT_PROGRESS}: the queen's remaining life. */
    public static final String NBT_PROGRESS = "gt.progress";
    /** GT6's {@code NBT_COOLDOWN}: the breeding countdown. */
    public static final String NBT_COOLDOWN = "gt.cooldown";
    /** GT6's {@code NBT_INV_OUT}: the pending brood, one {@code gt.invout.<i>} per child. */
    public static final String NBT_INV_OUT = "gt.invout";
    /** The port's own key for the container (every port block entity stores its inventory here). */
    public static final String NBT_INVENTORY = "inventory";

    /** GT6's countdown of a working breeder and of a machine without a royal bee. */
    public static final int COUNTDOWN = 1200;
    /** GT6's countdown while a princess has no drone to pair with. */
    public static final int COUNTDOWN_PAIRING = 600;
    /** GT6's penalty for opening the machine: five minutes without breeding. */
    public static final int COUNTDOWN_OPEN_PENALTY = 6000;

    /** GT6's {@code onTick2} / {@code onTickFirst2}: how often the environment is re-read. */
    public static final int ENVIRONMENT_INTERVAL = 1200;

    private static final ItemStack[] NO_BROOD = new ItemStack[0];
    private static final Direction[] SIDES_BUT_BOTTOM = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * GT6's slot groups, so the standard and the advanced machine share one implementation
     * ({@code MultiTileEntityBumbliary:333-337} and {@code MultiTileEntityBumbliaryAdvanced:333-337}).
     */
    public static final class Layout extends gregtech.content.bumble.BumbliarySlotLayouts.Layout {
        public Layout(boolean advanced,int slots,int royal,int drone,int[] combs,int[] drones,int[] dead,int[] accessible,int productRoll){super(advanced,slots,royal,drone,combs,drones,dead,accessible,productRoll);}
        private Layout(gregtech.content.bumble.BumbliarySlotLayouts.Layout source){this(source.advanced(),source.slots(),source.royal(),source.drone(),source.combs(),source.drones(),source.dead(),source.accessible(),source.productRoll());}
    }
    public static final int SLOT_ROYAL=gregtech.content.bumble.BumbliarySlotLayouts.SLOT_ROYAL;
    public static final int SLOT_DRONE=gregtech.content.bumble.BumbliarySlotLayouts.SLOT_DRONE;
    public static final int ADV_SLOT_ROYAL=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOT_ROYAL;
    public static final int ADV_SLOT_DRONE=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOT_DRONE;
    public static final int[] SLOTS_COMBS=gregtech.content.bumble.BumbliarySlotLayouts.SLOTS_COMBS;
    public static final int[] SLOTS_DRONE=gregtech.content.bumble.BumbliarySlotLayouts.SLOTS_DRONE;
    public static final int[] SLOTS_DEAD=gregtech.content.bumble.BumbliarySlotLayouts.SLOTS_DEAD;
    public static final int[] ADV_SLOTS_COMBS=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOTS_COMBS;
    public static final int[] ADV_SLOTS_DRONE=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOTS_DRONE;
    public static final int[] ADV_SLOTS_DEAD=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOTS_DEAD;
    public static final int[] ADV_SLOTS_AUTO=gregtech.content.bumble.BumbliarySlotLayouts.ADV_SLOTS_AUTO;
    public static final Layout LAYOUT=new Layout(gregtech.content.bumble.BumbliarySlotLayouts.LAYOUT),ADVANCED_LAYOUT=new Layout(gregtech.content.bumble.BumbliarySlotLayouts.ADVANCED_LAYOUT);

    private final Layout layout;
    private final ItemStackHandler inventory;
//? if >=1.21.1 {
    /*private IItemHandler automation;
*///?} else {
    private LazyOptional<IItemHandler> automation;
//?}

    /** GT6's {@code mOffSpring}: the brood the dead queen hands out. */
    private ItemStack[] brood = NO_BROOD;
    /** GT6's {@code mLife}: the queen's remaining life, counted down one tick at a time. */
    private long life;
    /** GT6's {@code mBreedingCountDown}. */
    private long countdown = COUNTDOWN;
    /** GT6's {@code mSky}: the machine has been rained on from above. */
    private boolean sky;
    /** GT6's {@code mTemperature}, seeded with GT6's default environment temperature. */
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    /** GT6's {@code mHumidity}. */
    private float humidity = 1.0F;
    /** GT6's {@code mEndedQueen}, true for the one tick a queen dies on. */
    private boolean endedQueen;
    /** GT6 refreshes the environment on the first tick ({@code onTickFirst2}). */
    private boolean environmentKnown;

    public BumbliaryBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.BUMBLIARY.get(), pos, state);
        this.layout = state.getBlock() instanceof BumbliaryBlock block && block.advanced()
                ? ADVANCED_LAYOUT : LAYOUT;
        this.inventory = new ItemStackHandler(layout.slots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BumbliaryBlockEntity.this.isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                // GT6's getInventoryStackLimitGUI: the royal slot holds one bee.
                return slot == layout.royal() ? 1 : 64;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
//? if >=1.21.1 {
        /*this.automation = new AutomationHandler();
*///?} else {
        this.automation = LazyOptional.of(() -> new AutomationHandler());
//?}
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
    /** The pending brood, a copy so a caller cannot change what the machine still owes. */
    public ItemStack[] brood() { return brood.clone(); }

    // ── environment (GT6 onTickFirst2 and the 1200-tick refresh) ────────────────

    /**
     * GT6's {@code onTick2}: the whole machine, server side only.
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
     * Re-reads the place the machine stands in ({@code onTickFirst2}).
     *
     * <p>Port difference: GT6 latches {@code mSky} - it only ever sets it to true
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
        if (level == null) return true;
        if (!level.dimensionType().hasSkyLight()) return false;             // provider.hasNoSky
        BlockPos neighbour = worldPosition.relative(side);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, neighbour.getX(), neighbour.getZ())
                <= neighbour.getY();
    }

    // ── the machine (GT6 onTick2) ───────────────────────────────────────────────────────

    /** GT6's {@code onTick2} body, one tick of the bumbliary. */
    public void tickLogic() {
        if (level == null || level.isClientSide) return;
        long previousLife = life;
        long previousCountdown = countdown;
        ItemStack[] previousBrood = brood;
        endedQueen = false;

        ItemStack royal = inventory.getStackInSlot(layout.royal());
        BumbleBeeType royalType = BumbleBeeType.of(royal);
        if (!royal.isEmpty() && royalType != null) {
            // `Util.getBumbleTag(tRoyalStack)`: a bee without a genome gets one here.
            CompoundTag royalGenes = BumbleBeeGenes.of(royal, level.random);
            if (life > 0 && royalType.aliveVariant() == BumbleBeeType.QUEEN) {   // `% 5 == 2`
                if (layout.advanced()) {
                    if (countdown < COUNTDOWN) countdown = COUNTDOWN;
                } else {
                    countdown = COUNTDOWN;
                }
                if (checkEnvironment(royalGenes)) {
                    if (--life <= 0) {
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
                        produceComb(royal, royalGenes);
                    }
                } else {
                    // a queen whose genome does not fit this place dies right away.
                    ItemStack dead = kill(royal);
                    inventory.setStackInSlot(layout.royal(), ItemStack.EMPTY);
                    addToDeadSlots(dead);
                }
            } else {
                // no working queen - a princess waits for a drone, everything else idles.
                life = 0;
                brood = NO_BROOD;
                if (royalType.aliveVariant() == BumbleBeeType.PRINCESS) {        // `% 5 == 1`
                    if (--countdown <= 0) {
                        countdown = COUNTDOWN_PAIRING;
                        tryBreed(royal);
                    }
                } else {
                    countdown = COUNTDOWN;
                }
            }
        } else {
            life = 0;
            brood = NO_BROOD;
            countdown = COUNTDOWN;
        }
        // The original base/proxy marks loaded machines for saving every second. Here the native
        // chunk must be dirtied even when no inventory slot changed (life and pairing waits).
        if (life != previousLife || countdown != previousCountdown || brood != previousBrood) {
            setChanged();
        }
    }

    /**
     * GT6's breeding step with {@link BumbleBreeding} doing the maths.
     *
     * <p>The drone slots are handed over in GT6's own order: index 0 is the main drone slot
     * and the rest are {@link #SLOTS_DRONE} in order, which is
     * exactly what {@link BumbleBreeding#findDroneSlot} expects.</p>
     */
    private void tryBreed(ItemStack royal) {
        int[] spare = layout.drones();
        ItemStack[] droneSlots = new ItemStack[spare.length + 1];
        droneSlots[0] = inventory.getStackInSlot(layout.drone());
        for (int i = 0; i < spare.length; i++) droneSlots[i + 1] = inventory.getStackInSlot(spare[i]);

        int index = BumbleBreeding.findDroneSlot(royal, droneSlots);
        if (index < 0) return;
        int slot = index == 0 ? layout.drone() : spare[index - 1];

        BumbleBreeding.Result result = BumbleBreeding.breed(royal, droneSlots[index],
                index == 0, level.random);
        if (result == null) return;                                             // the scan proved it is a drone

        countdown = COUNTDOWN;
        brood = result.offspring();                                             // mOffSpring
        life = result.lifeSpan();
        addToDeadSlots(result.droneDead());
        shrink(slot, result.droneCost());                                       // decrStackSize
        inventory.setStackInSlot(layout.royal(), single(result.queen()));       // ST.amount(1, crown)

        for (int droneSlot : spare) {
            ItemStack found = inventory.getStackInSlot(droneSlot);
            if (found.isEmpty() || BumbleBeeType.of(found) == null) continue;
            if (BumbleBeeType.of(found).aliveVariant() == BumbleBeeType.DRONE) continue;
            inventory.setStackInSlot(droneSlot, ItemStack.EMPTY);
            addToDeadSlots(kill(found));
        }
    }

    /**
     * GT6's queen death: the queen, every bee of the spare drone slots and the
     * children that no longer fit the environment die, the brood moves into the drone slots and the
     * most aggressive princess becomes the new queen.
     *
     * <p>Note that GT6 only ever looks at {@link #SLOTS_DRONE} here - never at the main drone slot -
     * so a drone parked in {@link #SLOT_DRONE} survives a queen's death and cannot be promoted.</p>
     */
    private void endQueen() {
        endedQueen = true;

        for (int slot : layout.dead()) {
            ItemStack found = inventory.getStackInSlot(slot);
            if (BumbleBeeType.of(found) != null) inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }

        ItemStack queen = inventory.getStackInSlot(layout.royal());
        inventory.setStackInSlot(layout.royal(), ItemStack.EMPTY);
        addToDeadSlots(kill(queen));

        for (int slot : layout.drones()) {
            ItemStack found = inventory.getStackInSlot(slot);
            if (found.isEmpty() || BumbleBeeType.of(found) == null) continue;
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            addToDeadSlots(kill(found));
        }

        for (ItemStack child : brood) {
            if (child == null || child.isEmpty() || BumbleBeeType.of(child) == null) continue;
            if (checkEnvironment(BumbleBeeGenes.of(child, level.random))) {
                // An empty main slot is deliberately skipped: promotion only scans spare slots.
                if (inventory.getStackInSlot(layout.drone()).isEmpty()
                        || !addStackToSlot(layout.drone(), child)) {
                    for (int slot : layout.drones()) if (addStackToSlot(slot, child)) break;
                }
            } else {
                addToDeadSlots(kill(child));
            }
        }

        brood = NO_BROOD;

        int princessSlot = -1;
        for (int slot : layout.drones()) {
            ItemStack candidate = inventory.getStackInSlot(slot);
            BumbleBeeType type = BumbleBeeType.of(candidate);
            if (type == null || type.aliveVariant() != BumbleBeeType.PRINCESS) continue;
            if (princessSlot < 0 || aggressiveness(candidate) > aggressiveness(inventory.getStackInSlot(princessSlot))) {
                princessSlot = slot;
            }
        }
        if (princessSlot >= 0) {
            inventory.setStackInSlot(layout.royal(), single(inventory.getStackInSlot(princessSlot)));
            shrink(princessSlot, 1);
        }
    }

    /** GT6's comb production. */
    private void produceComb(ItemStack royal, CompoundTag royalGenes) {
        if (life % 1200 != 600) return;
        if (level.random.nextInt(GTBumbleProducts.ROLL) >= (int) BumbleBeeGenes.workForce(royalGenes)) return;
        if (!checkWork(royalGenes)) return;
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(royal);
        if (species == null) return;
        if (!BumbleWorkplace.hasWorkplace(level, worldPosition, species.id(), layout.advanced())) return;

        for (int i = 0, products = GTBumbleProducts.count(); i < products; i++) {
            if (level.random.nextInt(layout.productRoll())
                    >= GTBumbleProducts.chance(species.id())) continue;
            ItemStack product = GTBumbleProducts.stack(species.id(), 1);
            if (!product.isEmpty()) addProductToCombSlots(product);
        }
    }

    /**
     * GT6's comb placement: a comb slot that already holds the same product is
     * filled first, otherwise the first comb slot that takes it wins; a product that fits nowhere is
     * lost, exactly like in GT6.
     */
    private void addProductToCombSlots(ItemStack product) {
        for (int slot : layout.combs()) {
            ItemStack existing = inventory.getStackInSlot(slot);
//? if >=1.21.1 {
            /*if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, product)) continue;
*///?} else {
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, product)) continue;
//?}
            if (addStackToSlot(slot, product)) return;
        }
        for (int slot : layout.combs()) if (addStackToSlot(slot, product)) return;
    }

    // ── GT6's environment checks ─────────────────────────────────────────────────────

    /**
     * GT6's {@code checkEnvironment}: the genome has to accept the temperature, the
     * humidity and the inside/outside side of the machine.
     */
    private boolean checkEnvironment(CompoundTag genes) {
        return BumbleBeeGenes.temperatureMatches(genes, temperature)
                && BumbleBeeGenes.humidityMatches(genes, humidity)
                && (sky ? BumbleBeeGenes.outsideActive(genes) : BumbleBeeGenes.insideActive(genes));
    }

    /**
     * GT6's {@code checkWork}: a machine in the rain needs a rainproof bee, one in
     * a thunderstorm a stormproof bee, and the time of day has to match the bee's activity.
     */
    private boolean checkWork(CompoundTag genes) {
        if (sky) {
            if (level.isThundering() && !BumbleBeeGenes.stormproof(genes)) return false;
            if (level.isRaining() && humidity > 0 && !BumbleBeeGenes.rainproof(genes)) return false;
        }
        return level.isDay() ? BumbleBeeGenes.dayActive(genes) : BumbleBeeGenes.nightActive(genes);
    }

    // ── slot rules ───────────────────────────────────────────────────────────────

    /** GT6's {@code isItemValidForSlotGUI}: princesses and drones, nothing else. */
    private boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        BumbleBeeType type = BumbleBeeType.of(stack);
        if (type == null) return false;                                         // IItemBumbleBee
        if (slot == layout.drone()) return type.aliveVariant() == BumbleBeeType.DRONE;      // `% 5 == 0`
        if (slot == layout.royal()) return type.aliveVariant() == BumbleBeeType.PRINCESS;   // `% 5 == 1`
        return false;                                                           // everything else
    }

    /**
     * GT6's {@code canTakeOutOfSlotGUI} for one slot of this machine: everything may
     * leave except a royal bee that is still alive - the queen has to die first.
     */
    public boolean canTakeOutOfSlot(int slot) {
        return slot != layout.royal() || canLeaveRoyalSlot(inventory.getStackInSlot(slot));
    }

    /**
     * The stack half of GT6's {@code canTakeOutOfSlotGUI}: only a live queen may not
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
        if (stack == null || stack.isEmpty()) return true;
        if (slot < 0 || slot >= inventory.getSlots()) return false;
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, stack.copy());
            return true;
        }
        int limit = Math.min(Math.max(1, existing.getMaxStackSize()), inventory.getSlotLimit(slot));
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() + stack.getCount() <= limit) {
*///?} else {
        if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() + stack.getCount() <= limit) {
//?}
            inventory.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + stack.getCount()));
            return true;
        }
        return false;
    }

    /** GT6's dead-slot loop: the first slot that takes the stack wins, otherwise the stack is lost. */
    private void addToDeadSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (int slot : layout.dead()) if (addStackToSlot(slot, stack)) return;
    }

    /** GT6's {@code decrStackSize}. */
    private void shrink(int slot, int amount) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || amount <= 0) return;
        int left = stack.getCount() - amount;
        inventory.setStackInSlot(slot, left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left));
    }

    /** One item of a stack, GT6's {@code ST.amount(1, stack)}. */
    private static ItemStack single(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    /** GT6's {@code Util.getAggressiveness(Util.getBumbleTag(stack))}. */
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

    // ── automation (GT6 getAccessibleSlotsFromSide2) ────────────────────────────────────

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
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;                                                       // canInsertItem2
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int index = mapped(slot);
            if (index < 0) return ItemStack.EMPTY;
            return inventory.extractItem(index, amount, simulate);              // canExtractItem2
        }

        private int mapped(int slot) {
            int[] accessible = layout.accessible();
            return slot < 0 || slot >= accessible.length ? -1 : accessible[slot];
        }
    }

    // ── NBT (GT6 readFromNBT2) ────────────────────────────────────────────────────────────

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        if (tag.contains(NBT_INVENTORY)) inventory.deserializeNBT(lookup,tag.getCompound(NBT_INVENTORY));
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_INVENTORY)) inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
//?}
        if (tag.contains(NBT_PROGRESS)) life = tag.getLong(NBT_PROGRESS);
        if (tag.contains(NBT_COOLDOWN)) countdown = tag.getLong(NBT_COOLDOWN);
//? if >=1.21.1 {
        /*brood = NO_BROOD;
*///?} else {
//?}
        if (tag.contains(NBT_INV_OUT)) {
            int size = tag.getInt(NBT_INV_OUT);
            brood = new ItemStack[Math.max(0, size)];
            for (int i = 0; i < brood.length; i++) {
                brood[i] = tag.contains(NBT_INV_OUT + "." + i)
//? if >=1.21.1 {
/*// Version-specific branch.
                        ? ItemStack.parseOptional(lookup,tag.getCompound(NBT_INV_OUT + "." + i)) : ItemStack.EMPTY;
*///?} else {
// Version-specific branch.
                        ? ItemStack.of(tag.getCompound(NBT_INV_OUT + "." + i)) : ItemStack.EMPTY;
//?}
            }
        }
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.put(NBT_INVENTORY, inventory.serializeNBT(lookup));
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
//?}
        tag.putLong(NBT_PROGRESS, life);
        tag.putLong(NBT_COOLDOWN, countdown);
        if (brood.length > 0) {
            tag.putInt(NBT_INV_OUT, brood.length);
            for (int i = 0; i < brood.length; i++) {
                if (brood[i] != null && !brood[i].isEmpty()) {
//? if >=1.21.1 {
                    /*tag.put(NBT_INV_OUT + "." + i, brood[i].saveOptional(lookup));
*///?} else {
                    tag.put(NBT_INV_OUT + "." + i, brood[i].save(new CompoundTag()));
//?}
                }
            }
        }
    }

    // ── contents, break and menu ────────────────────────────────────────────────────────────────

    /**
     * GT6's {@code breakDrop}: while the machine is running ({@code mLife > 0}) every
     * bee it holds dies as it is handed out, otherwise the bees drop as they are.
     */
    public List<ItemStack> breakDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {               // GT6 canDrop
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
        for (ItemStack stack : breakDrops()) gregtech.engine.inventory.BlockContents.drop(this, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(layout.advanced()
                ? "block.gt6ce.advanced_bumbliary" : "block.gt6ce.bumbliary");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new gregtech.client.gui.BumbliaryContainerMenu(containerId, playerInventory, this, player.isCreative());
    }

    /**: only a living, currently working queen defends the machine. */
    public boolean attackEntity(net.minecraft.world.entity.LivingEntity target) {
        ItemStack royal = inventory.getStackInSlot(layout.royal());
        BumbleBeeType type = BumbleBeeType.of(royal);
        return life > 0 && type != null && type.aliveVariant() == BumbleBeeType.QUEEN
                && gregtech.content.bumble.BumbleSting.attack(royal, target);
    }

    /** GT6's {@code onBlockActivated3} / {@code onToolClick2}: opening costs five minutes. */
    public void onOpened() {
        countdown = COUNTDOWN_OPEN_PENALTY;
        setChanged();
    }

//? if >=1.21.1 {
    /*public IItemHandler itemCapability(@Nullable Direction side){return automation;}
*///?} else {
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
//?}
}
