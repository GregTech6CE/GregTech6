package gregtech.content.item;

import gregtech.content.block.misc.SpikeBlock;
import net.minecraft.world.item.BlockItem;

/** Each material and spike form has its own block item; stacks carry no variant state. */
public final class SpikeBlockItem extends BlockItem {
    public SpikeBlockItem(SpikeBlock block, Properties properties) {
        super(block, properties);
    }
}
