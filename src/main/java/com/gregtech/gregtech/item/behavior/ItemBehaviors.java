package com.gregtech.gregtech.item.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The port's item-behaviour layer: one stateless entry point per GT6 {@code gregtech.items.behaviors}
 * class, plus the three helpers every one of those behaviours calls.
 *
 * <h2>Why a layer and not methods on the items</h2>
 *
 * <p>GT6 hangs its behaviours on {@code MultiItem} meta-items: {@code MultiItemRandomTools} and the
 * {@code ToolsGT} tool classes call {@code addItemBehavior(...)}/pass a behaviour into
 * {@code addItem(...)}, and the meta-item then forwards {@code onItemUseFirst}/{@code onItemRightClick}
 * to it (e.g. {@code MultiItemRandomTools:279} for the C-Foam removal spray, {@code :517} for the
 * portable scanner, {@code GT_Tool_Plunger:86-87} for the two plunger behaviours). The port registers
 * those items as plain {@code TechItem}s ({@code GTMultiItems:65-78}) with no per-item behaviour
 * slot, so the behaviour lives here as {@code static} methods over the objects they touch —
 * world, position, face, holder stack, player — exactly the shape
 * {@code content/cover/CoverAttachmentBehaviors} uses for the GUI-less covers.</p>
 *
 * <p>Every class below is a pure function of its arguments: no GUI, no packet, no new block, and no
 * hidden state beyond the {@code gt.remaining} NBT GT6 itself stores on the consumable. Callers
 * (the item classes) are expected to put the returned stack back where it came from.</p>
 *
 * <h2>What is in this file</h2>
 *
 * <ul>
 *   <li>{@link #obstructed} — GT6 {@code WD.obstructed} ({@code WD.java:127-148}), used by the tape,
 *       the flint-and-tinder and the extinguisher as their "can the player actually reach this face"
 *       gate;</li>
 *   <li>{@link #creative} — GT6 {@code UT.Entities.hasInfiniteItems} ({@code UT.java:3187-3189});</li>
 *   <li>{@link #mayEdit} — GT6 {@code EntityPlayer.canPlayerEdit} ({@code 1.7.10} signature) in its
 *       1.20.1 form;</li>
 *   <li>{@link Consumable} — the shared "empty / used / full + {@code gt.remaining}" shape GT6
 *       repeats in every tape, lighter and spray class;</li>
 *   <li>{@link Ignitable} plus {@link #igniteToolClick}, {@link #lightVanillaFire} and
 *       {@link #primeTnt} — GT6's {@code TOOL_igniter} click, i.e. {@code IBlockToolable.Util
 *       .onToolClick(TOOL_igniter, ...)} together with the vanilla half of that tool kind
 *       ({@code ToolCompat.java:201-223}).</li>
 * </ul>
 *
 * <h2>Registered behaviour ids</h2>
 *
 * <p>{@link #PORTED} lists what this batch covers. The ids are the port's own item registry paths
 * ({@code gregtech:duct_tape} and friends, all present in {@code GTMultiItemsGen.ENTRIES} or
 * {@code GTTechnological}); the class column names the GT6 behaviour each one ports. An item that is
 * not in the table has no behaviour yet.</p>
 */
public final class ItemBehaviors {

    // ── the batch's behaviour table ──────────────────────────────────────

    /** One ported GT6 behaviour: the port's item id, the GT6 class it comes from. */
    public record Entry(String itemId, Class<?> behaviour) {}

    /** Every GT6 {@code gregtech.items.behaviors} class this layer implements, with its port item. */
    public static final List<Entry> PORTED = com.gregtech.gregtech.content.tool.ItemBehaviorCatalog.ALL.stream().map(v->new Entry(v.itemId(),switch(v.behavior()){case "BehaviorChunkEraser" -> BehaviorChunkEraser.class;case "BehaviorDataStorage" -> BehaviorDataStorage.class;case "BehaviorDataStorage16" -> BehaviorDataStorage16.class;case "BehaviorDuctTape" -> BehaviorDuctTape.class;case "BehaviorFlintAndTinder" -> BehaviorFlintAndTinder.class;case "BehaviorLighter" -> BehaviorLighter.class;case "BehaviorPlungerFluid" -> BehaviorPlungerFluid.class;case "BehaviorRemote" -> BehaviorRemote.class;case "BehaviorScanner" -> BehaviorScanner.class;case "BehaviorSprayColorRemover" -> BehaviorSprayColorRemover.class;case "BehaviorSprayExtinguisher" -> BehaviorSprayExtinguisher.class;case "BehaviorSprayFoamHardener" -> BehaviorSprayFoamHardener.class;case "BehaviorSprayFoamRemover" -> BehaviorSprayFoamRemover.class;case "BehaviorWorldgenDebugger" -> BehaviorWorldgenDebugger.class;default -> null;})).toList();

    /** The behaviour class registered for a port item id, or empty when the item has none yet. */
    public static Optional<Class<?>> behaviourOf(String itemId) {
        for (Entry entry : PORTED) {
            if (entry.itemId().equals(itemId)) return Optional.ofNullable(entry.behaviour());
        }
        return Optional.empty();
    }

    /** Whether this layer implements the behaviour of the named item. */
    public static boolean isPorted(String itemId) {
        return behaviourOf(itemId).isPresent();
    }

    private ItemBehaviors() {}

    /**
     * What one behaviour entry point did.
     *
     * <p>Every GT6 behaviour returns two things at once — whether it handled the click
     * ({@code onItemUseFirst}'s boolean, which decides whether {@code Block#use} still runs) and the
     * stack it left behind ({@code Behavior_Duct_Tape:63-87} swaps the item, decrements the counter
     * and may consume the stack) — and 1.20.1 cannot express the second in place, so both travel
     * together.</p>
     *
     * @param acted whether the behaviour did something; GT6's boolean return
     * @param stack the stack to store back where it came from
     */
    public record Outcome(boolean acted, ItemStack stack) {
        /** GT6's {@code F} return: nothing happened, the stack is untouched. */
        public static Outcome refused(ItemStack stack) { return new Outcome(false, stack); }

        /** GT6's {@code T} return. */
        public static Outcome acted(ItemStack stack) { return new Outcome(true, stack); }
    }

    // ── GT6 helpers the behaviours share ─────────────────────────────────

    /** GT6 {@code WD.obstructed} ({@code WD.java:127-148}) uses 1/8 and 1/4 pixel bands. */
    private static final double PX_P_2 = 0.125D, PX_N_2 = 0.875D, PX_P_4 = 0.25D, PX_N_4 = 0.75D;

    /**
     * GT6 {@code WD.obstructed} ({@code WD.java:127-148}): whether the block <em>behind</em> the
     * clicked face reaches into that face far enough that the player cannot put a tool through it.
     *
     * <p>The original works on the neighbour's 1.7.10 collision bounding box and asks, per side,
     * whether the box fills at least the quarter of the neighbour's own block touching the shared
     * plane ({@code PX_N[4] = 0.75} / {@code PX_P[4] = 0.25}) and spans more than one pixel in both
     * tangential axes ({@code > PX_P[2] = 0.125} and {@code < PX_N[2] = 0.875}). All six branches are
     * kept verbatim; {@code shape.bounds()} is the same union box
     * {@code getCollisionBoundingBoxFromPool} returned. Trapdoors, doors and ladders never obstruct
     * ({@code WD.java:136}), and a block without a collision box never does
     * ({@code WD.java:138}).</p>
     *
     * @param level the level the clicked block stands in
     * @param pos   the clicked block; the neighbour is {@code pos.relative(side)}
     * @param side  the clicked face
     * @return {@code true} when the face is blocked, which is what makes the tape and the igniters
     *         refuse to act
     */
    public static boolean obstructed(Level level, BlockPos pos, Direction side) {
        BlockPos neighbour = pos.relative(side);
        if (!level.hasChunkAt(neighbour)) return false;
        BlockState state = level.getBlockState(neighbour);
        // WD.java:136 - these three are explicitly never obstructing.
        if (state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof LadderBlock) {
            return false;
        }
        var shape = state.getCollisionShape(level, neighbour);
        // WD.java:138 - no collision box at all means no obstruction.
        if (shape.isEmpty()) return false;
        AABB box = shape.bounds();
        boolean spansX = box.maxX > PX_P_2 && box.minX < PX_N_2;
        boolean spansY = box.maxY > PX_P_2 && box.minY < PX_N_2;
        boolean spansZ = box.maxZ > PX_P_2 && box.minZ < PX_N_2;
        return switch (side) {
            case DOWN -> box.maxY > PX_N_4 && spansX && spansZ;
            case UP -> box.minY < PX_P_4 && spansX && spansZ;
            case NORTH -> box.maxZ > PX_N_4 && spansX && spansY;
            case SOUTH -> box.minZ < PX_P_4 && spansX && spansY;
            case WEST -> box.maxX > PX_N_4 && spansY && spansZ;
            case EAST -> box.minX < PX_P_4 && spansY && spansZ;
        };
    }

    /** GT6 {@code UT.Entities.hasInfiniteItems} ({@code UT.java:3187-3189}): creative mode only. */
    public static boolean creative(@Nullable Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    /**
     * GT6's {@code aPlayer.canPlayerEdit(x, y, z, side, stack)} gate, in its 1.20.1 form:
     * {@code player.mayBuild()} plus {@code level.mayInteract(player, pos)}.
     *
     * <p>A {@code null} player is GT6's auto-tool case and always passes, which is why this returns
     * {@code true} rather than {@code false} for it.</p>
     */
    public static boolean mayEdit(Level level, @Nullable Player player, BlockPos pos) {
        if (player == null) return true;
        return player.mayBuild() && level.mayInteract(player, pos);
    }

    /** Registry lookup by full id, e.g. {@code stack("gregtech:duct_tape")}; empty when unknown. */
    public static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The registry path of a stack, e.g. {@code duct_tape}; {@code null} for an unregistered item. */
    @Nullable
    public static String itemId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : id.getPath();
    }

    // ── the one call an item class needs ─────────────────────────────────

    /**
     * One right-click on a block, dispatched to whichever behaviour owns this stack.
     *
     * <p>This is the wiring point: an item class only has to call this and store
     * {@link Outcome#stack()} back into the hand. Everything the dispatched behaviours need —
     * GT6's per-family consumable triples, the scan level of each scanner, the lighter chances — is
     * looked up from the item's own registry path ({@link #PORTED}).</p>
     *
     * <p><b>Not modelled here:</b> GT6's scanner pays for its scan out of the item's own energy
     * buffer ({@code Behavior_Scanner:53} calls {@code aItem.useEnergy}), and the port registers
     * {@code portable_scanner} / {@code debug_scanner} as plain items with no buffer. The scan still
     * runs and reports its {@code CS.V[3]} cost, it just cannot be charged yet.</p>
     *
     * @return whether the click was handled, plus the stack to store back
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                ItemStack stack, float hitX, float hitY, float hitZ) {
        String id = itemId(stack);
        if (id == null) return Outcome.refused(stack);
        // The id is tested before anything is allocated: every multi-item of the port reaches this
        // method, and only the thirty-odd ids below have a behaviour at all.
        if (id.startsWith("tape") || id.startsWith("duct_tape") || id.startsWith("braintech_aerospace")) {
            return BehaviorDuctTape.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        BehaviorLighter.LighterSpec spec = lighterSpec(id);
        if (spec != null) {
            return BehaviorLighter.useOn(level, pos, side, player, stack, spec, level.getRandom(),
                    hitX, hitY, hitZ);
        }
        // The four spray families; the "_2" ids of GTMultiItemsGen.ENTRIES:64-71 are the used cans of
        // the same family, so a prefix test covers both halves.
        if (id.startsWith("hardening_spray")) {
            return BehaviorSprayFoamHardener.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.startsWith("paint_removal_spray")) {
            return BehaviorSprayColorRemover.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.startsWith("c_foam_removal_spray")) {
            return BehaviorSprayFoamRemover.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.startsWith("fire_extinguisher_co2")) {
            return BehaviorSprayExtinguisher.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.equals("portable_scanner") || id.equals("debug_scanner")) {
            int scanLevel = id.equals("debug_scanner") ? BehaviorScanner.DEBUG_LEVEL
                    : BehaviorScanner.PORTABLE_LEVEL;
            List<String> lines = new ArrayList<>();
            long cost = BehaviorScanner.scan(level, pos, side, scanLevel, player, lines);
            if (player != null) {
                for (String line : lines) player.displayClientMessage(Component.literal(line), false);
            }
            lastScanCost = cost;
            return Outcome.acted(stack);          // Behavior_Scanner:54 returns T for a server player
        }
        // §110: the debug pair (MultiItemRandomTools:519-520) and the Remote Activator's bind half.
        // Both debug items are deliberately exempt from the build-permission gate, exactly like GT6's
        // (Behavior_Chunk_Remover:39 and Behavior_Worldgen_Debugger:43 check only `isRemote`).
        if (id.equals("chunk_eraser")) {
            return BehaviorChunkEraser.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.equals("worldgen_debug_wand")) {
            return BehaviorWorldgenDebugger.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        if (id.equals("remote_activator")) {
            return BehaviorRemote.useOn(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        return Outcome.refused(stack);
    }

    /**
     * One right-click into the air, with no block under the cursor — GT6's {@code onItemRightClick}.
     *
     * <p>Only the Remote Activator uses this slot ({@code Behavior_Remote:76-90} fires every bound
     * coordinate within {@link BehaviorRemote#RANGE}); the port has no other behaviour that reacts to a
     * click in the air, so the lookup is by id and everything else is refused without allocating.</p>
     */
    public static Outcome useInAir(Level level, @Nullable Player player, ItemStack stack) {
        String id = itemId(stack);
        if (id == null) return Outcome.refused(stack);
        if (id.equals("remote_activator")) return BehaviorRemote.useInAir(level, player, stack);
        return Outcome.refused(stack);
    }

    /**
     * The tooltip slot of the behaviour layer — GT6's {@code IBehavior.getAdditionalToolTips}.
     *
     * <p>Two behaviours live here and nowhere else: the USB stick ({@code Behavior_DataStorage}) and the
     * USB drive ({@code Behavior_DataStorage16}) render the data their item carries, so they have no
     * click half at all. Dispatch is by id, like every other entry point in this class, and an item
     * without a tooltip behaviour adds nothing.</p>
     */
    public static void tooltip(ItemStack stack, List<Component> lines) {
        String id = itemId(stack);
        if (id == null) return;
        if (id.endsWith("_hdd") && id.startsWith("usb")) {
            BehaviorDataStorage16.tooltip(stack, lines);
        } else if (id.endsWith("_stick") && id.startsWith("usb")) {
            BehaviorDataStorage.tooltip(stack, lines);
        }
    }

    /**
     * One right-click on an entity, dispatched the same way — the flint and tinder's and the
     * lighter's creeper branch ({@code Behavior_FlintAndTinder:69-78},
     * {@code Behavior_Lighter:61-79}).
     */
    public static Outcome useOnEntity(Entity entity, @Nullable Player player, ItemStack stack) {
        String id = itemId(stack);
        if (id == null) return Outcome.refused(stack);
        if (id.equals("tool_flint_and_tinder")) {
            return BehaviorFlintAndTinder.igniteCreeper(entity, stack, player)
                    ? Outcome.acted(stack) : Outcome.refused(stack);
        }
        BehaviorLighter.LighterSpec spec = lighterSpec(id);
        if (spec != null) return BehaviorLighter.useOnEntity(entity, player, stack, spec);
        return Outcome.refused(stack);
    }

    /**
     * The lighter family an item id belongs to, or {@code null} when it is not a lighter —
     * the seven factories of {@link BehaviorLighter}, keyed by the ids of
     * {@code GTMultiItemsGen.ENTRIES:72-86}.
     */
    @Nullable
    public static BehaviorLighter.LighterSpec lighterSpec(String itemId) {
        return switch (itemId) {
            case "match" -> BehaviorLighter.match();
            case "match_box", "match_box_full" -> BehaviorLighter.matchBox();
            case "lighter_empty", "lighter", "lighter_full" -> BehaviorLighter.invar();
            case "shiny_lighter_empty", "shiny_lighter", "shiny_lighter_full" -> BehaviorLighter.platinum();
            case "plastic_lighter_empty", "plastic_lighter", "plastic_lighter_full", "plastic_lighter_broken" ->
                    BehaviorLighter.plastic();
            case "fire_starter" -> BehaviorLighter.fireStarter();
            case "fire_starter_2" -> BehaviorLighter.fireStarterBark();
            default -> null;
        };
    }

    /** The last scan cost, so a caller without a chat channel can still report it. */
    private static long lastScanCost;

    /** GT6 charges the scan out of the item's energy buffer ({@code Behavior_Scanner:53}). */
    public static long lastScanCost() {
        return lastScanCost;
    }

    // ── the empty / used / full consumable GT6 repeats in every class ─────

    /**
     * GT6's consumable shape, which every tape, lighter and spray class repeats: a full item, a used
     * item that carries the remaining uses in {@code gt.remaining}, and an optional empty item the
     * stack turns into when the uses run out ({@code Behavior_Duct_Tape:42-51} and {@code :80-87},
     * {@code Behavior_Spray_Extinguisher:49-54} and {@code :84-91}, {@code Behavior_Lighter:52-58}
     * and {@code :130-137}).
     *
     * <p>The original mutates the {@link ItemStack} in place ({@code func_150996_a} swaps the item,
     * {@code UT.NBT.setNumber} rewrites the counter). 1.20.1's {@code ItemStack} has no way to change
     * its item, so the same operations are expressed as {@link #prepared} and {@link #spent}, which
     * return the stack the caller must store back instead.</p>
     *
     * @param empty the item the stack becomes when the uses run out, or {@link ItemStack#EMPTY} when
     *              the stack is simply consumed (GT6's {@code mEmpty == null} branch, e.g. the tapes)
     * @param used  the item that carries the counter in NBT
     * @param full  the item the canning machine fills; using it once converts it to {@code used}
     * @param uses  GT6's {@code mUses} — the counter a full item starts with
     * @param key   the NBT tag the counter lives in: {@link #USES_KEY} for the tapes and sprays
     *              ({@code Behavior_Duct_Tape:61}, {@code Behavior_Spray_Extinguisher:64}),
     *              {@link #LIGHTER_KEY} for the lighters ({@code UT.java:2087,2093})
     */
    public record Consumable(ItemStack empty, ItemStack used, ItemStack full, long uses, String key) {

        /** GT6's {@code "gt.remaining"} tag, written at {@code Behavior_Duct_Tape:77}. */
        public static final String USES_KEY = "gt.remaining";

        /** GT6's {@code "gt.lighter"} tag ({@code UT.NBT.setLighterFuel}, {@code UT.java:2085-2093}). */
        public static final String LIGHTER_KEY = "gt.lighter";

        public Consumable(ItemStack empty, ItemStack used, ItemStack full, long uses) {
            this(empty, used, full, uses, USES_KEY);
        }

        public Consumable {
            if (empty == null) empty = ItemStack.EMPTY;
        }

        /** True for GT6's {@code ST.equal(aStack, mFull, T)}: same item, NBT ignored. */
        public boolean isFull(ItemStack stack) {
            return !full.isEmpty() && !stack.isEmpty() && stack.is(full.getItem());
        }

        /** True for GT6's {@code ST.equal(aStack, mUsed, T)}. */
        public boolean isUsed(ItemStack stack) {
            return !used.isEmpty() && !stack.isEmpty() && stack.is(used.getItem());
        }

        /** Whether this stack is any variant of the consumable. */
        public boolean matches(ItemStack stack) {
            return isFull(stack) || isUsed(stack);
        }

        /**
         * The remaining uses, i.e. GT6's {@code tNBT.getLong("gt.remaining")} — except that a full
         * item reports {@link #uses} without having been used yet, which is how
         * {@code Behavior_Duct_Tape:111} displays it.
         */
        public long remaining(ItemStack stack) {
            if (isFull(stack)) return uses;
            CompoundTag tag = stack.getTag();
            return tag == null ? 0L : tag.getLong(key);
        }

        /**
         * GT6's {@code prepare} ({@code Behavior_Lighter:122-128}, inlined at
         * {@code Behavior_Duct_Tape:63-67}): a full stack becomes the used one and starts at full
         * fuel. A stack that is already used (or unknown) is returned unchanged.
         */
        public ItemStack prepared(ItemStack stack) {
            if (!isFull(stack)) return stack;
            ItemStack next = new ItemStack(used.getItem(), stack.getCount());
            CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
            tag.putLong(key, uses);
            next.setTag(tag);
            return next;
        }

        /**
         * GT6's use bookkeeping ({@code Behavior_Duct_Tape:76-87},
         * {@code Behavior_Spray_Foam_Hardener:80-91}): subtract, and when nothing is left run
         * {@link #usedUp}.
         *
         * @param stack  the used stack, after {@link #prepared}
         * @param amount the uses this operation consumed
         * @return the stack to store back; {@link ItemStack#isEmpty()} when the last one was consumed
         */
        public ItemStack spent(ItemStack stack, long amount) {
            long left = com.gregtech.gregtech.content.tool.ConsumableRules.remainingAfter(remaining(stack),amount);
            if (left <= 0) return usedUp(stack);
            CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
            tag.putLong(key, left);
            ItemStack next = stack.copy();
            next.setTag(tag);
            return next;
        }

        /**
         * GT6's {@code useUp} ({@code Behavior_Lighter:130-137}): the stack either turns into
         * {@link #empty} — the counter tag is dropped on the way, exactly like the original's
         * {@code tNBT.removeTag}/{@code func_150996_a} pair — or one of it is consumed.
         */
        public ItemStack usedUp(ItemStack stack) {
            CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
            tag.remove(key);
            if (empty.isEmpty()) {
                ItemStack next = stack.copy();
                next.shrink(1);
                return next;
            }
            ItemStack next = new ItemStack(empty.getItem(), stack.getCount());
            if (!tag.isEmpty()) next.setTag(tag);
            return next;
        }
    }

    // ── the spray-can click every spray class repeats ────────────────────

    /**
     * One spray operation — GT6's {@code foam} ({@code Behavior_Spray_Foam:109-178}),
     * {@code harden} ({@code Behavior_Spray_Foam_Hardener:95-116}),
     * {@code remove} ({@code Behavior_Spray_Foam_Remover:95-115}) and
     * {@code extinguish} ({@code Behavior_Spray_Extinguisher:95-130}).
     */
    public interface SprayEffect {
        /**
         * @param uses   the uses the caller is willing to spend — the can's counter, or
         *               {@link Consumable#uses()} when the player is in creative
         * @param player the player, or {@code null} for GT6's auto-tool case
         * @param can    the spray can stack
         * @return the uses this operation consumed, {@code 0} when it did nothing
         */
        long apply(Level level, BlockPos pos, Direction side, long uses, @Nullable Player player, ItemStack can,
                   float hitX, float hitY, float hitZ);
    }

    /**
     * GT6's five spray classes share one click shape, repeated verbatim in each of them
     * ({@code Behavior_Spray_Foam_Remover:58-93} is the shortest copy): refuse while the client side
     * runs, while the stack is stacked and when the player may not edit the block; turn a full can
     * into the used one and start its counter; run the operation with the counter the creative flag
     * allows; subtract what it reports; and finally empty the can when the counter reaches zero.
     *
     * <p>That last step is outside the "did anything happen" test in every copy
     * ({@code Behavior_Spray_Foam_Remover:84-91}), which is why it is outside the {@code spent > 0}
     * test here too: a can whose counter is already zero is emptied by the next click even when the
     * operation did nothing.</p>
     *
     * @return whether the operation did something, plus the can to store back
     */
    public static Outcome useSpray(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                   ItemStack stack, Consumable consumable, SprayEffect effect,
                                   float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);
        if (stack.getCount() != 1) return Outcome.refused(stack);
        if (!mayEdit(level, player, pos)) return Outcome.refused(stack);

        ItemStack prepared = consumable.prepared(stack);
        if (!consumable.isUsed(prepared)) return Outcome.refused(prepared);

        boolean creative = creative(player);
        long uses = creative ? consumable.uses() : consumable.remaining(prepared);
        long spent = Math.max(0L, effect.apply(level, pos, side, uses, player, prepared, hitX, hitY, hitZ));

        ItemStack after = prepared;
        if (!creative) {
            long left = consumable.remaining(prepared) - spent;
            after = left <= 0 ? consumable.usedUp(prepared) : consumable.spent(prepared, spent);
        }
        return spent > 0 ? Outcome.acted(after) : Outcome.refused(after);
    }

    // ── GT6's TOOL_igniter click ─────────────────────────────────────────
    /**
     * A block (or block entity) that reacts to GT6's {@code TOOL_igniter}
     * ({@code CS.java:1035}), i.e. the port's half of {@code IBlockToolable.onToolClick}.
     *
     * <p>GT6's implementors are the fire boxes and fluid/gas generators
     * ({@code MultiTileEntityGeneratorSolid:226}, {@code MultiTileEntityGeneratorLiquid:180},
     * {@code MultiTileEntityGeneratorFluidBed:202}, {@code MultiTileEntityBasicMachine:373}), the
     * dynamite ({@code MultiTileEntityDynamite:100}) and the mini portals
     * ({@code MultiTileEntityMiniPortalNether:118} and its five siblings). None of them is a GT6
     * random-tool item, so a block only has to implement this interface once.</p>
     */
    public interface Ignitable {
        /**
         * @param side     the clicked face, in GT6's own side order translated to {@link Direction}
         * @param player   the player, or {@code null} for GT6's auto-tool igniter
         * @param igniter  the stack that is igniting
         * @param sneaking whether the player sneaks, which some GT6 machines use as a mode switch
         * @return GT6's tool-click return value, i.e. the durability cost in 1/10000 units;
         *         {@code 0} when this block does not react to the igniter
         */
        long onIgnite(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack igniter,
                      boolean sneaking, float hitX, float hitY, float hitZ);
    }

    /**
     * GT6's {@code IBlockToolable.Util.onToolClick(TOOL_igniter, ...)} — ask the block to react, and
     * report its durability cost.
     *
     * <p>The block entity is asked first and the block second, which is the resolution order
     * {@code WD.te(..., aDelegator = T)} produces in every other behaviour of this package. The
     * return value is handed back unchanged so a caller can apply GT6's own conversion
     * ({@code Behavior_FlintAndTinder:58} uses {@code units(damage, 10000, 100, T)} for a tool,
     * {@code Behavior_Lighter:112} uses {@code units(damage, 10000, 1, T)} for a fuel unit).</p>
     *
     * @return the cost in GT6's 1/10000 units, or {@code 0} when nothing reacted
     */
    public static long igniteToolClick(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                       ItemStack igniter, boolean sneaking, float hitX, float hitY, float hitZ) {
        Ignitable target = level.getBlockEntity(pos) instanceof Ignitable be ? be
                : level.getBlockState(pos).getBlock() instanceof Ignitable block ? block : null;
        if (target == null) return 0L;
        return target.onIgnite(level, pos, side, player, igniter, sneaking, hitX, hitY, hitZ);
    }

    /** GT6 {@code ToolCompat:206}: a TNT block ignited by a GT igniter costs one full unit. */
    public static final long TNT_COST = 10000L;

    /**
     * The vanilla half of GT6's {@code TOOL_igniter} ({@code ToolCompat.java:201-223}): a GT igniter
     * that no block claims lights a fire block in front of the clicked face.
     *
     * <p>The original's conditions, in order: the target must be air
     * ({@code aWorld.isAirBlock}, {@code ToolCompat:217}) and the clicked block must have oxygen
     * ({@code WD.oxygen}, {@code ToolCompat:218}) — which is {@code WD.java:396-398}, i.e. a
     * Galacticraft-only check that is unconditionally {@code true} without that mod, so the port
     * keeps only the air test. The fire state comes from
     * {@link BaseFireBlock#getState(net.minecraft.world.level.BlockGetter, BlockPos)}, the 1.20.1
     * replacement for GT6's bare {@code Blocks.fire}.</p>
     *
     * <p><b>Not ported from {@code ToolCompat:203-213}:</b> the Forestry Candle and the Twilight
     * Forest Lamp of Cinders branches — neither mod exists in the port.</p>
     *
     * @return whether a fire block was placed, i.e. whether the operation cost a use
     */
    public static boolean lightVanillaFire(Level level, @Nullable Player player, BlockPos pos, Direction side,
                                           ItemStack igniter) {
        BlockPos target = pos.relative(side);
        if (!level.hasChunkAt(target)) return false;
        if (!level.isEmptyBlock(target)) return false;
        if (!mayEdit(level, player, target)) return false;
        return level.setBlock(target, BaseFireBlock.getState(level, target), 3);
    }

    /**
     * GT6 {@code ToolCompat:203-207}: a TNT block is primed instead of being set on fire, and the
     * block is removed. 1.20.1 folds both halves into
     * {@link TntBlock#onCaughtFire(BlockState, Level, BlockPos, Direction, net.minecraft.world.entity.LivingEntity)}.
     *
     * @return whether the block was TNT and got primed
     */
    public static boolean primeTnt(Level level, BlockPos pos, @Nullable Player player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TntBlock tnt)) return false;
        tnt.onCaughtFire(state, level, pos, null, player);
        level.removeBlock(pos, false);
        return true;
    }
}
