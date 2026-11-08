package gregtech.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import gregtech.content.hazard.WorldFluidEffects;
import gregtech.content.worldgen.FluidSpringRules;
import gregtech.engine.fluid.FluidRenderLayers;
import gregtech.engine.fluid.LegacyWorldWater;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class LegacyWaterTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void oldPalettesKeepEveryWaterLevel(GameTestHelper helper) {
        for (String path : LegacyWorldWater.BLOCK_PATHS) {
            for (int level = 0; level < 16; level++) {
                var json = JsonParser.parseString(
                        "{\"Name\":\"gt6ce:" + path + "\",\"Properties\":{\"level\":\"" + level + "\"}}");
                // Chunk section palettes use BlockState.CODEC, not an item or display lookup.
                var state = BlockState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                helper.assertTrue(state.is(Blocks.WATER), "legacy water resolves to vanilla water");
                helper.assertTrue(state.getValue(LiquidBlock.LEVEL) == level, "water level survives migration");
                var saved = NbtUtils.writeBlockState(state);
                helper.assertTrue(saved.getString("Name").equals("minecraft:water"), "resave uses canonical ID");
            }
            helper.assertTrue(
                    BuiltInRegistries.FLUID.get(ResourceLocation.parse("gt6ce:" + path)) == Fluids.WATER,
                    "source fluid alias");
            helper.assertTrue(
                    BuiltInRegistries.FLUID.get(ResourceLocation.parse("gt6ce:" + path + "_flowing"))
                            == Fluids.FLOWING_WATER,
                    "flowing fluid alias");
            helper.assertTrue(
                    !BuiltInRegistries.BLOCK.keySet().contains(ResourceLocation.parse("gt6ce:" + path)),
                    "alias does not create another block registration");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void geothermalSpringRemainsDistinct(GameTestHelper helper) {
        var id = ResourceLocation.parse("gt6ce:watergeothermal");
        var fluid = BuiltInRegistries.FLUID.get(id);
        helper.assertTrue(fluid != Fluids.EMPTY && fluid != Fluids.WATER, "geothermal fluid restored");
        helper.assertTrue(
                BuiltInRegistries.BLOCK
                                .get(id)
                                .defaultBlockState()
                                .getFluidState()
                                .getType()
                        == fluid,
                "geothermal world block uses its own fluid");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("gt6ce:fluid_spring_watergeothermal")),
                "spring registered");
        helper.assertTrue(FluidSpringRules.byFluid(id.toString()) != null, "spring included in world generation");
        helper.assertTrue(FluidRenderLayers.worldFluidPaths().contains("watergeothermal"), "world render layer");
        helper.assertTrue(WorldFluidEffects.bathingRows(fluid).size() == 2, "geothermal bathing effects restored");
        helper.assertTrue(LegacyWorldWater.fluid("watergeothermal") == null, "geothermal is not migrated to water");
        helper.succeed();
    }
}
