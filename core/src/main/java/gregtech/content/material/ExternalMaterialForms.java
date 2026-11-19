package gregtech.content.material;

import gregtech.content.data.MaterialPrefix;
import java.util.List;

/** Material identities admitted from external item tags. */
public final class ExternalMaterialForms {
    private ExternalMaterialForms() {}

    public static final MaterialPrefix WASHING_INPUT = MaterialPrefix.crushed;

    /** Foreign ore blocks are tags rather than a GT item prefix, but still participate in hand conversion. */
    public static final String TAGGED_ORE_FORM = "ore";
    /** Common groups without a one-to-one {@link MaterialPrefix} item identity. */
    public static final String TAGGED_STORAGE_BLOCK_GROUP = "storage_blocks";

    public static final String TAGGED_CABLE_GROUP = "cables";
    /** Explicit external metadata forms; this list never registers GT-owned items. */
    public static final List<MaterialPrefix> EXTERNAL_FORMS = List.of(
            MaterialPrefix.rawOreChunk,
            MaterialPrefix.chunk,
            MaterialPrefix.rubble,
            MaterialPrefix.pebbles,
            MaterialPrefix.clump,
            MaterialPrefix.reduced,
            MaterialPrefix.crystalline,
            MaterialPrefix.cleanGravel,
            MaterialPrefix.cluster,
            MaterialPrefix.dirtyGravel,
            MaterialPrefix.crystal);
    /** Include tagged crushed ore so world listeners also recognize foreign items. */
    public static final List<MaterialPrefix> COMPOSITION_FORMS = java.util.stream.Stream.concat(
                    EXTERNAL_FORMS.stream(), java.util.stream.Stream.of(WASHING_INPUT))
            .distinct()
            .toList();
    /** Common item forms other mods publish under forge/c tags with quantity-stable GT6 weights. */
    public static final List<MaterialPrefix> TAGGED_MATERIAL_FORMS = List.of(
            MaterialPrefix.ingot,
            MaterialPrefix.nugget,
            MaterialPrefix.gem,
            MaterialPrefix.dust,
            MaterialPrefix.dustSmall,
            MaterialPrefix.dustTiny,
            MaterialPrefix.plate,
            MaterialPrefix.stick,
            MaterialPrefix.gearGt,
            MaterialPrefix.gearGtSmall,
            MaterialPrefix.ring,
            MaterialPrefix.bolt,
            MaterialPrefix.screw,
            MaterialPrefix.foil);

    public static boolean isCompositionForm(String form) {
        if (TAGGED_ORE_FORM.equals(form)) return true;
        for (var prefix : COMPOSITION_FORMS) if (prefix.getName().equals(form)) return true;
        for (var prefix : TAGGED_MATERIAL_FORMS) if (prefix.getName().equals(form)) return true;
        return false;
    }
}
