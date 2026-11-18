package gregtech.gametest;

import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.item.PocketToolItem;
import gregtech.content.registry.GTManualStations;
import gregtech.engine.tool.GTToolType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ManualToolWearTests {
    private static final int FILE_WEAR = 400;
    private static final int SCREWDRIVER_WEAR = 400;

    private ManualToolWearTests() {}

    @GameTest(template = "test_empty", batch = "manual_tool_wear")
    public static void fileWearInShapelessCrafting(GameTestHelper helper) {
        var recipe = recipe(helper, "clay_molds/imprint_154_all");
        var mold = item("clay_mold");
        var file = tool(GTToolType.FILE);
        file.setDamageValue(17);
        var input = grid(2, 1, List.of(mold, file));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "The clay mold recipe should accept a file");
        assertWear(helper, file, recipe.getRemainingItems(input).get(1), FILE_WEAR);
        helper.assertTrue(file.getDamageValue() == 17, "Calculating crafting remainders changed the input file");

        file.setDamageValue(file.getMaxDamage() - FILE_WEAR);
        input = grid(2, 1, List.of(mold, file));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "The file should allow its last craft");
        helper.assertTrue(recipe.getRemainingItems(input).get(1).isEmpty(), "The exhausted file should break");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "manual_tool_wear")
    public static void screwdriverWearInShapedCrafting(GameTestHelper helper) {
        var recipe = recipe(helper, "hand_components/integrated_circuit_1");
        var circuit = item("integrated_circuit_0");
        var screwdriver = tool(GTToolType.SCREWDRIVER);
        var input = grid(2, 2, List.of(screwdriver, ItemStack.EMPTY, ItemStack.EMPTY, circuit));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "The circuit recipe should accept a screwdriver");
        assertWear(helper, screwdriver, recipe.getRemainingItems(input).get(0), SCREWDRIVER_WEAR);
        helper.assertTrue(screwdriver.getDamageValue() == 0,
                "Calculating crafting remainders changed the input screwdriver");

        var pocketFile = tool(GTToolType.POCKET_FILE);
        pocketFile.setDamageValue(23);
        var fileInput = grid(2, 2, List.of(pocketFile, ItemStack.EMPTY, ItemStack.EMPTY, circuit));
        helper.assertFalse(recipe.matches(fileInput, helper.getLevel()), "The circuit recipe accepted pocket file mode");

        var pocketScrewdriver = switchPocketMode(helper, pocketFile);
        helper.assertTrue(pocketScrewdriver.getDamageValue() == 23, "Switching pocket modes changed existing wear");
        var screwdriverInput = grid(2, 2, List.of(pocketScrewdriver, ItemStack.EMPTY, ItemStack.EMPTY, circuit));
        helper.assertTrue(recipe.matches(screwdriverInput, helper.getLevel()),
                "The circuit recipe should accept pocket screwdriver mode");
        assertWear(helper, pocketScrewdriver, recipe.getRemainingItems(screwdriverInput).get(0), SCREWDRIVER_WEAR);
        helper.assertTrue(pocketScrewdriver.getDamageValue() == 23,
                "Calculating crafting remainders changed the input pocket tool");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "manual_tool_wear")
    public static void softHammerDurabilityAndHarvestSpeed(GameTestHelper helper) {
        var spruce = GT6Materials.Woods.Spruce;
        var hammer = GTToolItem.create(GTToolType.SOFT_HAMMER, spruce, spruce);
        helper.assertTrue(hammer.getMaxDamage() == spruce.getToolDurability() * 100L * 8L,
                "The soft hammer should have eight times the material durability");
        float expectedSpeed = spruce.getToolSpeed() * 0.1F;
        var dustFunnel = GTManualStations.DUST_FUNNEL.get();
        helper.assertTrue(hammer.getDestroySpeed(Blocks.REDSTONE_LAMP.defaultBlockState()) == expectedSpeed,
                "The soft hammer should mine redstone lamps at one tenth of the material speed");
        helper.assertTrue(hammer.getDestroySpeed(dustFunnel.defaultBlockState()) == expectedSpeed,
                "The soft hammer should mine dust funnels at one tenth of the material speed");
        helper.assertTrue(hammer.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 0.0F,
                "The soft hammer should have no effective mining speed on ordinary stone");

        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        var target = helper.spawn(EntityType.COW, new BlockPos(3, 1, 1));
        hammer.getItem().hurtEnemy(hammer, target, attacker);
        helper.assertTrue(hammer.getDamageValue() == 200, "Attacking with the soft hammer should add 200 wear");
        var knife = tool(GTToolType.BUTCHERY_KNIFE);
        knife.getItem().hurtEnemy(knife, target, attacker);
        helper.assertTrue(knife.getDamageValue() == 400, "Attacking with the butchery knife should add 400 wear");
        helper.succeed();
    }

