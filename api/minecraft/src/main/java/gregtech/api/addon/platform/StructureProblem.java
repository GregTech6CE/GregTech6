/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.platform;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Detached results of an explicit, read-only structure diagnosis. No result claims or loads a chunk. */
public sealed interface StructureProblem {
    BlockPos position();

    /** Expected entries are block ids, #tags or named structure predicates; alternatives are retained. */
    record WrongBlock(BlockPos position, List<String> expected, ResourceLocation found) implements StructureProblem {
        public WrongBlock { position = position.immutable(); expected = List.copyOf(expected); Objects.requireNonNull(found); }
    }

    record Unloaded(BlockPos position) implements StructureProblem {
        public Unloaded { position = position.immutable(); }
    }

    record Layers(BlockPos position, String group, int actual, int minimum) implements StructureProblem {
        public Layers { position = position.immutable(); Objects.requireNonNull(group); }
    }

    /** Selection is the declaration selector, actual counts only accepted cells; maximum -1 is unbounded. */
    record Count(BlockPos position, String selection, int actual, int minimum, int maximum) implements StructureProblem {
        public Count { position = position.immutable(); Objects.requireNonNull(selection); }
    }

    record DifferentBlocks(BlockPos position, String selection, BlockPos firstPosition,
                           ResourceLocation first, ResourceLocation found) implements StructureProblem {
        public DifferentBlocks {
            position = position.immutable(); firstPosition = firstPosition.immutable();
            Objects.requireNonNull(selection); Objects.requireNonNull(first); Objects.requireNonNull(found);
        }
    }

    record Occupied(BlockPos position, BlockPos controller) implements StructureProblem {
        public Occupied { position = position.immutable(); controller = controller.immutable(); }
    }

    /** The block identity matched, but the controller requires a different part implementation or condition. */
    record Rejected(BlockPos position) implements StructureProblem {
        public Rejected { position = position.immutable(); }
    }

    /** Diagnosis cannot be performed (removed/client-side machine or a host without diagnostic support). */
    record Unavailable(BlockPos position) implements StructureProblem {
        public Unavailable { position = position.immutable(); }
    }
}
