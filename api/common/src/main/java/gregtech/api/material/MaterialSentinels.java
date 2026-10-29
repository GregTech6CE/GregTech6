package gregtech.api.material;


/** Sentinel lookup must not initialize the entire built-in material alias table. */
public final class MaterialSentinels {
    private MaterialSentinels() {}
    public static final GTMaterial Invalid = new GTMaterial(-1, "NULL", "Null", 0xFF00FF)
            .put(MaterialProperty.HIDDEN);
    public static final GTMaterial Empty = new GTMaterial(0, "Empty", "Empty", 0xFFFFFF)
            .setAtomicProperties(AtomicProperties.ZERO)
            .put(MaterialProperty.HIDDEN);
}
