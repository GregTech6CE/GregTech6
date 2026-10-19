import com.gregtech.gregtech.api.energy.EnergyGate;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.EnergyNodeDefinitions;
import com.gregtech.gregtech.content.energy.OriginalRotaryConverter;
import com.gregtech.gregtech.content.energy.SteamTurbineConversion;
import com.gregtech.gregtech.data.GregTechTags;

/** Offline observations only: native faces, fluids, world hazards and saved-world restart are not exercised. */
class EnergyConverterAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        var specifications = EnergyNodeDefinitions.specifications();
        for (var spec : specifications) {
            if (OriginalRotaryConverter.handles(spec) || spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
                System.out.println("DEVICE|" + spec.id() + "|" + spec.inputRate() + "|"
                        + spec.outputRate() + "|" + spec.capacity());
            }
        }
        var motor = specifications.stream().filter(s -> s.id().equals("electric_motor_lv")).findFirst().orElseThrow();
        var state = new OriginalRotaryConverter.State(motor);
        boolean[] reachedStorage = {false};
        long consumed = EnergyGate.gateInjection(GregTechTags.Energy.EU, true, 8, 16, 3, () -> {
            reachedStorage[0] = true;
            return 0;
        });
        observation("undersized_eu_consumed_without_storage", consumed == 3 && !reachedStorage[0]);

        var injection = state.inject(60, 16, 2, true);
        observation("last_whole_packet_exceeds_capacity", injection.energy() == 76 && injection.consumed() == 1);
        var simulated = state.inject(0, 65, 2, false);
        var executed = state.inject(0, 65, 2, true);
        observation("overvoltage_simulation_has_no_overload", simulated.consumed() == 2 && !simulated.overloaded()
                && executed.consumed() == 2 && executed.overloaded());

        long[] output = {0};
        var stopped = new OriginalRotaryConverter.State(motor).tick(32, true, (size, amount) -> {
            output[0] = size;
            return amount;
        });
        observation("stopped_buffer_still_converts", output[0] == 16 && stopped.energy() == 0 && stopped.visual() == 0);
        var refused = new OriginalRotaryConverter.State(motor).tick(32, false, (size, amount) -> 0);
        observation("refused_output_still_wastes_input", refused.energy() == 0 && refused.possible() && !refused.emitted());

        state = new OriginalRotaryConverter.State(motor);
        state.reverse();
        state.tick(32, false, (size, amount) -> { output[0] = size; return amount; });
        observation("reverse_motor_packet_sign", output[0] == -16);

        var turbine = specifications.stream().filter(s -> s.id().equals("steam_turbine_bronze")).findFirst().orElseThrow();
        var turbineState = new OriginalRotaryConverter.State(turbine);
        boolean first = turbineState.tick(300, false, (size, amount) -> 0).overloaded();
        boolean second = turbineState.tick(300, false, (size, amount) -> 0).overloaded();
        boolean third = turbineState.tick(300, false, (size, amount) -> 0).overloaded();
        observation("steam_overload_grace_two_ticks", !first && !second && third);

        var below = SteamTurbineConversion.step(0, 0, 47, 0, 48);
        var batch = SteamTurbineConversion.step(0, 0, 201, 0, 48);
        var pending = SteamTurbineConversion.step(0, batch.pending(), 300, batch.remainder(), 48);
        observation("steam_threshold_and_integer_rounding", below.consumed() == 0
                && batch.energy() == 100 && batch.pending() == 100 && batch.consumed() == 201
                && batch.condensate() == 1 && batch.remainder() == 1);
        observation("pending_tick_does_not_consume_next_batch", pending.energy() == 100
                && pending.pending() == 0 && pending.consumed() == 0 && pending.remainder() == 1);
    }

    private static void observation(String name, boolean matchesSourceExample) {
        System.out.println("CASE|" + name + "|" + matchesSourceExample);
        if (!matchesSourceExample) throw new AssertionError(name);
    }
}
