package gregtech.mixin;

import gregtech.api.multiblock.tracking.StructureScanProbe;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code Level.hasChunkAt(BlockPos)} is a default method on an interface, which this Mixin
 * version cannot inject. The server implementation that default method reaches is here.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheStructureProbeMixin {
    @Inject(method = "hasChunk(II)Z", at = @At("HEAD"))
    private void gt$probeChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        StructureScanProbe.noteChunkQuery(x, z);
    }
}
