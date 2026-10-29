package gregtech.mixin;

import gregtech.content.block.OreBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ore debris samples both model layers; the host stone must never inherit the ore's tint. */
@Mixin(TerrainParticle.class)
public abstract class OreTerrainParticleMixin extends TextureSheetParticle {
    protected OreTerrainParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL"))
    private void gt6ce$oreFragment(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            BlockState state,
            BlockPos pos,
            CallbackInfo ci) {
        gt6ce$sampleLayer(state, pos);
    }

    // Both loaders refresh the sprite after constructing hit/break particles.
    @Inject(method = "updateSprite", at = @At("RETURN"), remap = false)
    private void gt6ce$refreshOreFragment(BlockState state, BlockPos pos, CallbackInfoReturnable<?> ci) {
        gt6ce$sampleLayer(state, pos == null ? BlockPos.containing(x, y, z) : pos);
    }

    @Unique
    private void gt6ce$sampleLayer(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof OreBlock)) return;
        var client = Minecraft.getInstance();
        var model = client.getBlockRenderer().getBlockModel(state);
        var quads = model.getQuads(state, Direction.getRandom(random), random);
        if (quads.isEmpty()) return;
        var quad = quads.get(random.nextInt(quads.size()));
        setSprite(quad.getSprite());
        int color =
                quad.isTinted() ? client.getBlockColors().getColor(state, level, pos, quad.getTintIndex()) : 0xffffff;
        setColor(.6f * (color >> 16 & 255) / 255f, .6f * (color >> 8 & 255) / 255f, .6f * (color & 255) / 255f);
    }
}
