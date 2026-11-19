package gregtech.client;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;

/** Builds the search index's suffix order by sorting pairs of ranks with counting sort. */
public final class SuffixArraySort {
    private SuffixArraySort() {}

    public static int[] positions(IntList text, int longestWord) {
        int size = text.size();
        int[] ranks = new int[size];
        if (size < 2) return ranks;

        int alphabetSize = 1;
        for (int index = 0; index < size; index++) {
            // Minecraft stores UTF-16 code units and uses -1 to terminate each word.
            alphabetSize = Math.max(alphabetSize, text.getInt(index) + 2);
        }
        int[] buckets = new int[Math.max(size, alphabetSize)];
        for (int index = 0; index < size; index++) buckets[text.getInt(index) + 1]++;
        bucketStarts(buckets, alphabetSize);

        int[] order = new int[size];
        int[] nextOrder = new int[size];
        for (int index = 0; index < size; index++) order[buckets[text.getInt(index) + 1]++] = index;
        int rankCount = 0;
        int previousCharacter = -2;
        for (int index : order) {
            int character = text.getInt(index);
            if (character != previousCharacter) rankCount++;
            ranks[index] = rankCount - 1;
            previousCharacter = character;
        }

        for (int span = 1; span <= longestWord && rankCount < size; span *= 2) {
            Arrays.fill(buckets, 0, rankCount, 0);
            for (int rank : ranks) buckets[rank]++;
            bucketStarts(buckets, rankCount);

            // Shift the old order to visit second halves by rank, with missing halves first.
            // A stable bucket pass on the first half then orders the pairs.
            int tailStart = size - span;
            for (int index = tailStart; index < size; index++) nextOrder[buckets[ranks[index]]++] = index;
            for (int suffix : order) {
                int index = suffix - span;
                if (index >= 0) nextOrder[buckets[ranks[index]]++] = index;
            }

            // The old order is now scratch space for the new ranks.
            int[] previousRanks = ranks;
            ranks = order;
            order = nextOrder;
            nextOrder = previousRanks;
            rankCount = 0;
            int previousFirst = -1;
            int previousSecond = -1;
            for (int index : order) {
                int first = previousRanks[index];
                int second = index < tailStart ? previousRanks[index + span] : -1;
                if (first != previousFirst || second != previousSecond) rankCount++;
                ranks[index] = rankCount - 1;
                previousFirst = first;
                previousSecond = second;
            }
            // Word terminators distinguish suffixes of different lengths. Text beyond them can only break ties.
            if (span > longestWord / 2) break;
        }

        for (int index = 0; index < size; index++) ranks[order[index]] = index;
        return ranks;
    }

    private static void bucketStarts(int[] buckets, int count) {
        int start = 0;
        for (int rank = 0; rank < count; rank++) {
            int frequency = buckets[rank];
            buckets[rank] = start;
            start += frequency;
        }
    }
}
