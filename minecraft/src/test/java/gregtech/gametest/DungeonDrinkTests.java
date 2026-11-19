package gregtech.gametest;

import gregtech.content.block.stone.StoneType;
import gregtech.content.blockentity.inventory.BottleCrateBlockEntity;
import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import gregtech.content.item.BottleItem;
import gregtech.content.registry.GTFluids;
import gregtech.content.worldgen.dungeon.DungeonBottleSupplies;
import gregtech.content.worldgen.dungeon.GTDungeonData;
import gregtech.engine.stack.StackCompat;
import gregtech.platform.Mc;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class DungeonDrinkTests {
    private static final String BATCH = "dungeon_drinks";

    private DungeonDrinkTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void dungeonCupPlacesTheSelectedDrink(GameTestHelper helper) {
        var coreMead = GTFluids.stack("Mead", 250);
        var expected = ModList.get().isLoaded("gt6food")
                ? Mc.fluid(foodId("mead"))
                : coreMead == null || coreMead.isEmpty() ? Fluids.WATER : coreMead.getFluid();
        var actual = placeCup(helper, "Mead");
        helper.assertTrue(actual.getFluid() == expected && actual.getAmount() == 250,
                "the cup contains 250 mB of the selected registered fluid");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void dungeonCupFallsBackWhenItsDrinkHasNoCoreFluid(GameTestHelper helper) {
        var expected = ModList.get().isLoaded("gt6food") ? Mc.fluid(foodId("holy_water")) : Fluids.WATER;
        var actual = placeCup(helper, "Holywater");
        helper.assertTrue(actual.getFluid() == expected && actual.getAmount() == 250,
                "the cup uses the addon's Holy Water or water when it is unavailable");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void dungeonCupCanStillBeEmpty(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(0, 1, 0));
        var data = dungeon(helper, origin, 0);
        helper.assertTrue(data.cup(0, 0, 0), "the dungeon can place an empty cup");
        var entity = helper.getLevel().getBlockEntity(origin);
        helper.assertTrue(entity instanceof PortableContainerBlockEntity, "the dungeon placed a fluid cup");
        var cup = (PortableContainerBlockEntity) entity;
        helper.assertTrue(StackCompat.fluidHandlerOrThrow(cup.contents()).getFluidInTank(0).isEmpty(),
                "the unfilled cup remains empty");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void workshopCratePlacesSelectedDrinkBottles(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(0, 1, 0));
        // Seed 2 selects the drink on the first inventory roll.
        var data = dungeon(helper, origin, 2);
        DungeonBottleSupplies.drinks(data, 0, 0, 0, "Mead");
        var crate = crateAt(helper, origin);
        helper.assertTrue(crate.items().getStackInSlot(0).is(expectedDrinkBottle("mead")),
                "the workshop crate stocks the selected Mead bottle");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void chemicalCratePlacesSpecialDrinksAndFillsNinthSlot(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(0, 1, 0));
        // Seed 168 selects a filled bottle for all nine slots.
        var data = dungeon(helper, origin, 168);
        DungeonBottleSupplies.chemicals(data, 0, 0, 0);
        var items = crateAt(helper, origin).items();
        helper.assertTrue(items.getStackInSlot(5).is(Mc.item(Mc.gregtechId("ink_bottle"))),
                "the chemical crate keeps its core ink bottle");
        helper.assertTrue(items.getStackInSlot(6).is(expectedDrinkBottle("purple_drink")),
                "the chemical crate stocks Purple Drink");
        helper.assertTrue(items.getStackInSlot(7).is(expectedDrinkBottle("holy_water")),
                "the chemical crate stocks Holy Water");
        var indigo = Mc.item(Mc.gregtechId("bottled_indigo_dye"));
        var ninth = indigo == null || indigo == Items.AIR ? BottleItem.emptyBottle().getItem() : indigo;
        helper.assertTrue(items.getStackInSlot(8).is(ninth) && items.getStackInSlot(8).getCount() == 3,
                "the crate resolves the ninth slot and preserves its rolled count");
        helper.succeed();
    }

    private static GTDungeonData dungeon(GameTestHelper helper, BlockPos origin, long seed) {
        StoneType rock = StoneType.values()[0];
        return new GTDungeonData(
                helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(), rock, rock, 0,
                new byte[][] {{1}}, 0, 0, 0, new long[0], new ItemStack[0], new boolean[0],
                new HashSet<>(), new HashSet<>(), RandomSource.create(seed));
    }

    private static FluidStack placeCup(GameTestHelper helper, String field) {
        var origin = helper.absolutePos(new BlockPos(0, 1, 0));
        var data = dungeon(helper, origin, 0);
        helper.assertTrue(data.cup(0, 0, 0, field), "the dungeon can place a filled cup");
        var entity = helper.getLevel().getBlockEntity(origin);
        helper.assertTrue(entity instanceof PortableContainerBlockEntity, "the dungeon placed a fluid cup");
        var cup = (PortableContainerBlockEntity) entity;
        return StackCompat.fluidHandlerOrThrow(cup.contents()).getFluidInTank(0);
    }

    private static BottleCrateBlockEntity crateAt(GameTestHelper helper, BlockPos origin) {
        var entity = helper.getLevel().getBlockEntity(origin);
        helper.assertTrue(entity instanceof BottleCrateBlockEntity, "the dungeon placed a bottle crate");
        return (BottleCrateBlockEntity) entity;
    }

    private static Item expectedDrinkBottle(String path) {
        if (ModList.get().isLoaded("gt6food")) return Mc.item(foodId(path));
        Item item = Mc.item(Mc.gregtechId(path));
        return item == null || item == Items.AIR ? BottleItem.emptyBottle().getItem() : item;
    }

    private static ResourceLocation foodId(String path) {
        return ResourceLocation.tryParse("gt6food:" + path);
    }
}
