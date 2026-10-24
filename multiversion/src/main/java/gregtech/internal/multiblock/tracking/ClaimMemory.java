package gregtech.internal.multiblock.tracking;

import gregtech.internal.multiblock.MultiblockLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

/** Last successful claim. A repeated scan with the same cells does not bind or rewrite design numbers. */
final class ClaimMemory {
    private long[] positions = new long[0];
    private int[] roles = new int[0];
    private int designs;

    boolean matches(Map<BlockPos, MultiblockLayout.Role> parts, int designHash) {
        if (designs != designHash || parts.size() != positions.length) return false;
        int index = 0;
        for (var entry : parts.entrySet()) {
            if (positions[index] != entry.getKey().asLong()) return false;
            if (roles[index] != entry.getValue().ordinal()) return false;
            index++;
        }
        return true;
    }

    void remember(Map<BlockPos, MultiblockLayout.Role> parts, int designHash) {
        positions = new long[parts.size()];
        roles = new int[parts.size()];
        int index = 0;
        for (var entry : parts.entrySet()) {
            positions[index] = entry.getKey().asLong();
            roles[index] = entry.getValue().ordinal();
            index++;
        }
        designs = designHash;
    }

    void forget() {
        positions = new long[0];
        roles = new int[0];
        designs = 0;
    }

    static int designs(int... values) {
        int hash = 1;
        for (int value : values) hash = 31 * hash + value;
        return hash;
    }

    static int cellDesigns(Iterable<Integer> values) {
        int hash = 1;
        int count = 0;
        for (int value : values) {
            hash = 31 * hash + value;
            count++;
        }
        return hash * 31 + count;
    }
}
