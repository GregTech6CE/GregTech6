package gregtech.content.cover;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.common.util.NonNullFunction;

/** Per-owner derived capabilities, forwarding the source's native invalidation lifetime. */
public final class ForgeCoverCapabilityCache {
    private record Key(Capability<?> capability, Direction side) {}
    private final Map<Key, Entry<?>> entries = new HashMap<>();

    public <T> LazyOptional<T> wrap(Capability<T> capability, Direction side,
                                    LazyOptional<T> source, NonNullFunction<T, T> decorate) {
        Key key = new Key(capability, side);
        Entry<?> previous = entries.get(key);
        if (previous != null) {
            if (previous.source == source && source.isPresent() && previous.mapped.isPresent()) return previous.mapped.cast();
            entries.remove(key);
            previous.detach();
            // A callback's lookup is newer than this request's source snapshot.
            // Reuse its matching source, but never evict a different replacement
            // or return that replacement as though it came from our source.
            Entry<?> replacement = entries.get(key);
            if (replacement != null) {
                if (replacement.source == source && source.isPresent() && replacement.mapped.isPresent())
                    return replacement.mapped.cast();
                return LazyOptional.empty();
            }
        }
        if (!source.isPresent()) return LazyOptional.empty();
        Entry<T> entry = new Entry<>(key, source, decorate);
        entries.put(key, entry);
        source.addListener(entry.listener);
        return entry.mapped;
    }

    private final class Entry<T> {
        private final LazyOptional<T> source;
        private final LazyOptional<T> mapped;
        private final NonNullConsumer<LazyOptional<T>> listener;

        private Entry(Key key, LazyOptional<T> source, NonNullFunction<T, T> decorate) {
            this.source = source;
            mapped = source.lazyMap(decorate);
            listener = ignored -> {
                entries.remove(key, this);
                // Source.invalidate clears its listener set after notification. Do not
                // remove from that set while its forEach is still in progress.
                mapped.invalidate();
            };
        }

        private void detach() {
            // A different face may detach us reentrantly during source.invalidate().
            // That source is already invalid and is iterating its listener set.
            if (source.isPresent()) source.removeListener(listener);
            mapped.invalidate();
        }
    }
}
