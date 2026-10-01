package com.gregtech.gregtech.content.bumble;

import com.gregtech.gregtech.block.IconSetPlantBlock;
import com.gregtech.gregtech.block.misc.DiggableBlock;
import com.gregtech.gregtech.block.plant.BedrockFlowerBlock;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.StoneVariantFlags;
import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * The species-dependent work site required by GT6 {@code MultiItemBumbles.bumbleCanProduce:216-349}.
 * A bumbliary searches a cube of radius three; the advanced machine searches radius one.
 * Missing chunks are never loaded just to make a bee work.
 *
 * <p>The original oxygen check is only restrictive in Galacticraft dimensions. Without that
 * optional integration it always succeeds in GT6, as it does here. Old optional-mod flowers and
 * crops are matched only when an equivalent block exists in this port (see the switch below).</p>
 */
public final class BumbleWorkplace {
    private static final Set<String> MAGICAL_BIOMES = Set.of(
            "magical_forest", "eldritch", "enchanted_forest", "mystic_grove", "alfheim",
            "tainted_land", "eerie", "wyvern_biome", "ominous_woods");

    private BumbleWorkplace() {}

    public static boolean hasWorkplace(Level level, BlockPos machine, int speciesId, boolean advanced) {
        int tier = speciesId / 100;
        if (tier == 4 || tier == 202) {
            // GT6's no-EtFuturum branch: the End biome itself is enough, without a nearby block.
            if (level.dimension() == Level.END) return true;
        }
        if (tier == 2 && magicalBiome(level, machine)) return true;

        int distance = advanced ? 1 : 3;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -distance; y <= distance; y++) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    cursor.set(machine.getX() + x, machine.getY() + y, machine.getZ() + z);
                    if (!level.hasChunkAt(cursor)) continue;
                    if (matches(level.getBlockState(cursor), tier)) return true;
                }
            }
        }
        return false;
    }

    /** Kept separately from the search so all GT6 bee tiers can be verified without RNG. */
    public static boolean matches(BlockState state, int tier) {
        Block block = state.getBlock();
        return switch (tier) {
            case 1 -> state.getFluidState().is(FluidTags.WATER);
            case 2 -> isRainbowLeaf(block) || isThaumicPlant(block);
            case 3, 200 -> block == Blocks.NETHER_WART || block == Blocks.WITHER_ROSE;
            case 4, 202 -> block == Blocks.END_PORTAL || block == Blocks.DRAGON_EGG;
            case 5, 203 -> isStone(state);
            case 6 -> block == Blocks.COCOA;
            case 7, 201 -> block == Blocks.ICE || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK
                    || block == Blocks.PACKED_ICE;
            case 8 -> block == Blocks.MYCELIUM || block == Blocks.RED_MUSHROOM
                    || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM_BLOCK
                    || block == Blocks.BROWN_MUSHROOM_BLOCK;
            case 9, 105 -> isCactusOrDesertFlower(block);
            case 100 -> block == Blocks.CLAY || block instanceof DiggableBlock diggable
                    && diggable.variant().isClay();
            case 101 -> block instanceof TreeHoleBlock hole && hole.species() == WoodSpecies.RUBBER;
            case 103 -> block == Blocks.SOUL_SAND;
            default -> isFlower(block);
        };
    }

    private static boolean magicalBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> MAGICAL_BIOMES.contains(key.location().getPath()))
                .orElse(false);
    }

    private static boolean isRainbowLeaf(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getNamespace().equals("gregtech")
                && (id.getPath().equals("leaves_rainbowood")
                || id.getPath().equals("leaves_opaque_rainbowood"));
    }

    private static boolean isThaumicPlant(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getNamespace().equals("thaumcraft")
                && (id.getPath().equals("blockcustomplant") || id.getPath().equals("block_custom_plant"));
    }

    private static boolean isStone(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof GTStoneBlock gtStone) {
            return StoneVariantFlags.spawnsCreatures(gtStone.variant())
                    || StoneVariantFlags.isMossy(gtStone.variant());
        }
        return block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE
                || block == Blocks.MOSSY_STONE_BRICKS || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean isCactusOrDesertFlower(Block block) {
        if (block == Blocks.CACTUS || block instanceof BedrockFlowerBlock) {
            return true;
        }
        return block instanceof FlowerPotBlock pot && pot.getContent() == Blocks.CACTUS;
    }

    private static boolean isFlower(Block block) {
        if (block instanceof FlowerPotBlock pot) return isUnpottedFlower(pot.getContent());
        return isUnpottedFlower(block);
    }

    private static boolean isUnpottedFlower(Block block) {
        if (block instanceof FlowerBlock) return true;
        if (block == Blocks.SUNFLOWER || block == Blocks.LILAC || block == Blocks.ROSE_BUSH
                || block == Blocks.PEONY) return true;
        return block instanceof IconSetPlantBlock plant && plant.iconName().startsWith("flower_")
                && !plant.iconName().equals("flower_hexalily");
    }
}
