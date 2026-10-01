package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialForms;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GT6's per-material form flags ({@code MaterialForms}, imported from {@code TD.java}/{@code MT.java})
 * are what {@code MaterialPrefix}'s conditions consult for the forms the port's own material
 * properties do not imply. The lookup has to find them reliably:
 *
 * <ul>
 *   <li>keyed by GT6 material id (the id {@code MT.java} passes to the declaration factory, which the
 *       port carries), because the port looks materials up by *display* name while GT6's table is
 *       written in *field* names — {@code MT.Ke} is Trinium, {@code MT.Nq} is Naquadah, and every wood
 *       the port renamed ({@code MT.Mossy} is {@code WoodMossy}) used to miss its flags;</li>
 *   <li>keyed by name only when unambiguous, because normalisation collides: {@code Co} cobalt vs
 *       {@code CO} carbon monoxide, and {@code Gold} the BiomesOPlenty wood vs the {@code gold()}
 *       factory in {@code MT.java:468} that used to shadow {@code Clay}'s flags with a one-flag
 *       pseudo-material.</li>
 * </ul>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialFormParityTests {

    private static void hasFlags(GameTestHelper h, GTMaterial material, String... flags) {
        h.assertTrue(material != null && material.isValid(), "material exists: " + material);
        var found = MaterialForms.of(material);
        h.assertTrue(!found.isEmpty(), material.getName() + " has GT6 form flags (id "
                + material.getId() + ")");
        for (String flag : flags) {
            h.assertTrue(found.contains(flag), material.getName() + " carries " + flag
                    + " (has " + found + ")");
        }
    }

    /** GT6 flags are found through the material id, whatever the port calls the material. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void gt6FormFlagsResolveById(GameTestHelper h) {
        // MT.Ke = Trinium (1260): G_INGOT_MACHINE_ORES, i.e. PROJECTILES among others
        hasFlags(h, Materials.Trinium, "PROJECTILES", "INGOTS", "PLATES", "MULTIPLATES");
        // MT.Nq = Naquadah (1740), same form set
        hasFlags(h, Materials.Naquadah, "PROJECTILES", "INGOTS", "PLATES");
        // MT.Gold = the BiomesOPlenty *wood* Goldwood (9369), declared through the gold() factory
        hasFlags(h, WoodMaterials.Goldwood, "PLATES", "STICKS", "FOILS", "PROJECTILES");
        // MT.Mossy = "Mossy Wood" (9311), a renamed wood: G_WOOD (TD.java:604)
        hasFlags(h, WoodMaterials.WoodMossy, "DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES");
        h.succeed();
    }

    /** Two names that normalise alike must not shadow each other (or a factory's pseudo-material). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void ambiguousNamesDoNotShadowMaterials(GameTestHelper h) {
        // Clay used to read the `clay` pseudo-material of MT.java's clay() factory: PLATES only.
        var clay = MaterialForms.of(Materials.Clay);
        h.assertTrue(clay.contains("ORES") && clay.contains("DUSTS"),
                "Clay keeps its own flags (ORES, DUSTS), not a factory's: " + clay);
        // metal gold (790) has rails, the wood Goldwood (9369) does not — the two must not swap flags
        var gold = MaterialForms.of(Materials.Gold);
        h.assertTrue(gold.contains("RAILS") && gold.contains("INGOTS"),
                "metal gold keeps the metal flags: " + gold);
        var goldwood = MaterialForms.of(WoodMaterials.Goldwood);
        h.assertTrue(!goldwood.contains("RAILS") && !goldwood.contains("INGOTS"),
                "the wood Goldwood does not inherit metal gold's flags: " + goldwood);
        h.assertTrue(!MaterialForms.of(Materials.Gold).equals(goldwood),
                "metal gold and Goldwood resolve to different flags");
        h.succeed();
    }

    /** The forms those flags drive exist as items — this is what the loot rows needed. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void projectileAndFoilFormsExist(GameTestHelper h) {
        ItemStack arrowHead = GTItems.getStack(MaterialPrefix.toolHeadArrow, Materials.Trinium, 1);
        h.assertTrue(!arrowHead.isEmpty(), "Trinium has an arrow head (GT6 OP.java:254)");
        ItemStack arrow = GTItems.getStack(MaterialPrefix.arrowGtWood, Materials.Trinium, 1);
        h.assertTrue(!arrow.isEmpty(), "Trinium has a wooden arrow (GT6 OP.java:281)");
        // G_WOOD includes FOILS, which the port's own wood properties never implied
        ItemStack foil = GTItems.getStack(MaterialPrefix.foil, WoodMaterials.WoodMossy, 1);
        h.assertTrue(!foil.isEmpty(), "Mossy Wood has the GT6 foil form");
        // …and the same for the metals the flags now reach through their id
        ItemStack naquadahFoil = GTItems.getStack(MaterialPrefix.foil, Materials.Naquadah, 1);
        h.assertTrue(!naquadahFoil.isEmpty(), "Naquadah has the GT6 foil form");
        h.succeed();
    }
}
