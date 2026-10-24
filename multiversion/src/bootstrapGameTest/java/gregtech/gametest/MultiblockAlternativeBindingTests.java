package gregtech.gametest;

import gregtech.api.addon.*;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real declaration conversion and controller validation; isolated fixture layout, not global overlays. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MultiblockAlternativeBindingTests {
    private static LargeMachineLayouts.Layout layout(boolean plainFirst, boolean repeated) {
        var part = CellMatch.part("centrifuge_part");
        var plain = CellMatch.block("minecraft:stone");
        var choice = plainFirst ? CellMatch.oneOf(plain, part) : CellMatch.oneOf(part, plain);
        var builder = StructureLayout.builder("alternative_binding_fixture")
                .cell(-1, 0, 0, part, StructureRole.ENERGY_INPUT);
        if (repeated) builder.repeat(LayoutRepeat.builder("layers", 0, 1, 2)
                .cell(1, 0, 0, choice, StructureRole.CASING)
                .cell(2, 0, 0, choice, StructureRole.CASING).build());
        else builder.cell(1, 0, 0, choice, StructureRole.CASING);
        var shared = AddonMachines.sharedLayout("Native alternative fixture", builder.build());
        var repeats = shared.repeats().stream().map(r -> new LargeMachineLayouts.Repeat(
                r.key(), r.up(), r.min(), r.max(), r.step(), r.height(), LargeMachineLayouts.fromShared(r.cells()))).toList();
        return new LargeMachineLayouts.Layout(LargeMachineLayouts.fromShared(shared.cells()), repeats);
    }

    private static LargeRecipeMachineBlockEntity controller(GameTestHelper h, LargeMachineLayouts.Layout layout) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock machine && machine.basicSpec().machineName().equals("largecentrifuge"))
                .findFirst().orElseThrow();
        var pos = h.absolutePos(new BlockPos(3, 3, 3));
        h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        var state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        h.getLevel().setBlock(pos, state, 2);
        var original = (LargeRecipeMachineBlockEntity) h.getLevel().getBlockEntity(pos);
        var fixture = new LargeRecipeMachineBlockEntity(original.getType(), pos, state) {
            @Override protected LargeMachineLayouts.Layout structureLayout() { return layout; }
        };
        fixture.setSpec(original.spec());
        h.getLevel().removeBlockEntity(pos);
        h.getLevel().setBlockEntity(fixture);
        return fixture;
    }

    private static MultiblockPortBlockEntity part(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, LargeMachineParts.block(18100).defaultBlockState(), 2);
        return (MultiblockPortBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void exercise(GameTestHelper h, boolean repeated) {
        // This isolated declaration is converted after registry bootstrap, so its plain block
        // did not participate in PlainStructureBlocks.resolve(). Supply that exact resolution
        // while the fixture runs, then restore the registry-boundary cache even on assertion failure.
        final java.util.Map<String, net.minecraft.world.level.block.Block> resolved;
        try {
            var field = gregtech.content.multiblock.PlainStructureBlocks.class.getDeclaredField("RESOLVED");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cache = (java.util.Map<String, net.minecraft.world.level.block.Block>) field.get(null);
            resolved = cache;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Plain block fixture resolution unavailable", failure);
        }
        var previous = resolved.put("minecraft:stone", Blocks.STONE);
        try {
            exerciseResolved(h, repeated);
        } finally {
            if (previous == null) resolved.remove("minecraft:stone");
            else resolved.put("minecraft:stone", previous);
        }
    }

    private static void exerciseResolved(GameTestHelper h, boolean repeated) {
        for (boolean plainFirst : new boolean[]{false, true}) {
            var machine = controller(h, layout(plainFirst, repeated));
            var origin = machine.getBlockPos();
            var energy = part(h, origin.west());
            var mixedPos = origin.east();
            MultiblockPortBlockEntity incomplete = null;
            if (repeated) {
                h.getLevel().setBlock(origin.east(2), Blocks.STONE.defaultBlockState(), 2);
                incomplete = part(h, mixedPos.above());
                h.getLevel().setBlock(origin.east(2).above(), Blocks.AIR.defaultBlockState(), 2);
            }
            for (boolean usePart : new boolean[]{false, true, false, true}) {
                h.getLevel().setBlock(mixedPos, Blocks.AIR.defaultBlockState(), 2);
                var casing = usePart ? part(h, mixedPos) : null;
                if (!usePart) h.getLevel().setBlock(mixedPos, Blocks.STONE.defaultBlockState(), 2);
                for (int check = 0; check < 2; check++) {
                    h.assertTrue(StructureTestSupport.ok(machine), "valid mixed alternative forms on every check, order=" + plainFirst);
                    h.assertTrue(energy.isBoundTo(origin) && energy.getEnergyTypes(Direction.DOWN).contains(machine.spec().energyTag()),
                            "independent energy role stays bound with its original permissions");
                    if (casing != null) h.assertTrue(casing.isBoundTo(origin) && casing.getEnergyTypes(Direction.DOWN).isEmpty(),
                            "actual alternative part is bound as CASING, without energy privileges");
                    if (incomplete != null) h.assertTrue(!incomplete.isBoundTo(origin), "failed repeat prefix is not claimed");
                }
            }
            h.getLevel().setBlock(mixedPos, Blocks.DIRT.defaultBlockState(), 2);
            h.assertTrue(!StructureTestSupport.ok(machine) && !energy.isBoundTo(origin), "destroyed required cell invalidates and releases loaded ports");
            h.getLevel().setBlock(mixedPos, Blocks.STONE.defaultBlockState(), 2);
            h.assertTrue(StructureTestSupport.ok(machine) && energy.isBoundTo(origin), "restoring valid choice reforms without manual reset");
            h.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), 2);
            for (var pos : new BlockPos[]{origin.west(), mixedPos, mixedPos.above(), origin.east(2), origin.east(2).above()})
                h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void mixedFixedAlternativesBindOnlyActualPartsInEitherOrder(GameTestHelper h) { exercise(h, false); }

    @GameTest(template="test_empty")
    public static void mixedRepeatedAlternativesIgnoreIncompleteCopyAndReform(GameTestHelper h) { exercise(h, true); }
}
