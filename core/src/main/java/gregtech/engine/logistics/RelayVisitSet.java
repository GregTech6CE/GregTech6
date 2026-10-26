package gregtech.engine.logistics;

import java.util.Set;
import java.util.HashSet;

/** Per-thread visits used by relay operations; the caller removes state when the outer operation returns. */
public final class RelayVisitSet<T> {
    private final ThreadLocal<Set<T>> visited = ThreadLocal.withInitial(HashSet::new);

    public Set<T> get() {
        return visited.get();
    }

    public void remove() {
        visited.remove();
    }
}
