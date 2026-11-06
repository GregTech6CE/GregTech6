package gregtech.gametest;

import gregtech.content.damage.GTHazmat;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The shared hazard-suit tags reach the running game on both loaders; 1.21.1 ships them through
 * the generated item-tag tree.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class HazmatTagTests {
    private static final String BATCH = "hazmat_tags";
    private static final List<String> SLOTS = List.of("helmet", "chestplate", "leggings", "boots");
    private static final List<TagKey<Item>> ALL_SETS = List.of(
            GTHazmat.CHEM,
            GTHazmat.HEAT,
            GTHazmat.FROST,
            GTHazmat.ELECTRIC,
            GTHazmat.BIO,
            GTHazmat.INSECT,
            GTHazmat.GAS,
            GTHazmat.RADIATION);

    private HazmatTagTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void universalPiecesBelongToEveryHazardSet(GameTestHelper helper) {
        for (String slot : SLOTS) {
            var stack = stack(helper, "universal_suit_" + slot);
            for (var set : ALL_SETS)
                helper.assertTrue(stack.is(set), "universal_suit_" + slot + " is missing from " + set.location());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void eachSuitCoversItsOwnHazards(GameTestHelper helper) {
        assertSet(helper, "biochemgas_suit", List.of(GTHazmat.CHEM, GTHazmat.BIO, GTHazmat.GAS));
        assertSet(helper, "heat_suit", List.of(GTHazmat.HEAT));
        assertSet(helper, "frost_suit", List.of(GTHazmat.FROST));
        assertSet(helper, "insect_suit", List.of(GTHazmat.INSECT));
        assertSet(helper, "radiation_suit", List.of(GTHazmat.RADIATION));
        helper.succeed();
    }

    private static void assertSet(GameTestHelper helper, String suit, List<TagKey<Item>> sets) {
        for (String slot : SLOTS) {
            var stack = stack(helper, suit + "_" + slot);
            for (var set : sets)
                helper.assertTrue(stack.is(set), suit + "_" + slot + " is missing from " + set.location());
        }
    }

    private static ItemStack stack(GameTestHelper helper, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
        var item = BuiltInRegistries.ITEM.getOptional(id);
        helper.assertTrue(item.isPresent(), "missing item gt6ce:" + path);
        return new ItemStack(item.orElseThrow());
    }
}
