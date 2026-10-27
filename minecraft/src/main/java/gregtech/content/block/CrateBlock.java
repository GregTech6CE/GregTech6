package gregtech.content.block;

import net.minecraft.world.level.block.FallingBlock;

/** The ordinary empty crate falls when unsupported; loot and bottle crates keep their own rules. */
public final class CrateBlock extends FallingBlock {
    public CrateBlock(Properties properties) { super(properties); }
//? if >=1.21.1 {
    /*@Override protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?}
}
