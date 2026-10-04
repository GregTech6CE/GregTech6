package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.machine.crucible.CrucibleReactions;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.ModReferences;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.*;

/** Full imported domain bootstrap and original numeric fixtures, with no game dependencies. */
public final class MaterialBehaviorContracts {
    // Original scope/source hashes remain in provenance/material-baseline.json.
    // These fingerprints retain every observed field and include the authorized
    // Community Edition display name; the name-only differential against the
    // original fingerprints is recorded in provenance/community-material-baseline.json.
    // The restored port also lowercases explicit texture filenames for Minecraft;
    // its isolated differential is recorded in provenance/restored-prefix-baseline.json.
    // MT.java source corrections: PetrifiedWood stone/wood/rod forms; LigniteCoke and PetCoke fuel/ash.
    // See docs/integration/verification/tools-power-issues-20261003.md.
    // Full 110-row MT.woodnormal source restoration and isolated graph differential:
    // docs/integration/verification/normal-wood-differential-20261003.json.
    // Only five EMPTY ammunition form booleans change; isolated full graph proof:
    // docs/integration/verification/ammunition-differential-20261004.json.
    // Eight source Diamond bindings + Rubber hammer-head form; isolated graph proof:
    // docs/integration/verification/technology-differential-20261004.json.
    // Only Gunpowder/Dynamite source reaction flags changed; full graph differential:
    // docs/integration/verification/explosive-material-differential-20261004.json.
    // Four original external ore-prefix metadata entries; all prior graph rows unchanged:
    // docs/integration/verification/external-ore-material-differential-20261004.json.
    // Five further source external forms; all 133876 prior observations per stage remain:
    // docs/integration/verification/external-rest-material-differential-20261004.json.
    private static final String DEFINITIONS_SHA256 = "92765dc102da5275bd5a7080c0ef0d15f5bef194df99fd4193288dd786035ba5";
    private static final String POST_INIT_SHA256 = "5e0a71529936d275af1b1c4f134e24cedf14070959c4f15f17e3ba97bc053b0a";
    private static int assertions;
    private MaterialBehaviorContracts() {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        expectThrows(IllegalStateException.class, () -> new ModData("unbound", "Unbound"),
                "The platform must bind presence before creating metadata");
        check(ModData.MODS.isEmpty(), "Failed unbound construction must not publish metadata");
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gregtech"));
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.DEFINITIONS,
                "Initial registration phase must remain DEFINITIONS");
        // Same domain holder order as Loader_Data, excluding its game-only tables.
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        check(com.gregtech.gregtech.data.MaterialPrefix.stickLong.isValidFor(GTMaterialRegistry.get("Obsidian")), "source miniature Nether recipe has its Obsidian long rod form");
        check(com.gregtech.gregtech.data.MaterialPrefix.stickLong.isValidFor(GTMaterialRegistry.get("Endstone")), "source miniature End recipe has its Endstone long rod form");

