package gregtech.block.wood;

import net.minecraft.world.level.block.Block;

/** GT6-style tinted planks block. */
public class WoodPlanksBlock extends Block {
//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<WoodPlanksBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
*///?} else {
//?}
    private final WoodSpecies species;

    public WoodPlanksBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public WoodSpecies species() { return species; }
}
