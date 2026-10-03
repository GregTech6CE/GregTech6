package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One authoritative item definition stream for both platforms, extracted from saltnya Loader_Items.
 * Call after material linking/roles and before platform RegisterEvents; does not register Items.
 * Source hashes and unresolved original authorization are recorded in material-item-extraction.json.
 */
public final class MaterialItemDefinitions {
    private MaterialItemDefinitions() {}

    public record Definition(String itemId, String baseItemId, MaterialPrefix prefix, GTMaterial material) {
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

    /** Preserves prefix order, sorted material order, filtering, treated-wood exception and ID fallback. */
    public static List<Definition> all() {
        Set<String> seenIds = new HashSet<>();
        List<Definition> definitions = new ArrayList<>();
        for (MaterialPrefix prefix : candidatePrefixes()) {
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                // Prefix validation retains the original five EMPTY ammunition components.
                if (material.resolve() != material) continue;
                if (!prefix.isValidFor(material)) continue;
                // Original Loader_Woods binds this plate to the placeable treated plank block.
                if (prefix == MaterialPrefix.plate && material.getName().equals("WoodTreated")) continue;
                String base = prefix.getItemId(material);
                String itemId = uniqueItemId(prefix, material, base, seenIds);
                definitions.add(new Definition(itemId, base, prefix, material));
            }
        }
        return List.copyOf(definitions);
    }

    private static String uniqueItemId(MaterialPrefix prefix, GTMaterial material, String base, Set<String> seenIds) {
        if (seenIds.add(base)) return base;
        String withId = base + "_" + material.getId();
        if (seenIds.add(withId)) return withId;
        throw new IllegalStateException("Could not allocate unique item id for " + prefix.getName() + " / " + material.getName());
    }
}