//? if >=1.21.1 {
    /*@GameTest(template = "test_empty", batch = "manual_tool_wear")
    public static void savedSawUsesCurrentAttackDamage(GameTestHelper helper) {
        var saw = tool(GTToolType.SAW);
        int quality = GT6Materials.Compounds.VanadiumSteel.getToolQuality();
        var savedDamage = new AttributeModifier(
                GTToolItem.ATTACK_DAMAGE_MODIFIER_ID, 2.0F + quality, AttributeModifier.Operation.ADD_VALUE);
        var externalDamage = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("gt6ce", "test_saw_bonus"),
                0.5, AttributeModifier.Operation.ADD_VALUE);
        saw.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, savedDamage, EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_DAMAGE, externalDamage, EquipmentSlotGroup.MAINHAND)
                .build());

        var effectiveModifiers = saw.getAttributeModifiers().modifiers();
        var expectedDamage = new AttributeModifier(
                savedDamage.id(), 1.75F + quality, AttributeModifier.Operation.ADD_VALUE);
        helper.assertTrue(effectiveModifiers.contains(new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_DAMAGE, expectedDamage, EquipmentSlotGroup.MAINHAND)),
                "A saw with a saved modifier should use the current base damage plus head quality");
        helper.assertTrue(effectiveModifiers.contains(new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_DAMAGE, externalDamage, EquipmentSlotGroup.MAINHAND)),
                "Updating the saw attack modifier should retain external modifiers");
        helper.succeed();
    }
*///?}

    private static ItemStack switchPocketMode(GameTestHelper helper, ItemStack pocketFile) {
//? if >=1.21.1 {
        /*Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        Player player = helper.makeMockPlayer();
//?}
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, pocketFile);
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.STONE);
        var target = helper.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(((PocketToolItem) pocketFile.getItem()).onItemUseFirst(pocketFile, context).consumesAction(),
                "Sneak-using the pocket file should switch modes");
        var switched = player.getMainHandItem();
        helper.assertTrue(switched.getItem() instanceof PocketToolItem pocket
                        && pocket.toolType() == GTToolType.POCKET_SCREWDRIVER,
                "The pocket file should switch to screwdriver mode");
        return switched;
    }

    private static CraftingRecipe recipe(GameTestHelper helper, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
        var entry = helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new AssertionError("Missing " + id));
//? if >=1.21.1 {
        /*return (CraftingRecipe) entry.value();
*///?} else {
        return (CraftingRecipe) entry;
//?}
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path)));
    }

    private static ItemStack tool(GTToolType type) {
        return GTToolItem.create(type, GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce);
    }

    private static void assertWear(GameTestHelper helper, ItemStack original, ItemStack remainder, int wear) {
        helper.assertTrue(remainder.is(original.getItem()) && remainder.getCount() == 1,
                "Crafting should return one tool of the same type");
        int addedWear = remainder.getDamageValue() - original.getDamageValue();
        helper.assertTrue(addedWear == wear,
                "Crafting should add " + wear + " wear to " + original.getItem() + ", got " + addedWear);
    }

//? if >=1.21.1 {
    /*private static CraftingInput grid(int width, int height, List<ItemStack> items) {
        return CraftingInput.ofPositioned(width, height, items).input();
    }
*///?} else {
    private static TransientCraftingContainer grid(int width, int height, List<ItemStack> items) {
        var menu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        var grid = new TransientCraftingContainer(menu, width, height);
        for (int index = 0; index < items.size(); index++) grid.setItem(index, items.get(index));
        return grid;
    }
//?}
}
