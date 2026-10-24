package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.addon.LayoutRepeat;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Coordinates and port roles translated from the corresponding GT6 MultiTileEntity classes. */
public final class LargeMachineLayouts {
    public record Cell(int right,int up,int back,int part,Role role,String plain,int design,List<SharedLargeMachineLayouts.Choice> alternatives) {
        public Cell(int right,int up,int back,int part,Role role) { this(right, up, back, part, role, null, -1, java.util.List.of()); }
        public Cell(int right,int up,int back,int part,Role role,String plain) { this(right, up, back, part, role, plain, -1, java.util.List.of()); }
        public Cell(int right,int up,int back,int part,Role role,String plain,int design) { this(right, up, back, part, role, plain, design, java.util.List.of()); }
        public Cell { alternatives = alternatives == null ? java.util.List.of() : java.util.List.copyOf(alternatives); }
        public BlockPos at(BlockPos origin,Direction front) { return at(origin,front,up); }
        public BlockPos at(BlockPos origin,Direction front,int layer) { return origin.relative(front.getClockWise(),right).above(layer).relative(front.getOpposite(),back); }
        public boolean plainBlock() { return plain != null; }
        public Block block() {
            if (plain != null) return PlainStructureBlocks.preview(plain);
            return switch(part) {
            case 18002 -> GTMultiblocks.TANK_WALL.get();
            case 18022 -> GTMultiblocks.TANK_WALL_DENSE.get();
            case 18100 -> GTMultiblocks.CENTRIFUGE_PART.get();
            case 18101 -> GTMultiblocks.HEAT_TRANSMITTER.get();
            case 18102 -> GTMultiblocks.DISTILLATION_TOWER_PART.get();
            case 18105 -> GTMultiblocks.ELECTROLYZER_PART.get();
            case 0 -> net.minecraft.world.level.block.Blocks.AIR;
            default -> LargeMachineParts.block(part);
            };
        }
        public boolean matches(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return plain != null && PlainStructureBlocks.matches(level, pos, plain, state);
        }
    }
    /** Repeat cells keep coordinates relative to the anchor. */
    public record Repeat(String key,int up,int min,int max,int step,int height,List<Cell> cells){
        public int absoluteUp(Cell cell,int copy){return up+step*(cell.up()+copy*height);}
    }
    /** Fixed cells plus repeat groups. */
    public record Layout(List<Cell> cells,List<Repeat> repeats){}
    /** One cell resolved to a world position. */
    public record Placed(Cell cell,BlockPos pos){}
    /** Upper bound for an unbounded repeat; the world height still caps the walk. Same cap as {@link LayoutRepeat#MAX_COPIES}. */
    public static final int MAX_REPEATS=LayoutRepeat.MAX_COPIES;
    /**
     * Resolves every fully matching copy of {@code repeat}. Returns null when fewer than
     * {@code min} copies match. The first copy with a failing cell ends the walk.
     */
    public static List<Placed> repeatCells(net.minecraft.world.level.Level level,BlockPos origin,Direction front,
                                           Repeat repeat,java.util.function.BiPredicate<Cell,BlockPos> matches){
        return repeatCells(level, origin, front, repeat, matches, null);
    }
    /** Same walk, with the number of complete copies written to {@code matchedOut[0]} even on failure. */
    public static List<Placed> repeatCells(net.minecraft.world.level.Level level,BlockPos origin,Direction front,
                                           Repeat repeat,java.util.function.BiPredicate<Cell,BlockPos> matches,
                                           int[] matchedOut){
        int span=Math.max(1,level.getHeight());
        int limit=Math.min(span,repeat.max()<0?MAX_REPEATS:repeat.max());
        var out=new ArrayList<Placed>();
        int matched=0;
        for(int copy=0;copy<limit;copy++){
            var copyCells=new ArrayList<Placed>();
            boolean complete=true;
            for(var cell:repeat.cells()){
                var pos=cell.at(origin,front,repeat.absoluteUp(cell,copy));
                if(!matches.test(cell,pos)){complete=false;break;}
                copyCells.add(new Placed(cell,pos));
            }
            if(!complete)break;
            out.addAll(copyCells);
            matched++;
        }
        if(matchedOut!=null)matchedOut[0]=matched;
        return matched<repeat.min()?null:List.copyOf(out);
    }
    public static List<Cell> fromShared(List<SharedLargeMachineLayouts.Cell> cells){return cells==null?null:cells.stream().map(cell->new Cell(cell.right(),cell.up(),cell.back(),cell.part(),Role.valueOf(cell.role().name()),cell.plain(),cell.design(),cell.alternatives())).toList();}
    /** Matches a plain block choice without resolving a part id. */
    public static boolean matchesPlain(net.minecraft.world.level.Level level,BlockPos pos,String plain){return PlainStructureBlocks.matches(level,pos,plain,level.getBlockState(pos));}
    private static final Map<String,List<Cell>> CACHE=new LinkedHashMap<>();
    private static final Map<String,Layout> LAYOUTS=new LinkedHashMap<>();
    static { for (String machine : SharedLargeMachineLayouts.machines()) CACHE.put(machine, fromShared(SharedLargeMachineLayouts.cells(machine))); }
    /** Fixed cells plus repeats expanded to their minimum count, for previews. */
    public static List<Cell> cells(String machine) {
        var cached = CACHE.get(machine);
        if (cached != null) return cached;
        var shared = SharedLargeMachineLayouts.cells(machine);
        if (shared == null) return null;
        var converted = fromShared(shared);
        CACHE.put(machine, converted);
        return converted;
    }
    /** The layout a structure check reads. Null when the machine has no layout. */
    public static Layout layout(String machine) {
        var cached = LAYOUTS.get(machine);
        if (cached != null) return cached;
        var shared = SharedLargeMachineLayouts.layout(machine);
        if (shared == null) return null;
        var repeats = new ArrayList<Repeat>();
        for (var repeat : shared.repeats())
            repeats.add(new Repeat(repeat.key(), repeat.up(), repeat.min(), repeat.max(), repeat.step(),
                    repeat.height(), fromShared(repeat.cells())));
        var converted = new Layout(fromShared(shared.cells()), List.copyOf(repeats));
        LAYOUTS.put(machine, converted);
        return converted;
    }
    public static Set<String> machines() {
        for (String machine : SharedLargeMachineLayouts.machines()) cells(machine);
        return Set.copyOf(CACHE.keySet());
    }
    /** Drops cached cells so the next check reads a reloaded layout. Built-in caches refill from the baseline. */
    public static void forget(Iterable<String> machines) {
        if (machines == null) return;
        for (String machine : machines) { CACHE.remove(machine); LAYOUTS.remove(machine); }
    }
}
