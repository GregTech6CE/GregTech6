/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

/**
 * One material and its GT amount, used by composite {@link GTMaterial} compositions
 * (GT6 {@code OreDictMaterialStack}) and by weighted construction-material lists.
 * A null material is stored as {@link MaterialSentinels#INVALID}, which
 * {@link GTMaterial#setComposition(long, MaterialComponent...)} then drops.
 * <p>
 * The material is resolved when the component is created, so a component that names an alias
 * before that alias is registered keeps the alias object. Read material compositions through
 * {@link GTMaterial#getCompositionComponents()}, which resolves each entry again, or call
 * {@link GTMaterial#resolve()} on the accessor result.
 */
public record MaterialComponent(GTMaterial material, long amount) {
    public MaterialComponent {
        material = material == null ? MaterialSentinels.INVALID : material.resolve();
    }

    public static MaterialComponent of(GTMaterial material, long amount) {
        return new MaterialComponent(material, amount);
    }
}
