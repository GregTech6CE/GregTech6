import com.gregtech.gregtech.api.energy.EnergyGate;
import com.gregtech.gregtech.api.energy.EnergyPackets;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.EnergyNodeDefinitions;
import com.gregtech.gregtech.content.energy.TransformerControlState;
import com.gregtech.gregtech.data.GregTechTags;

/** Core observations only; native transfer, world ticking and NBT are not instantiated. */
class TransformerAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : EnergyNodeDefinitions.specifications()) {
            if (spec.id().startsWith("transformer_")) {
                System.out.println("DEVICE|" + spec.id() + "|" + spec.material().getName()
                        + "|" + spec.inputRate() + "|" + spec.outputRate() + "|" + spec.capacity());
            }
        }

        System.out.println("FITTING|space=4|packet=32|requested=1|accepted="
                + EnergyPackets.fitting(1, 4, 32));
        System.out.println("FITTING|space=32|packet=32|requested=2|accepted="
                + EnergyPackets.fitting(2, 32, 32));
        System.out.println("EU_MAGNITUDE|negative32=" + EnergyPackets.magnitude(-32, false));
        System.out.println("EU_GATE|negative32|min16|callbackMarker="
                + EnergyGate.gateInjection(GregTechTags.Energy.EU, true, -32, 16, 1, () -> 99));

        var state = new TransformerControlState(() -> {});
        observe("initial", state);
        state.tick(true, false);
        observe("output_refused", state);
        state.select(true);
        observe("reverse_has_independent_output_flags", state);
        state.select(false);
        for (int tick = 1; tick < 64; tick++) state.tick(true, true);
        observe("64_possible_ticks", state);
        state.setEnabled(false);
        observe("stopped", state);
        state.setEnabled(true);
        state.setMode(15);
        System.out.println("LIMIT|mode15|max64|" + state.limit(64));
        var saved = state.snapshot();
        state.restore(saved, true);
        observe("restored_reverse", state);
        state.select(false);
        observe("restored_forward", state);
    }

    private static void observe(String name, TransformerControlState state) {
        System.out.println("STATE|" + name + "|accepts=" + state.accepts()
                + "|running=" + state.running() + "|active=" + state.active()
                + "|visual=" + state.visual() + "|mode=" + state.mode());
    }
}
