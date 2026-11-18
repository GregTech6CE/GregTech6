package gregtech.engine.fluid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FluidPipeChannelsTest {
    @Test
    void balancedTargetsRespectCapacityAndTraversalOrder() {
        var capacities = new long[] {30, 99, 4, 40};
        assertArrayEquals(
                new long[] {16, 0, 4, 17}, FluidPipeChannels.balancedTargets(List.of(3, 2, 0), capacities, 37));
    }

    @Test
    void balancedTargetsCanSumToTheLargestLong() {
        long half = Long.MAX_VALUE / 2;
        var capacities = new long[] {Long.MAX_VALUE, Long.MAX_VALUE};
        assertArrayEquals(
                new long[] {half, half + 1},
                FluidPipeChannels.balancedTargets(List.of(1, 0), capacities, Long.MAX_VALUE));
    }
}
