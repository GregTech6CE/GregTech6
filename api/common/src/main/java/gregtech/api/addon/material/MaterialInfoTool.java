/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;
import gregtech.api.addon.InstalledService;
import gregtech.api.material.MaterialToolProfile;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;

/**
 * Lifecycle-bound registration and read entry for material auxiliary data.
 *
 * <p>Material identity and intrinsic physical facts stay in the material registry. Tool profiles,
 * compositions, forms, tags and process rules are stored in one auxiliary state per material and
 * published as a frozen snapshot. Convenience accessors are compatibility projections.</p>
 */
public final class MaterialInfoTool {
    private static final InstalledService<Host> HOST = new InstalledService<>();

    private MaterialInfoTool() {}

    @ApiStatus.Internal
    public static void bindHost(Host host) {
        HOST.bind(host, "Material info host is missing", "Material info host is already installed");
    }

    /**
     * Compatibility entry for opening a typed registration session by ID.
     *
     * @deprecated Override {@link AddonMaterialContributor#materialInfo(Registrar)} and use the
     *     lifecycle-bound registrar supplied by the host.
     */
    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Registrar forAddon(String addonId) {
        return host().registrar(AddonNames.requireAddonId(addonId));
    }

    /**
     * Frozen auxiliary tool profile for a known material. Missing, not-ready and failed-index reads
     * return empty instead of inventing a zero profile.
     */
    public static Optional<MaterialToolProfile> tool(MaterialKey material) {
        Objects.requireNonNull(material, "material");
        return HOST.isBound() ? host().tool(material) : Optional.empty();
    }

    public static Optional<MaterialToolProfile> tool(String materialName) {
        return tool(new MaterialKey(materialName));
    }

    /** Frozen composition, if one was registered. */
    public static Optional<MaterialComposition> composition(MaterialKey material) {
        Objects.requireNonNull(material, "material");
        return HOST.isBound() ? host().composition(material) : Optional.empty();
    }

    /** Empty before the host installs or before the definition snapshot is frozen. */
    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<MaterialToolProfile> frozenTool(MaterialKey material) {
        return tool(material);
    }

    private static Host host() {
        return HOST.get("GregTech material info API is not installed");
    }

    @ApiStatus.Internal
    public interface Host {
        Registrar registrar(String addonId);

        Optional<MaterialToolProfile> tool(MaterialKey material);

        Optional<MaterialComposition> composition(MaterialKey material);
    }

    /** Typed writes only; no untyped fact bag or arbitrary mutable property map. */
    public interface Registrar {
        Registrar registerTool(MaterialKey material, MaterialToolProfile profile);

        Registrar registerComposition(MaterialKey material, MaterialComposition composition);

        Registrar registerForm(MaterialKey material, FormKey form);

        Registrar registerTag(MaterialKey material, DataKey tag);

        Registrar registerMetadata(MaterialMetadataDeclaration declaration);

        Registrar registerTagDeclaration(MaterialTagDeclaration declaration);

        Registrar registerGroup(MaterialGroupDeclaration declaration);

        <T> Registrar registerProcess(MaterialKey material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule);

        default <T> Registrar registerProcess(MaterialProcessDeclaration<T> declaration) {
            return registerProcess(
                    new MaterialKey(declaration.materialName()),
                    declaration.process(),
                    new MaterialProcessRule<>(declaration.forms(), declaration.value()));
        }

        default Registrar registerTool(String material, MaterialToolProfile profile) {
            return registerTool(new MaterialKey(material), profile);
        }

        @Deprecated(since = "1.7.0", forRemoval = false)
        default Registrar registerTool(GregTechMaterials.Material material, MaterialToolProfile profile) {
            return registerTool(material.key(), profile);
        }

        default Registrar registerForm(String material, String form) {
            return registerForm(new MaterialKey(material), new FormKey(form));
        }

        @Deprecated(since = "1.7.0", forRemoval = false)
        default Registrar registerForm(GregTechMaterials.Material material, FormKey form) {
            return registerForm(material.key(), form);
        }

        @Deprecated(since = "1.7.0", forRemoval = false)
        default Registrar registerComposition(GregTechMaterials.Material material, MaterialComposition composition) {
            return registerComposition(material.key(), composition);
        }

        @Deprecated(since = "1.7.0", forRemoval = false)
        default Registrar registerTag(GregTechMaterials.Material material, DataKey tag) {
            return registerTag(material.key(), tag);
        }

        @Deprecated(since = "1.7.0", forRemoval = false)
        default <T> Registrar registerProcess(
                GregTechMaterials.Material material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule) {
            return registerProcess(material.key(), process, rule);
        }
    }
}
