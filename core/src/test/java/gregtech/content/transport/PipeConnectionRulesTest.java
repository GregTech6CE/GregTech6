package gregtech.content.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gregtech.content.transport.PipeConnectionRules.Action;
import gregtech.content.transport.PipeConnectionRules.Neighbor;
import org.junit.jupiter.api.Test;

class PipeConnectionRulesTest {
    @Test
    void clickedSideStartsPipePairsAndAcceptsOrdinaryCapabilities() {
        assertEquals(Action.OPEN_BOTH, PipeConnectionRules.decide(true, Neighbor.CLOSED_PIPE));
        assertEquals(Action.OPEN_BOTH, PipeConnectionRules.decide(true, Neighbor.OPEN_PIPE));
        assertEquals(Action.OPEN_LOCAL, PipeConnectionRules.decide(true, Neighbor.DECLARED_ENDPOINT));
        assertEquals(Action.OPEN_LOCAL, PipeConnectionRules.decide(true, Neighbor.CAPABILITY_ENDPOINT));
    }

    @Test
    void neighborScanOnlyCompletesOpenPipesOrDeclaredEndpoints() {
        assertEquals(Action.NONE, PipeConnectionRules.decide(false, Neighbor.CLOSED_PIPE));
        assertEquals(Action.OPEN_LOCAL, PipeConnectionRules.decide(false, Neighbor.OPEN_PIPE));
        assertEquals(Action.OPEN_LOCAL, PipeConnectionRules.decide(false, Neighbor.DECLARED_ENDPOINT));
        assertEquals(Action.NONE, PipeConnectionRules.decide(false, Neighbor.CAPABILITY_ENDPOINT));
    }

    @Test
    void wrenchLockedPipesAndUnrelatedBlocksAlwaysStayDisconnected() {
        assertEquals(Action.NONE, PipeConnectionRules.decide(true, Neighbor.LOCKED_PIPE));
        assertEquals(Action.NONE, PipeConnectionRules.decide(false, Neighbor.LOCKED_PIPE));
        assertEquals(Action.NONE, PipeConnectionRules.decide(true, Neighbor.NONE));
        assertEquals(Action.NONE, PipeConnectionRules.decide(false, Neighbor.NONE));
    }
}
