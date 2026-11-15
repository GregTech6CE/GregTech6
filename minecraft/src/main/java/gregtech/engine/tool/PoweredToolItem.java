package gregtech.engine.tool;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Contract for actual powered tool items, preserving manual dispatch semantics. */
public interface PoweredToolItem {
    GTToolType interactionType(ItemStack stack);

    boolean canInteract(ItemStack stack);

    void consumeInteractionEnergy(ItemStack stack, long amount, LivingEntity user);

    /** Wrench-style machine harvest check (target list plus material quality), not general tool harvest. */
    boolean canHarvest(ItemStack stack, BlockState state);

    /** Wrench-style machine harvest speed: definition speed multiplier times head material speed. */
    float harvestSpeed(ItemStack stack);
}
