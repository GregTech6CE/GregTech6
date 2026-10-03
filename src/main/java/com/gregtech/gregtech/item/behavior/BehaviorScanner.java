package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.data.GregTechTags;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GT6 {@code Behavior_Scanner} ({@code Behavior_Scanner.java:42-87}) — the Portable Scanner and the
 * Debug Scanner.
 *
 * <h2>Where the actual work is</h2>
 *
 * <p>The behaviour itself is three lines: it calls {@code WD.scan(...)} with its scan level and the
 * block's coordinates ({@code :53}), pays for it out of the item's energy buffer, and chats the lines
 * back. All the content is {@code WD.scan} ({@code WD.java:911-1055}), which is what this class
 * ports.</p>
 *
 * <h2>Scan levels</h2>
 *
 * <p>{@code MultiItemRandomTools:517} gives the Portable Scanner scan level {@code 2} and
 * {@code :516} gives the Debug Scanner {@code Integer.MAX_VALUE}; {@code WD.java:925} is the only
 * level test in the original — from {@code 10} on it also prints the block's and the block entity's
 * Java class. {@code Behavior_Scanner:62-76} additionally prints the class of an entity that is
 * clicked when the level is above {@code 100}. The port keeps all three thresholds:
 * {@link #CLASS_LEVEL}, {@link #ENTITY_LEVEL}.</p>
 *
 * <h2>Cost</h2>
 *
 * <p>{@code WD.scan} charges {@code CS.V[3] = 512} EU for every block-entity <em>section</em> it can
 * fill in ({@code WD.java:939,943,947,951,959,964,969,988,995,1003,1045} all do
 * {@code rEUAmount += V[3]}), and nothing for the block-level header. The portable scanner's own
 * energy stats are {@code V[3] * 8000} capacity at {@code V[3]} per operation
 * ({@code MultiItemRandomTools:517}), which is exactly one section per operation of a full buffer.
 * {@link #COST_PER_SECTION} keeps the number.</p>
 *
 * <h2>What the port cannot scan</h2>
 *
 * <p>Each of these is a GT6 interface the port has no counterpart for, so the section is dropped
 * rather than faked: {@code ITileEntityWeight} ({@code WD.java:938-941}),
 * {@code ITileEntityTemperature} ({@code :942-945}), {@code ITileEntityGibbl} ({@code :946-949}), the
 * IC2 reactor/chamber, wrenchable, energy sink/source/conductor and energy storage branches
 * ({@code :1010-1042}), {@code IBlockDebugable} ({@code :1044-1048}), the Galacticraft sealable check
 * ({@code :935}) and the {@code BlockScanningEvent} post ({@code :1050-1053}) — the port has no GT6
 * event bus, so the "cancel and rewrite the lines" hook is gone with it.</p>
 *
 * <p>Two 1.7.10 concepts are translated instead of dropped:</p>
 *
 * <ul>
 *   <li>{@code MetaData} ({@code WD.java:923}) does not exist in 1.20.1. The state's properties are
 *       printed in its place, sorted by name so the line is stable.</li>
 *   <li>{@code ITileEntityProgress}/{@code SwitchableOnOff}/{@code SwitchableMode}/
 *       {@code Running*} ({@code WD.java:950-983}) are one interface in the port:
 *       {@link MachineControl}. Its {@code active()} is GT6's "Actively", {@code running()} without
 *       {@code active()} is "Passively" and neither is "Not Possible"; GT6's extra
 *       "Successfully"/"Possible" wording needs state the port does not keep.</li>
 * </ul>
 */
public final class BehaviorScanner {

    /** GT6 {@code CS.V[3]} ({@code CS.java:151}): the EU one scanned section costs ({@code WD.java:939}). */
    public static final long COST_PER_SECTION = com.gregtech.gregtech.content.tool.ScannerEnergyRules.SECTION_COST;

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
     * @param out       the lines are appended here, in the original's order; nothing is added when
     *                  the original would add nothing
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
        var id = ForgeRegistries.BLOCKS.getKey(block);
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
            // WD.java:950-983 — progress, on/off, mode and running, which the original prints as one
            // "State: ON --- Mode: x --- Running: y" line but charges per part.
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
            IFluidHandler handler = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
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
        return com.gregtech.gregtech.content.tool.ConsumableRules.blastResistance(resistance);
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
