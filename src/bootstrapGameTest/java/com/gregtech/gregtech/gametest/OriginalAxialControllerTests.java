package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.LargeDynamoControllerBlock;
import com.gregtech.gregtech.block.machine.LargeGasTurbineControllerBlock;
import com.gregtech.gregtech.block.machine.LargeTurbineControllerBlock;
import com.gregtech.gregtech.blockentity.machine.AxialGeneratorBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.TurbineStructure;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6's numeric 172xx housings must be working controllers, not passive structure ports. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class OriginalAxialControllerTests {
    private static final String[] STEAM_RECIPES = {
            "large_turbine_main", "large_steam_turbine_trinitanium",
            "large_steam_turbine_graphene", "large_steam_turbine_vibramantium"};
    private static final String[] DYNAMO_RECIPES = {
            "large_dynamo_main", "large_dynamo_titanium",
            "large_dynamo_tungstensteel", "large_dynamo_adamantium"};
    private static final String[] GAS_RECIPES = {
            "large_gas_turbine_main", "large_gas_turbine_trinitanium",
            "large_gas_turbine_graphene", "large_gas_turbine_vibramantium"};

    @GameTest(template = "test_blueprint_empty")
    public static void originalSteamAndDynamoHousingsFormAtEveryGrade(GameTestHelper helper) {
        BlockPos origin = new BlockPos(6, 3, 6);
        Direction front = Direction.NORTH;
        for (int gradeIndex = 0; gradeIndex < 4; gradeIndex++) {
            for (boolean steam : new boolean[]{true, false}) {
                int originalId = (steam ? 17211 : 17221) + gradeIndex;
                Block block = LargeMachineParts.block(originalId);
                var grade = (steam ? AxialGeneratorDefinitions.STEAM : AxialGeneratorDefinitions.DYNAMO).get(gradeIndex);
                helper.assertTrue(steam ? block instanceof LargeTurbineControllerBlock : block instanceof LargeDynamoControllerBlock,
                        originalId + " is a functional controller class");
                helper.setBlock(origin, Blocks.AIR);
                helper.setBlock(origin, block.defaultBlockState().setValue(DirectionalBlock.FACING, front));
                grade.cells().forEach((cell, part) -> helper.setBlock(
                        origin.relative(front.getClockWise(), cell.getX()).above(cell.getY()).relative(front.getOpposite(), cell.getZ()), part));
                var machine = (AxialGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(origin));
                helper.assertTrue(machine != null && machine.grade() == grade && machine.isStructureOk(),
                        originalId + " forms with the original grade's walls and coils");
                helper.assertTrue((steam ? GTBlockEntities.LARGE_TURBINE : GTBlockEntities.LARGE_DYNAMO).get()
                                .isValid(machine.getBlockState()), originalId + " belongs to a ticking block entity type");
                var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                        "gregtech", "axial/" + (steam ? STEAM_RECIPES : DYNAMO_RECIPES)[gradeIndex]));
                helper.assertTrue(recipe.isPresent() && recipe.get().getResultItem(helper.getLevel().registryAccess()).is(block.asItem()),
                        originalId + " is the canonical survival recipe output");
                var drops = Block.getDrops(machine.getBlockState(), helper.getLevel(), helper.absolutePos(origin), machine);
                helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()), originalId + " drops its own housing");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_empty")
    public static void originalGasTurbineHousingsFormAndUseOriginalSteamHousing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(6, 3, 6);
        Direction front = Direction.NORTH;
        for (int gradeIndex = 0; gradeIndex < 4; gradeIndex++) {
            int originalId = 17231 + gradeIndex;
            Block block = LargeMachineParts.block(originalId);
            var grade = GasTurbineDefinitions.GRADES.get(gradeIndex);
            helper.assertTrue(block instanceof LargeGasTurbineControllerBlock turbine && turbine.grade() == grade,
                    originalId + " uses its original gas turbine grade");
            helper.setBlock(origin, Blocks.AIR);
            helper.setBlock(origin, block.defaultBlockState().setValue(DirectionalBlock.FACING, front));
            for (var cell : TurbineStructure.LAYOUT.cells()) helper.setBlock(cell.at(origin, front), grade.wall());
            var machine = (LargeGasTurbineControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(origin));
            helper.assertTrue(machine != null && machine.isStructureOk() && machine.grade() == grade,
                    originalId + " forms with the original material wall");
            helper.assertTrue(GTBlockEntities.LARGE_GAS_TURBINE.get().isValid(machine.getBlockState()),
                    originalId + " belongs to the ticking gas turbine block entity type");
            var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                    "gregtech", "axial/" + GAS_RECIPES[gradeIndex]));
            helper.assertTrue(recipe.isPresent() && recipe.get().getResultItem(helper.getLevel().registryAccess()).is(block.asItem()),
                    originalId + " is the canonical survival recipe output");
            var requiredHousing = recipe.orElseThrow().getIngredients().get(4);
            helper.assertTrue(requiredHousing.test(new net.minecraft.world.item.ItemStack(LargeMachineParts.block(17211 + gradeIndex))),
                    originalId + " uses its original steam turbine housing in the recipe");
            var drops = Block.getDrops(machine.getBlockState(), helper.getLevel(), helper.absolutePos(origin), machine);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()), originalId + " drops its own housing");
        }
        helper.succeed();
    }
}
