package gregtech.content.block.energy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Structural casing for reactor multiblocks. Simple inert block. */
public class ReactorCasingBlock extends Block {
//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
*///?} else {
//?}
    public ReactorCasingBlock() {
        super(Properties.of()
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
