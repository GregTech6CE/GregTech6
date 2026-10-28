package gregtech.engine.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BasicMachineEnergyTest {
    @Test
    void startupIsPaidFromAuxiliaryCharge() {
        assertEquals(new BasicMachineEnergy.Startup(60, 0), BasicMachineEnergy.payStartup(100, 40));
    }

    @Test
    void startupKeepsTheUnpaidRemainder() {
        assertEquals(new BasicMachineEnergy.Startup(0, 20), BasicMachineEnergy.payStartup(20, 40));
    }

    @Test
    void noAuxiliaryChargeLeavesStartupPending() {
        assertEquals(new BasicMachineEnergy.Startup(0, 40), BasicMachineEnergy.payStartup(0, 40));
    }

    @Test
    void paidStartupIsANoOp() {
        assertEquals(new BasicMachineEnergy.Startup(100, 0), BasicMachineEnergy.payStartup(100, 0));
    }

    @Test
    void repeatedPaymentsConverge() {
        var first = BasicMachineEnergy.payStartup(20, 40);
        assertEquals(
                new BasicMachineEnergy.Startup(0, 20),
                BasicMachineEnergy.payStartup(first.auxStored(), first.startup()));
    }

    @Test
    void bufferMathStaysBounded() {
        assertEquals(1, BasicMachineEnergy.demanded(0, 8, 8));
        assertEquals(8, BasicMachineEnergy.add(0, 8, 8, 1));
        assertEquals(8, BasicMachineEnergy.add(8, 8, 8, 1));
        assertEquals(0, BasicMachineEnergy.drain(8, 8));
        assertEquals(0, BasicMachineEnergy.accepted(8, 8, 1, 1));
    }
}
