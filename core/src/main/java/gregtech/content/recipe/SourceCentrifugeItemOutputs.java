package gregtech.content.recipe;

import gregtech.engine.recipe.chem.MaterialAmount;

/** Source LoaderUnificationTargets 1306, 1310-1311, consumed only by reviewed C output tokens.
 * These are default bindings, never a new global unifier or a ban on explicit addon forms.
 */
public final class SourceCentrifugeItemOutputs {
    private SourceCentrifugeItemOutputs() {}

    public record Binding(
            String materialName, int materialId, String prefixName, String vanillaPath, long dustSourceUnits) {}

    /**
     * The reviewed default output in common material-unit semantics. The descriptor form exists
     * only for the legacy {@code GeneratedRecipeResolver} token boundary.
     */
    public record Output(String materialName, MaterialAmount units) {
        public Output {
            if (materialName == null || materialName.isBlank() || units == null)
                throw new IllegalArgumentException("Centrifuge material output");
        }
    }

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

    public static boolean useDefault(
            Binding binding,
            int id,
            String name,
            boolean coreIdentity,
            boolean nativeTargetMatches,
            boolean explicitOverride) {
        return binding != null
                && binding.materialId() == id
                && binding.materialName().equals(name)
                && coreIdentity
                && nativeTargetMatches
                && !explicitOverride;
    }
    /** The default output in material units, or null when the token form has no amount. */
    public static Output output(Binding binding, int count) {
        if (binding == null || count <= 0 || binding.dustSourceUnits() <= 0) return null;
        try {
            return new Output(
                    binding.materialName(), MaterialAmount.units(Math.multiplyExact(binding.dustSourceUnits(), count)));
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    /**
     * Legacy descriptor for {@code GeneratedRecipeResolver}. The typed {@link Output} is the
     * production value; this is the single place that still renders {@code omdu:}.
     */
    public static String fallback(Binding binding, int count) {
        if (binding == null || count <= 0) return null;
        if (binding.dustSourceUnits() == 0)
            return "i:" + binding.prefixName() + ":" + binding.materialName() + ":" + count;
        Output output = output(binding, count);
        return output == null
                ? null
                : "omdu:" + output.materialName() + ":" + output.units().units();
    }
}
