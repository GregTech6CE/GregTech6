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
 * compositions, forms, tags and process rules are stored in independent, keyed indexes. A
 * material's convenience accessors are projections over those indexes.</p>
 */
public final class MaterialInfoTool {
    private static final InstalledService<Host> HOST = new InstalledService<>();

    private MaterialInfoTool() {}

    @ApiStatus.Internal
    public static void bindHost(Host host) {
        HOST.bind(host, "Material info host is missing", "Material info host is already installed");
    }

    /** Opens a typed registration session owned by the addon. It closes at the material freeze. */
    public static Registrar forAddon(String addonId) {
        return host().registrar(AddonNames.requireAddonId(addonId));
    }

    /** Frozen auxiliary tool profile for a material. */
    public static MaterialToolProfile tool(MaterialKey material) {
        return frozenTool(material)
                .orElseThrow(() ->
                        new IllegalStateException("Material auxiliary indexes are not frozen: " + material.name()));
    }

    public static MaterialToolProfile tool(String materialName) {
        return tool(new MaterialKey(materialName));
    }

    /** Frozen composition, if one was registered. */
    public static Optional<MaterialComposition> composition(MaterialKey material) {
        return host().composition(material);
    }

    /** Empty before the host installs or before the definition snapshot is frozen. */
    public static Optional<MaterialToolProfile> frozenTool(MaterialKey material) {
        Objects.requireNonNull(material, "material");
        return HOST.isBound() ? host().toolIfReady(material) : Optional.empty();
    }

    private static Host host() {
        return HOST.get("GregTech material info API is not installed");
    }

    @ApiStatus.Internal
    public interface Host {
        Registrar registrar(String addonId);

        default Optional<MaterialToolProfile> toolIfReady(MaterialKey material) {
            return Optional.of(tool(material));
        }

        MaterialToolProfile tool(MaterialKey material);

        Optional<MaterialComposition> composition(MaterialKey material);
    }

    /** Typed writes only; no untyped fact bag or arbitrary mutable property map. */
    public interface Registrar {
        Registrar registerTool(MaterialKey material, MaterialToolProfile profile);

        Registrar registerComposition(MaterialKey material, MaterialComposition composition);

        Registrar registerForm(MaterialKey material, FormKey form);

        Registrar registerTag(MaterialKey material, DataKey tag);

        <T> Registrar registerProcess(MaterialKey material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule);

        default Registrar registerTool(String material, MaterialToolProfile profile) {
            return registerTool(new MaterialKey(material), profile);
        }

        default Registrar registerTool(GregTechMaterials.Material material, MaterialToolProfile profile) {
            return registerTool(material.key(), profile);
        }

        default Registrar registerForm(String material, String form) {
            return registerForm(new MaterialKey(material), new FormKey(form));
        }

        default Registrar registerForm(GregTechMaterials.Material material, FormKey form) {
            return registerForm(material.key(), form);
        }

        default Registrar registerComposition(GregTechMaterials.Material material, MaterialComposition composition) {
            return registerComposition(material.key(), composition);
        }

        default Registrar registerTag(GregTechMaterials.Material material, DataKey tag) {
            return registerTag(material.key(), tag);
        }

        default <T> Registrar registerProcess(
                GregTechMaterials.Material material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule) {
            return registerProcess(material.key(), process, rule);
        }
    }
}
