package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.plant.BedrockFlowerBlock;
import com.gregtech.gregtech.block.plant.BedrockHexalilyBlock;
import com.gregtech.gregtech.content.plant.BedrockFlowers;
import com.gregtech.gregtech.content.recipe.BedrockFlowerProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Parity checks against GT6 BlockFlowersA/B source metadata and processing rows. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BedrockFlowerRepairTests {
    private BedrockFlowerRepairTests() {}

    @GameTest(template = "coin_pile_space")
    public static void allSeventeenIndicatorsHaveOriginalMetaAndMaterials(GameTestHelper h) {
        String[] ids = {
                "altered_andesite_buckwheat", "crosby_buckwheat", "alpine_catchfly",
                "viola_calaminaria", "thlaspi_lereschianum", "tufted_evening_primrose",
                "narcissus_sheldonia", "orechid", "hexalily",
                "sagebrush", "four_wing_saltbush", "desert_trumpet", "copper_plant",
                "prince_s_plume", "thompsons_locoweed", "pandanus_candelabrum", "tungstus"
        };
        String[] indicators = {
                "gold", "silver", "copper", "zinc", "nickel", "uranium", "platinum",
                "ore", "hexorium", "arsenic", "antimony", "gold", "copper", "redstone",
                "uranium", "diamond", "tungsten"
        };
        String[] materials = {
                "Wheat", "Wheat", null, null, null, null, null, null, null,
                "Acacia", "Acacia", null, null, null, null, "Palm", null
        };
        h.assertTrue(BedrockFlowers.ALL.size() == 17, "GT6 has nine A and eight B indicator flowers");
        for (int i = 0; i < ids.length; i++) {
            String id = "flower_" + ids[i];
            var spec = BedrockFlowers.byId(id);
            h.assertTrue(spec != null && spec.family() == (i < 9 ? 'A' : 'B')
                            && spec.meta() == (i < 9 ? i : i - 9)
                            && spec.indicator().equals(indicators[i]),
                    id + " retains its GT6 family, metadata and deposit tooltip");
            Block block = block(id);
            h.assertTrue(i == 8 ? block instanceof BedrockHexalilyBlock
                            : block instanceof BedrockFlowerBlock,
                    id + " uses the matching GT6 flower block shape");
            ItemStack flower = new ItemStack(block);
            h.assertTrue(!flower.isEmpty(), id + " exists as a placeable item");
            var composition = ItemMaterialRegistry.get(flower);
            if (materials[i] == null) {
                h.assertTrue(composition.isEmpty() && spec.material() == null,
                        id + " does not fabricate ore material from an indicator hint");
            } else {
                h.assertTrue(composition.isPresent()
                                && composition.get().material() == spec.material()
                                && composition.get().amount() == GTValues.U
                                && spec.material().getName().equals(materials[i]),
                        id + " carries its exact one-unit GT6 flower material");
            }
            var drops = block.getDrops(block.defaultBlockState(), new LootParams.Builder(h.getLevel()));
            h.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()),
                    id + " drops its own flower on harvest");
        }
        BlockPos ground = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(ground, Blocks.SAND.defaultBlockState());
        h.assertTrue(block("flower_sagebrush").defaultBlockState().canSurvive(h.getLevel(), ground.above()),
                "GT6 B flowers survive on cactus-compatible sand");
        h.getLevel().setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        h.assertTrue(!block("flower_sagebrush").defaultBlockState().canSurvive(h.getLevel(), ground.above()),
                "GT6 B desert flowers reject ordinary dirt");
        h.assertTrue(block("flower_altered_andesite_buckwheat").defaultBlockState()
                        .canSurvive(h.getLevel(), ground.above()),
                "GT6 A flowers accept ordinary dirt");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void originalGrindingDyeAndBiomassRecipesAreUsable(GameTestHelper h) {
        for (var flower : BedrockFlowers.ALL) {
            String id = flower.id();
            var ferment = recipe(id + "/biomass/Water");
            h.assertTrue(ferment.map() == MachineRecipeMaps.Fermenter
                            && ferment.recipe().mInputs[0].is(block(id).asItem())
                            && ferment.recipe().mFluidInputs[0].getAmount() == 135
                            && ferment.recipe().mFluidOutputs[0].getAmount() == 135
                            && ferment.recipe().mDuration == 32,
                    id + " has GT6 RM.biomass(8) water fermentation");
        }
        var wheat = recipe("flower_altered_andesite_buckwheat/mortar");
        h.assertTrue(wheat.map() == MachineRecipeMaps.Mortar
                        && wheat.recipe().mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust,
                                com.gregtech.gregtech.content.material.Materials.Wheat).getItem()),
                "A0 mortars to Wheat dust, not gold dust");
        var palm = recipe("flower_pandanus_candelabrum/shredder");
        h.assertTrue(palm.map() == MachineRecipeMaps.Shredder
                        && palm.recipe().mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust,
                                com.gregtech.gregtech.content.material.generated.WoodMaterials.Palm).getItem()),
                "B6 shreds to Palm dust");
        var violet = recipe("flower_viola_calaminaria/squeezer");
        h.assertTrue(violet.map() == MachineRecipeMaps.Squeezer
                        && violet.recipe().mFluidOutputs[0].isFluidEqual(GTFluids.stack("Dye_Flower_Yellow", 144))
                        && violet.recipe().mFluidOutputs[0].getAmount() == 144,
                "A3 yields the original yellow flower dye, despite indicating zinc");
        var cactus = recipe("flower_tungstus/juicer");
        h.assertTrue(cactus.map() == MachineRecipeMaps.Juicer
                        && cactus.recipe().mOutputs[0].is(Items.GREEN_DYE)
                        && cactus.recipe().mOutputs[0].getCount() == 2
                        && cactus.recipe().mFluidOutputs[0].getAmount() == 75,
                "B7 juicer yields two cactus dyes and 75 mB cactus juice");

        var manager = h.getLevel().getRecipeManager();
        h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech",
                        "bedrock_flowers/flower_sagebrush/hand")).isPresent(),
                "B0 shapeless one-stick recipe is available");
        h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech",
                        "bedrock_flowers/flower_sagebrush/saw")).isPresent(),
                "B0 two-stick saw recipe is available");
        h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech",
                        "bedrock_flowers/tungstus_smelting")).isPresent(),
                "B7 vanilla smelting into cactus dye is available");
        h.succeed();
    }

    private static BedrockFlowerProcessingRecipes.Entry recipe(String path) {
        return BedrockFlowerProcessingRecipes.INSTANCE.entries().stream()
                .filter(e -> e.id().equals(path)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing GT6 flower processing row " + path));
    }

    private static Block block(String path) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }
}
