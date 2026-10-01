package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.CrystalOreBlock;
import com.gregtech.gregtech.block.RockOreBlock;
import com.gregtech.gregtech.block.BlackSandBlock;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefixes;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** GT6 BlockCrystalOres: Nether worldgen minerals must yield raw ore and mining XP. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class CrystalOreRepairTests {
    private static final String[] KINDS = {
            "arsenopyrite", "chalcopyrite", "cinnabar", "cobaltite", "galena", "kesterite",
            "molybdenite", "pyrite", "sphalerite", "stannite", "stibnite", "tetrahedrite"
    };

    private CrystalOreRepairTests() {}

    @GameTest(template = "test_empty")
    public static void everyNetherCrystalHasGt6RawOreDropsAndExperience(GameTestHelper h) {
        ItemStack plainPick = new ItemStack(Items.IRON_PICKAXE);
        ItemStack fortunePick = new ItemStack(Items.DIAMOND_PICKAXE);
        fortunePick.enchant(Enchantments.BLOCK_FORTUNE, 3);
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(Enchantments.SILK_TOUCH, 1);

        for (String kind : KINDS) {
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "crystal_ore_" + kind));
            h.assertTrue(block instanceof CrystalOreBlock, kind + " is the GT6 mining block, not an icon shell");
            CrystalOreBlock crystal = (CrystalOreBlock) block;
            var state = crystal.defaultBlockState();
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), kind + " requires a pickaxe");
            ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, crystal.material());
            h.assertTrue(!raw.isEmpty(), kind + " has a registered raw ore output");

            List<ItemStack> normal = crystal.getDrops(state, loot(h, plainPick));
            h.assertTrue(normal.size() == 1 && normal.get(0).is(raw.getItem())
                            && normal.get(0).getCount() == 2,
                    kind + " drops exactly two raw ores without Fortune");

            boolean fortuneIncreasedYield = false;
            for (int i = 0; i < 32; i++) {
                List<ItemStack> fortunate = crystal.getDrops(state, loot(h, fortunePick));
                h.assertTrue(fortunate.size() == 1 && fortunate.get(0).is(raw.getItem())
                                && fortunate.get(0).getCount() >= 2 && fortunate.get(0).getCount() <= 9,
                        kind + " Fortune III stays within GT6's 2..9 raw ore range");
                fortuneIncreasedYield |= fortunate.get(0).getCount() > 2;
            }
            h.assertTrue(fortuneIncreasedYield, kind + " Fortune III sometimes increases yield");

            List<ItemStack> silk = crystal.getDrops(state, loot(h, silkPick));
            h.assertTrue(silk.size() == 1 && silk.get(0).is(block.asItem()),
                    kind + " can still be collected as a crystal block with Silk Touch");
            assertDenseOreIdentity(h, block, crystal.material(), silk.get(0));

            RandomSource random = RandomSource.create(317L);
            for (int i = 0; i < 24; i++) {
                int xp = crystal.getExpDrop(state, h.getLevel(), random, h.absolutePos(net.minecraft.core.BlockPos.ZERO), 0, 0);
                h.assertTrue(xp >= 3 && xp <= 6, kind + " grants GT6's 3..6 mining XP");
            }
        }
        h.succeed();
    }

    private static LootParams.Builder loot(GameTestHelper h, ItemStack tool) {
        return new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.TOOL, tool);
    }

    @GameTest(template = "test_empty")
    public static void allGt6RockLayersMineIntoTheirOwnRawOre(GameTestHelper h) {
        // BlockRockOres meta 0..8: raw material, harvest tier, stone-hardness multiplier, burn level.
        String[][] expected = {
                {"anthracite", "Coal", "0", "0.5", "30"},
                {"lignite", "Lignite", "0", "0.5", "30"},
                {"salt", "NaCl", "1", "1.0", "0"},
                {"rocksalt", "KCl", "1", "1.0", "0"},
                {"bauxite", "Bauxite", "2", "2.0", "0"},
                {"oil", "Oilshale", "1", "0.5", "30"},
                {"gypsum", "Gypsum", "0", "0.5", "0"},
                {"milkyquartz", "MilkyQuartz", "1", "1.0", "0"},
                {"netherquartz", "NetherQuartz", "1", "1.0", "0"}
        };
        ItemStack plainPick = new ItemStack(Items.IRON_PICKAXE);
        ItemStack fortunePick = new ItemStack(Items.DIAMOND_PICKAXE);
        fortunePick.enchant(Enchantments.BLOCK_FORTUNE, 3);
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(Enchantments.SILK_TOUCH, 1);
        for (String[] row : expected) {
            String id = "block_ore_" + row[0];
            Block registered = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            h.assertTrue(registered instanceof RockOreBlock, id + " is a GT6 rock ore, not a texture shell");
            RockOreBlock rock = (RockOreBlock) registered;
            var state = rock.defaultBlockState();
            int harvest = Integer.parseInt(row[2]);
            float hardness = 1.5F * Float.parseFloat(row[3]);
            h.assertTrue(rock.material() == GTMaterialRegistry.get(row[1]),
                    id + " maps to GT6 material " + row[1]);
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                            && state.is(BlockTags.NEEDS_IRON_TOOL) == (harvest == 2)
                            && state.is(BlockTags.NEEDS_STONE_TOOL) == (harvest == 1),
                    id + " has GT6 pickaxe harvest level " + harvest);
            h.assertTrue(Math.abs(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) - hardness) < 0.001F,
                    id + " has GT6 stone hardness multiplier " + row[3]);
            h.assertTrue(rock.getFlammability(state, h.getLevel(), BlockPos.ZERO, Direction.UP)
                            == Integer.parseInt(row[4])
                            && rock.getFireSpreadSpeed(state, h.getLevel(), BlockPos.ZERO, Direction.UP) == 0,
                    id + " has GT6 burn level without spreading fire");
            h.assertTrue(rock.getExplosionResistance(state, h.getLevel(), BlockPos.ZERO, null)
                            == Blocks.STONE.getExplosionResistance(),
                    id + " keeps vanilla stone blast resistance");

            ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, GTMaterialRegistry.get(row[1]));
            h.assertTrue(!raw.isEmpty(), id + " raw ore output exists");
            List<ItemStack> plain = rock.getDrops(state, loot(h, plainPick));
            h.assertTrue(plain.size() == 1 && plain.get(0).is(raw.getItem()) && plain.get(0).getCount() == 2,
                    id + " drops two raw ores without Fortune");
            boolean bonus = false;
            for (int i = 0; i < 24; i++) {
                List<ItemStack> drops = rock.getDrops(state, loot(h, fortunePick));
                h.assertTrue(drops.size() == 1 && drops.get(0).is(raw.getItem())
                                && drops.get(0).getCount() >= 2 && drops.get(0).getCount() <= 9,
                        id + " Fortune III yield stays within GT6's 2..9 range");
                bonus |= drops.get(0).getCount() > 2;
            }
            h.assertTrue(bonus, id + " Fortune III can increase yield");
            List<ItemStack> silk = rock.getDrops(state, loot(h, silkPick));
            h.assertTrue(silk.size() == 1 && silk.get(0).is(rock.asItem()),
                    id + " supports GT6 Silk Touch block pickup");
            assertDenseOreIdentity(h, rock, rock.material(), silk.get(0));

            int xpDrops = 0;
            RandomSource random = RandomSource.create(46L);
            for (int i = 0; i < 128; i++) {
                int xp = rock.getExpDrop(state, h.getLevel(), random, BlockPos.ZERO, 0, 0);
                h.assertTrue(xp == 0 || xp == 1, id + " yields at most one XP");
                xpDrops += xp;
            }
            h.assertTrue(xpDrops > 0 && xpDrops < 128, id + " has GT6's one-in-eight XP chance");
            h.assertTrue(rock.getExpDrop(state, h.getLevel(), RandomSource.create(0), BlockPos.ZERO, 0, 1) == 0,
                    id + " grants no XP with Silk Touch");
        }
        h.succeed();
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 100)
    public static void allGt6BlackSandsFallAndReturnTheirBlock(GameTestHelper h) {
        String[][] expected = {
                {"sand_magnetite", "Magnetite"},
                {"sand_basalt_magnetite", "BasalticMineralSand"},
                {"sand_granite_magnetite", "GraniticMineralSand"}
        };
        for (int i = 0; i < expected.length; i++) {
            String id = expected[i][0];
            Block registered = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            h.assertTrue(registered instanceof BlackSandBlock && registered instanceof FallingBlock,
                    id + " is a falling GT6 sand, not a decorative icon block");
            BlackSandBlock sand = (BlackSandBlock) registered;
            var state = sand.defaultBlockState();
            h.assertTrue(sand.material() == GTMaterialRegistry.get(expected[i][1]),
                    id + " maps to GT6's mineral sand material");
            ItemStack sandItem = new ItemStack(sand);
            var composition = com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(sandItem).orElseThrow();
            h.assertTrue(composition.material() == sand.material()
                            && composition.amount() == com.gregtech.gregtech.api.material.GTValues.U * 9,
                    id + " carries a nine-unit GT6 blockDust composition");
            var crucible = com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(sandItem);
            h.assertTrue(crucible.size() == 1 && crucible.get(0).material == sand.material()
                            && crucible.get(0).amount == com.gregtech.gregtech.api.material.GTValues.U * 9,
                    id + " supplies its mineral sand to a crucible");
            String materialTag = com.gregtech.gregtech.api.material.MaterialEquivalence.materialName(sand.material());
            ResourceLocation formTag = ResourceLocation.fromNamespaceAndPath("gregtech", "block_dust/" + materialTag);
            h.assertTrue(sandItem.is(ItemTags.create(formTag)) && state.is(BlockTags.create(formTag)),
                    id + " has matching GT6 blockDust item and block tags");
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_SHOVEL), id + " is mined with a shovel");
            h.assertTrue(Math.abs(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) - 0.5F) < 0.001F,
                    id + " uses vanilla sand hardness");
            List<ItemStack> drops = sand.getDrops(state, loot(h, new ItemStack(Items.IRON_SHOVEL)));
            h.assertTrue(drops.size() == 1 && drops.get(0).is(sand.asItem()),
                    id + " drops itself, as GT6 BlockSands does");
            BlockPos ground = h.absolutePos(new BlockPos(i + 2, 1, 2));
            h.getLevel().setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(ground.above(3), state);
        }
        h.runAfterDelay(40, () -> {
            for (int i = 0; i < expected.length; i++) {
                Block sand = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(
                        "gregtech", expected[i][0]));
                BlockPos ground = h.absolutePos(new BlockPos(i + 2, 1, 2));
                h.assertTrue(h.getLevel().getBlockState(ground.above()).is(sand),
                        expected[i][0] + " falls to the ground");
            }
            h.succeed();
        });
    }

    private static void assertDenseOreIdentity(GameTestHelper h, Block block, GTMaterial material, ItemStack silkBlock) {
        String id = ForgeRegistries.BLOCKS.getKey(block).toString();
        h.assertTrue(MaterialPrefixes.oreDense.getDelegate() == MaterialPrefix.oreDense
                        && MaterialPrefix.oreDense.getMaterialWeight() == GTValues.U * 4,
                "GT6 oreDense is a 4U form carried by block items");
        var composition = ItemMaterialRegistry.get(silkBlock).orElseThrow();
        h.assertTrue(composition.prefix() == MaterialPrefix.oreDense
                        && composition.material().resolve() == material.resolve()
                        && composition.amount() == GTValues.U * 4,
                id + " Silk Touch block keeps its 4U dense-ore material identity");

        var payload = CrucibleItemInput.parse(silkBlock);
        h.assertTrue(payload.size() == 1
                        && payload.get(0).material == material.getTargetCrushingMaterial().resolve()
                        && payload.get(0).amount == material.getTargetCrushingAmount() * material.getOreMultiplier() * 2,
                id + " crucible uses GT6's twice-standard dense-ore crushing target");

        String name = MaterialEquivalence.materialName(material);
        var denseTag = ResourceLocation.fromNamespaceAndPath("gregtech", "ore_dense/" + name);
        var commonTag = ResourceLocation.fromNamespaceAndPath("forge", "ores/" + name);
        h.assertTrue(silkBlock.is(ItemTags.create(denseTag)) && block.defaultBlockState().is(BlockTags.create(denseTag))
                        && silkBlock.is(ItemTags.create(commonTag)) && block.defaultBlockState().is(BlockTags.create(commonTag)),
                id + " has ore_dense and common ore item/block tags");

        var hammer = MachineRecipeMaps.Hammer.findRecipe(List.of(silkBlock), List.of(), false, 1, 12);
        var crusher = MachineRecipeMaps.Crusher.findRecipe(List.of(silkBlock), List.of(), false, 1, 12);
        h.assertTrue(hammer != null && crusher != null && hammer.mOutputs.length == 1
                        && crusher.mOutputs.length == 2
                        && ItemStack.isSameItemSameTags(hammer.mOutputs[0], crusher.mOutputs[0])
                        && hammer.mOutputs[0].getCount() == crusher.mOutputs[0].getCount(),
                id + " has GT6 dense-ore Hammer/Crusher processing routes");

        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material.resolve(), 2);
        h.assertTrue(!raw.isEmpty() && h.getLevel().getRecipeManager().getRecipes().stream().anyMatch(recipe ->
                        recipe.getId().getPath().startsWith("form_conversion/oredense_to_oreraw/")
                                && recipe.getIngredients().size() == 1
                                && recipe.getIngredients().get(0).test(silkBlock)
                                && ItemStack.isSameItemSameTags(recipe.getResultItem(h.getLevel().registryAccess()), raw)
                                && recipe.getResultItem(h.getLevel().registryAccess()).getCount() == 2),
                id + " can be unpacked by hand into two raw ores as in GT6");
    }
}
