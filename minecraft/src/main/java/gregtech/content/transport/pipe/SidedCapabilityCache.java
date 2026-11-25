package gregtech.content.transport.pipe;

import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Caches the unsided view and each requested sided view for one pipe. */
final class SidedCapabilityCache<T> {
    private final Supplier<T> unsidedFactory;
    private final Function<Direction, T> sidedFactory;
    private @Nullable T unsided;
    private @Nullable EnumMap<Direction, T> sides;

    SidedCapabilityCache(Supplier<T> unsidedFactory, Function<Direction, T> sidedFactory) {
        this.unsidedFactory = unsidedFactory;
        this.sidedFactory = sidedFactory;
    }

    T get(@Nullable Direction side) {
        if (side == null) {
            if (unsided == null) unsided = unsidedFactory.get();
            return unsided;
        }
        if (sides == null) sides = new EnumMap<>(Direction.class);
        return sides.computeIfAbsent(side, sidedFactory);
    }

    @Nullable
    T remove(Direction side) {
        return sides == null ? null : sides.remove(side);
    }

    void clear(Consumer<T> invalidate) {
        T previousUnsided = unsided;
        var previousSides = sides;
        unsided = null;
        sides = null;
        if (previousUnsided != null) invalidate.accept(previousUnsided);
        if (previousSides != null) previousSides.values().forEach(invalidate);
    }
}
