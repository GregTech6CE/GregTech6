package gregtech.content.item.behavior;

import gregtech.engine.energy.IEnergyBlock;
import gregtech.api.machine.MachineControl;
import gregtech.engine.tool.GTToolHelper;
import gregtech.content.data.BlockHarvestPolicy;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
//?}

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Portable/debug block diagnostics adapted from GT6 Behavior_Scanner and WD.scan.
 * GTMultiItems registers ScannerItem; ItemBehaviors gathers crop or block results,
 * then ScannerItem pays the returned section cost before sending chat lines.
 *
 * <p>Block properties replace old metadata. Class details start at scan level 10;
 * entity class output requires a level above 100. The portable scanner uses level 2.
 * Each reported native section currently costs 512 EU.</p>
 *
 * <p>Alignment remains incomplete. This scan omits physical weight, temperature,
 * compressed-volume and native progress-source diagnostics, even where the port
 * already has source interfaces/getters. Generic MachineControl also does not expose
 * original independent possible/success states, and the scanner currently treats any
 * control as an on/off and running-state source without checking optional support.</p>
 *
 * <p>The original isolated failures per section, requested block debug information,
 * and posted a mutable/cancellable BlockScanningEvent that could change the cost.
 * Those contracts are not implemented here. Legacy IC2/Galacticraft integrations
 * require a separate modern compatibility decision, not a same-name class port.</p>
 */
public final class BehaviorScanner {

    /** GT6 {@code CS.V[3]} ({@code CS.java:151}): the EU one scanned section costs ({@code WD.java:939}). */
    public static final long COST_PER_SECTION = gregtech.engine.tool.ScannerEnergyRules.SECTION_COST;

    /** GT6 {@code WD.java:925}: from this scan level on the Java classes are printed too. */
    public static final int CLASS_LEVEL = 10;

    /** GT6 {@code Behavior_Scanner:62,71}: from this scan level on a clicked entity reports its class. */
    public static final int ENTITY_LEVEL = 100;

    /** GT6 {@code MultiItemRandomTools:517}: the Portable Scanner scans at level 2. */
    public static final int PORTABLE_LEVEL = 2;

    /** GT6 {@code MultiItemRandomTools:516}: the Debug Scanner scans at {@code Integer.MAX_VALUE}. */
    public static final int DEBUG_LEVEL = Integer.MAX_VALUE;

    private BehaviorScanner() {}

