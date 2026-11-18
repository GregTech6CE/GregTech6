package gregtech.content.blockentity.machine;

import gregtech.api.addon.platform.BoundPart;
import gregtech.api.addon.platform.EnergyPart;
import gregtech.api.addon.platform.PartBehavior;
import gregtech.api.addon.platform.PartContext;
import gregtech.api.addon.platform.StructureAccess;
import gregtech.api.addon.platform.StructureCell;
import gregtech.api.addon.platform.StructureProblem;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.multiblock.StructureRole;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.engine.multiblock.PartBindings;
import gregtech.engine.multiblock.tracking.PatternMatch;
import gregtech.engine.multiblock.tracking.StructureClaims;
import gregtech.engine.multiblock.tracking.StructureDiagnostic;
import gregtech.engine.multiblock.tracking.StructureTracking;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Owns the controller's claims and the snapshots exposed to addon callbacks. */
final class BehaviorMachineStructure implements StructureAccess {
    private final BehaviorMachineBlockEntity machine;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private boolean formed;
    private List<EnergyPart> energyParts = List.of();
    private List<BoundPart> parts = List.of();
    private List<StructureCell> cells = List.of();
    private Map<String, Integer> repeatCounts = Map.of();

    BehaviorMachineStructure(BehaviorMachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public boolean formed() {
        return formed;
    }

    @Override
    public List<EnergyPart> energyParts() {
        return formed ? energyParts : List.of();
    }

    @Override
    public List<BoundPart> parts() {
        return formed ? parts : List.of();
    }

    @Override
    public List<StructureCell> cells() {
        return formed ? cells : List.of();
    }

    @Override
    public int repeats(String key) {
        return formed && key != null ? repeatCounts.getOrDefault(key, 0) : 0;
    }

    @Override
    public boolean conditionsMet() {
        for (EnergyPart part : energyParts()) if (part.condition() && !part.satisfied()) return false;
        return true;
    }

    @Override
    public List<StructureProblem> diagnose() {
        return StructureDiagnostic.inspect(machine);
    }

    @Nullable
    EnergyPartView boundEnergyPart(String name) {
        if (!formed || name == null) return null;
        for (EnergyPart part : energyParts)
            if (part instanceof EnergyPartView view && view.name().equals(name)) return view;
        return null;
    }

    void setFormed(boolean value) {
        formed = value;
        if (!value) clearSnapshots();
    }

    boolean apply(PatternMatch match) {
        Level level = machine.getLevel();
        if (level == null || machine.isRemoved()) {
            bindings.clear(this::releasePart);
            return false;
        }

        List<PatternMatch.FixedCell> fixedCells = match.fixedCells();
        List<LargeMachineLayouts.Placed> repeatedCells = match.repeatPlaced();
        var matchedParts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : fixedCells) addMatchedPart(matchedParts, fixed.pos(), fixed.cell());
        for (var repeated : repeatedCells) addMatchedPart(matchedParts, repeated.pos(), repeated.cell());

        BlockPos owner = machine.getBlockPos();
        var claimed = StructureClaims.update(
                machine,
                bindings,
                matchedParts,
                pos -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(owner),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(owner, role),
                this::releasePart);
        if (claimed == StructureClaims.Result.FAILED) return false;

        var foundEnergy = new ArrayList<EnergyPart>();
        var foundParts = new ArrayList<BoundPart>();
        for (BlockPos pos : matchedParts.keySet()) {
            var occupant = level.getBlockEntity(pos);
            if (occupant instanceof AddonEnergyPartBlockEntity energy) {
                foundEnergy.add(new EnergyPartView(energy));
                foundParts.add(new BoundView(energy, energy, energy.function()));
            } else if (occupant instanceof AddonFunctionalPartBlockEntity functional) {
                foundParts.add(new BoundView(functional, null, functional.function()));
            }
        }
        energyParts = List.copyOf(foundEnergy);
        parts = List.copyOf(foundParts);
        repeatCounts = Map.copyOf(match.counts());
        var snapshot = new ArrayList<StructureCell>(fixedCells.size() + repeatedCells.size());
        for (var fixed : fixedCells) snapshot.add(snapshotCell(level, fixed.pos(), fixed.cell()));
        for (var repeated : repeatedCells) snapshot.add(snapshotCell(level, repeated.pos(), repeated.cell()));
        cells = List.copyOf(snapshot);
        return true;
    }

