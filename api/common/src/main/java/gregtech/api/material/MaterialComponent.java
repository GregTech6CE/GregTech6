package gregtech.api.material;


/**
 * One material and its GT amount, used by composite {@link GTMaterial} compositions
 * (GT6 {@code OreDictMaterialStack}) and by weighted construction-material lists.
 * A null material is stored as {@link MaterialSentinels#Invalid}, which
 * {@link GTMaterial#setComposition(long, MaterialComponent...)} then drops.
 */
public record MaterialComponent(GTMaterial material, long amount) {
    public MaterialComponent {
        material = material == null ? MaterialSentinels.Invalid : material.resolve();
    }

    public static MaterialComponent of(GTMaterial material, long amount) {
        return new MaterialComponent(material, amount);
    }
}
