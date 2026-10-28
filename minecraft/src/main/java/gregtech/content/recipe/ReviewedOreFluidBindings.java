package gregtech.content.recipe;
/** Bounded original liquid-unit bindings, NOT a replacement for legacy materialFluid().
 * Source Loader_Fluids132 (Petrotheum),131 (Cryotheum),205-212 (wax),618 (Hg).
 * FL.createMolten1074-1077:144 mB/U. Explicit liquids may use250 or1000, never infer by flag.
 */
public final class ReviewedOreFluidBindings {
    public record Binding(int materialId, String materialName, String field, String registryPath, long mbPerU) {}

    private static final Binding[] VALUES = {
        new Binding(800, "Mercury", "GenLiquid_Mercury", "mercury", 144),
        new Binding(8213, "Cryotheum", "Cryotheum", "cryotheum", 250),
        new Binding(8245, "Petrotheum", "Petrotheum", "petrotheum", 250),
        new Binding(8235, "Wax", "GenMolten_Wax", "molten.wax", 144),
        new Binding(8236, "WaxBee", "GenMolten_WaxBee", "molten.waxbee", 144),
        new Binding(8237, "WaxRefractory", "GenMolten_WaxRefractory", "molten.waxrefractory", 144),
        new Binding(8238, "WaxParaffin", "GenMolten_WaxParaffin", "molten.waxparaffin", 144),
        new Binding(8239, "WaxPlant", "GenMolten_WaxPlant", "molten.waxplant", 144),
        new Binding(8240, "WaxMagic", "GenMolten_WaxMagic", "molten.waxmagic", 144),
        new Binding(8280, "WaxAmnesic", "GenMolten_WaxAmnesic", "molten.waxamnesic", 144),
        new Binding(8281, "WaxSoulful", "GenMolten_WaxSoulful", "molten.waxsoulful", 144)
    };

    private ReviewedOreFluidBindings() {}

    public static Binding liquid(int id, String name) {
        for (var row : VALUES) if (row.materialId == id && row.materialName.equals(name)) return row;
        return null; // Unknown is unsupported, not a 144/1000 guess.
    }

    public static int amount(Binding binding, long sourceAmount, boolean roundUp) {
        return binding == null
                ? -1
                : OriginalMaterialFluidUnits.millibuckets(
                        sourceAmount, OriginalMaterialFluidUnits.SOURCE_UNIT, binding.mbPerU, roundUp);
    }
}
