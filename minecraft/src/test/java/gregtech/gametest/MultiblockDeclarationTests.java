package gregtech.gametest;

import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.content.multiblock.ControllerStructureLayouts;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.multiblock.tracking.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.EntityBlock;
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
