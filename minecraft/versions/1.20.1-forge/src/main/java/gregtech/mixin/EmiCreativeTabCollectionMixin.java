package gregtech.mixin;

import gregtech.compat.emi.ForgeEmiCreativeTabs;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** EMI sends creative-tab collection to the client thread where the tab state is owned. */
@Pseudo
@Mixin(targets = "dev.emi.emi.registry.EmiStackList", remap = false)
public abstract class EmiCreativeTabCollectionMixin {
    @Redirect(
            method = "reload",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Minecraft;submit(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
                            remap = true),
            require = 0,
            remap = false)
    private static CompletableFuture<?> gt6ce$collectTemporaryTabs(Minecraft client, Supplier<?> collection) {
        return client.submit(() -> ForgeEmiCreativeTabs.collect(collection));
    }
}
