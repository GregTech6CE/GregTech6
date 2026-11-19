package gregtech.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.searchtree.FullTextSearchTree;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FullTextSearchTree.class)
public abstract class RecipeBookSearchMixin<T> {
    @Shadow
    @Final
    private List<T> contents;

    @ModifyArg(
            method = "refresh()V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/searchtree/PlainTextSearchTree;create(Ljava/util/List;Ljava/util/function/Function;)Lnet/minecraft/client/searchtree/PlainTextSearchTree;"),
            index = 1,
            require = 0)
    private Function<T, Stream<String>> gregtech$deduplicateRecipeTerms(Function<T, Stream<String>> filler) {
        if (contents.isEmpty() || !(contents.get(0) instanceof RecipeCollection)) return filler;
        return collection -> {
            var seen = new HashSet<String>();
            // Match the suffix array's case folding, and pass the first occurrence to the original builder.
            return filler.apply(collection).sequential().filter(text -> seen.add(text.toLowerCase(Locale.ROOT)));
        };
    }
}
