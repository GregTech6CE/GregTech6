package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Behaviour gates for GT6 MultiTileEntityCrucible IDs 17302–17312. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LargeCrucibleControllerTests {
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private static LargeCrucibleControllerBlockEntity build(GameTestHelper helper, LargeCrucibleSpecs.Variant variant) {
        helper.setBlock(CENTER, LargeMachineParts.block(variant.originalId()).defaultBlockState());
        for (int y = 0; y < 3; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || z != 0) helper.setBlock(CENTER.offset(x, y, z), variant.wall().defaultBlockState());
        }
        return (LargeCrucibleControllerBlockEntity) helper.getBlockEntity(CENTER);
    }

    @GameTest(template = "test_empty")
    public static void formedVesselHidesPartsAndSharesCrucibleJade(GameTestHelper helper) {
        var be = build(helper, LargeCrucibleSpecs.byOriginalId(17309));
        helper.assertTrue(be.isStructureOk() && be.isFormedForRendering(), "empty vessel forms before any contents exist");
        helper.assertTrue(be.getBlockState().getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE,
                "formed main is replaced by the whole-vessel renderer");
        for (int y=0;y<3;y++) for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            if (x == 0 && z == 0) continue;
            helper.assertTrue(helper.getBlockState(CENTER.offset(x,y,z)).getRenderShape()
                    == net.minecraft.world.level.block.RenderShape.INVISIBLE, "all 24 structural cubes hide after formation");
        }
        var part = (MultiblockPortBlockEntity)helper.getBlockEntity(CENTER.offset(1,1,0));
        var data = new net.minecraft.nbt.CompoundTag();
        var accessor = (snownee.jade.api.BlockAccessor)java.lang.reflect.Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> method.getName().equals("getBlockEntity") ? part : null);
        com.gregtech.gregtech.integration.jade.CrucibleJadeProvider.INSTANCE.appendServerData(data,accessor);
        helper.assertTrue(data.getLong("gt_crucible_temp") == be.getTemperature()
                && data.getLong("gt_crucible_melt_limit") == be.getMeltDownLimitK()
                && data.contains("gt_crucible_content"), "Jade on a wall receives its controller's actual crucible data");
        helper.setBlock(CENTER.offset(-1,2,0),Blocks.AIR);
        helper.assertTrue(!be.isStructureOk() && !be.isFormedForRendering(), "breaking the structure disables its large hull");
        helper.assertTrue(part.getBlockState().getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL,
                "remaining walls return to their ordinary models");
        var after = new net.minecraft.nbt.CompoundTag();
        com.gregtech.gregtech.integration.jade.CrucibleJadeProvider.INSTANCE.appendServerData(after,accessor);
        helper.assertTrue(after.isEmpty(), "unbound walls stop advertising another block's contents");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void eightGt6VariantsAreControllersWithMaterialWallsAndSurvivalRecipes(GameTestHelper helper) {
        helper.assertTrue(LargeCrucibleSpecs.all().size() == 8, "GT6 has eight large crucible controllers");
        for (var variant : LargeCrucibleSpecs.all()) {
            helper.assertTrue(variant.wall() != Blocks.AIR, variant.path() + " wall is registered");
            helper.assertTrue(LargeMachineParts.block(variant.originalId()) instanceof
                    com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock,
                    variant.path() + " is a controller, not an inert port");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "machines/multiblock/" + variant.path())).isPresent(),
                    variant.path() + " has a survival recipe");
            helper.assertTrue(variant.crucible().thermalMassKg() > 0, variant.path() + " has thermal mass");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void openVesselRequiresCorrectWallAndRoutesBottomHeatTopItems(GameTestHelper helper) {
        var steel = LargeCrucibleSpecs.byOriginalId(17309);
        var be = build(helper, steel);
        helper.assertTrue(be.isStructureOk(), "3x3x3 steel ring with open centre forms");
        var bottom = (MultiblockPortBlockEntity) helper.getBlockEntity(CENTER.offset(1, 0, 0));
        var middle = (MultiblockPortBlockEntity) helper.getBlockEntity(CENTER.offset(1, 1, 0));
        var upper = (MultiblockPortBlockEntity) helper.getBlockEntity(CENTER.offset(1, 2, 0));
        helper.assertTrue(bottom.getEnergyTypes(Direction.EAST).contains(GregTechTags.Energy.HU),
                "bottom wall accepts heat");
        helper.assertTrue(!middle.getEnergyTypes(Direction.EAST).contains(GregTechTags.Energy.HU),
                "middle wall is for casting, not heat");
        helper.assertTrue(upper.isCrucibleUpperPort() && be.portItems(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.ITEM_FLUID_IO) != null,
                "upper wall provides the input cache");
        helper.assertTrue(bottom.doInject(GregTechTags.Energy.HU, Direction.EAST, 2048, 5, true) == 5,
                "bottom wall forwards HU packets");
        helper.assertTrue(be.getEnergyBuffer() == 10_240, "heat reaches the crucible material engine");
        long before = be.getTemperature();
        LargeCrucibleControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CENTER), be.getBlockState(), be);
        helper.assertTrue(be.getTemperature() > before, "HU raises the crucible temperature");
        helper.setBlock(CENTER.above(), Blocks.STONE);
        helper.assertTrue(!be.isStructureOk(), "the vessel must stay open above its controller");
        helper.setBlock(CENTER.above(), Blocks.AIR);
        helper.setBlock(CENTER.offset(1, 2, 0), LargeCrucibleSpecs.byOriginalId(17307).wall());
        helper.assertTrue(!be.isStructureOk(), "another material's wall cannot complete the structure");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void materialCapacityAndTopFluidPortShareOneContentList(GameTestHelper helper) {
        var be = build(helper, LargeCrucibleSpecs.byOriginalId(17302));
        helper.assertTrue(be.isStructureOk(), "stainless crucible forms with stainless wall");
        var tag = be.saveWithoutMetadata();
        tag.putLong("gt.temperature", Materials.Iron.getMeltingPoint() + 25L);
        be.load(tag);
        helper.assertTrue(be.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Iron, GTValues.U)),
                be.getTemperature()), "hot iron enters material storage");
        var port = be.portFluids(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.ITEM_FLUID_IO);
        var molten = GTFluids.stack("GenMolten_" + Materials.Iron.getName(), 1);
        helper.assertTrue(molten != null, "generated molten iron exists");
        helper.assertTrue(port.getFluidInTank(0).getAmount() == 144, "one iron unit is 144 mB");
        helper.assertTrue(port.drain(72, IFluidHandler.FluidAction.EXECUTE).getAmount() == 72,
                "top fluid output consumes half an iron unit");
        helper.assertTrue(be.getCrucibleContentAmount() == GTValues.U / 2,
                "fluid drain and material storage are one inventory");
        helper.assertTrue(port.fill(new net.minecraftforge.fluids.FluidStack(molten.getFluid(), 72),
                IFluidHandler.FluidAction.EXECUTE) == 72, "top fluid input restores the same iron");
        helper.assertTrue(be.getCrucibleContentAmount() == GTValues.U, "fluid fill restores material units");
        helper.assertTrue(be.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Iron,
                LargeCrucibleControllerBlockEntity.MAX_AMOUNT - GTValues.U)), be.getTemperature()),
                "exactly 432 material units fit");
        helper.assertTrue(!be.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Iron, 1)),
                be.getTemperature()), "capacity rejects the next unit");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void incompleteWallCoolsWithoutProcessingOrSpendingHeat(GameTestHelper helper) {
        var variant = LargeCrucibleSpecs.byOriginalId(17309);
        var be = build(helper, variant);
        helper.assertTrue(be.isStructureOk(), "steel crucible starts formed");
        var bottom = (MultiblockPortBlockEntity) helper.getBlockEntity(CENTER.offset(1, 0, 0));
        helper.assertTrue(bottom.doInject(GregTechTags.Energy.HU, Direction.EAST, 2048, 5, true) == 5,
                "formed bottom wall accepts buffered heat");
        var upper = be.portItems(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.ITEM_FLUID_IO);
        helper.assertTrue(upper.insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),
                false).isEmpty(), "formed upper wall accepts an iron ingot into the cache");
        var saved = be.saveWithoutMetadata();
        saved.putLong("gt.temperature", 600);
        be.load(saved);
        long heat = be.getEnergyBuffer();
        helper.setBlock(CENTER.offset(1, 2, 0), Blocks.AIR);
        helper.assertTrue(!be.isStructureOk(), "missing upper wall invalidates the vessel");

        helper.runAfterDelay(25, () -> {
            helper.assertTrue(be.getTemperature() < 600 && be.getTemperature() >= 596,
                    "invalid large crucible cools slowly instead of freezing its temperature");
            helper.assertTrue(be.getEnergyBuffer() == heat,
                    "invalid vessel does not consume its buffered HU");
            helper.assertTrue(be.getCrucibleContentAmount() == 0 && be.getCacheStack().getCount() == 1,
                    "invalid vessel does not process the cached ingot");
            helper.succeed();
        });
    }
}
