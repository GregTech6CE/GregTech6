package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.mod.GregTechIdentity;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One authoritative item definition stream for both platforms, extracted from saltnya Loader_Items.
 * Call after material linking/roles and before platform RegisterEvents; does not register Items.
 * Source hashes and unresolved original authorization are recorded in material-item-extraction.json.
 */
public final class MaterialItemDefinitions {
    private MaterialItemDefinitions() {}

    public record Definition(String namespace, String itemId, String baseItemId, MaterialPrefix prefix, GTMaterial material) {
        public Definition(String itemId, String baseItemId, MaterialPrefix prefix, GTMaterial material) {
            this(GregTechIdentity.MOD_ID, itemId, baseItemId, prefix, material);
        }

        public boolean hasIdSuffix() { return !itemId.equals(baseItemId); }
    }

    /** Original explicit prefix order, rather than every PrefixRegistry entry or an unordered set. */
    public static List<MaterialPrefix> candidatePrefixes() {
        PrefixRegistry.ensurePrefixesLoaded();
        return List.of(
                MaterialPrefix.dust,
                MaterialPrefix.dustSmall,
                MaterialPrefix.dustTiny,
                MaterialPrefix.dustDiv72,
                MaterialPrefix.dustImpure,
                MaterialPrefix.unit,
                MaterialPrefix.crushed,
                MaterialPrefix.crushedTiny,
                MaterialPrefix.crushedPurified,
                MaterialPrefix.crushedPurifiedTiny,
                MaterialPrefix.crushedCentrifuged,
                MaterialPrefix.crushedCentrifugedTiny,
                MaterialPrefix.rockGt,
                MaterialPrefix.gemChipped,
                MaterialPrefix.gemFlawed,
                MaterialPrefix.gem,
                MaterialPrefix.gemFlawless,
                MaterialPrefix.gemExquisite,
                MaterialPrefix.gemLegendary,
                MaterialPrefix.bouleGt,
                MaterialPrefix.nugget,
                MaterialPrefix.chunkGt,
                MaterialPrefix.billet,
                MaterialPrefix.ingot,
                MaterialPrefix.ingotHot,
                MaterialPrefix.ingotDouble,
                MaterialPrefix.ingotTriple,
                MaterialPrefix.ingotQuadruple,
                MaterialPrefix.ingotQuintuple,
                MaterialPrefix.plateGemTiny,
                MaterialPrefix.plateGem,
                MaterialPrefix.plateTiny,
                MaterialPrefix.plate,
                MaterialPrefix.plateDouble,
                MaterialPrefix.plateTriple,
                MaterialPrefix.plateQuadruple,
                MaterialPrefix.plateQuintuple,
                MaterialPrefix.plateDense,
                MaterialPrefix.plateCurved,
                MaterialPrefix.foil,
                MaterialPrefix.scrapGt,
                MaterialPrefix.oreRaw,
                MaterialPrefix.stick,
                MaterialPrefix.stickLong,
                MaterialPrefix.bolt,
                MaterialPrefix.screw,
                MaterialPrefix.gearGt,
                MaterialPrefix.gearGtSmall,
                MaterialPrefix.ring,
                MaterialPrefix.chain,
                MaterialPrefix.spring,
                MaterialPrefix.springSmall,
                MaterialPrefix.rotor,
                MaterialPrefix.lens,
                MaterialPrefix.round,
                MaterialPrefix.itemCasing,
                MaterialPrefix.wireFine,
                MaterialPrefix.minecartWheels,
                MaterialPrefix.railGt,
                MaterialPrefix.plantGtBerry,
                MaterialPrefix.plantGtBlossom,
                MaterialPrefix.plantGtFiber,
                MaterialPrefix.plantGtTwig,
                MaterialPrefix.plantGtWart,
                MaterialPrefix.glasstube,
                MaterialPrefix.coin,
                MaterialPrefix.toolHeadSword,
                MaterialPrefix.toolHeadRawSword,
                MaterialPrefix.toolHeadPickaxe,
                MaterialPrefix.toolHeadRawPickaxe,
                MaterialPrefix.toolHeadPickaxeGem,
                MaterialPrefix.toolHeadConstructionPickaxe,
                MaterialPrefix.toolHeadBuilderwand,
                MaterialPrefix.toolHeadShovel,
                MaterialPrefix.toolHeadRawShovel,
                MaterialPrefix.toolHeadSpade,
                MaterialPrefix.toolHeadRawSpade,
                MaterialPrefix.toolHeadAxe,
                MaterialPrefix.toolHeadRawAxe,
                MaterialPrefix.toolHeadAxeDouble,
                MaterialPrefix.toolHeadRawAxeDouble,
                MaterialPrefix.toolHeadHoe,
                MaterialPrefix.toolHeadRawHoe,
                MaterialPrefix.toolHeadHammer,
                MaterialPrefix.toolHeadFile,
                MaterialPrefix.toolHeadChisel,
                MaterialPrefix.toolHeadRawChisel,
                MaterialPrefix.toolHeadSaw,
                MaterialPrefix.toolHeadRawSaw,
                MaterialPrefix.toolHeadDrill,
                MaterialPrefix.toolHeadChainsaw,
                MaterialPrefix.toolHeadWrench,
                MaterialPrefix.toolHeadScrewdriver,
                MaterialPrefix.toolHeadUniversalSpade,
                MaterialPrefix.toolHeadRawUniversalSpade,
                MaterialPrefix.toolHeadSense,
                MaterialPrefix.toolHeadRawSense,
                MaterialPrefix.toolHeadPlow,
                MaterialPrefix.toolHeadRawPlow,
                MaterialPrefix.toolHeadBuzzSaw,
                MaterialPrefix.toolHeadArrow,
                MaterialPrefix.toolHeadRawArrow,
                MaterialPrefix.arrowGtWood,
                MaterialPrefix.arrowGtPlastic,
                MaterialPrefix.bulletGtSmall,
                MaterialPrefix.bulletGtMedium,
                MaterialPrefix.bulletGtLarge
        );
    }

