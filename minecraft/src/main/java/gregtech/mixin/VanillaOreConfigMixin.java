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
        for (var target : context.config().targetStates) {
            var id = BuiltInRegistries.BLOCK.getKey(target.state.getBlock());
            if (!id.getNamespace().equals("minecraft")) continue;
            // These two resources have independent switches, even when other vanilla ores are disabled.
            boolean disabled = switch (id.getPath()) {
                case "nether_gold_ore" -> !gregtech.GregTechConfig.Rule.GENERATE_NETHER_GOLD_ORE.enabled();
                case "ancient_debris" -> !gregtech.GregTechConfig.Rule.GENERATE_ANCIENT_DEBRIS.enabled();
                default -> id.getPath().endsWith("_ore") && gregtech.GregTechConfig.Rule.DISABLE_VANILLA_ORES.enabled();
            };
            if (disabled) {
                result.setReturnValue(false);
                return;
            }
        }
    }
}
