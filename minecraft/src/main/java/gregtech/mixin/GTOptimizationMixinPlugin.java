package gregtech.mixin;

import java.util.List;
import java.util.Set;
//? if >=1.21.1 {
/*import net.neoforged.fml.loading.LoadingModList;
*///?} else {
import net.minecraftforge.fml.loading.LoadingModList;
//?}
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Select state storage before applying mixins, without loading optional mod or game classes. */
public final class GTOptimizationMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.equals("gregtech.mixin.GTStateTransitionsMixin")) return true;
        // FerriteCore replaces both initialization and state lookup. Cancelling its initializer
        // leaves its lookup uninitialized; adjusting injection priority cannot make them coexist.
        // Defer the entire mixin when FerriteCore is present, including if its options are disabled.
        var mods = LoadingModList.get();
        return mods != null && mods.getModFileById("ferritecore") == null;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
