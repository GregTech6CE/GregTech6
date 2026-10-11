package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** GT6 bedrock drill products are 2U broken ore blocks that can be placed and crushed. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BedrockBrokenOreRepairTests {
    private BedrockBrokenOreRepairTests() {}

    @GameTest(template = "coin_pile_space")
    public static void brokenOrePlacesDropsAndEntersOreProcessing(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack product = OreBlock.brokenStack(Materials.Iron, OreHostStone.GRANITE_BLACK);
        helper.assertTrue(!product.isEmpty() && product.getItem() instanceof BlockItem,
                "an iron bedrock-drill product is a placeable ore block");
        helper.assertTrue(OreBlock.isBrokenStack(product)
                        && OreBlock.stoneOfStack(product) == OreHostStone.GRANITE_BLACK,
                "item NBT records both broken host and granite-black rock");
        var data = ItemMaterialRegistry.get(product).orElseThrow();
        helper.assertTrue(data.prefix() == MaterialPrefix.oreVanillastone
                        && data.material().resolve() == Materials.Iron.resolve()
                        && data.amount() == GTValues.U * 2,
                "broken ore retains the ordinary GT6 2U ore material identity");
        helper.assertTrue(ItemMaterialRegistry.canRecover(product)
                        && !CrucibleItemInput.parse(product).isEmpty(),
                "broken host NBT does not prevent material recovery or crucible processing");
        helper.assertTrue(MachineRecipeMaps.Crusher.findRecipe(List.of(product), List.of(), false, 1, 12) != null
                        && MachineRecipeMaps.Hammer.findRecipe(List.of(product), List.of(), false, 1, 12) != null,
                "broken ore enters the existing 2U Crusher and Hammer chain");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        player.setPos(pos.getX() + 3, pos.getY() + 1, pos.getZ() + 3);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5, 0, .5),
                Direction.UP, pos.below(), false);
        helper.assertTrue(((BlockItem) product.getItem()).place(new BlockPlaceContext(level, player,
                InteractionHand.MAIN_HAND, product.copy(), hit)).consumesAction(),
                "the broken ore item places as a block");
        var state = level.getBlockState(pos);
        helper.assertTrue(state.getBlock() instanceof OreBlock
                        && OreBlock.isBroken(state)
                        && OreBlock.stoneOf(state) == OreHostStone.GRANITE_BLACK,
                "placement preserves broken state and host rock without a block entity");
        helper.assertTrue(level.getBlockEntity(pos) == null, "broken ore adds no per-block entity");
        var drops = state.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.DIAMOND_PICKAXE)));
        helper.assertTrue(drops.size() == 1 && OreBlock.isBrokenStack(drops.get(0))
                        && OreBlock.stoneOfStack(drops.get(0)) == OreHostStone.GRANITE_BLACK,
                "mining an already broken ore keeps its 2U block and its host");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void bedrockDrillSelectsAPlaceableBrokenOre(GameTestHelper helper) throws Exception {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, GTMultiblocks.BEDROCK_DRILL_MAIN.get().defaultBlockState(), 3);
        var drill = (BedrockDrillControllerBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(drill != null, "bedrock drill controller creates its block entity");

        // Seed the 18-entry deposit list that checkStructure collects from nine full bedrock ores.
        // Invoke the production selector directly so this test checks its item before a 32768 RU cycle.
        Field depositField = BedrockDrillControllerBlockEntity.class.getDeclaredField("deposit");
        depositField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<GTMaterial> deposit = (List<GTMaterial>) depositField.get(drill);
        for (int i = 0; i < 18; i++) deposit.add(Materials.Iron);
        Method nextProduct = BedrockDrillControllerBlockEntity.class.getDeclaredMethod("nextProduct");
        nextProduct.setAccessible(true);
        ItemStack mined = ItemStack.EMPTY;
        // A 18/128 ore roll; 256 trials make a missed ore astronomically unlikely.
        for (int i = 0; i < 256; i++) {
            ItemStack sample = (ItemStack) nextProduct.invoke(drill);
            if (OreBlock.isBrokenStack(sample)) { mined = sample; break; }
        }
        helper.assertTrue(!mined.isEmpty() && mined.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() instanceof OreBlock,
                "actual drill selection yields a broken, placeable ore rather than oreRaw");
        helper.assertTrue(OreBlock.stoneOfStack(mined) != OreHostStone.BEDROCK,
                "drill output uses surface host rock, not the unbreakable bedrock deposit");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 100)
    public static void onlyBrokenOreFallsLikeGT6Rubble(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack sample = OreBlock.brokenStack(Materials.Iron, OreHostStone.BASALT);
        helper.assertTrue(sample.getItem() instanceof BlockItem, "broken ore block item exists");
        var ore = (OreBlock) ((BlockItem) sample.getItem()).getBlock();
        BlockPos rubble = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos intact = helper.absolutePos(new BlockPos(4, 4, 2));
        level.setBlock(rubble.below(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(rubble.below(2), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(intact.below(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(rubble, ore.brokenStateFor(OreHostStone.BASALT), 3);
        level.setBlock(intact, ore.stateFor(OreHostStone.BASALT), 3);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(level.getBlockState(rubble).isAir(),
                    "broken ore has left its unsupported original position");
            helper.assertTrue(level.getBlockState(intact).is(ore) && !OreBlock.isBroken(level.getBlockState(intact)),
                    "intact worldgen ore remains anchored over air");
            helper.assertTrue(level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(rubble).inflate(2))
                            .stream().anyMatch(entity -> entity.getBlockState().is(ore)
                                    && OreBlock.isBroken(entity.getBlockState())
                                    && OreBlock.stoneOf(entity.getBlockState()) == OreHostStone.BASALT),
                    "the falling entity keeps the broken ore material and host rock");
            helper.succeed();
        });
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 40)
    public static void failedFallingBrokenOreKeepsItsItemState(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack sample = OreBlock.brokenStack(Materials.Iron, OreHostStone.BASALT);
        helper.assertTrue(sample.getItem() instanceof BlockItem, "broken iron ore block item exists");
        var ore = (OreBlock) ((BlockItem) sample.getItem()).getBlock();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 4, 2));
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(2), Blocks.AIR.defaultBlockState(), 3);
        var state = ore.brokenStateFor(OreHostStone.BASALT);
        level.setBlock(pos, state, 3);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        falling.time = 600;

        helper.runAfterDelay(2, () -> {
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                    .filter(entity -> entity.getItem().getItem() == sample.getItem()).toList();
            helper.assertTrue(drops.size() == 1, "failed fall drops exactly one iron ore block item");
            ItemStack dropped = drops.get(0).getItem();
            helper.assertTrue(OreBlock.isBrokenStack(dropped)
                            && OreBlock.stoneOfStack(dropped) == OreHostStone.BASALT,
                    "failed fall keeps the broken state and basalt host on the dropped item");
            var material = ItemMaterialRegistry.get(dropped).orElseThrow();
            helper.assertTrue(material.material().resolve() == Materials.Iron.resolve()
                            && material.amount() == GTValues.U * 2,
                    "failed fall keeps the broken ore's iron material and 2U amount");
            helper.succeed();
        });
    }
}
