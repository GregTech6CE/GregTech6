package gregtech.gametest;

import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.api.addon.platform.MachineEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Adapter contract only: controlled legacy return values distinguish packets from part-buffer units. */
@GameTestHolder("gt6ce_api_contracts")
@PrefixGameTestTemplate(false)
public final class AddonEnergyAccessTests {
    private AddonEnergyAccessTests() {}

    @GameTest(template = "test_empty")
    public static void energyResultsPreserveUnitsAndBufferOwnership(GameTestHelper helper) {
        Host host = new Host();
        MachineEnergy energy = host.context().energyAccess();
        var adjacent = energy.emitStored(-32, 4);
        helper.assertTrue(adjacent.transferredUnits() == 96 && adjacent.completePackets() == 3
                && adjacent.partialPacketUnits() == 0, "legacy adjacent return is accepted packets");
        helper.assertTrue(host.stored == 904 && host.lastSize == -32 && host.lastCount == 4,
                "stored emission passes signed packets unchanged and debits once");

        BlockPos target = new BlockPos(8, 9, 10);
        var remote = energy.emitStoredTo(target, Direction.WEST, 16, 4);
        helper.assertTrue(remote.transferredUnits() == 48 && remote.completePackets() == 3
                && host.stored == 856, "remote emission converts packets to units without a second debit");
        helper.assertTrue(host.target.equals(target) && host.receivingSide == Direction.WEST,
                "target and receiver-facing side are forwarded unchanged");

        var part = energy.transferStoredToPart("output", 32, 4);
        helper.assertTrue(part.transferredUnits() == 70 && part.completePackets() == 2
                && part.partialPacketUnits() == 6 && host.stored == 786,
                "part transfer is measured in exact debited units, including a partial packet");
        long beforeSupply = host.stored;
        var external = energy.supplyPart("two_way", "RU", -32, 4);
        helper.assertTrue(external.transferredUnits() == 70 && external.partialPacketUnits() == 6
                && host.stored == beforeSupply && host.externallySupplied == 70,
                "external supply delegates to pushToPart without consuming the host buffer");
        helper.assertTrue(host.calls == 4 && host.part.equals("two_way") && host.type.equals("RU"),
                "each operation delegates once to its intended legacy path");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void energyValidationRunsBeforeHostMutation(GameTestHelper helper) {
        Host host = new Host();
        MachineEnergy energy = host.context().energyAccess();
        helper.assertTrue(energy.emitStored(0, 0).transferredUnits() == 0
                && energy.emitStoredTo(BlockPos.ZERO, null, 32, 0).transferredUnits() == 0
                && energy.transferStoredToPart("output", -32, 0).transferredUnits() == 0
                && energy.supplyPart("two_way", "RU", 0, 0).transferredUnits() == 0,
                "empty transfers return an empty result");
        expect(helper, ArithmeticException.class, () -> energy.emitStored(Long.MAX_VALUE, 2));
        expect(helper, ArithmeticException.class,
                () -> energy.emitStoredTo(BlockPos.ZERO, null, Long.MAX_VALUE, 2));
        expect(helper, ArithmeticException.class,
                () -> energy.transferStoredToPart("output", Long.MAX_VALUE, 2));
        expect(helper, ArithmeticException.class,
                () -> energy.supplyPart("two_way", "RU", Long.MAX_VALUE, 2));
        expect(helper, IllegalArgumentException.class, () -> energy.emitStored(32, -1));
        expect(helper, IllegalArgumentException.class, () -> energy.emitStored(0, 1));
        expect(helper, IllegalArgumentException.class, () -> energy.emitStored(Long.MIN_VALUE, 1));
        expect(helper, IllegalArgumentException.class, () -> energy.consumeMainInput(-1));
        expect(helper, IllegalArgumentException.class, () -> energy.consumeAuxiliary("HU", -1));
        expect(helper, IllegalArgumentException.class, () -> energy.generate(-1));
        expect(helper, IllegalArgumentException.class, () -> energy.setCapacity(-1));
        helper.assertTrue(host.calls == 0 && host.stored == 1000 && host.externallySupplied == 0,
                "no-op and invalid requests never invoke or mutate the legacy host");
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, Class<? extends RuntimeException> type, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            helper.assertTrue(type.isInstance(failure), "expected " + type.getSimpleName() + ", got " + failure);
            return;
        }
        helper.assertTrue(false, "expected " + type.getSimpleName());
    }

    /** The legacy host owns debits. Its deliberately partial charge result must not be rounded. */
    private static final class Host implements InvocationHandler {
        private long stored = 1000;
        private long externallySupplied;
        private int calls;
        private long lastSize;
        private long lastCount;
        private BlockPos target;
        private Direction receivingSide;
        private String part;
        private String type;

        private MachineBehaviorContext context() {
            return (MachineBehaviorContext) Proxy.newProxyInstance(MachineBehaviorContext.class.getClassLoader(),
                    new Class<?>[]{MachineBehaviorContext.class}, this);
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method,
                    args == null ? new Object[0] : args);
            calls++;
            return switch (method.getName()) {
                case "emit" -> {
                    if (args.length == 2) {
                        lastSize = (long) args[0];
                        lastCount = (long) args[1];
                        stored -= Math.abs(lastSize) * 3;
                        yield 3L;
                    }
                    part = (String) args[0];
                    lastSize = (long) args[1];
                    lastCount = (long) args[2];
                    stored -= 70;
                    yield 70L;
                }
                case "emitTo" -> {
                    target = (BlockPos) args[0];
                    receivingSide = (Direction) args[1];
                    lastSize = (long) args[2];
                    lastCount = (long) args[3];
                    stored -= Math.abs(lastSize) * 3;
                    yield 3L;
                }
                case "pushToPart" -> {
                    part = (String) args[0];
                    type = (String) args[1];
                    lastSize = (long) args[2];
                    lastCount = (long) args[3];
                    externallySupplied += 70;
                    yield 70L;
                }
                default -> throw new AssertionError("Unexpected host operation " + method.getName());
            };
        }
    }
}
