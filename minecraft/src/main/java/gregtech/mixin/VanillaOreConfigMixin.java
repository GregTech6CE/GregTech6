package gregtech.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OreFeature.class, ScatteredOreFeature.class})
public abstract class VanillaOreConfigMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, require = 1)
    private void gt6ce$vanillaOres(FeaturePlaceContext<OreConfiguration> context, CallbackInfoReturnable<Boolean> result) {
        if (!gregtech.GregTechConfig.Rule.DISABLE_VANILLA_ORES.enabled()) return;
        for (var target : context.config().targetStates) {
            var id = BuiltInRegistries.BLOCK.getKey(target.state.getBlock());
            if (id.getNamespace().equals("minecraft") && (id.getPath().endsWith("_ore") || id.getPath().equals("ancient_debris"))) {
                result.setReturnValue(false);
                return;
            }
        }
    }
}
