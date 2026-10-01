package com.gregtech.gregtech.platform.neoforge.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.machine.crucible.CrucibleReactions;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.ModReferences;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Full shared-domain checks in game bootstrap, with original numeric fixtures; no item/worldgen claim. */
@GameTestHolder("gregtech_bootstrap")
@PrefixGameTestTemplate(false)
public final class SharedMaterialBootstrapGameTests {
    // Recorded from original Git blobs and original Loader_Materials order, not this port's output.
    private static final String ROLE_SHA256 = "734a7fee39e9326dbfae519c34f3b9a0cb510d7d7a5535858cd04ab0aea99ccb";
    private SharedMaterialBootstrapGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void completeMaterialDirectory(GameTestHelper helper) throws ReflectiveOperationException {
        helper.assertTrue(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY,
                "Material linking must finish before game bootstrap");
        helper.assertTrue(GTMaterialRegistry.allMaterials().size() == 1156, "Original 1156 objects must remain");
        var ids = new HashSet<Integer>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.getId() > 0) {
                helper.assertTrue(material.getId() < 10000 && ids.add(material.getId()),
                        "Positive material IDs must be unique and in the original range: " + material);
                helper.assertTrue(GTMaterialRegistry.get(material.getId()) == material.resolve(),
                        "Numeric ID must retain canonical object identity: " + material);
            }
            helper.assertTrue(GTMaterialRegistry.get(material.getName()) == material.resolve(),
                    "Material name must resolve to the same canonical object: " + material);
            for (var component : material.getCompositionComponents()) {
                helper.assertTrue(component.material().isValid() && component.amount() > 0,
                        "Composition links must retain valid positive material inputs: " + material);
            }
        }
        helper.assertTrue(ids.size() == 1101, "Original 1101 positive IDs must remain");
        // Test-only observation of the entire alias map; no second runtime material directory.
        var field = GTMaterialRegistry.class.getDeclaredField("BY_NAME");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var aliases = (Map<String, GTMaterial>) field.get(null);
        helper.assertTrue(aliases.size() == 1519, "Original 1519 name entries must remain");
        for (var alias : aliases.entrySet()) {
            helper.assertTrue(GTMaterialRegistry.get(alias.getKey()) == alias.getValue().resolve(),
                    "Alias must retain canonical identity: " + alias.getKey());
        }
        helper.assertTrue(PrefixRegistry.all().size() == 109
                        && PrefixRegistry.byName("casingSmall") == MaterialPrefix.itemCasing,
                "Original 109 prefixes and identical-form alias must remain");
        helper.assertTrue(ModReferences.MC.isLoaded() && ModReferences.GT.isLoaded()
                        && !ModReferences.UNKNOWN.isLoaded(), "Neo loader presence must precede metadata initialization");
        helper.assertTrue(Materials.Copper == GT6Materials.Elements.Cu && Materials.Copper == GTMaterialRegistry.get("Cu")
                        && Materials.Copper == GTMaterialRegistry.get(290) && Materials.Tin == GT6Materials.Elements.Sn
                        && Materials.Tin == GTMaterialRegistry.get("Sn") && Materials.Tin == GTMaterialRegistry.get(500)
                        && Materials.Bronze == GTMaterialRegistry.get(8610), "Original Cu/Sn/Bronze IDs and aliases must retain identity");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void originalRolesUnitsAndComposition(GameTestHelper helper) throws Exception {
        helper.assertTrue(StoneType.values().length == 27, "All 27 original stone descriptors must remain");
        for (StoneType stone : StoneType.values()) {
            helper.assertTrue(stone.material().has(MaterialProperty.STONE), "Original stone role must be applied: " + stone);
        }
        helper.assertTrue(ROLE_SHA256.equals(roleSnapshotSha256()),
                "Every material's three catalog roles and all 27 stone descriptors must match the original goldens");
        helper.assertTrue(GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.GENERATE_ORE)).count() == 210
                        && GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.GENERATE_ORE_PROCESSING)).count() == 415
                        && GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.STONE)).count() == 81,
                "Original counts of ore/ore-processing/stone roles must remain210/415/81");
        for (GTMaterial material : List.of(Materials.Iron, Materials.WroughtIron, Materials.Steel,
                Materials.Titanium, Materials.Tungsten)) {
            helper.assertTrue(material.has(MaterialProperty.NEVER_FURNACE), "Original domain post-init must run: " + material);
        }
        helper.assertTrue(GTMaterialRegistry.get("Graphene").getTargetPulverMaterial() == Materials.Carbon,
                "Original post-init pulverization mapping must be linked");
        helper.assertTrue(Materials.Copper.getMeltingPoint() == 1357 && Materials.Copper.getBoilingPoint() == 2835
                        && Materials.Tin.getMeltingPoint() == 505 && Materials.Tin.getBoilingPoint() == 2875
                        && Materials.Bronze.getMeltingPoint() == 1357 && Materials.Bronze.getBoilingPoint() == 2835
                        && Float.floatToIntBits(Materials.Bronze.getDensity()) == Float.floatToIntBits(8.54175F),
                "Original Cu/Sn/Bronze physical values must remain");
        var composition = Materials.Bronze.getCompositionComponents();
        helper.assertTrue(Materials.Bronze.getCompositionDivider() == 4 && composition.size() == 2
                        && composition.get(0).material() == Materials.Copper && composition.get(0).amount() == 1_945_944_000L
                        && composition.get(1).material() == Materials.Tin && composition.get(1).amount() == 648_648_000L,
                "Bronze must retain ordered 3U Cu+1U Sn with dimensionless divider4");
        helper.assertTrue(MaterialPrefix.ingot.getMaterialWeight() == 648_648_000L
                        && MaterialPrefix.nugget.getMaterialWeight() == 72_072_000L
                        && MaterialPrefix.dustDiv72.getMaterialWeight() == 9_009_000L
                        && GregTechConstants.C == 273 && GregTechConstants.DEF_ENV_TEMP == 293
                        && GregTechConstants.L == 144, "Original unit fractions and Kelvin/litre boundaries must remain");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void originalCopperTinReaction(GameTestHelper helper) {
        var contents = stacks(Materials.Copper, 2_270_268_000L, Materials.Tin, 648_648_000L);
        helper.assertTrue(CrucibleMaterialStack.total(contents) == 2_918_916_000L,
                "Original 3.5U Cu+1U Sn input must retain exact amounts");
        helper.assertTrue(!CrucibleReactions.react(contents, 1356)
                        && amountOf(contents, Materials.Copper) == 2_270_268_000L
                        && amountOf(contents, Materials.Tin) == 648_648_000L,
                "Original cold rejection must consume no material");
        helper.assertTrue(CrucibleReactions.react(contents, 1357), "Original bronze melting point must admit reaction");
        helper.assertTrue(amountOf(contents, Materials.Bronze) == 2_594_592_000L
                        && amountOf(contents, Materials.Copper) == 324_324_000L
                        && amountOf(contents, Materials.Tin) == 0
                        && CrucibleMaterialStack.total(contents) == 2_918_916_000L,
                "Original 4U bronze yield and halfU excess copper must conserve exact material units");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void originalAlternateReactions(GameTestHelper helper) {
        helper.assertTrue(CrucibleReactions.recipes().size() == 42 && CrucibleReactions.allRecipes().size() == 173,
                "All 42 explicit and 173 total original reactions must remain");
        var annealed = stacks(Materials.AnnealedCopper, 1_945_944_000L, Materials.Tin, 648_648_000L);
        helper.assertTrue(Materials.AnnealedCopper.getMeltingPoint() == 2800 && CrucibleReactions.react(annealed, 1357)
                        && annealed.size() == 1 && amountOf(annealed, Materials.Bronze) == 2_594_592_000L,
                "Original explicit alternate Cu recipe must retain its below-input-melting-point yield");
        var flux = stacks(GT6Materials.Ores.Fe2O3, 3_243_240_000L, Materials.Carbon, 648_648_000L);
        CrucibleMaterialStack.of(GT6Materials.Ores.CaCO3, 648_648_000L).addToList(flux);
        helper.assertTrue(!CrucibleReactions.react(flux, 300), "Original cold hematite reduction must be rejected");
        helper.assertTrue(GT6Materials.Elements.Fe.getMeltingPoint() == 1811
                        && CrucibleReactions.react(flux, 1811)
                        && flux.size() == 1 && amountOf(flux, GT6Materials.Elements.Fe) == 1_297_296_000L,
                "Original reduction consumes carbon/flux and yields exactly2U iron");
        var noFlux = stacks(GT6Materials.Ores.Fe2O3, 3_243_240_000L, Materials.Carbon, 648_648_000L);
        helper.assertTrue(!CrucibleReactions.react(noFlux, 1811),
                "Original reduction must require calcite");
        helper.succeed();
    }

    /** Test-only observation, also used with original Git blobs to capture an independent golden. */
    public static String roleSnapshotSha256() throws Exception {
        var rows = new ArrayList<String>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            rows.add("M|" + material.getId() + ':' + material.getName() + '|'
                    + material.has(MaterialProperty.GENERATE_ORE) + '|'
                    + material.has(MaterialProperty.GENERATE_ORE_PROCESSING) + '|'
                    + material.has(MaterialProperty.STONE));
        }
        for (StoneType stone : StoneType.values()) {
            rows.add("S|" + stone.ordinal() + '|' + stone.name() + '|' + stone.registryId() + '|' + stone.textureFolder()
                    + '|' + stone.material().getId() + ':' + stone.material().getName()
                    + '|' + Float.toHexString(stone.resistanceMultiplier()) + '|' + Float.toHexString(stone.hardnessMultiplier())
                    + '|' + stone.harvestLevel() + '|' + stone.witherProof());
        }
        Collections.sort(rows);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(String.join("\n", rows).getBytes(StandardCharsets.UTF_8)));
    }

    private static ArrayList<CrucibleMaterialStack> stacks(GTMaterial a, long amountA, GTMaterial b, long amountB) {
        return new ArrayList<>(List.of(CrucibleMaterialStack.of(a, amountA), CrucibleMaterialStack.of(b, amountB)));
    }

    private static long amountOf(List<CrucibleMaterialStack> contents, GTMaterial material) {
        return contents.stream().filter(stack -> stack.material == material).mapToLong(stack -> stack.amount).sum();
    }
}
