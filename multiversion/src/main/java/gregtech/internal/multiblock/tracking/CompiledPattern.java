package gregtech.internal.multiblock.tracking;

import gregtech.internal.multiblock.MultiblockLayout.Role;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.multiblock.PlainStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A layout resolved for one facing: offsets instead of {@link BlockPos}, one matcher per cell.
 * Immutable. Built when a tracker first evaluates and again whenever the epoch or facing changes.
 */
public final class CompiledPattern {
    public static final int FAILED = -1;
    public static final int UNKNOWN = Integer.MIN_VALUE;

    private final Direction front;
    private final Fixed[] fixed;
    private final Group[] groups;
    private final RepairCell[] repeatCells;
    private final int[] groupFirst;
    private final int[] groupCells;
    private final Map<Long, Integer> fixedByOffset;
    private final Map<Long, int[]> repeatByColumn;
    private final List<Aggregate> aggregates;
    private final int maxCopies;

    private CompiledPattern(Direction front, List<Fixed> fixed, List<Group> groups,
                            List<RepairCell> repeatCells, List<Aggregate> aggregates) {
        this.front = front;
        this.fixed = fixed.toArray(Fixed[]::new);
        this.groups = groups.toArray(Group[]::new);
        this.repeatCells = repeatCells.toArray(RepairCell[]::new);
        this.groupFirst = new int[this.groups.length];
        this.groupCells = new int[this.groups.length];
        for (int index = 0; index < this.repeatCells.length; index++) groupCells[this.repeatCells[index].group]++;
        this.aggregates = aggregates == null ? List.of() : List.copyOf(aggregates);
        this.fixedByOffset = new HashMap<>(this.fixed.length * 2);
        for (int index = 0; index < this.fixed.length; index++) {
            Fixed cell = this.fixed[index];
            fixedByOffset.put(key(cell.dx, cell.dy, cell.dz), index);
        }
        Map<Long, List<Integer>> columns = new HashMap<>();
        int max = 0;
        for (int index = 0; index < this.repeatCells.length; index++) {
            RepairCell cell = this.repeatCells[index];
            columns.computeIfAbsent(columnKey(cell.dx, cell.dz), ignored -> new ArrayList<>()).add(index);
        }
        this.repeatByColumn = new HashMap<>(columns.size() * 2);
        for (var entry : columns.entrySet()) {
            int[] list = new int[entry.getValue().size()];
            for (int index = 0; index < list.length; index++) list[index] = entry.getValue().get(index);
            this.repeatByColumn.put(entry.getKey(), list);
        }
        int first = 0;
        for (int group = 0; group < this.groups.length; group++) {
            groupFirst[group] = first;
            first += groupCells[group];
        }
        for (Group group : this.groups) max = Math.max(max, group.limit());
        this.maxCopies = max;
    }

    public static CompiledPattern compile(TrackedMultiblock machine, LargeMachineLayouts.Layout layout,
                                          Direction front, List<Aggregate> aggregates) {
        if (layout == null || front == null) return null;
        List<Fixed> fixed = new ArrayList<>();
        for (LargeMachineLayouts.Cell cell : layout.cells()) {
            int[] offset = offset(front, cell.right(), cell.up(), cell.back());
            fixed.add(new Fixed(offset[0], offset[1], offset[2], cell.role(),
                    cell.role() == Role.AIR ? null : matcher(machine, cell), cell));
        }
        List<Group> groups = new ArrayList<>();
        List<RepairCell> repeat = new ArrayList<>();
        for (LargeMachineLayouts.Repeat group : layout.repeats()) {
            int groupIndex = groups.size();
            groups.add(new Group(group.key(), group.min(), group.max(),
                    group.max() < 0 ? LargeMachineLayouts.MAX_REPEATS : group.max()));
            int step = group.step() * group.height();
            for (LargeMachineLayouts.Cell cell : group.cells()) {
                int[] offset = offset(front, cell.right(), 0, cell.back());
                int dy = group.up() + group.step() * cell.up();
                repeat.add(new RepairCell(groupIndex, offset[0], dy, offset[2], step, cell.role(),
                        cell.role() == Role.AIR ? null : matcher(machine, cell), cell));
            }
        }
        return new CompiledPattern(front, fixed, groups, repeat, aggregates);
    }

