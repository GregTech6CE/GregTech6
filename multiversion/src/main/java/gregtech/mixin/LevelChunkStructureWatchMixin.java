package gregtech.mixin;

import gregtech.api.multiblock.tracking.SectionWatchMask;
import gregtech.api.multiblock.tracking.StructureChunkWatch;
import gregtech.api.multiblock.tracking.StructureWatchIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/** Marks a controller dirty when a watched cell's structure projection changes. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkStructureWatchMixin implements StructureChunkWatch {
    @Unique
    private long[] gt$watchedSections = new long[1];

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"))
    private void gt$structureChanged(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous == null) return;
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel().isClientSide || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!gt$structureWatched(chunk.getSectionIndex(pos.getY()))) return;
        StructureWatchIndex.onChanged(level, pos, previous, state);
    }

    @Override
    public void gt$structureWatch(int sectionIndex, boolean watched) {
        if (sectionIndex < 0) return;
        int words = SectionWatchMask.wordsFor(sectionIndex + 1);
        if (gt$watchedSections.length < words) gt$watchedSections = Arrays.copyOf(gt$watchedSections, words);
        SectionWatchMask.set(gt$watchedSections, sectionIndex, watched);
    }

    @Override
    public boolean gt$structureWatched(int sectionIndex) {
        return SectionWatchMask.get(gt$watchedSections, sectionIndex);
    }
}
