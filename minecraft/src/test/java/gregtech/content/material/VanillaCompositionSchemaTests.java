package gregtech.content.material;

import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class VanillaCompositionSchemaTests {
    private VanillaCompositionSchemaTests() {}

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void invalidRowsAreRejectedWithSpecificDiagnostics(GameTestHelper helper) {
        assertRejected(helper, "unknown_material", "{\"components\":{\"NotAMaterial\":1}}", "unknown material");
        assertRejected(helper, "invalid_amount", "{\"components\":{\"Iron\":0}}", "must be positive");
        assertRejected(
                helper,
                "unknown_prefix",
                "{\"components\":{\"Iron\":1},\"prefix\":\"not_a_prefix\"}",
                "unknown prefix");
        assertRejected(
                helper,
                "missing_inherit",
                "{\"components\":{\"Iron\":1},\"inherit\":\"vanilla-unification\"}",
                "no vanilla-unification declaration");
        assertRejected(
                helper,
                "contradictory_keys",
                "{\"components\":{\"Iron\":1},\"prefix\":\"ingot\",\"inherit\":\"vanilla-unification\"}",
                "mutually exclusive");
        assertRejected(
                helper,
                "contradictory_recovery",
                "{\"components\":{\"Iron\":1},\"recoverable\":true,\"bulkRecovery\":false}",
                "disagree");
        helper.succeed();
    }

    private static void assertRejected(GameTestHelper helper, String item, String json, String message) {
        var errors = VanillaCompositionLoader.validateRow(item, JsonParser.parseString(json).getAsJsonObject());
        helper.assertTrue(
                errors.stream().anyMatch(error -> error.contains(message)),
                item + " did not report '" + message + "': " + errors);
    }
}