        check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY,
                "The complete directory must finish linking");
        check(GTMaterialRegistry.allMaterials().size() == 1160, "All 1160 original material objects must remain");
        check(MaterialCatalogSnapshot.aliases().size() == 1523, "All 1523 original name entries must remain");
        check(PrefixRegistry.all().size() == 118, "109 prior prefixes plus nine source external ore forms must remain");
        check(DEFINITIONS_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Full definitions/aliases/forms must match the Community Edition snapshot");
        GTMaterialRegistry.init();
        check(DEFINITIONS_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Repeated init must not mutate or duplicate definitions");
        GTMaterialRegistry.postInit();
        check(POST_INIT_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Full domain post-init must match the Community Edition snapshot");
        var sourceGrinding = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        var wrought = GTMaterialRegistry.get("WroughtIron");
        var iron = GTMaterialRegistry.get("Iron");
        var hardGrinding = GTMaterialRegistry.get("Tungstensteel");
        check(sourceGrinding.get(3).allows(wrought), "source MORTAR WroughtIron permits Anvil external grinding");
        check(!sourceGrinding.get(3).allows(hardGrinding), "source non-MORTAR Tungstensteel rejects Anvil external grinding");
        check(sourceGrinding.get(16).allows(wrought), "source MORTAR WroughtIron permits external Mortar grinding");
        check(!sourceGrinding.get(16).allows(hardGrinding), "source non-MORTAR Tungstensteel rejects external Mortar grinding");
        check(sourceGrinding.get(0).allows(hardGrinding), "source non-MORTAR Tungstensteel still has its hard Shredder branch");
        check(sourceGrinding.get(0).outputMaterial(wrought) == iron, "source shredding turns WroughtIron into its Iron pulver target");
        var petrified = GTMaterialRegistry.get("PetrifiedWood");
        check(petrified.has(MaterialProperty.STONE) && petrified.has(MaterialProperty.WOOD), "MT 1259 PetrifiedWood is stone and wood");
        check(MaterialPrefix.rockGt.isValidFor(petrified) && MaterialPrefix.stick.isValidFor(petrified), "PetrifiedWood early rock and rod forms");
        for (var fuel : Map.of("CoalCoke", 3200, "LigniteCoke", 1600, "PetCoke", 6400).entrySet()) {
            var material = GTMaterialRegistry.get(fuel.getKey());
            check(material.getFurnaceBurnTime() == fuel.getValue(), "MT coke fuel " + fuel.getKey());
            check(material.getTargetBurningMaterial() == GTMaterialRegistry.get("DarkAshes") && material.getTargetBurningAmount() == GregTechConstants.U / 9, "MT coke dark ash " + fuel.getKey());
        }
        assertions += SourceWoodFixtures.validate();
        assertions += AmmunitionSourceSamples.verify();
        validateIdentityGraph();
        validateMetadata();
        check(com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("Knightmetal"), "Steel"), "ANY.Steel accepts Knightmetal screws and rings");
        check(com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("MeteoricSteel"), "Steel"), "ANY.Steel accepts MeteoricSteel screws and rings");
        check(com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(GTMaterialRegistry.get("EnderAmethyst"), GTMaterialRegistry.get("Steel")), "ANY.Iron accepts Steel rods for EnderAmethyst tools");
        check(!com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(GTMaterialRegistry.get("EnderAmethyst"), GTMaterialRegistry.get("Wood")), "EnderAmethyst does not fall back to a default wooden handle");
        check(com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("WoodTainted"), "WoodMagical"), "ANY.WoodMagical includes MT.WOODS.Tainted under its registered name");
        validateCopperTinBronze();
        validateAmountsAndReactions();
        System.out.println("Material behavior contracts passed: " + assertions
                + " assertions; 1160 materials, 1105 positive IDs, 1523 name entries, 118 prefixes, 173 reactions");
    }

    private static void validateIdentityGraph() throws Exception {
        Set<Integer> ids = new HashSet<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.getId() > 0) {
                check(material.getId() < 10000 && ids.add(material.getId()), "Numeric ID must be unique and retain the original range: " + material);
                check(GTMaterialRegistry.get(material.getId()) == material.resolve(), "ID lookup must retain canonical object identity: " + material);
            }
            check(GTMaterialRegistry.get(material.getName()) == material.resolve(), "Name lookup must resolve to the original object: " + material);
            for (MaterialComponent component : material.getCompositionComponents()) {
                check(component.material().isValid() && component.amount() > 0, "Composition must retain valid positive inputs: " + material);
            }
        }
        check(ids.size() == 1105, "All 1105 positive IDs must remain unique");
        for (var alias : MaterialCatalogSnapshot.aliases().entrySet())
            check(GTMaterialRegistry.get(alias.getKey()) == alias.getValue().resolve(), "Alias must retain identity: " + alias.getKey());
        check(GTMaterialRegistry.get(-1) == Materials.Invalid && GTMaterialRegistry.get(0) == Materials.Invalid
                        && GTMaterialRegistry.get(10000) == Materials.Invalid,
                "Invalid numeric lookups must retain sentinel behavior");
        check(PrefixRegistry.byName("casingSmall") == MaterialPrefix.itemCasing, "Original identical-form alias must remain");
        expectThrows(IllegalStateException.class,
                () -> GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(9999, "LateDefinition").build()),
                "Strict definitions must stay closed after linking");
    }

    private static void validateMetadata() {
        check(ModReferences.MC.isLoaded() && ModReferences.GT.isLoaded() && !ModReferences.UNKNOWN.isLoaded(),
                "Explicit platform presence and UNKNOWN override must be retained");
        check(ModReferences.MC.owns("minecraft:stone") && !ModReferences.MC.owns("other:stone")
                        && !ModReferences.MC.owns((String) null), "String ownership must retain loaded namespace behavior");
        expectThrows(IllegalStateException.class, () -> ModData.bindPresence(id -> true),
                "Late presence rebinding must fail instead of corrupting already-declared metadata");
        check(Materials.Copper.getTranslationKey().equals("material.gregtech.copper")
                        && Materials.Copper.getDisplayNameFallback().equals("Copper"), "Display data must remain available to platform Component adapters");
    }

    private static void validateCopperTinBronze() {
        GTMaterial copper = Materials.Copper, tin = Materials.Tin, bronze = Materials.Bronze;
        check(copper == GT6Materials.Elements.Cu && copper == GTMaterialRegistry.get("Cu")
                        && copper == GTMaterialRegistry.get(290), "Copper names and ID290 must be the same object");
        check(tin == GT6Materials.Elements.Sn && tin == GTMaterialRegistry.get("Sn")
                        && tin == GTMaterialRegistry.get(500), "Tin names and ID500 must be the same object");
        check(bronze == GTMaterialRegistry.get(8610), "Bronze ID8610 must retain identity");
        check(copper.getMeltingPoint() == 1357 && copper.getBoilingPoint() == 2835, "Copper must retain Kelvin1357/2835");
        check(tin.getMeltingPoint() == 505 && tin.getBoilingPoint() == 2875, "Tin must retain Kelvin505/2875");
        check(bronze.getMeltingPoint() == 1357 && bronze.getBoilingPoint() == 2835, "Bronze must retain Kelvin1357/2835");
        check(Float.floatToIntBits(bronze.getDensity()) == Float.floatToIntBits(8.54175F), "Bronze density must retain original float bits");
        check(bronze.getCompositionDivider() == 4, "Bronze divider is the dimensionless original ratio4, while components are stored in U");
        check(bronze.getCompositionComponents().size() == 2
                        && bronze.getCompositionComponents().get(0).material() == copper
                        && bronze.getCompositionComponents().get(0).amount() == 3 * GTValues.U
                        && bronze.getCompositionComponents().get(1).material() == tin
                        && bronze.getCompositionComponents().get(1).amount() == GTValues.U,
                "Original ordered 3U Cu+1U Sn composition must remain");
        check(MaterialPrefix.ingot.getMaterialWeight() == 648648000L
                        && MaterialPrefix.nugget.getMaterialWeight() == 72072000L
                        && MaterialPrefix.dustDiv72.getMaterialWeight() == 9009000L,
                "Original item form weights must retain exact GT6 fractions");
        var rawCopper = com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.materialItem(copper, MaterialPrefix.oreRaw);
        var rawTin = com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.materialItem(tin, MaterialPrefix.oreRaw);
        check(rawCopper.material == copper && rawCopper.amount == GTValues.U
                && rawTin.material == tin && rawTin.amount == GTValues.U,
                "Original smeltery raw Cu/Sn each admit one U, separate from oreRaw shell weight two U");
        var copperPreview = com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.smeltingPreview(copper, MaterialPrefix.oreRaw);
        var tinPreview = com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.smeltingPreview(tin, MaterialPrefix.oreRaw);
        check(copperPreview != null && copperPreview.material()==copper && copperPreview.amount()==GTValues.U && copperPreview.temperatureK()==1357,
                "Raw copper display agrees with actual one-ingot payload at 1357K");
        check(tinPreview != null && tinPreview.material()==tin && tinPreview.amount()==GTValues.U && tinPreview.temperatureK()==505,
                "Raw tin display agrees with actual one-ingot payload at 505K");
        check(com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.ore(copper,9).amount==9*GTValues.U,
                "Raw copper block is nine actual raw charges, not eighteen melted ingots");
        var rawCharges = stacks(copper, rawCopper.amount*3, tin, rawTin.amount);
        com.gregtech.gregtech.api.machine.crucible.CrucibleProcess.process(rawCharges,1356,293,true,true);
        check(amountOf(rawCharges,bronze)==0, "Actual raw Cu/Sn charges cannot alloy before 1357K");
        com.gregtech.gregtech.api.machine.crucible.CrucibleProcess.process(rawCharges,1357,1356,false,true);
        check(rawCharges.size()==1 && amountOf(rawCharges,bronze)==4*GTValues.U,
                "Three admitted raw copper and one tin charge form exactly four U bronze");
        check(GregTechConstants.C == 273 && GregTechConstants.DEF_ENV_TEMP == 293 && GregTechConstants.L == 144,
                "Integer Kelvin offset and external litre boundary must stay distinct from U");
    }

    private static void validateAmountsAndReactions() {
        check(CrucibleReactions.recipes().size() == 42 && CrucibleReactions.allRecipes().size() == 173,
                "All explicit and derived original reactions must remain");
        var content = stacks(Materials.Copper, 3 * GTValues.U + GTValues.U2, Materials.Tin, GTValues.U);
        long originalAmount = CrucibleMaterialStack.total(content);
        check(!CrucibleReactions.react(content, 1356), "Copper/tin below bronze melting point must not react");
        check(CrucibleMaterialStack.total(content) == originalAmount, "Rejected cold reaction must not consume anything");
        check(CrucibleReactions.react(content, 1357), "Copper/tin must react at the original bronze melting point");
        check(amountOf(content, Materials.Bronze) == 4 * GTValues.U && amountOf(content, Materials.Copper) == GTValues.U2
                        && amountOf(content, Materials.Tin) == 0, "3U copper and 1U tin must yield4U bronze and retain halfU excess copper");
        check(CrucibleMaterialStack.total(content) == originalAmount, "Copper/tin reaction must conserve material units");
        var annealed = stacks(Materials.AnnealedCopper, 3 * GTValues.U, Materials.Tin, GTValues.U);
        check(Materials.AnnealedCopper.getMeltingPoint() == 2800 && CrucibleReactions.react(annealed, 1357),
                "Original explicit reaction permits the one still-solid annealed copper ingredient");
        check(annealed.size() == 1 && amountOf(annealed, Materials.Bronze) == 4 * GTValues.U,
                "Explicit annealed copper reaction must retain yield4");
        var flux = stacks(GT6Materials.Ores.Fe2O3, 5 * GTValues.U, Materials.Carbon, GTValues.U);
        CrucibleMaterialStack.of(GT6Materials.Ores.CaCO3, GTValues.U).addToList(flux);
        check(!CrucibleReactions.react(flux, 300), "Original cold hematite reduction is rejected");
        check(CrucibleReactions.react(flux, GT6Materials.Elements.Fe.getMeltingPoint()), "Original hematite reduction must run");
        check(flux.size() == 1 && amountOf(flux, GT6Materials.Elements.Fe) == 2 * GTValues.U,
                "Original reduction consumes carbon/flux without adding them to iron yield");
        var noFlux = stacks(GT6Materials.Ores.Fe2O3, 5 * GTValues.U, Materials.Carbon, GTValues.U);
        check(!CrucibleReactions.react(noFlux, GT6Materials.Elements.Fe.getMeltingPoint()), "Original hematite reduction requires calcite");
        var merged = stacks(Materials.Copper, GTValues.U2, GTMaterialRegistry.get("Cu"), GTValues.U2);
        CrucibleMaterialStack.consolidate(merged);
        check(merged.size() == 1 && merged.get(0).material == Materials.Copper && merged.get(0).amount == GTValues.U,
                "Canonical and alias stacks must consolidate without losing units");
        var copy = merged.get(0).copy();
        copy.amount = 0;
        check(merged.get(0).amount == GTValues.U && copy.material == merged.get(0).material,
                "Copies retain material identity while isolating mutable amount");
    }

    private static ArrayList<CrucibleMaterialStack> stacks(GTMaterial a, long amountA, GTMaterial b, long amountB) {
        return new ArrayList<>(List.of(CrucibleMaterialStack.of(a, amountA), CrucibleMaterialStack.of(b, amountB)));
    }
    private static long amountOf(List<CrucibleMaterialStack> contents, GTMaterial material) {
        return contents.stream().filter(s -> s.material == material).mapToLong(s -> s.amount).sum();
    }
    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        assertions++;
        try { action.run(); } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(message, failure);
        }
        throw new AssertionError(message);
    }
}
