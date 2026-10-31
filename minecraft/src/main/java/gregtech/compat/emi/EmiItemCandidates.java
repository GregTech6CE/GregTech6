package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Registration-scoped sharing of exact, ordered candidates; slot amounts and chances stay private. */
final class EmiItemCandidates {
    private Map<Candidates, List<EmiStack>> lists = new HashMap<>();
    private Map<StackKey, EmiStack> stacks = new HashMap<>();

    private record StackKey(ItemStack stack) {
        @Override
        public boolean equals(Object other) {
            return other instanceof StackKey key && ItemStack.matches(stack, key.stack);
        }

        @Override
        public int hashCode() {
            return 31 * stack.getItem().hashCode() + stack.getCount();
        }
    }

    private record Candidates(List<ItemStack> stacks) {
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Candidates key) || stacks.size() != key.stacks.size()) return false;
            for (int i = 0; i < stacks.size(); i++)
                if (!ItemStack.matches(stacks.get(i), key.stacks.get(i))) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 1;
            for (var stack : stacks) hash = 31 * hash + 31 * stack.getItem().hashCode() + stack.getCount();
            return hash;
        }
    }

    List<EmiStack> of(List<ItemStack> options) {
        if (lists == null) return options.stream().map(EmiStack::of).toList();
        var found = lists.get(new Candidates(options));
        if (found != null) return found;
        // Keys must not observe later mutations of the caller's recipe stacks.
        var snapshot = options.stream().map(ItemStack::copy).toList();
        var shared = snapshot.stream()
                .map(stack -> stacks.computeIfAbsent(new StackKey(stack), key -> EmiStack.of(key.stack())))
                .toList();
        lists.put(new Candidates(snapshot), shared);
        return shared;
    }

    /** Recipes retain the shared lists, not the lookup tables or native-stack snapshots. */
    void finish() {
        lists = null;
        stacks = null;
    }
}
