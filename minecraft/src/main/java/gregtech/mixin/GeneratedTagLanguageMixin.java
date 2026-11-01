package gregtech.mixin;

import gregtech.engine.material.TagNameTranslations;
import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resource-pack translations win; generated material tags share the active language's templates. */
@Mixin(ClientLanguage.class)
public abstract class GeneratedTagLanguageMixin {
    @Shadow
    @Final
    private Map<String, String> storage;

    @Unique
    private volatile TagNameTranslations gt6ce$tagNames;

    @Unique
    private TagNameTranslations gt6ce$tagVocabulary() {
        TagNameTranslations names = gt6ce$tagNames;
        if (names == null) {
            synchronized (this) {
                names = gt6ce$tagNames;
                if (names == null) gt6ce$tagNames = names = new TagNameTranslations(storage);
            }
        }
        return names;
    }

    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void gt6ce$tagName(String key, String fallback, CallbackInfoReturnable<String> callback) {
        if (!key.startsWith("tag.") || storage.containsKey(key)) return;
        String name = gt6ce$tagVocabulary().resolve(key);
        if (name != null) callback.setReturnValue(name);
    }

    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private void gt6ce$hasTagName(String key, CallbackInfoReturnable<Boolean> callback) {
        if (key.startsWith("tag.")
                && !storage.containsKey(key)
                && gt6ce$tagVocabulary().resolve(key) != null) callback.setReturnValue(true);
    }
}
