package gregtech.gametest;

import gregtech.content.data.RegisteredFluids;
import gregtech.engine.fluid.FluidVisualPolicy;
import gregtech.engine.mod.GregTechIdentity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
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

/**
 * A molten fluid draws the icon of its material texture set, resolved along the set's parent chain
 * against sprite ids. The policy receives atlas keys, never texture file paths, so the probe here
 * fails for anything carrying a {@code textures/} prefix or a {@code .png} suffix.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class FluidVisualPolicyTests {
    private static final String BATCH = "fluid_visual_policy";
    private static final String FINE_MOLTEN = "block/material_icons/fine/molten";
    private static final String DULL_MOLTEN = "block/material_icons/dull/molten";
    private static final String TEMPLATE_MOLTEN = "block/fluids/templates/molten";

    private FluidVisualPolicyTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void moltenCheeseKeepsItsFineIcon(GameTestHelper helper) {
        List<String> probed = new ArrayList<>();
        FluidVisualPolicy.Visual visual =
                FluidVisualPolicy.select(RegisteredFluids.Molten_Cheese, moltenSprites(probed, FINE_MOLTEN));
        helper.assertTrue(
                visual.texture().equals(spriteId(FINE_MOLTEN)),
                "molten cheese keeps its own FINE icon, got " + visual.texture());
        assertSpriteIdProbes(helper, probed);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void moltenIronBorrowsTheDullIconAlongItsParentChain(GameTestHelper helper) {
        RegisteredFluids.FluidEntry iron = RegisteredFluids.get("GenMolten_Iron");
        helper.assertTrue(iron != null, "iron has a generated molten fluid");
        List<String> probed = new ArrayList<>();
        FluidVisualPolicy.Visual visual = FluidVisualPolicy.select(iron, moltenSprites(probed, DULL_MOLTEN));
        helper.assertTrue(
                visual.texture().equals(spriteId(DULL_MOLTEN)),
                "molten iron borrows the DULL icon its METALLIC set inherits, got " + visual.texture());
        assertSpriteIdProbes(helper, probed);
        helper.succeed();
    }

    /** Only the named molten icons and the generic template exist, as in a pack without fluid art. */
    private static Predicate<ResourceLocation> moltenSprites(List<String> probed, String... present) {
        List<String> existing = new ArrayList<>(List.of(present));
        existing.add(TEMPLATE_MOLTEN);
        return id -> {
            String path = id.getPath();
            probed.add(path);
            return id.getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE) && existing.contains(path);
        };
    }

    private static ResourceLocation spriteId(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, path);
    }

    private static void assertSpriteIdProbes(GameTestHelper helper, List<String> probed) {
        helper.assertTrue(!probed.isEmpty(), "the policy probes at least one sprite id");
        for (String path : probed) {
            helper.assertTrue(!path.startsWith("textures/"), "sprite id without textures/ prefix, got " + path);
            helper.assertTrue(!path.endsWith(".png"), "sprite id without .png suffix, got " + path);
        }
    }
}