    /**
     * Preserves prefix order, sorted material order, filtering and the treated-wood exception.
     * Core materials claim ids in the {@code gt6ce} namespace before any addon material, so the
     * built-in directory keeps its ids. Addon materials claim the namespace of their owning addon
     * ({@link AddonMaterials#namespaceOf}); a collision inside that namespace fails instead of
     * receiving the numeric suffix.
     */
    public static List<Definition> all() {
        Set<String> seenIds = new HashSet<>();
        Map<GTMaterial, Map<MaterialPrefix, String>> claimed = new HashMap<>();
        List<GTMaterial> core = new ArrayList<>();
        List<GTMaterial> addon = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.resolve() != material) continue;
            (GTMaterialRegistry.isCoreMaterial(material) ? core : addon).add(material);
        }
        claim(core, true, seenIds, claimed);
        claim(addon, false, seenIds, claimed);
        for (AddonMaterials.AcceptedForm form : AddonMaterials.acceptedForms()) {
            MaterialPrefix prefix = PrefixRegistry.byName(form.prefixName());
            GTMaterial material = GTMaterialRegistry.get(form.materialName()).resolve();
            if (prefix == null || !material.isValid() || generates(prefix, material)) continue;
            if (!AddonMaterials.explicitPlateOrFoil(prefix, material)) continue;
            claimPair(prefix, material, false, seenIds, claimed);
        }
        for (MaterialPrefix prefix : PrefixRegistry.addonPrefixes()) {
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                if (!generates(prefix, material)) continue;
                claimPair(prefix, material, false, seenIds, claimed, prefix.itemNamespace());
            }
        }
        List<Definition> definitions = new ArrayList<>();
        List<MaterialPrefix> automatic = candidatePrefixes();
        Set<MaterialPrefix> automaticPrefixes = new HashSet<>(automatic);
        for (MaterialPrefix prefix : automatic) {
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                if (!generates(prefix, material)) continue;
                definitions.add(new Definition(AddonMaterials.namespaceOf(material),
                        claimed.get(material).get(prefix), prefix.getItemId(material), prefix, material));
            }
        }
        for (AddonMaterials.AcceptedForm form : AddonMaterials.acceptedForms()) {
            MaterialPrefix prefix = PrefixRegistry.byName(form.prefixName());
            GTMaterial material = GTMaterialRegistry.get(form.materialName()).resolve();
            if (prefix == null || !material.isValid() || generates(prefix, material)) continue;
            if (!AddonMaterials.explicitPlateOrFoil(prefix, material)) continue;
            definitions.add(new Definition(AddonMaterials.namespaceOf(material),
                    claimed.get(material).get(prefix), prefix.getItemId(material), prefix, material));
        }
        for (MaterialPrefix prefix : PrefixRegistry.addonPrefixes()) {
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                if (!generates(prefix, material)) continue;
                definitions.add(new Definition(prefix.itemNamespace(), claimed.get(material).get(prefix),
                        prefix.getItemId(material), prefix, material));
            }
        }
        rejectUngeneratedForms(automaticPrefixes);
        return List.copyOf(definitions);
    }

    /**
     * An addon form may only name a prefix this list already generates. A pair that is already
     * emitted is left as that one item. A prefix outside the list is rejected instead of creating
     * an item the client has no icon or translation for.
     */
    private static void rejectUngeneratedForms(Set<MaterialPrefix> automaticPrefixes) {
        for (AddonMaterials.AcceptedForm form : AddonMaterials.acceptedForms()) {
            MaterialPrefix prefix = PrefixRegistry.byName(form.prefixName());
            GTMaterial material = GTMaterialRegistry.get(form.materialName()).resolve();
            if (prefix == null || !material.isValid())
                throw new IllegalStateException("Addon '" + form.addonId() + "' form '" + form.prefixName()
                        + " / " + form.materialName() + "' did not survive material startup");
            if (!automaticPrefixes.contains(prefix))
                throw new IllegalStateException("Addon '" + form.addonId() + "' form '" + prefix.getName()
                        + " / " + material.getName() + "' is not a generated form");
            if (!generates(prefix, material) && !AddonMaterials.explicitPlateOrFoil(prefix, material))
                throw new IllegalStateException("Addon '" + form.addonId() + "' form '" + prefix.getName()
                        + " / " + material.getName() + "' is not generated for that material");
        }
    }

    private static void claim(List<GTMaterial> materials, boolean core, Set<String> seenIds, Map<GTMaterial, Map<MaterialPrefix, String>> claimed) {
        for (MaterialPrefix prefix : candidatePrefixes()) {
            for (GTMaterial material : materials) {
                if (!generates(prefix, material)) continue;
                claimPair(prefix, material, core, seenIds, claimed);
            }
        }
    }

    private static void claimPair(MaterialPrefix prefix, GTMaterial material, boolean core, Set<String> seenIds,
                                  Map<GTMaterial, Map<MaterialPrefix, String>> claimed) {
        claimPair(prefix, material, core, seenIds, claimed, AddonMaterials.namespaceOf(material));
    }

    private static void claimPair(MaterialPrefix prefix, GTMaterial material, boolean core, Set<String> seenIds,
                                  Map<GTMaterial, Map<MaterialPrefix, String>> claimed, String namespace) {
        String base = prefix.getItemId(material);
        String itemId = base;
        if (!seenIds.add(namespace + ":" + base)) {
            if (!core) {
                throw new IllegalStateException("Addon material " + material.getName() + " (id " + material.getId()
                        + ") would take item id '" + namespace + ":" + base
                        + "', which is already assigned. Core item ids stay fixed; rename the material.");
            }
            itemId = base + "_" + material.getId();
            if (!seenIds.add(namespace + ":" + itemId)) {
                throw new IllegalStateException("Could not allocate unique item id for " + prefix.getName() + " / " + material.getName());
            }
        }
        claimed.computeIfAbsent(material, ignored -> new HashMap<>()).put(prefix, itemId);
    }

    /** Prefix validation retains the original five EMPTY ammunition components. */
    private static boolean generates(MaterialPrefix prefix, GTMaterial material) {
        if (material.resolve() != material) return false;
        if (!prefix.isValidFor(material)) return false;
        // Original Loader_Woods binds this plate to the placeable treated plank block.
        return prefix != MaterialPrefix.plate || !material.getName().equals("WoodTreated");
    }
}
