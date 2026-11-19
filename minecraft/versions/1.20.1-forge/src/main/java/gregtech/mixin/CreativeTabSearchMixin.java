package gregtech.mixin;

import gregtech.compat.emi.ForgeEmiCreativeTabs;
import java.util.Collection;
import java.util.Set;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge rebuilds its creative search trees when tab contents are built. */
@Mixin(CreativeModeTab.class)
public abstract class CreativeTabSearchMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("HEAD"))
    private void gt6ce$preserveTabContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo callback) {
        if (!ForgeEmiCreativeTabs.isCollecting()) return;
        var previousItems = displayItems;
        var previousSearchItems = displayItemsSearchTab;
        // The build replaces the collections, so EMI can keep its generated entries after restoration.
        ForgeEmiCreativeTabs.restoreAfterCollection(() -> {
            displayItems = previousItems;
            displayItemsSearchTab = previousSearchItems;
        });
    }

    @Inject(method = "rebuildSearchTree", at = @At("HEAD"), cancellable = true)
    private void gt6ce$skipTemporarySearchTree(CallbackInfo callback) {
        if (ForgeEmiCreativeTabs.isCollecting()) callback.cancel();
    }
}