    private static Matcher matcher(TrackedMultiblock machine, LargeMachineLayouts.Cell cell) {
        List<Candidate> candidates = new ArrayList<>(1 + cell.alternatives().size());
        candidates.add(new Candidate(machine, cell, cell.part(), cell.plain()));
        for (var alternative : cell.alternatives()) {
            LargeMachineLayouts.Cell choice = new LargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(),
                    alternative.part(), cell.role(), alternative.plain(), cell.design(), List.of());
            candidates.add(new Candidate(machine, choice, alternative.part(), alternative.plain()));
        }
        return new Matcher(candidates.toArray(Candidate[]::new));
    }

    /** Fixed world offset of a cell relative to the controller for one facing. */
    private static int[] offset(Direction front, int right, int up, int back) {
        Direction sideways = front.getClockWise();
        Direction backwards = front.getOpposite();
        int dx = sideways.getStepX() * right + backwards.getStepX() * back;
        int dz = sideways.getStepZ() * right + backwards.getStepZ() * back;
        return new int[]{dx, up, dz};
    }

    public Direction front() {
        return front;
    }

    public List<Aggregate> aggregates() {
        return aggregates;
    }

    public int fixedCount() {
        return fixed.length;
    }

    public int dx(int index) {
        return fixed[index].dx;
    }

    public int dy(int index) {
        return fixed[index].dy;
    }

    public int dz(int index) {
        return fixed[index].dz;
    }

    public boolean air(int index) {
        return fixed[index].matcher == null;
    }

    public Matcher matcher(int index) {
        return fixed[index].matcher;
    }

    public Role role(int index) {
        return fixed[index].role;
    }

    public int part(int index, int choice) {
        Fixed cell = fixed[index];
        return cell.matcher == null ? cell.cell.part() : cell.matcher.candidates[choice].part;
    }

    public String plain(int index, int choice) {
        Fixed cell = fixed[index];
        return cell.matcher == null ? cell.cell.plain() : cell.matcher.candidates[choice].plain;
    }

    public LargeMachineLayouts.Cell cell(int index, int choice) {
        Fixed cell = fixed[index];
        return cell.matcher == null ? cell.cell : cell.matcher.candidates[choice].cell;
    }

    public int groupCount() {
        return groups.length;
    }

    public String groupKey(int group) {
        return groups[group].key;
    }

    public int groupMinimum(int group) {
        return groups[group].min;
    }

    public int groupLimit(int group, int worldHeight) {
        return Math.min(Math.max(1, worldHeight), groups[group].limit);
    }

    public int repeatCellCount() {
        return repeatCells.length;
    }

    /** First repeat-cell index of a group. Cells of one group are contiguous and in layout order. */
    public int groupFirstCell(int group) {
        return groupFirst[group];
    }

    public int groupCellCount(int group) {
        return groupCells[group];
    }

    public int repeatGroup(int cell) {
        return repeatCells[cell].group;
    }

    public int repeatDx(int cell) {
        return repeatCells[cell].dx;
    }

    public int repeatDy(int cell, int copy) {
        return repeatCells[cell].dy + repeatCells[cell].step * copy;
    }

    public int repeatDz(int cell) {
        return repeatCells[cell].dz;
    }

    public boolean repeatAir(int cell) {
        return repeatCells[cell].matcher == null;
    }

    public Matcher repeatMatcher(int cell) {
        return repeatCells[cell].matcher;
    }

    public Role repeatRole(int cell) {
        return repeatCells[cell].role;
    }

    public int repeatPart(int cell, int choice) {
        RepairCell value = repeatCells[cell];
        return value.matcher == null ? value.cell.part() : value.matcher.candidates[choice].part;
    }

    public String repeatPlain(int cell, int choice) {
        RepairCell value = repeatCells[cell];
        return value.matcher == null ? value.cell.plain() : value.matcher.candidates[choice].plain;
    }

    public LargeMachineLayouts.Cell repeatCell(int cell, int choice) {
        RepairCell value = repeatCells[cell];
        return value.matcher == null ? value.cell : value.matcher.candidates[choice].cell;
    }

    /** Highest copy count any group can reach before the world height cap. */
    public int maxCopies() {
        return maxCopies;
    }

    public int capacity(int worldHeight) {
        return maxCopies == 0 ? 0 : Math.min(maxCopies, Math.max(1, worldHeight));
    }

    /** Calls {@code visitor} for every compiled cell at this offset. Returns true when any matched. */
    public boolean locate(int dx, int dy, int dz, CellVisitor visitor) {
        boolean found = false;
        Integer index = fixedByOffset.get(key(dx, dy, dz));
        if (index != null) {
            visitor.visit(false, index, 0);
            found = true;
        }
        int[] cells = repeatByColumn.get(columnKey(dx, dz));
        if (cells != null) {
            for (int cell : cells) {
                int step = repeatCells[cell].step;
                int delta = dy - repeatCells[cell].dy;
                if (delta % step != 0) continue;
                int copy = delta / step;
                if (copy < 0) continue;
                visitor.visit(true, cell, copy);
                found = true;
            }
        }
        return found;
    }

    @FunctionalInterface
    public interface CellVisitor {
        void visit(boolean repeat, int index, int copy);
    }

    public static final class Matcher {
        private final Candidate[] candidates;

        private Matcher(Candidate[] candidates) {
            this.candidates = candidates;
        }

        /** Index of the first accepted candidate, or {@link #FAILED}. */
        public int match(Level level, BlockPos pos, BlockState state) {
            BlockEntity entity = null;
            for (int index = 0; index < candidates.length; index++) {
                Candidate candidate = candidates[index];
                if (candidate.plain != null) {
                    if (PlainStructureBlocks.matches(level, pos, candidate.plain, state)) return index;
                } else {
                    if (entity == null) entity = level.getBlockEntity(pos);
                    if (entity instanceof MultiblockPortBlockEntity
                            && candidate.machine.acceptsTrackedPart(candidate.cell, candidate.expected(), state.getBlock()))
                        return index;
                }
            }
            return FAILED;
        }
    }

    private record Candidate(TrackedMultiblock machine, LargeMachineLayouts.Cell cell, int part, String plain) {
        private Block expected() {
            return plain == null ? LargeMachineParts.block(part) : null;
        }
    }

    private record Fixed(int dx, int dy, int dz, Role role, Matcher matcher, LargeMachineLayouts.Cell cell) {}
    private record RepairCell(int group, int dx, int dy, int dz, int step, Role role, Matcher matcher,
                              LargeMachineLayouts.Cell cell) {}
    private record Group(String key, int min, int max, int limit) {}

    private static long key(int dx, int dy, int dz) {
        return ((long) (dx & 0x1FFFFF) << 42) | ((long) (dy & 0x1FFFFF) << 21) | (dz & 0x1FFFFF);
    }

    private static long columnKey(int dx, int dz) {
        return ((long) (dx & 0x1FFFFF) << 21) | (dz & 0x1FFFFF);
    }
}