    /** Keep recognized parts bound while a player is still building the structure. */
    void applyIncomplete(PatternMatch match) {
        if (machine.getLevel() == null || machine.isRemoved()) return;
        StructureClaims.bindMatched(machine, bindings, match, false, this::releasePart);
    }

    void releaseClaims() {
        bindings.clear(this::releasePart);
        StructureClaims.cleared(machine);
        clearSnapshots();
    }

    void onRemoved() {
        if (!StructureTracking.unloading(machine)) bindings.clear(this::releasePart);
        StructureTracking.onRemoved(machine);
        setFormed(false);
    }

    private static void addMatchedPart(
            Map<BlockPos, MultiblockLayout.Role> parts, BlockPos pos, LargeMachineLayouts.Cell cell) {
        if (cell.role() != MultiblockLayout.Role.AIR && !cell.plainBlock()) parts.put(pos, cell.role());
    }

    private static StructureCell snapshotCell(Level level, BlockPos pos, LargeMachineLayouts.Cell cell) {
        return new StructureCell(pos, StructureRole.valueOf(cell.role().name()), level.getBlockState(pos));
    }

    private void clearSnapshots() {
        energyParts = List.of();
        parts = List.of();
        cells = List.of();
        repeatCounts = Map.of();
    }

    private void releasePart(BlockPos pos) {
        Level level = machine.getLevel();
        if (level != null
                && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
            part.release(machine.getBlockPos());
    }

    final class EnergyPartView implements EnergyPart {
        private final AddonEnergyPartBlockEntity part;

        private EnergyPartView(AddonEnergyPartBlockEntity part) {
            this.part = part;
        }

        @Override
        public BlockPos pos() {
            return part.getBlockPos();
        }

        @Override
        public String name() {
            return part.path();
        }

        @Override
        public long stored() {
            return part.stored();
        }

        @Override
        public long capacity() {
            return part.capacity();
        }

        @Override
        public long charge(long amount) {
            return part.isBoundTo(machine.getBlockPos()) ? part.charge(amount) : 0;
        }

        @Override
        public void setOutput(long size, long amount) {
            if (!part.isBoundTo(machine.getBlockPos()))
                throw new IllegalStateException("Energy part is not bound to this machine");
            part.setOutput(size, amount);
        }

        @Override
        public void stop() {
            if (part.isBoundTo(machine.getBlockPos())) part.stop();
        }

        @Override
        public boolean condition() {
            return part.condition();
        }

        @Override
        public boolean satisfied() {
            return part.satisfied();
        }

        boolean controllerInput() {
            return output() != null && part.declaration().energyInput() != null;
        }

        @Nullable
        EnergyOutput output() {
            return part.declaration().energy();
        }

        String outputType() {
            return part.outputType();
        }
    }

    private final class BoundView implements BoundPart {
        private final MultiblockPortBlockEntity port;

        @Nullable
        private final AddonEnergyPartBlockEntity energy;

        @Nullable
        private final FunctionalPartRuntime function;

        private BoundView(
                MultiblockPortBlockEntity port,
                @Nullable AddonEnergyPartBlockEntity energy,
                @Nullable FunctionalPartRuntime function) {
            this.port = port;
            this.energy = energy;
            this.function = function;
        }

        private boolean live() {
            return !port.isRemoved() && port.isBoundTo(machine.getBlockPos());
        }

        @Override
        public BlockPos pos() {
            return port.getBlockPos();
        }

        @Override
        public String name() {
            if (energy != null) return energy.path();
            return port instanceof AddonFunctionalPartBlockEntity functional ? functional.name() : "";
        }

        @Override
        public EnergyPart energy() {
            return energy != null && live() ? new EnergyPartView(energy) : null;
        }

        @Override
        public PartBehavior behavior() {
            return live() && function != null ? function.behavior() : null;
        }

        @Override
        public PartContext context() {
            return behavior() == null ? null : function.context();
        }

        @Override
        public int slots() {
            return live() && function != null ? function.slotCount() : 0;
        }

        @Override
        public ItemStack item(int slot) {
            return live() && function != null ? function.item(slot) : ItemStack.EMPTY;
        }
    }
}
