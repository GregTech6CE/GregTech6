import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.BatteryBoxDefinitions;
import com.gregtech.gregtech.content.energy.BatteryBoxState;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.content.energy.ItemBatteryRules;
import java.util.ArrayList;

/** Pure-state observations with explicit fake slot adapters, not native item/world simulation. */
class BatteryStorageAudit {
    private static final class Cell implements BatteryBoxState.Battery {
        private long charge = 10000;
        public boolean accepts() { return true; }
        public boolean canInject(long voltage) { return voltage == 32; }
        public boolean canExtract(long voltage) { return voltage == 32; }
        public long stored() { return charge; }
        public long capacity() { return 64000; }
        public long extract(long voltage, long packets) {
            long used = ItemBatteryRules.extractionPackets(charge, 32, voltage, packets);
            charge -= used * voltage;
            return used;
        }
        public long inject(long voltage, long packets) {
            long used = ItemBatteryRules.injectionPackets(capacity(), charge, 32, voltage, packets);
            charge = Math.min(capacity(), charge + used * voltage);
            return used;
        }
    }

    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : BatteryBoxDefinitions.specifications()) {
            System.out.println("BOX|" + spec.id() + "|" + spec.material().getName() + "|"
                    + spec.inputRate() + "|" + spec.outputRate() + "|" + spec.batterySlots());
        }
        for (var spec : ChemicalBatterySpec.all()) {
            System.out.println("CELL|" + spec.id() + "|" + spec.voltage() + "|" + spec.capacity()
                    + "|" + spec.minimumPacket() + "|" + spec.maximumPacket() + "|" + spec.chemistry().advanced);
        }
        for (var item : MaterialItemDefinitions.all()) {
            if (item.material().getName().startsWith("Energium") && item.prefix().getName().startsWith("gem")) {
                System.out.println("CRYSTAL_FORM|" + item.material().getName() + "|"
                        + item.prefix().getName() + "|" + item.itemId());
            }
        }

        for (int band = 0; band < 8; band++) {
            var cells = new ArrayList<BatteryBoxState.Battery>();
            for (int slot = 0; slot < 4; slot++) cells.add(new Cell());
            var state = new BatteryBoxState(cells, 32);
            long before = band * 32L * 40 * 4;
            state.restore(new BatteryBoxState.Snapshot(before, false, 0));
            state.tick(1);
            System.out.println("BAND|" + band + "|bufferDelta=" + (state.buffer() - before)
                    + "|itemEnergy=" + state.stored());
        }

        var cells = new ArrayList<BatteryBoxState.Battery>();
        for (int slot = 0; slot < 4; slot++) cells.add(new Cell());
        var state = new BatteryBoxState(cells, 32);
        state.restore(new BatteryBoxState.Snapshot(32, false, 0));
        state.tick(2);
        System.out.println("LOW_BUFFER|providers=" + state.providers() + "|offered=" + state.offered());
        state.emitted(4);
        System.out.println("LOW_BUFFER_AFTER|buffer=" + state.buffer() + "|emitted=" + state.emitted());
        state.tick(3);
        System.out.println("NEXT_TICK|emitted=" + state.emitted());
        state.enabled(false);
        long before = state.buffer();
        long simulated = state.inject(32, 100, false);
        System.out.println("STOPPED_SIMULATION|accepted=" + simulated + "|bufferUnchanged=" + (state.buffer() == before));
        System.out.println("STOPPED_INJECTION|accepted=" + state.inject(32, 100, true)
                + "|buffer=" + state.buffer() + "|offered=" + state.offered());
        state.restore(new BatteryBoxState.Snapshot(state.bufferCapacity() - 4, false, 2));
        state.tick(4);
        System.out.println("LAST_PACKET|accepted=" + state.inject(32, 1, true)
                + "|excess=" + (state.buffer() - state.bufferCapacity()));
        var saved = state.snapshot();
        state.restore(saved);
        System.out.println("RESTORE|buffer=" + state.buffer() + "|mode=" + state.mode()
                + "|canReceive=" + state.canReceive() + "|emitted=" + state.emitted());
        System.out.println("ITEM_LAST_PACKET|" + ItemBatteryRules.injectionPackets(64000, 63996, 32, 32, 10));
        System.out.println("ITEM_EMPTY_EXTRACTION|" + ItemBatteryRules.extractionPackets(0, 32, 32, 10));
    }
}
