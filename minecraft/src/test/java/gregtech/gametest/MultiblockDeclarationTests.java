package gregtech.gametest;

import gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.content.multiblock.ControllerStructureLayouts;
import gregtech.content.multiblock.OriginalMultiblockPortRules;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.multiblock.MultiblockLayout.Role;
import gregtech.engine.multiblock.tracking.*;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Registration, saved-entity factories and previews must describe the same controllers. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MultiblockDeclarationTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void registeredControllersUseTheSameDeclaration(GameTestHelper helper) {
        var controllers = MultiblockRegistration.controllers();
        int discovered = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gt6ce")
                    || !(block instanceof EntityBlock source)) continue;
            var placed = source.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            if (!(placed instanceof TrackedMultiblock tracked) || tracked.structureDefinition().formsImmediately()) continue;
            discovered++;
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            helper.assertTrue(controllers.containsKey(block), "Missing declared controller " + id);
            var loaded = placed.getType().create(BlockPos.ZERO, block.defaultBlockState());
            helper.assertTrue(loaded != null && loaded.getClass() == placed.getClass(), "Saved entity factory differs: " + id);
            var pattern = ((StructureDefinition.Declarative) tracked.structureDefinition()).compile(Direction.NORTH);
            var preview = ControllerStructureLayouts.plans().get(block);
            var expected = StructurePlan.preview(pattern).stream()
                    .map(cell -> new StructurePlan.Cell(cell.offset(), cell.specification(), null)).toList();
            helper.assertTrue(expected.equals(preview), "Preview cells or roles differ: " + id);
        }
        helper.assertTrue(discovered > 0, "No built-in multiblocks were checked");
        var recipes = gregtech.compat.jei.MultiblockInfoData.recipes();
        var items = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (var info : recipes) helper.assertTrue(items.add(info.controller().getItem()), "Duplicate controller item page");
        var lightning = recipes.stream().filter(info -> info.blocks().values().stream()
                .anyMatch(stack -> stack.is(gregtech.content.multiblock.LargeMachineParts.block(18104).asItem())))
                .findFirst().orElseThrow(() -> new AssertionError("Lightning preview lost its optional rod"));
        helper.assertTrue(gregtech.compat.jei.MultiblockInfoData.parts(lightning).stream()
                .anyMatch(stack -> stack.is(gregtech.content.multiblock.LargeMachineParts.block(18104).asItem()) && stack.getCount() == 1),
                "Lightning materials lost the example rod");
        System.out.println("MULTIBLOCK_DECLARATIONS controllers=" + discovered);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void largeBoilerKeepsFacingAndPortAssignments(GameTestHelper helper) {
        var controller = GTMultiblocks.findPart(17202).get();
        var origin = helper.absolutePos(new BlockPos(3, 1, 2));
        var level = helper.getLevel();
        level.setBlockAndUpdate(origin,
                controller.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.assertTrue(level.getBlockEntity(origin) instanceof OriginalLargeBoilerControllerBlockEntity,
                "Titanium boiler controller has no block entity");
        var boiler = (OriginalLargeBoilerControllerBlockEntity) level.getBlockEntity(origin);
        var definition = boiler.createStructureDefinition();
        var openings = Set.of(new BlockPos(0, 2, 1), new BlockPos(-1, 1, 1), new BlockPos(1, 1, 1),
                new BlockPos(0, 1, 0), new BlockPos(0, 1, 2));
        for (Direction front : Direction.Plane.HORIZONTAL) {
            var cells = StructurePlan.minimum(definition.compile(front));
            helper.assertTrue(cells.size() == 35, "Expected 35 checked boiler cells facing " + front);
            helper.assertTrue(cells.stream().noneMatch(cell -> cell.offset().equals(BlockPos.ZERO)),
                    "The boiler matcher includes the controller position");
            for (var cell : cells) {
                var spec = cell.specification();
                var local = new BlockPos(spec.right(), spec.up(), spec.back());
                helper.assertTrue(cell.offset().equals(spec.at(BlockPos.ZERO, front)),
                        "Wrong boiler offset for " + spec + " facing " + front);
                helper.assertTrue(spec.design() == (openings.contains(local) ? 1 : -1),
                        "Wrong opening design at " + local);
                var role = switch (local.getY()) {
                    case -1 -> Role.HEAT_INPUT;
                    case 0 -> Role.FLUID_INPUT;
                    case 1 -> local.equals(new BlockPos(0, 1, 1)) ? Role.AIR : Role.FLUID_OUTPUT;
                    case 2 -> Role.FLUID_OUTPUT;
                    default -> throw new AssertionError("Unexpected boiler cell " + spec);
                };
                helper.assertTrue(spec.role() == role, "Wrong boiler port role at " + local);
                switch (role) {
                    case HEAT_INPUT -> helper.assertTrue(spec.part() == 18101, "Expected a heat transmitter");
                    case FLUID_INPUT -> helper.assertTrue(spec.part() == 18026
                            && spec.ports().equals(OriginalMultiblockPortRules.BOILER_WATER),
                            "Expected a titanium wall with the boiler water port");
                    case FLUID_OUTPUT -> helper.assertTrue(spec.part() == 18026
                            && spec.ports().equals(OriginalMultiblockPortRules.BOILER_STEAM),
                            "Expected a titanium wall with the boiler steam port");
                    default -> helper.assertTrue(spec.part() == 0, "Expected an empty boiler cavity");
                }
            }
        }
        for (var cell : StructurePlan.minimum(definition.compile(Direction.EAST))) {
            level.setBlockAndUpdate(origin.offset(cell.offset()), cell.specification().block().defaultBlockState());
        }
        var cavity = origin.west().above();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(boiler.isStructureOk(), "Large boiler did not form"))
                .thenExecute(() -> level.setBlockAndUpdate(cavity, Blocks.DIRT.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertFalse(boiler.isStructureOk(), "Boiler formed without a hollow center"))
                .thenSucceed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void incorrectControllerClassIsRejected(GameTestHelper helper) {
        var furnace = new net.minecraft.world.level.block.entity.FurnaceBlockEntity(
                BlockPos.ZERO, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
        try {
            ControllerDeclarations.COKE_OVEN.bind(furnace);
            throw new AssertionError("A furnace was accepted as a coke-oven controller");
        } catch (IllegalStateException expected) {
            helper.assertTrue(expected.getMessage().contains("cokeoven"), "Error must identify the structure");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void savedEntityTypeRejectsAnotherControllersBlock(GameTestHelper helper) {
        try {
            GTBlockEntities.COKE_OVEN.get().create(
                    BlockPos.ZERO, GTMultiblocks.CRYO_DISTILLATION_MAIN.get().defaultBlockState());
            throw new AssertionError("A coke-oven entity type accepted a cryogenic distillation controller");
        } catch (IllegalStateException expected) {
            helper.assertTrue(expected.getMessage().contains("gt6ce:coke_oven"), "Error must identify the saved entity type");
            helper.assertTrue(expected.getMessage().contains("gt6ce:cryo_distillation_main"), "Error must identify the block");
        }
        helper.succeed();
    }
}
