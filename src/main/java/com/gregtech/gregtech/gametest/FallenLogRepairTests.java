package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.wood.FallenLogBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 BlockTreeLog1: four fallen woods, their ordinary drops and tool-click harvest. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class FallenLogRepairTests {
    private static final String[] IDS = {"log_dry", "log_rotten", "log_mossy", "log_frozen"};
    private static final FallenLogBlock.Kind[] KINDS = {
            FallenLogBlock.Kind.DRY, FallenLogBlock.Kind.ROTTEN,
            FallenLogBlock.Kind.MOSSY, FallenLogBlock.Kind.FROZEN
    };

    private FallenLogRepairTests() {}

    @GameTest(template = "coin_pile_space")
    public static void fourFallenLogsKeepTheirGt6MiningProperties(GameTestHelper h) {
        for (int i = 0; i < IDS.length; i++) {
            Block block = block(IDS[i]);
            h.assertTrue(block instanceof FallenLogBlock && ((FallenLogBlock) block).kind() == KINDS[i],
                    IDS[i] + " has its GT6 log behavior");
            var state = block.defaultBlockState();
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), IDS[i] + " is axe-mineable");
            h.assertTrue(Math.abs(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) - 1.0F) < 0.001F,
                    IDS[i] + " has half the original log hardness");
            h.assertTrue(block.getFlammability(state, h.getLevel(), BlockPos.ZERO, Direction.UP) == 5
                            && block.getFireSpreadSpeed(state, h.getLevel(), BlockPos.ZERO, Direction.UP) == 5,
                    IDS[i] + " retains GT6 beam fire behavior");
            var drops = block.getDrops(state, new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_AXE)));
            h.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem())
                            && drops.get(0).getCount() == 1,
                    IDS[i] + " drops its own block when mined normally");
        }
        h.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void gt6AxeSawAndKnifeYieldEachFallenLogsMaterial(GameTestHelper h) {
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        GTToolType[] tools = {GTToolType.AXE, GTToolType.SAW, GTToolType.KNIFE};
        for (int i = 0; i < IDS.length; i++) {
            FallenLogBlock log = (FallenLogBlock) block(IDS[i]);
            ItemStack expected = switch (i) {
                case 0 -> new ItemStack(item("dry_bark"));
                case 1 -> GTItems.getStack(MaterialPrefix.dust, WoodMaterials.WoodRotten);
                case 2 -> GTItems.getStack(MaterialPrefix.dust, WoodMaterials.WoodMossy);
                default -> GTItems.getStack(MaterialPrefix.dust, Materials.Ice);
            };
            h.assertTrue(!expected.isEmpty() && log.toolProduct().is(expected.getItem()),
                    IDS[i] + " has the registered GT6 tool-click product");
            for (GTToolType type : tools) {
                player.getInventory().clearContent();
                ItemStack tool = GTToolItem.create(type, Materials.Steel, GTMaterialRegistry.get("Wood"));
                h.assertTrue(!tool.isEmpty(), type + " exists as a usable GT tool");
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                h.getLevel().setBlockAndUpdate(pos, log.defaultBlockState());
                int before = count(player, expected.getItem());
                InteractionResult result = log.defaultBlockState().use(
                        h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                h.assertTrue(result.consumesAction() && h.getLevel().getBlockState(pos).isAir(),
                        type + " consumes " + IDS[i]);
                h.assertTrue(count(player, expected.getItem()) == before + 1,
                        type + " harvests one " + expected.getHoverName().getString() + " from " + IDS[i]);
                h.assertTrue(tool.getDamageValue() == 10,
                        type + " pays GT6's 1000 tool-click cost on " + IDS[i]);
            }
        }

        FallenLogBlock log = (FallenLogBlock) block("log_dry");
        h.getLevel().setBlockAndUpdate(pos, log.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(log.defaultBlockState().use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                        == InteractionResult.PASS && h.getLevel().getBlockState(pos).is(log),
                "ordinary right-click leaves the fallen log intact");
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.succeed();
    }

    private static int count(net.minecraft.world.entity.player.Player player, Item item) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) amount += stack.getCount();
        }
        return amount;
    }

    private static Block block(String path) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }

    private static Item item(String path) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }
}
