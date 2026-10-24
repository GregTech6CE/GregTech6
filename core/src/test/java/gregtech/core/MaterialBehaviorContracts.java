package gregtech.core;

import gregtech.api.machine.crucible.CrucibleMaterialStack;
import gregtech.api.machine.crucible.CrucibleReactions;
import gregtech.api.material.*;
import gregtech.api.mod.ModData;
import gregtech.api.prefix.PrefixRegistry;
import gregtech.content.material.Materials;
import gregtech.data.GregTechConstants;
import gregtech.data.MaterialPrefix;
import gregtech.data.ModReferences;
import gregtech.data.generated.GT6Materials;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    // dirtyGravel/crystal source metadata only; 139681 prior observations per stage retained:
    // docs/integration/verification/external-thermal-material-differential-20261004.json.
    // dustPure/refined metadata plus source dustImpure weight correction; full delta audited:
    // docs/integration/verification/dust-listeners-material-differential-20261004.json.
    // Source UUM flags and Mercury LIQUID restoration; complete isolated delta:
    // docs/integration/verification/ci-material-differential-20261006.json.
    // Ten nongenerating native pipe associations; complete isolated delta:
    // docs/integration/verification/transport-material-differential-20261006.json.
    // 2026-10-18 namespace migration compared with HEAD 1e1069ab9: all 155935 rows
    // per stage remain. Only 1160 translation keys, 53 own-mod IDs/prefixes and
    // four external GT5U presence flags change; every physical/form field is retained.
    // docs/integration/verification/gt6ce-material-differential-20261018.json.
    // Exact identity normalization reproduces the prior definitions fingerprint
    // 9c743d9d39c4e41d11279f99d32b173208b40456ee4884a00a12853dd4468146
    // and post-init fingerprint 149d06616708c6871e2b0622fad173acb56f801406875b03a277eb611a0f1efe.
    // Restore MT.stoneelec's original STONE texture on the three granite materials only.
    // Normalizing just those fields reproduces both prior full graph fingerprints:
    // docs/integration/verification/stone-rock-material-differential-20261019.json.
    // Forty reviewed numeric qual(long) rows restore source quality/minimum durability only.
    // Complete 155935-row snapshots at both stages normalize exactly to the previous fingerprints;
    // every form/alias/prefix and all 56085 core material item IDs remain unchanged.
    // Evidence: docs/integration/verification/material-quality-differential-20261024.json.
    private static final String DEFINITIONS_SHA256 = "9dd543e019f6352f64023be930ba999d763beeaec1ef2e1b7cb70bfd9eb6d20e";
    /** MaterialItemDefinitions.all() before addon materials existed, 56085 rows, zero suffixes. */
    private static final String ITEM_ID_SHA256 = "c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef";
    private static final String POST_INIT_SHA256 = "bb41a81162be04fc7e7a64f3f7be0dc224551ea8ebfb71c80fffad3e39fbbfac";
    private static int assertions;
    private MaterialBehaviorContracts() {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        checkMobDropSourceBoundaries();
        expectThrows(IllegalStateException.class, () -> new ModData("unbound", "Unbound"),
                "The platform must bind presence before creating metadata");
        check(ModData.MODS.isEmpty(), "Failed unbound construction must not publish metadata");
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.DEFINITIONS,
                "Initial registration phase must remain DEFINITIONS");
        // Same domain holder order as Loader_Data, excluding its game-only tables.
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        gregtech.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        for(var type:gregtech.api.tool.ToolDefinition.values())
            check(gregtech.content.tool.OriginalToolFlags.of(type.name())!=null,"Every native Nexus tool has its source classification: "+type);
        for(var tool:gregtech.content.tool.ElectricToolCatalog.ALL)
            check(gregtech.content.tool.OriginalToolFlags.of(tool.original())!=null,"Every powered Nexus tool has its source classification: "+tool.id());
        check(gregtech.content.tool.MaterialToolEnchantments.ammunition(GTMaterialRegistry.get("DarkMatter")).getOrDefault("looting",0)==6,"Source ammunition uses Ammo looting, twice its Weapons level");
        check(gregtech.data.MaterialPrefix.stickLong.isValidFor(GTMaterialRegistry.get("Obsidian")), "source miniature Nether recipe has its Obsidian long rod form");
        check(gregtech.data.MaterialPrefix.stickLong.isValidFor(GTMaterialRegistry.get("Endstone")), "source miniature End recipe has its Endstone long rod form");

        check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY,
                "The complete directory must finish linking");
        check(GTMaterialRegistry.allMaterials().size() == 1160, "All 1160 original material objects must remain");
        check(MaterialCatalogSnapshot.aliases().size() == 1523, "All 1523 original name entries must remain");
        check(PrefixRegistry.all().size() == 132, "122 prior prefixes plus ten source native pipe associations must remain");
        check(DEFINITIONS_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Full definitions/aliases/forms must match the Community Edition snapshot");
        GTMaterialRegistry.init();
        check(DEFINITIONS_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Repeated init must not mutate or duplicate definitions");
        GTMaterialRegistry.postInit();
        check(POST_INIT_SHA256.equals(MaterialCatalogSnapshot.sha256()), "Full domain post-init must match the Community Edition snapshot");
        var sourceGrinding = gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        var wrought = GTMaterialRegistry.get("WroughtIron");
        var iron = GTMaterialRegistry.get("Iron");
        check(gregtech.content.tool.OriginalToolMaterials.inFamily(iron, "AnyIronOrSteel"),
                "Concrete Iron recognizes its source parent group, not the group's member list");
        check(MaterialPrefix.crystal.isValidFor(GTMaterialRegistry.get("Diamond")), "source OP.crystal accepts a gem material");
        check(!MaterialPrefix.crystal.isValidFor(iron), "source OP.crystal does not become an ordinary ore form");
        check(MaterialPrefix.dirtyGravel.isValidFor(iron), "source OP.dirtyGravel accepts an ore material");
        check(gregtech.content.recipe.FurnaceSmeltingRules.allows(GTMaterialRegistry.get("Copper")), "source Copper is furnace-capable");
        check(!gregtech.content.recipe.FurnaceSmeltingRules.allows(iron), "source Iron requires the crucible and is not furnace-capable");
        check(gregtech.content.recipe.FurnaceSmeltingRules.amount(GTValues.U, GTMaterialRegistry.get("Cassiterite").getTargetSmeltingAmount()) == 486_486_000L,
                "full Cassiterite directory keeps source three-quarter Tin yield");
        check(gregtech.content.recipe.FurnaceSmeltingRules.amount(GTValues.U, GTMaterialRegistry.get("Malachite").getTargetSmeltingAmount()) == 108_108_000L,
                "full Malachite directory keeps source one-sixth Copper yield");
        check(sourceGrinding.get(22).allows(wrought), "source impure dust has a soft Shredder branch");
        check(sourceGrinding.get(23).allows(iron), "source foreign pure dust processing is allowed when an actual input exists");
        check(!sourceGrinding.get(24).allows(GTMaterialRegistry.get("Bedrock")), "source refined Bedrock dust is excluded");
        check(!MaterialPrefix.dustPure.isValidFor(iron), "source Iron has ORES but no DIRTY_DUSTS generation flag");
        check(!MaterialPrefix.dustRefined.isValidFor(GTMaterialRegistry.get("Obsidian"))
                && !MaterialPrefix.dustRefined.isValidFor(GTMaterialRegistry.get("Glowstone")), "source disables refined Obsidian and Glowstone item generation");
        check(gregtech.content.recipe.MaterialWashingRules.ROWS.get(1).outputMaterial(wrought) == wrought,
                "source washing retains WroughtIron instead of using its Iron pulver target");
        check(sourceGrinding.get(22).outputMaterial(wrought) == iron, "source impure-dust shredding uses the Iron pulver target");
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
        validateMaterialBerryBushes();
        validateRecipeMapPresentation();
        validateIdentityGraph();
        validateMetadata();
        check(gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("Knightmetal"), "Steel"), "ANY.Steel accepts Knightmetal screws and rings");
        check(gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("MeteoricSteel"), "Steel"), "ANY.Steel accepts MeteoricSteel screws and rings");
        check(gregtech.content.tool.OriginalToolMaterials.acceptsHandle(GTMaterialRegistry.get("EnderAmethyst"), GTMaterialRegistry.get("Steel")), "ANY.Iron accepts Steel rods for EnderAmethyst tools");
        check(!gregtech.content.tool.OriginalToolMaterials.acceptsHandle(GTMaterialRegistry.get("EnderAmethyst"), GTMaterialRegistry.get("Wood")), "EnderAmethyst does not fall back to a default wooden handle");
        check(gregtech.content.tool.OriginalToolMaterials.inFamily(GTMaterialRegistry.get("WoodTainted"), "WoodMagical"), "ANY.WoodMagical includes MT.WOODS.Tainted under its registered name");
        validateCopperTinBronze();
        validateAmountsAndReactions();
        validateMaterialItemIds();
        System.out.println("Material behavior contracts passed: " + assertions
                + " assertions; 1160 materials, 1105 positive IDs, 1523 name entries, " + PrefixRegistry.all().size() + " prefixes, "
                + CrucibleReactions.allRecipes().size() + " reactions");
    }

    private static void validateRecipeMapPresentation() throws Exception {
        var maps=new ArrayList<>(gregtech.data.MachineRecipeMapDefinitions.all());
        for(var field:gregtech.data.FuelRecipeMapDefinitions.class.getFields())
            if(field.getType()==gregtech.api.recipe.RecipeMapSpec.class)maps.add((gregtech.api.recipe.RecipeMapSpec)field.get(null));
        var expected=new HashMap<String,String[]>();
        try(var stream=MaterialBehaviorContracts.class.getResourceAsStream("/recipe-map-presentation-source.tsv");
                var reader=new java.io.BufferedReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8))) {
            for(String line; (line=reader.readLine())!=null;)if(!line.startsWith("#")&&!line.isBlank()) {var columns=line.split("\\|");expected.put(columns[0],columns);}
        }
        check(maps.size()==93&&expected.size()==93,"all 85 machine and eight fuel source viewer flags are covered");
        for(var map:maps) {
            var source=expected.remove(map.mNameInternal);check(source!=null,"unique source presentation identity "+map.mNameInternal);
            check(map.mViewerAllowed==Boolean.parseBoolean(source[1])&&map.mShowVoltageAmperage==Boolean.parseBoolean(source[2]),"original NEI flags "+map.mNameInternal);
        }
        check(expected.isEmpty(),"no source presentation map omitted");
        var one=gregtech.api.recipe.RecipeCaptionVisibility.of(1,false,false);
        check(!one.usage()&&!one.tier()&&!one.power(),"source one-unit informational recipes omit voltage/usage");
        var generator=gregtech.api.recipe.RecipeCaptionVisibility.of(-32,false,false);
        check(generator.usage()&&!generator.tier()&&!generator.power(),"source hidden voltage still shows non-unit generation rate");
        var unspecified=gregtech.api.recipe.RecipeCaptionVisibility.of(0,false,true);
        check(!unspecified.usage()&&unspecified.tier()&&!unspecified.power(),"source zero-power tier unspecified only if flag allows it");
    }

    private static void validateMaterialBerryBushes() {
        var variants=gregtech.content.plant.MaterialBerryBushCatalog.variants();
        check(gregtech.content.creative.SourceCreativeCatalog.entry("bush_plant_gt_berry_copper").equals(gregtech.content.creative.SourceCreativeCatalog.entry("bush")), "material bush variants retain the original bush creative category and order");
        check(variants.size()==1034, "all existing plantGtBerry definitions acquire a bush identity");
        check(variants.stream().map(v->v.blockPath()).distinct().count()==variants.size(), "material bushes have no duplicate identities");
        var copper=gregtech.content.plant.MaterialBerryBushCatalog.colours("gt6ce:plant_gt_berry_copper",GTMaterialRegistry.get("Copper").getColor());
        check(copper.bush()==0x009000 && copper.bloom()==0xff9090 && copper.immature()==0x80ff80 && copper.berry()==0xff825a,
                "original MultiTileEntityBush material branch, including MT Copper solid RGB");
        check(gregtech.content.plant.BerryBushCatalog.inventoryColour(copper)==0xff825a,"source material inventory shows solid RGB");
        check(gregtech.content.plant.BerryBushCatalog.inventoryColour(gregtech.content.plant.BerryBushCatalog.byId("blueberry"))==0x6666dd,
                "source food bush inventory shows immature color rather than ripe blue");
        check(gregtech.content.plant.BerryBushCatalog.inventoryColour(gregtech.content.plant.BerryBushCatalog.DEFAULT)==0x44cc44,
                "source cotton bush inventory also shows immature color");
        check(gregtech.content.plant.BerryBushCatalog.worldgenSize()==9,
                "material plants do not expand the source food/cotton world-generation pool");
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
        check(Materials.Copper.getTranslationKey().equals("material.gt6ce.copper")
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
        var rawCopper = gregtech.api.machine.crucible.CrucibleInputRules.materialItem(copper, MaterialPrefix.oreRaw);
        var rawTin = gregtech.api.machine.crucible.CrucibleInputRules.materialItem(tin, MaterialPrefix.oreRaw);
        check(rawCopper.material == copper && rawCopper.amount == GTValues.U
                && rawTin.material == tin && rawTin.amount == GTValues.U,
                "Original smeltery raw Cu/Sn each admit one U, separate from oreRaw shell weight two U");
        var copperPreview = gregtech.api.machine.crucible.CrucibleInputRules.smeltingPreview(copper, MaterialPrefix.oreRaw);
        var tinPreview = gregtech.api.machine.crucible.CrucibleInputRules.smeltingPreview(tin, MaterialPrefix.oreRaw);
        check(copperPreview != null && copperPreview.material()==copper && copperPreview.amount()==GTValues.U && copperPreview.temperatureK()==1357,
                "Raw copper display agrees with actual one-ingot payload at 1357K");
        check(tinPreview != null && tinPreview.material()==tin && tinPreview.amount()==GTValues.U && tinPreview.temperatureK()==505,
                "Raw tin display agrees with actual one-ingot payload at 505K");
        check(gregtech.api.machine.crucible.CrucibleInputRules.ore(copper,9).amount==9*GTValues.U,
                "Raw copper block is nine actual raw charges, not eighteen melted ingots");
        var rawCharges = stacks(copper, rawCopper.amount*3, tin, rawTin.amount);
        gregtech.api.machine.crucible.CrucibleProcess.process(rawCharges,1356,293,true,true);
        check(amountOf(rawCharges,bronze)==0, "Actual raw Cu/Sn charges cannot alloy before 1357K");
        gregtech.api.machine.crucible.CrucibleProcess.process(rawCharges,1357,1356,false,true);
        check(rawCharges.size()==1 && amountOf(rawCharges,bronze)==4*GTValues.U,
                "Three admitted raw copper and one tin charge form exactly four U bronze");
        check(GregTechConstants.C == 273 && GregTechConstants.DEF_ENV_TEMP == 293 && GregTechConstants.L == 144,
                "Integer Kelvin offset and external litre boundary must stay distinct from U");
    }

    private static void validateMaterialItemIds() throws Exception {
        check(ITEM_ID_SHA256.equals(itemIdFingerprint(true)), "built-in material item ids match the golden sample taken before the two-pass change");
        int free = freeMaterialId(1);
        var added = GTMaterialRegistry.createMaterial(free, "Aaaaddon", "Aaaaddon", 0xFFFFFF).put(MaterialProperty.DUST);
        check(!GTMaterialRegistry.isCoreMaterial(added), "a material registered after init is not part of the core directory");
        check(GTMaterialRegistry.isCoreMaterial(Materials.Iron), "Iron remains a core material");
        boolean generated = false;
        for (var definition : MaterialItemDefinitions.all()) {
            if (definition.material() != added) continue;
            check(!definition.hasIdSuffix(), "a non-colliding addon material keeps its base item id: " + definition.itemId());
            generated = true;
        }
        check(generated, "the addon dust material generates at least one item");
        check(ITEM_ID_SHA256.equals(itemIdFingerprint(true)), "adding an addon material does not change any core item id");
        GTMaterialRegistry.createMaterial(freeMaterialId(free + 1), "iron", "iron", 0xFFFFFF).put(MaterialProperty.DUST);
        expectThrows(IllegalStateException.class, MaterialItemDefinitions::all,
                "an addon material whose lowercased name takes a core item id is rejected instead of renaming the core item");
    }

    private static int freeMaterialId(int start) {
        for (int id = start; id < GTMaterialRegistry.MAX_MATERIALS; id++) {
            if (!GTMaterialRegistry.get(id).isValid()) return id;
        }
        throw new AssertionError("no free material id");
    }

    private static String itemIdFingerprint(boolean coreOnly) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        for (var definition : MaterialItemDefinitions.all()) {
            if (coreOnly && !GTMaterialRegistry.isCoreMaterial(definition.material())) continue;
            String line = definition.itemId() + "\t" + definition.baseItemId() + "\t"
                    + definition.prefix().getName() + "\t" + definition.material().getName() + "\t"
                    + definition.material().getId() + "\n";
            digest.update(line.getBytes(StandardCharsets.UTF_8));
        }
        var text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format("%02x", value));
        return text.toString();
    }

    private static void validateAmountsAndReactions() {
        // MT.java's 91 setAloy/uumAloy/alloySimple declarations, plus 46 explicit
        // rows including purification and air-blown steel. ALLOY chemistry alone
        // does not declare a crucible recipe (notably Steel.uumMcfg).
        // Source and IDs: verification/origin-crucible-compositions-20261004.json.
        check(CrucibleReactions.recipes().size() == 46,
                "Original explicit crucible rows: expected 46, actual " + CrucibleReactions.recipes().size());
        check(CrucibleReactions.allRecipes().size() == 137,
                "46 explicit and 91 declared composition rows: expected 137, actual " + CrucibleReactions.allRecipes().size());
        validateIronSteelReactions();
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

    private static void validateIronSteelReactions() {
        var iron = GT6Materials.Elements.Fe;
        var wrought = GT6Materials.Elements.WroughtIron;
        var steel = GT6Materials.Compounds.Steel;
        var air = GT6Materials.Compounds.Air;
        check(wrought.getMeltingPoint() == 2011 && steel.getMeltingPoint() == 2046,
                "Original wrought/steel reaction thresholds are 2011K/2046K");
        var ironCharge = new ArrayList<>(List.of(CrucibleMaterialStack.of(iron, 3 * GTValues.U)));
        check(!CrucibleReactions.react(ironCharge, 2010) && amountOf(ironCharge, iron) == 3 * GTValues.U,
                "Iron below 2011K cannot purify and must retain its charge");
        check(CrucibleReactions.react(ironCharge, 2011) && ironCharge.size() == 1
                        && amountOf(ironCharge, wrought) == 3 * GTValues.U,
                "Single-material iron charge purifies to wrought iron with source yield one");
        check(!CrucibleReactions.react(ironCharge, 2046) && amountOf(ironCharge, steel) == 0,
                "Steel chemistry must not fabricate an airless wrought-iron recipe");
        var coldSteel = stacks(wrought, 3 * GTValues.U, air, GTValues.U);
        check(!CrucibleReactions.react(coldSteel, 2045)
                        && amountOf(coldSteel, wrought) == 3 * GTValues.U && amountOf(coldSteel, air) == GTValues.U,
                "Air-blown steel below 2046K cannot consume either reagent");
        // Exercise the actual phase order: air must react before gas removal.
        var steelCharge = stacks(wrought, 3 * GTValues.U, air, GTValues.U);
        gregtech.api.machine.crucible.CrucibleProcess.process(steelCharge, 2046, 2045, true, true);
        check(steelCharge.size() == 2 && amountOf(steelCharge, steel) == GTValues.U
                        && amountOf(steelCharge, wrought) == 2 * GTValues.U && amountOf(steelCharge, air) == 0,
                "One U air converts one U wrought iron to steel and retains two U excess wrought iron");
        var meteoric = stacks(GT6Materials.Compounds.MeteoricIron, GTValues.U, air, GTValues.U);
        check(CrucibleReactions.react(meteoric, GT6Materials.Compounds.MeteoricSteel.getMeltingPoint())
                        && meteoric.size() == 1 && amountOf(meteoric, GT6Materials.Compounds.MeteoricSteel) == GTValues.U,
                "Meteoric steel also consumes air with source yield one");
        var copper = new ArrayList<>(List.of(CrucibleMaterialStack.of(Materials.Copper, GTValues.U)));
        check(!CrucibleReactions.react(copper, 2799) && amountOf(copper, Materials.Copper) == GTValues.U,
                "Annealed copper purification cannot bypass the original 2800K threshold");
        check(CrucibleReactions.react(copper, 2800) && copper.size() == 1
                        && amountOf(copper, Materials.AnnealedCopper) == GTValues.U,
                "Copper anneals at 2800K with source yield one");
    }

    private static ArrayList<CrucibleMaterialStack> stacks(GTMaterial a, long amountA, GTMaterial b, long amountB) {
        return new ArrayList<>(List.of(CrucibleMaterialStack.of(a, amountA), CrucibleMaterialStack.of(b, amountB)));
    }
    private static long amountOf(List<CrucibleMaterialStack> contents, GTMaterial material) {
        return contents.stream().filter(s -> s.material == material).mapToLong(s -> s.amount).sum();
    }
    private static void checkMobDropSourceBoundaries() {
        check(gregtech.content.loot.MobDropRules.rareBound(0) == 144
                && gregtech.content.loot.MobDropRules.rareBound(3) == 135
                && gregtech.content.loot.MobDropRules.rareBound(100) == 36,
                "Original rare mob drops clamp at 1/36 and include Looting");
        check(gregtech.content.loot.MobDropRules.partChance(0, 100) == 26
                && gregtech.content.loot.MobDropRules.partChance(0, 200) == 26,
                "Original <=25 comparison is 26 outcomes, including undead /200 parts");
        check(gregtech.content.loot.MobDropRules.partChance(100, 100) == 100,
                "Inclusive part probability saturates at 100 percent");
        check(gregtech.content.loot.MobDropRules.COOKIES.size() == 2
                && !gregtech.content.loot.MobDropRules.COOKIES.contains("chocolate_raisin_cookie"),
                "UT.Code.select ignores its fallback when choices exist");
        check(gregtech.content.loot.MobDropRules.SKELETON_RARE.size() == 7,
                "Original skeleton choices retain 3:3:1 weighting");
        var bacon = gregtech.content.loot.MobDropRules.meatReplacement(
                gregtech.content.loot.MobDropRules.Meat.PORK, 32, false, 1, bound -> 2);
        check(bacon.count() == 64, "Original bacon multiplication is capped at 64 items");
        check(gregtech.content.loot.MobDropRules.meatReplacement(
                gregtech.content.loot.MobDropRules.Meat.BEEF, 4, false, 2, bound -> 0) == null,
                "Third beef stack keeps the original meat and its metadata");
        check(gregtech.content.loot.MobDropRules.meatReplacement(
                gregtech.content.loot.MobDropRules.Meat.HORSE, 1, true, 1, bound -> 0) == null,
                "Every second horse meat stack remains horse meat");
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
