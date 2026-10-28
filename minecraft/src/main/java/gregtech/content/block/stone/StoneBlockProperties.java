package gregtech.content.block.stone;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Forge block construction boundary for the shared stone descriptors. */
public final class StoneBlockProperties {
    private StoneBlockProperties() {}

    public static BlockBehaviour.Properties properties(StoneType stoneType, StoneVariant variant) {
        float hardnessMult = stoneType.hardnessMultiplier();
        if (variant == StoneVariant.BRICKS_REINFORCED) hardnessMult *= 2.0F;
        float hardness = Math.max(0.1F, Blocks.STONE.defaultDestroyTime() * hardnessMult);
        float resistance = Math.max(0.1F, 6.0F * stoneType.resistanceMultiplier());
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .isValidSpawn((state, level, pos, entity) -> StoneVariantFlags.spawnsCreatures(variant)
                        && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP));
    }
}
