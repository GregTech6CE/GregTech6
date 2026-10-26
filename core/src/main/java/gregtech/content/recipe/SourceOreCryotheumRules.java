package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.List;

/**
 * Direct Ores 174-187 rows at original 3703e40308c8c030763fd6297dea8b210d2a77b1.
 * ANY.Amber is GoldenAmber + DominicanAmber, not the unrelated ordinary Amber identity.
 * Each row resolves its physical input and output independently, with no OM fallback.
 */
public final class SourceOreCryotheumRules {
    private SourceOreCryotheumRules() {}
    public static final int POWER = 0, TICKS = 256;
    public static final boolean OPTIMIZE = true;
    public record Binding(int materialId, String materialName) {}
    public enum Input { BLOCK_DUST, CRUSHED, CRUSHED_TINY }
    public record Shape(Input input, int inputCount, MaterialPrefix output,
                        int outputCount, int cryotheumMb) {}
    private static final List<Binding> BINDINGS = List.of(
            new Binding(8469, "GoldenAmber"), new Binding(8422, "DominicanAmber"),
            new Binding(8341, "Glowstone"), new Binding(8368, "GlowstoneCeres"),
            new Binding(8369, "GlowstoneIo"), new Binding(8370, "GlowstoneEnceladus"),
            new Binding(8371, "GlowstoneProteus"), new Binding(8372, "GlowstonePluto"),
            new Binding(8456, "Gloomstone"), new Binding(8470, "Carminite"),
            new Binding(8340, "Nikolite"), new Binding(8333, "Redstone"),
            new Binding(9114, "Cinnabar"));
    // Cryotheum U = 250 mB. Source liquid(U20,true) rounds 12.5 mB up to 13, not 50.
    private static final List<Shape> SHAPES = List.of(
            new Shape(Input.BLOCK_DUST, 1, MaterialPrefix.gemExquisite, 1, 25),
            new Shape(Input.CRUSHED, 1, MaterialPrefix.gemFlawless, 1, 13),
            new Shape(Input.CRUSHED_TINY, 9, MaterialPrefix.gemFlawless, 1, 13));
    public static List<Binding> bindings() { return BINDINGS; }
    public static List<Shape> shapes() { return SHAPES; }
    public static Binding binding(int id, String canonicalName) {
        for (Binding binding : BINDINGS)
            if (binding.materialId() == id && binding.materialName().equals(canonicalName)) return binding;
        return null;
    }
}
