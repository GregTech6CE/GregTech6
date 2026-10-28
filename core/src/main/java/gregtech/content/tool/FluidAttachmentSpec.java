package gregtech.content.tool;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import java.util.*;

/** Material/acid flags from GT6 Loader_MultiTileEntities 32079-32750. */
public record FluidAttachmentSpec(String id, String shape, GTMaterial material, boolean acidProof) {

    public boolean acceptsGas() {
        return gasShape(shape);
    }

    public boolean fillsTarget() {
        return shape.equals("fluid_funnel") || shape.equals("cap_nozzle");
    }

    private static boolean gasShape(String shape) {
        return shape.equals("cap_nozzle") || shape.equals("nozzle");
    }

    public static List<FluidAttachmentSpec> all() {
        var result = new ArrayList<FluidAttachmentSpec>();
        for (String kind : List.of("tap", "fluid_funnel", "cap_nozzle", "nozzle")) {
            result.add(new FluidAttachmentSpec(
                    kind + "_" + (gasShape(kind) ? "steel" : "ceramic"),
                    kind,
                    gasShape(kind) ? Materials.Steel : Materials.Ceramic,
                    false));
            result.add(new FluidAttachmentSpec(kind + "_plastic", kind, Materials.Plastic, false));
            result.add(new FluidAttachmentSpec(kind + "_stainless_steel", kind, Materials.StainlessSteel, true));
            result.add(new FluidAttachmentSpec(kind + "_tungsten", kind, Materials.Tungsten, true));
            result.add(new FluidAttachmentSpec(
                    kind + "_tantalum_hafnium_carbide", kind, Materials.TantalumHafniumCarbide, false));
            result.add(new FluidAttachmentSpec(kind + "_adamantium", kind, Materials.Adamantium, true));
        }
        return List.copyOf(result);
    }
}
