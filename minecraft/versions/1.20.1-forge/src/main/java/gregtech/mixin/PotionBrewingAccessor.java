package gregtech.mixin;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the code-built brewing table for the runtime probe. */
@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {
    @Accessor("POTION_MIXES")
    static List<PotionBrewing.Mix<Potion>> gregtech$potionMixes() {
        throw new AssertionError();
    }

    @Accessor("CONTAINER_MIXES")
    static List<PotionBrewing.Mix<Item>> gregtech$containerMixes() {
        throw new AssertionError();
    }

    @Accessor("ALLOWED_CONTAINERS")
    static List<Ingredient> gregtech$allowedContainers() {
        throw new AssertionError();
    }
}
