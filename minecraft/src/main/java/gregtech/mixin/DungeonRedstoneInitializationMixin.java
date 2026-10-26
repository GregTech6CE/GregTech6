package gregtech.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shape postprocessing alone does not initialize the power of generated dungeon wires. */
@Mixin(LevelChunk.class)
public abstract class DungeonRedstoneInitializationMixin {
    @Unique private final java.util.List<BlockPos> gregtech$dungeonWires = new java.util.ArrayList<>();

    @Inject(method = "postProcessGeneration", at = @At("HEAD"), require = 1)
    private void gregtech$rememberWires(CallbackInfo callback) {
        var chunk = (LevelChunk) (Object) this;
        gregtech$dungeonWires.clear();
        var sections = chunk.getPostProcessing();
        for (int section = 0; section < sections.length; section++) {
            if (sections[section] == null) continue;
            for (short offset : sections[section]) {
                var pos = ProtoChunk.unpackOffsetCoordinates(offset, chunk.getSectionYFromSectionIndex(section), chunk.getPos());
                if (chunk.getBlockState(pos).is(Blocks.REDSTONE_WIRE)
                        && BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos.below()).getBlock()).getNamespace().equals("gt6ce"))
                    gregtech$dungeonWires.add(pos);
            }
        }
    }

    @Inject(method = "postProcessGeneration", at = @At("TAIL"), require = 1)
    private void gregtech$powerWires(CallbackInfo callback) {
        var chunk = (LevelChunk) (Object) this;
        var level = chunk.getLevel();
        for (var pos : gregtech$dungeonWires) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.REDSTONE_WIRE))
                level.neighborChanged(pos, Blocks.REDSTONE_WIRE, pos);
        }
        gregtech$dungeonWires.clear();
    }
}
