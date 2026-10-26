package gregtech.content.recipe;

/** Source LoaderUnificationTargets 1306, 1310-1311, consumed only by reviewed C output tokens.
 * These are default bindings, never a new global unifier or a ban on explicit addon forms.
 */
public final class SourceCentrifugeItemOutputs {
    private SourceCentrifugeItemOutputs() {}
    public record Binding(String materialName, int materialId, String prefixName,
                          String vanillaPath, long dustSourceUnits) {}
    private static final Binding[] BINDINGS = {
        new Binding("Bone", 8219, "dust", "bone_meal", 648648000L),
        new Binding("Blaze", 8211, "dustTiny", "blaze_powder", 72072000L),
        new Binding("Blaze", 8211, "stick", "blaze_rod", 0)
    };
    public static Binding binding(String material, String prefix) {
        for (var binding : BINDINGS)
            if (binding.materialName().equals(material) && binding.prefixName().equals(prefix)) return binding;
        return null;
    }
    public static boolean useDefault(Binding binding, int id, String name, boolean coreIdentity,
                                     boolean nativeTargetMatches, boolean explicitOverride) {
        return binding != null && binding.materialId() == id && binding.materialName().equals(name)
                && coreIdentity && nativeTargetMatches && !explicitOverride;
    }
    /** Preserve the existing material selector on unsupported/explicitly extended domains. */
    public static String fallback(Binding binding, int count) {
        if (binding == null || count <= 0) return null;
        if (binding.dustSourceUnits() == 0)
            return "i:" + binding.prefixName() + ":" + binding.materialName() + ":" + count;
        try { return "omdu:" + binding.materialName() + ":" + Math.multiplyExact(binding.dustSourceUnits(), count); }
        catch (ArithmeticException overflow) { return null; }
    }
}
