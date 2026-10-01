package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.RockBlock;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTNetherScatterFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards GT6's Nether scatter ({@code WorldgenRacks}, {@code Loader_Worldgen:619}): loose items
 * lying on Nether ground — gems, flint, Ancient Debris raw rocks and rock pebbles.
 *
 * <p>GT6 skips half of the chunks, then makes 16 attempts per chunk with a ray from a random height
 * down to the first solid ground, and finally rolls a 1-in-24 table whose result depends on what the
 * item lies on (nether brick → Ancient Debris, soul sand → Gloomstone/quartz, gravel → flint).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class NetherScatterTests {
    private static final int BASE_X = 34000;
    private static final int BASE_Z = 34000;
    private static final int BASE_Y = 100;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 16, BASE_Y, BASE_Z);
    }

    /** A netherrack shelf with air above, ready for one GT6 attempt. */
    private static BlockPos shelf(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block ground) {
        level.getBlockState(pos);
        for (int dy = -1; dy <= 3; dy++) {
            level.setBlock(pos.above(dy), dy < 0 ? ground.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
        return pos;
    }

    /** GT6's table shape and the feature's registration. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void scatterTableMatchesGt6(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        helper.assertTrue(GTNetherScatterFeature.TABLE_SIZE == 24, "GT6 rolls a 1-in-24 table");
        helper.assertTrue(GTNetherScatterFeature.ATTEMPTS == 16, "GT6 makes 16 attempts per chunk");
        helper.assertTrue(GTNetherScatterFeature.RAY_LENGTH == 40, "GT6's ray is 40 blocks long");
        helper.assertTrue(GTNetherScatterFeature.TABLE.size() == 24,
                "the port carries all 24 table entries, got " + GTNetherScatterFeature.TABLE.size());
        // GT6's materials must resolve in the port, otherwise the scatter would drop nothing.
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("NetherQuartz", "NetherQuartz");
        expected.put("Glowstone", "Glowstone");
        expected.put("AncientDebris", "AncientDebris");
        expected.put("Obsidian", "Obsidian");
        expected.put("Basalt", "Basalt");
        expected.put("Blackstone", "Blackstone");
        for (String material : expected.keySet()) {
            if (com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material) == null) {
                problems.add("missing GT6 material " + material);
            }
        }
        helper.assertTrue(GTFeatures.NETHER_SCATTER.getId().getPath().equals("gt_nether_scatter"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTNetherScatterFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_nether_scatter.json is loaded");
        helper.assertTrue(problems.isEmpty(), "nether scatter table (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** GT6's contact-block special cases: nether brick, soul sand and gravel. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void scatterSpecialCases(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();

        // Nether brick: either an Ancient Debris raw rock or a nether quartz rock, never flint.
        int debris = 0, quartz = 0;
        for (int i = 0; i < 12; i++) {
            BlockPos pos = shelf(level, base(i), Blocks.NETHER_BRICKS);
            GTNetherScatterFeature.scatter((WorldGenLevel) level, pos.getX(), pos.getZ(), pos.getY() - 1,
                    RandomSource.create(100L + i));
            var be = (RockBlockEntity) level.getBlockEntity(pos);
            if (be == null) {
                problems.add("no rock placed on nether bricks (attempt " + i + ")");
                continue;
            }
            if (be.getMaterial().equals("AncientDebris") && be.hasRawOre()) debris++;
            else if (be.getMaterial().equals("NetherQuartz") && !be.hasRawOre()) quartz++;
            else problems.add("unexpected nether brick item: " + be.getMaterial() + " raw=" + be.hasRawOre());
        }
        if (debris == 0) problems.add("nether bricks never produced Ancient Debris (GT6: 1 in 4)");
        if (quartz == 0) problems.add("nether bricks never produced nether quartz");

        // Gravel: flint, exactly like GT6's `Items.flint` cases.
        BlockPos gravel = shelf(level, base(20), Blocks.GRAVEL);
        GTNetherScatterFeature.scatter((WorldGenLevel) level, gravel.getX(), gravel.getZ(), gravel.getY() - 1,
                RandomSource.create(7L));
        var gravelBe = (RockBlockEntity) level.getBlockEntity(gravel);
        helper.assertTrue(gravelBe != null, "gravel gets a ground item too");
        helper.assertTrue(gravelBe.itemId().equals("minecraft:flint"),
                "gravel gives flint (GT6 case 16-23), got " + gravelBe.itemId());

        // Soul sand: gloomstone or quartz gems.
        BlockPos soul = shelf(level, base(21), Blocks.SOUL_SAND);
        GTNetherScatterFeature.scatter((WorldGenLevel) level, soul.getX(), soul.getZ(), soul.getY() - 1,
                RandomSource.create(3L));
        var soulBe = (RockBlockEntity) level.getBlockEntity(soul);
        helper.assertTrue(soulBe != null, "soul sand gets a ground item");
        helper.assertTrue(soulBe.getMaterial().equals("Gloomstone") || soulBe.getMaterial().equals("NetherQuartz"),
                "soul sand gives gloomstone or quartz, got " + soulBe.getMaterial());

        helper.assertTrue(problems.isEmpty(), "nether scatter cases (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** The rock block hands out the item it carries (GT6's item-on-the-ground mode). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void groundItemYieldsItsItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = shelf(level, base(30), Blocks.NETHERRACK);
        GTNetherScatterFeature.place((WorldGenLevel) level, pos, "NetherQuartz", "minecraft:flint", false);
        var be = (RockBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(be != null, "the rock block entity exists");
        helper.assertTrue(be.itemId().equals("minecraft:flint"), "the item id round-trips");
        helper.assertTrue(be.itemStack().is(net.minecraft.world.item.Items.FLINT), "the stack resolves");
        var drops = level.getBlockState(pos).getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, be));
        boolean flint = drops.stream().anyMatch(stack -> stack.is(net.minecraft.world.item.Items.FLINT));
        helper.assertTrue(flint, "breaking the ground item drops the flint, got " + drops);
        // The block itself is the port's rock with the Netherrack ground look.
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.is(GTBlocks.ROCK.get())
                        && state.getValue(RockBlock.GROUND) == RockBlock.Ground.NETHERRACK,
                "the ground item uses the netherrack rock look, got " + state);
        helper.assertTrue(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("minecraft:flint")) != null,
                "vanilla flint exists");
        helper.succeed();
    }
}
