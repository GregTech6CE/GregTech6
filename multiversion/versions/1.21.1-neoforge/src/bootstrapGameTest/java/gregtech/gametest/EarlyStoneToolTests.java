package gregtech.gametest;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.tool.GTToolHelper;
import gregtech.api.tool.GTToolType;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** The ordinary rocks in the user's report must work in the real early crafting recipes. */
@GameTestHolder("gt6ce_early_survival")
@PrefixGameTestTemplate(false)
public final class EarlyStoneToolTests {
    private EarlyStoneToolTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void plainStoneRocksMakeTheirOwnTools(GameTestHelper helper) {
        var patterns = new LinkedHashMap<GTToolType, String[]>();
        patterns.put(GTToolType.AXE, new String[]{"XX", "XH"});
        patterns.put(GTToolType.HOE, new String[]{"XX", " H"});
        patterns.put(GTToolType.SHOVEL, new String[]{"X", "H"});
        patterns.put(GTToolType.PICKAXE, new String[]{"XXX", " H "});
        patterns.put(GTToolType.CLUB, new String[]{" XX", "XXX", "HX "});
        patterns.put(GTToolType.HARD_HAMMER, new String[]{"XX ", "XXH", "XX "});
        for (String name : new String[]{"GraniteBlack", "GraniteRed", "Basalt"}) {
            var material = GTMaterialRegistry.get(name);
            var rock = GTItems.getStack(MaterialPrefix.rockGt, material);
            helper.assertTrue(!rock.isEmpty(), name + " has a real registered rock");
            for (var entry : patterns.entrySet()) {
                var input = grid(entry.getValue(), rock);
                var recipe = helper.getLevel().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                        .orElseThrow(() -> new IllegalStateException("No real recipe for " + name + " " + entry.getKey()));
                var output = recipe.value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(GTToolHelper.isTool(output) && GTToolHelper.getType(output) == entry.getKey()
                        && GTToolHelper.getHead(output).resolve() == material.resolve(),
                        name + " rock recipe must retain the stone tool material for " + entry.getKey());
            }
        }
        var oreRock = GTItems.getStack(MaterialPrefix.rockGt, GTMaterialRegistry.get("Iron"));
        helper.assertTrue(!oreRock.isEmpty(), "the ore-bearing control rock exists");
        var invalid = grid(patterns.get(GTToolType.HARD_HAMMER), oreRock);
        helper.assertTrue(helper.getLevel().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, invalid, helper.getLevel()).isEmpty(),
                "ore-bearing iron rocks must not use the plain stone hammer route");
        helper.succeed();
    }

    private static CraftingInput grid(String[] pattern, ItemStack rock) {
        var stacks = new ArrayList<ItemStack>();
        for (String row : pattern) for (int x = 0; x < row.length(); x++)
            stacks.add(switch (row.charAt(x)) {
                case 'X' -> rock.copy();
                case 'H' -> new ItemStack(Items.STICK);
                default -> ItemStack.EMPTY;
            });
        return CraftingInput.of(pattern[0].length(), pattern.length, stacks);
    }
}
