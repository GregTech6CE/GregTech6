package gregtech.mixin;

import java.util.List;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the code-built brewing table for the runtime probe. */
@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {
    @Accessor("containers")
    List<Ingredient> gregtech$containers();

    @Accessor("potionMixes")
    List<?> gregtech$potionMixes();

    @Accessor("containerMixes")
    List<?> gregtech$containerMixes();
}
