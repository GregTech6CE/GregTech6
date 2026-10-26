package gregtech.content.item;

import gregtech.content.block.misc.SpikeBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Same registered item identity; metadata-equivalent variants remain native stack state. */
public final class SpikeBlockItem extends BlockItem {
    public SpikeBlockItem(SpikeBlock block, Properties properties) { super(block, properties); }

    public static ItemStack variant(SpikeBlock block, SpikeBlock.Mode mode, boolean secondary) {
        var stack = new ItemStack(block);
//? if >=1.21.1 {
        /*stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of(
                        "mode", mode.getSerializedName(), "secondary", Boolean.toString(secondary))));
*///?} else {
        var state = new net.minecraft.nbt.CompoundTag();
        state.putString("mode", mode.getSerializedName());
        state.putString("secondary", Boolean.toString(secondary));
        stack.getOrCreateTag().put("BlockStateTag", state);
//?}
        return stack;
    }

    @Override public Component getName(ItemStack stack) {
        var block = (SpikeBlock) getBlock();
        int meta = (SpikeBlock.itemSecondary(stack) ? 8 : 0) + switch (SpikeBlock.itemMode(stack)) {
            case WALL -> 0;
            case OMNI -> 6;
            case FALLING -> 7;
        };
        // Original subtype translations already exist; retain source family names, not material guesses.
        return Component.translatable("gt.block.spikes."
                + block.family().name().toLowerCase(java.util.Locale.ROOT) + "." + meta);
    }
}
