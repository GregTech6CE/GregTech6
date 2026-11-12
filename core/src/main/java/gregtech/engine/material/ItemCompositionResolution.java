package gregtech.engine.material;

import java.util.List;
import java.util.Objects;

/** The selected composition plus the source-level decisions that produced it. */
public record ItemCompositionResolution(
        ItemComposition selected, int selectedPriority, List<ItemCompositionDecision> decisions) {
    public ItemCompositionResolution {
        Objects.requireNonNull(selected, "selected");
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
        if (decisions.isEmpty()) throw new IllegalArgumentException("decisions must not be empty");
        boolean selectedCandidatePresent = decisions.stream()
                .anyMatch(decision -> decision.status() == ItemCompositionDecision.Status.SELECTED);
        if (!selectedCandidatePresent) throw new IllegalArgumentException("decisions need a selected candidate");
    }
}
