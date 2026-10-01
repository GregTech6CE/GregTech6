package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.block.VanillaOreBlock;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefixes;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** GT6 BlockVanillaOresA metas 0..15, separate from 4U BlockRockOres and BlockCrystalOres. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class VanillaOreRepairTests {
    private VanillaOreRepairTests() {}

    // GT6 BlockVanillaOresA arrays: icon, material, harvest level, hardness multiplier, burn, XP range.
    private static final Object[][] ORIGINAL = {
            {"sulfur", "Sulfur", 0, 0.5F, 30, 0, 2},
            {"apatite", "Apatite", 0, 0.5F, 30, 0, 2},
            {"ruby", "Ruby", 2, 1.5F, 0, 3, 7},
            {"amber", "Amber", 1, 1.0F, 0, 3, 7},
            {"amethyst", "Amethyst", 2, 1.0F, 0, 3, 7},
            {"galena", "Galena", 1, 1.0F, 0, 2, 5},
            {"tetrahedrite", "Tetrahedrite", 1, 1.0F, 0, 2, 5},
            {"cassiterite", "Cassiterite", 1, 1.0F, 0, 2, 5},
            {"sheldonite", "Cooperite", 2, 1.5F, 0, 2, 5},
            {"pentlandite", "Pentlandite", 1, 1.0F, 0, 2, 5},
            {"scheelite", "Scheelite", 2, 1.5F, 0, 2, 5},
            {"rutile", "Rutile", 2, 1.5F, 0, 2, 5},
            {"bastnasite", "Bastnasite", 2, 1.5F, 0, 2, 5},
            {"graphite", "Graphite", 0, 0.5F, 30, 2, 5},
            {"pitchblende", "Pitchblende", 3, 2.0F, 0, 2, 5},
            {"borax", "Borax", 0, 0.5F, 0, 2, 5}
    };

    @GameTest(template = "test_empty")
    public static void allSixteenOresMineLikeGt6(GameTestHelper h) {
        h.assertTrue(VanillaOreBlock.SPECS.size() == 16, "GT6 vanilla-style ores have exactly 16 metas");
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (Object[] row : ORIGINAL) {
            String slug = (String) row[0];
            String id = "block_ore_" + slug;
            Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            h.assertTrue(block instanceof VanillaOreBlock, id + " is an ore block, not an icon shell");
            VanillaOreBlock ore = (VanillaOreBlock) block;
            int tier = (int) row[2];
            float hardness = (float) row[3];
            var state = ore.defaultBlockState();
            h.assertTrue(ore.material() == GTMaterialRegistry.get((String) row[1]),
                    id + " has GT6's material identity");
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                            && state.is(BlockTags.NEEDS_STONE_TOOL) == (tier == 1)
                            && state.is(BlockTags.NEEDS_IRON_TOOL) == (tier == 2)
                            && state.is(BlockTags.NEEDS_DIAMOND_TOOL) == (tier == 3),
                    id + " has GT6 pickaxe harvest level " + tier);
            h.assertTrue(Math.abs(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) - 1.5F * hardness) < 0.001F,
                    id + " has GT6's stone-hardness multiplier");
            h.assertTrue(ore.getExplosionResistance(state, h.getLevel(), BlockPos.ZERO, null)
                            == Blocks.STONE.getExplosionResistance()
                            && ore.getFlammability(state, h.getLevel(), BlockPos.ZERO, Direction.UP) == (int) row[4]
                            && ore.getFireSpreadSpeed(state, h.getLevel(), BlockPos.ZERO, Direction.UP) == 0,
                    id + " has GT6 blast and burn properties");

            ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, ore.material());
            h.assertTrue(!raw.isEmpty(), id + " has a registered raw-ore output");
            var plainDrops = ore.getDrops(state, loot(h, pick));
            h.assertTrue(plainDrops.size() == 1 && plainDrops.get(0).is(raw.getItem())
                            && plainDrops.get(0).getCount() == 1,
                    id + " drops one raw ore without Fortune");
            boolean bonus = false;
            for (int trial = 0; trial < 24; trial++) {
                var drops = ore.getDrops(state, loot(h, fortune));
                h.assertTrue(drops.size() == 1 && drops.get(0).is(raw.getItem())
                                && drops.get(0).getCount() >= 1 && drops.get(0).getCount() <= 4,
                        id + " Fortune III stays within GT6's 1..4 raw ores");
                bonus |= drops.get(0).getCount() > 1;
            }
            h.assertTrue(bonus, id + " Fortune III can increase yield");
            var silkDrops = ore.getDrops(state, loot(h, silk));
            h.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(block.asItem()),
                    id + " supports Silk Touch block collection");

            RandomSource random = RandomSource.create(41L);
            for (int trial = 0; trial < 20; trial++) {
                int xp = ore.getExpDrop(state, h.getLevel(), random, BlockPos.ZERO, 0, 0);
                h.assertTrue(xp >= (int) row[5] && xp <= (int) row[6],
                        id + " has GT6's XP range");
            }
            h.assertTrue(ore.getExpDrop(state, h.getLevel(), random, BlockPos.ZERO, 0, 1) == 0,
                    id + " Silk Touch grants no XP");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void stoneOreIdentityLinksCrucibleAndMachines(GameTestHelper h) {
        h.assertTrue(MaterialPrefixes.oreVanillastone.getDelegate() == MaterialPrefix.oreVanillastone
                        && MaterialPrefix.oreVanillastone.getMaterialWeight() == GTValues.U * 2,
                "GT6 oreVanillastone is a 2U block form");
        for (Object[] row : ORIGINAL) {
            String id = "block_ore_" + row[0];
            var ore = (VanillaOreBlock) ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", id));
            ItemStack item = new ItemStack(ore);
            var data = ItemMaterialRegistry.get(item).orElseThrow();
            h.assertTrue(data.prefix() == MaterialPrefix.oreVanillastone
                            && data.material().resolve() == ore.material().resolve()
                            && data.amount() == GTValues.U * 2,
                    id + " carries an ordinary 2U ore identity");
            var crucible = CrucibleItemInput.parse(item);
            h.assertTrue(crucible.size() == 1
                            && crucible.get(0).material == ore.material().getTargetCrushingMaterial().resolve()
                            && crucible.get(0).amount == ore.material().getTargetCrushingAmount()
                                    * ore.material().getOreMultiplier(),
                    id + " enters a crucible as one GT6 ore payload");
            String name = MaterialEquivalence.materialName(ore.material());
            var formTag = ResourceLocation.fromNamespaceAndPath("gregtech", "ore_vanillastone/" + name);
            var commonTag = ResourceLocation.fromNamespaceAndPath("forge", "ores/" + name);
            h.assertTrue(item.is(ItemTags.create(formTag)) && ore.defaultBlockState().is(BlockTags.create(formTag))
                            && item.is(ItemTags.create(commonTag))
                            && ore.defaultBlockState().is(BlockTags.create(commonTag)),
                    id + " has its form tag and the common ore tag");
            var hammer = MachineRecipeMaps.Hammer.findRecipe(List.of(item), List.of(), false, 1, 12);
            var crusher = MachineRecipeMaps.Crusher.findRecipe(List.of(item), List.of(), false, 1, 12);
            h.assertTrue(hammer != null && crusher != null && hammer.mOutputs.length == 1
                            && crusher.mOutputs.length == 2
                            && ItemStack.isSameItemSameTags(hammer.mOutputs[0], crusher.mOutputs[0])
                            && hammer.mOutputs[0].getCount() == crusher.mOutputs[0].getCount(),
                    id + " has ordinary one-ore Hammer/Crusher routes");
        }
        h.succeed();
    }

    private static LootParams.Builder loot(GameTestHelper h, ItemStack tool) {
        return new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.TOOL, tool);
    }
}
