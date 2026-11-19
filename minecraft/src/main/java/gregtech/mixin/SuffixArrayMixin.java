package gregtech.mixin;

import gregtech.client.SuffixArraySort;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.searchtree.SuffixArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SuffixArray.class)
public abstract class SuffixArrayMixin {
    @Shadow
    @Final
    private static boolean DEBUG_ARRAY;

    @Shadow
    @Final
    private IntList chars;

    @Shadow
    @Final
    private IntList wordStarts;

    @Shadow
    private IntList suffixToT;

    @Shadow
    private IntList offsets;

    @Shadow
    private int maxStringLength;

    @Shadow
    private void print() {
        throw new AssertionError();
    }

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void gregtech$sortSuffixes(CallbackInfo callback) {
        int[] positions = SuffixArraySort.positions(chars, maxStringLength);
        int[] sortedWords = new int[positions.length];
        int[] sortedOffsets = new int[positions.length];
        int start = 0;
        for (int word = 0; word < wordStarts.size(); word++) {
            int end = word + 1 < wordStarts.size() ? wordStarts.getInt(word + 1) : positions.length;
            for (int index = start; index < end; index++) {
                int position = positions[index];
                sortedWords[position] = word;
                sortedOffsets[position] = index - start;
            }
            start = end;
        }
        suffixToT = IntArrayList.wrap(sortedWords);
        offsets = IntArrayList.wrap(sortedOffsets);
        if (DEBUG_ARRAY) print();
        callback.cancel();
    }
}
