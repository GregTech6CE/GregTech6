package gregtech.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Minimal GameTest wiring check; recipe issue cases are added in the next phase. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CraftingRecipeSmokeTests {
    // Verifies the GameTest server, namespace, template, and a known gt6ce item are reachable.
    @GameTest(template = "test_empty", batch = "crafting_issue_smoke", timeoutTicks = 200)
    public static void serverSeesRegisteredContent(GameTestHelper helper) {
        var id = ResourceLocation.tryParse("gt6ce:plate_graphene");
        helper.assertTrue(BuiltInRegistries.ITEM.getOptional(id).isPresent(), "gt6ce:plate_graphene is registered");
        helper.assertTrue(helper.getLevel().getServer() != null, "game test server is available");
        helper.succeed();
    }
}
