package gregtech.api.multiblock.tracking;

/** Bit mask of watched chunk sections. One bit per section index, so index 32 and above still fit. */
public final class SectionWatchMask {
    private final long[] words;

    public SectionWatchMask(int sectionCount) {
        int count = Math.max(1, sectionCount);
        words = new long[(count + 63) >>> 6];
    }

    public int wordCount() {
        return words.length;
    }

    public void set(int sectionIndex, boolean watched) {
        set(words, sectionIndex, watched);
    }

    public boolean get(int sectionIndex) {
        return get(words, sectionIndex);
    }

    public static int wordsFor(int sectionCount) {
        return (Math.max(1, sectionCount) + 63) >>> 6;
    }

    public static void set(long[] words, int sectionIndex, boolean watched) {
        if (words == null || sectionIndex < 0) return;
        int word = sectionIndex >>> 6;
        if (word >= words.length) return;
        long bit = 1L << (sectionIndex & 63);
        if (watched) words[word] |= bit;
        else words[word] &= ~bit;
    }

    public static boolean get(long[] words, int sectionIndex) {
        if (words == null || sectionIndex < 0) return false;
        int word = sectionIndex >>> 6;
        if (word >= words.length) return false;
        return (words[word] & (1L << (sectionIndex & 63))) != 0;
    }
}
