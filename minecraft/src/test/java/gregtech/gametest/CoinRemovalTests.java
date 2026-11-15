package gregtech.gametest;

import gregtech.api.material.GTValues;
import gregtech.content.block.misc.PileBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.blockentity.misc.PileBlockEntity;
import gregtech.content.data.CrateMaterialTables;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.worldgen.dungeon.GTDungeonData;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The dungeon ingot piles that replace the coin piles draw from {@code PILE_METALS}, whose composition
 * carries the precious-metal additions; the storage crate and gem pools keep their entries, and the
 * pile helper places a filled ingot pile. The workshop's private {@code WORKSHOP_METALS} pool is not
 * reachable from tests. plateTiny keeps its item and U9 weight.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CoinRemovalTests {
    private static final String BATCH = "coin_removal";

    private CoinRemovalTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void pilePoolsKeepTheirComposition(GameTestHelper helper) {
        helper.assertTrue(GTDungeonData.PILE_METALS.length == 17, "PILE_METALS has seventeen entries");
        helper.assertTrue(count(GTDungeonData.PILE_METALS, "Silver") == 2, "PILE_METALS carries two silver");
        helper.assertTrue(count(GTDungeonData.PILE_METALS, "Gold") == 2, "PILE_METALS carries two gold");
        helper.assertTrue(count(GTDungeonData.PILE_METALS, "Platinum") == 1, "PILE_METALS carries one platinum");
        helper.assertTrue(GTDungeonData.PILE_GEMS.length == 13, "PILE_GEMS keeps its thirteen entries");
        helper.assertTrue(CrateMaterialTables.CRATE_METALS.length == 12, "CRATE_METALS keeps its twelve entries");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void dungeonPilePlacesAFilledIngotPile(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        StoneType rock = StoneType.values()[0];
        GTDungeonData data = new GTDungeonData(
                helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(), rock, rock, 0,
                new byte[][] {{1}}, 0, 0, 0, new long[0], new ItemStack[0], new boolean[0],
                new HashSet<>(), new HashSet<>(), RandomSource.create());
        helper.assertTrue(data.pile(0, 0, 0, GTDungeonData.PileKind.INGOT), "the pile helper places a block");
        helper.assertBlockPresent(PileBlock.Kind.INGOT.block(), new BlockPos(0, 1, 0));
        helper.assertTrue(
                helper.getLevel().getBlockEntity(origin) instanceof PileBlockEntity pile && !pile.stored().isEmpty(),
                "the placed pile carries an ingot stack");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void plateTinyKeepsItsItemAndWeight(GameTestHelper helper) {
        ItemStack tiny = GTItems.getStack(MaterialPrefix.plateTiny, GTMaterialRegistry.get("Iron"));
        helper.assertTrue(!tiny.isEmpty(), "the Iron tiny plate still exists");
        helper.assertTrue(MaterialPrefix.plateTiny.getMaterialWeight() == GTValues.U9, "the tiny plate keeps U9");
        helper.succeed();
    }

    private static int count(String[] pool, String name) {
        int total = 0;
        for (String entry : pool) if (entry.equals(name)) total++;
        return total;
    }
}