    /**
     * GT6 {@code WD.scan} ({@code WD.java:911-1055}).
     *
     * @param level     the level the scanned block stands in
     * @param pos       the scanned block
     * @param side      the face that was clicked; every block-entity getter is asked about it
     * @param scanLevel GT6's scan level ({@link #PORTABLE_LEVEL} or {@link #DEBUG_LEVEL})
     * @param player    the scanning player, or {@code null} for GT6's auto-tool case
     * @param out the current diagnostic lines; supported sections and costs still differ from GT6
     * @return the EU the scan costs, i.e. {@code COST_PER_SECTION} per reported section
     */
    public static long scan(Level level, BlockPos pos, Direction side, int scanLevel, @Nullable Player player,
                            List<String> out) {
        if (out == null) return 0L;
        List<String> lines = new ArrayList<>();
        long cost = 0L;

        BlockState state = level.getBlockState(pos);
        var block = state.getBlock();
        var be = level.getBlockEntity(pos);

        // WD.java:921-936 — the block-level header, which the original never charges for.
        lines.add("--- X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ() + " ---");
        lines.add("Name: " + blockName(level, pos, be) + "  State: " + stateProperties(state));
//? if >=1.21.1 {
        /*var id = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
        var id = ForgeRegistries.BLOCKS.getKey(block);
//?}
        lines.add("Registry: " + (id == null ? "unknown" : id.toString()));
        if (scanLevel >= CLASS_LEVEL) {
            lines.add("Block Class: " + block.getClass().getName());
            if (be != null) lines.add("TileEntity Class: " + be.getClass().getName());
        }
        double resistance = block.getExplosionResistance();
        lines.add("Hardness: " + state.getDestroySpeed(level, pos) + " - " + blastResistance(resistance));
        lines.add(harvestLine(state));
        if (state.is(BlockTags.BEACON_BASE_BLOCKS)) lines.add("Is usable for Beacon Pyramids");

        if (be != null) {
            // WD.scan charges each supported state interface separately. This adapter
            // currently treats every MachineControl as both on/off and running-capable.
            MachineControl control = MachineControl.find(be, side);
            if (control != null) {
                if (control.supportsProgress() && control.progressMax() > 0) {
                    cost += COST_PER_SECTION;
                    lines.add("Progress: " + control.progress() + " / " + control.progressMax());
                }
                List<String> parts = new ArrayList<>();
                cost += COST_PER_SECTION;
                parts.add("State: " + (control.enabled() ? "ON" : "OFF"));
                if (control.supportsMode()) {
                    cost += COST_PER_SECTION;
                    parts.add("Mode: " + control.mode());
                }
                cost += COST_PER_SECTION;
                parts.add("Running: " + (control.active() ? "Actively"
                        : control.running() ? "Passively" : "Not Possible"));
                lines.add(String.join(" --- ", parts));
            }

            // WD.java:987-993 — ITileEntityEnergy.
            if (be instanceof IEnergyBlock energy) {
                cost += COST_PER_SECTION;
                for (GregTechTags.Tag type : energy.getEnergyTypes(side)) {
                    lines.add("Input: " + energy.getEnergySizeInputMin(type, side) + " to "
                            + energy.getEnergySizeInputMax(type, side) + type.getShortName());
                    lines.add("Output: " + energy.getEnergySizeOutputMin(type, side) + " to "
                            + energy.getEnergySizeOutputMax(type, side) + type.getShortName());
                }
            }
            // WD.java:994-999 — ITileEntityEnergyDataCapacitor.
            if (be instanceof IEnergyBlock capacitor) {
                var capacitorTypes = capacitor.getEnergyCapacitorTypes(side);
                if (!capacitorTypes.isEmpty()) {
                    cost += COST_PER_SECTION;
                    for (GregTechTags.Tag type : capacitorTypes) {
                        lines.add("Stored: " + capacitor.getEnergyStored(type, side) + " of "
                                + capacitor.getEnergyCapacity(type, side) + type.getShortName());
                    }
                }
            }
            // WD.java:1002-1008 — modern fluid capabilities expose tanks on the clicked face.
//? if >=1.21.1 {
            /*IFluidHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
            IFluidHandler handler = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
            if (handler == null && be instanceof IFluidHandler direct) handler = direct;
            if (handler != null) {
                cost += COST_PER_SECTION;
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fluid = handler.getFluidInTank(i);
                    lines.add("Tank " + i + ": " + fluid.getAmount() + " / " + handler.getTankCapacity(i) + " "
                            + (fluid.isEmpty() ? "Empty" : fluid.getDisplayName().getString()));
                }
            }
        }

        // Original debug providers and the cancellable scan event run before this return.
        out.addAll(lines);
        return cost;
    }

    /**
     * GT6 {@code Behavior_Scanner:61-67} and {@code :70-76}: an entity clicked with a scanner whose
     * level is above {@link #ENTITY_LEVEL} reports its class name.
     *
     * @return the line to chat, or empty when this scanner's level is too low
     */
    public static Optional<String> scanEntity(Entity entity, int scanLevel) {
        if (entity == null || scanLevel <= ENTITY_LEVEL) return Optional.empty();
        return Optional.of(entity.getClass().getName());
    }

    /**
     * GT6 {@code WD.java:923}: the block entity's inventory name when it has one, else the block's
     * name.
     *
     * <p>{@code IInventory.getInventoryName()} became {@link Nameable#getCustomName()} in 1.20.1;
     * the original's "name is set" test is {@link Nameable#hasCustomName()}.</p>
     */
    private static String blockName(Level level, BlockPos pos, @Nullable net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be instanceof Nameable nameable && nameable.hasCustomName()) {
            Component name = nameable.getCustomName();
            if (name != null && !name.getString().isEmpty()) return name.getString();
        }
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    /**
     * The port's replacement for GT6's {@code "  MetaData: " + aMeta} ({@code WD.java:923}); see the
     * class javadoc.
     */
    private static String stateProperties(BlockState state) {
        Map<Property<?>, Comparable<?>> values = state.getValues();
        if (values.isEmpty()) return "{}";
        List<String> parts = new ArrayList<>(values.size());
        for (var entry : values.entrySet()) {
            parts.add(entry.getKey().getName() + "=" + entry.getValue());
        }
        parts.sort(null);
        return "{" + String.join(", ", parts) + "}";
    }

    /**
     * GT6 {@code LH.getToolTipBlastResistance} ({@code LH.java:272}) without the colour codes: the
     * resistance as {@code int.frac} plus the same verdict words ({@code LH.java:590,617-622}).
     *
     * <p>The last bracket, {@code "(IC2 Nukes can still go through!)"}, is dropped: it needs
     * {@code COMPAT_IC2.isExplosionWhitelisted(block)}, and the port has no IC2.</p>
     */
    public static String blastResistance(double resistance) {
        return gregtech.content.tool.ConsumableRules.blastResistance(resistance);
    }

    /**
     * GT6 {@code WD.java:931-933}: {@code "Hand-Harvestable, but X is faster"} when neither a tool
     * nor a level is required, else {@code "Tool to Harvest: X (level)"}.
     *
     * <p>1.20.1's {@code requiresCorrectToolForDrops()} is the port's closest answer to GT6's
     * {@code getHarvestLevel(meta) == 0 && material.isAdventureModeExempt()}, and the tool name and
     * level come from the port's own two tables: {@link BlockHarvestPolicy#tool} and
     * {@link GTToolHelper#requiredHarvestLevel}.</p>
     */
    public static String harvestLine(BlockState state) {
        BlockHarvestPolicy.Tool tool = BlockHarvestPolicy.tool(state.getBlock());
        String name = toolName(tool);
        int level = GTToolHelper.requiredHarvestLevel(state);
        if (!state.requiresCorrectToolForDrops()) {
            return "Hand-Harvestable, but " + name + " is faster";
        }
        return "Tool to Harvest: " + name + " (" + level + ")";
    }

    /** GT6 {@code Code.capitalise(tHarvestTool)} ({@code WD.java:933}); {@code HAND} prints as None. */
    public static String toolName(BlockHarvestPolicy.Tool tool) {
        return switch (tool) {
            case PICKAXE -> "Pickaxe";
            case AXE -> "Axe";
            case SHOVEL -> "Shovel";
            case SWORD -> "Sword";
            case WRENCH -> "Wrench";
            case CROWBAR -> "Crowbar";
            case CUTTER -> "Wire Cutter";
            case SHEARS -> "Shears";
            case HAND -> "None";
        };
    }
}
