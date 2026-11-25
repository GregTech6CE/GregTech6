package gregtech.content.blockentity.machine;

import gregtech.content.transport.fluid.FluidPipeTopology;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.fluid.FluidUnits;
import gregtech.engine.stack.StackCompat;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ObjLongConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.jetbrains.annotations.Nullable;

/** Contents in physical pipe and channel order, independent of the shared-pool cursor. */
public final class FluidPipeContents {
    private FluidPipeContents() {}

    /** Identifies each distinct stack once, retaining the first sample for display and balancing. */
    static List<FluidStack> variants(FluidPipeTopology topology) {
        return scanVariants(topology, null);
    }

    private static List<FluidStack> scanVariants(FluidPipeTopology topology, @Nullable ObjLongConsumer<FluidStack> count) {
        Map<Fluid, List<FluidStack>> byType = new HashMap<>();
        List<FluidStack> order = new ArrayList<>();
        for (int node = 0; node < topology.size(); node++) {
            for (FluidTankGT tank : topology.pipe(node).channelTanks()) {
                if (tank.isEmpty()) continue;
                FluidStack sample = tank.getFluidLong();
                List<FluidStack> candidates = byType.computeIfAbsent(sample.getFluid(), ignored -> new ArrayList<>());
                FluidStack match = findVariant(candidates, sample);
                if (match == null) {
                    candidates.add(sample);
                    order.add(sample);
                    match = sample;
                }
                if (count != null) count.accept(match, tank.getAmount());
            }
        }
        return order;
    }

    private static @Nullable FluidStack findVariant(List<FluidStack> candidates, FluidStack sample) {
        for (FluidStack candidate : candidates) {
            if (StackCompat.sameFluid(candidate, sample)) return candidate;
        }
        return null;
    }

    /** The magnifying glass reports local channels followed by totals for each distinct fluid. */
    public static List<Component> describe(FluidPipeBlockEntity pipe) {
        Level level = pipe.getLevel();
        if (level == null) return List.of();
        List<Component> lines = new ArrayList<>();
        for (FluidTankGT tank : pipe.channelTanks()) {
            if (!tank.isEmpty()) lines.add(amountLine(tank.getFluidLong(), BigInteger.valueOf(tank.getAmount())));
        }

        FluidPipeTopology topology = FluidPipeTopology.scan(level, pipe);
        // A legacy tank can hold a long; the sum of several such tanks need not fit another long.
        Map<FluidStack, BigInteger> totals = new IdentityHashMap<>();
        List<FluidStack> variants = scanVariants(topology,
                (sample, amount) -> totals.merge(sample, BigInteger.valueOf(amount), BigInteger::add));
        if (variants.isEmpty()) {
            lines.add(Component.literal("=== This Fluid Pipe Network is empty ===").withStyle(ChatFormatting.WHITE));
        } else {
            lines.add(Component.literal("=== This Fluid Pipe Network contains: ===").withStyle(ChatFormatting.WHITE));
            for (FluidStack sample : variants) lines.add(amountLine(sample, totals.get(sample)));
        }
        return lines;
    }

    private static Component amountLine(FluidStack sample, BigInteger quantity) {
        String amount = String.format(Locale.ROOT, "%,d", quantity);
        if (quantity.compareTo(BigInteger.valueOf(1_000_000)) >= 0) amount = amount.replace(',', '_');
        return Component.empty()
                .append(Component.literal(amount + " " + FluidUnits.unit() + " of ").withStyle(ChatFormatting.WHITE))
                .append(sample.getDisplayName().copy().withStyle(ChatFormatting.AQUA));
    }
}
