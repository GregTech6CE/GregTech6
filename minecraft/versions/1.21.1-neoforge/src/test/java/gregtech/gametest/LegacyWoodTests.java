package gregtech.gametest;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.wood.WoodSlabBlock;
import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.registry.GTWoods;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(REGISTRY_NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LegacyWoodTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void oldBlueMahoeBlocksAndStacksResolveToCanonicalWood(GameTestHelper helper) {
        var species = WoodSpecies.BLUE_MAHOE;
        var leaves = GTWoods.leaves(species)
                .defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.WATERLOGGED, true);
        var sapling = GTWoods.sapling(species).defaultBlockState();
        var slab = BuiltInRegistries.BLOCK
                .get(id("slab_blue_mahoe"))
                .defaultBlockState()
                .setValue(WoodSlabBlock.FACING, Direction.EAST)
                .setValue(WoodSlabBlock.WATERLOGGED, true);
        Map<String, BlockState> expectedStates = Map.of(
                "beam_bluemahoe",
                        GTWoods.strippedLog(species)
                                .defaultBlockState()
                                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z),
                "log_bluemahoe",
                        GTWoods.log(species).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
                "planks_bluemahoe", GTWoods.planks(species).defaultBlockState(),
                "leaves_bluemahoe", leaves,
                "leaves_opaque_bluemahoe", leaves,
                "sapling_large_bluemahoe", sapling,
                "sapling_small_bluemahoe", sapling,
                "slab_bluemahoe", slab);
        var itemOps = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (var entry : expectedStates.entrySet()) {
            var oldId = id(entry.getKey());
            var expected = entry.getValue();
            helper.assertTrue(!BuiltInRegistries.BLOCK.keySet().contains(oldId), "obsolete block registered: " + oldId);
            helper.assertTrue(!BuiltInRegistries.ITEM.keySet().contains(oldId), "obsolete item registered: " + oldId);

            var paletteEntry = NbtUtils.writeBlockState(expected);
            var canonical = paletteEntry.getString("Name");
            paletteEntry.putString("Name", oldId.toString());
            var decoded = BlockState.CODEC.parse(NbtOps.INSTANCE, paletteEntry).getOrThrow();
            helper.assertTrue(decoded == expected, "old palette state did not resolve to " + expected + ": " + oldId);
            helper.assertTrue(
                    NbtUtils.writeBlockState(decoded).getString("Name").equals(canonical),
                    "saved palette did not use " + canonical);

            var stackTag = new CompoundTag();
            stackTag.putString("id", oldId.toString());
            stackTag.putInt("count", 2);
            var stack = ItemStack.CODEC.parse(itemOps, stackTag).getOrThrow();
            helper.assertTrue(stack.is(expected.getBlock().asItem()) && stack.getCount() == 2, "old stack: " + oldId);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void blueMahoeSlabUsesCanonicalRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String suffix : new String[] {"", "_to_planks"}) {
            var canonical = id("catalog_repairs/slab_blue_mahoe" + suffix);
            var legacy = id("catalog_repairs/slab_bluemahoe" + suffix);
            helper.assertTrue(recipes.byKey(canonical).isPresent(), "missing recipe: " + canonical);
            helper.assertTrue(recipes.byKey(legacy).isEmpty(), "obsolete recipe loaded: " + legacy);
        }
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path);
    }
}
