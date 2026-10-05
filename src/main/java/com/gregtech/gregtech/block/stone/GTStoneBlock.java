package com.gregtech.gregtech.block.stone;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;
import java.util.List;

/** GT decorative stone block (GT6 {@code BlockStonesGT} / {@code BlockStones}). */
public final class GTStoneBlock extends Block {
    private final StoneType stoneType;
    private final StoneVariant variant;
    private final GTMaterial stoneMaterial;

    public GTStoneBlock(StoneType stoneType, StoneVariant variant) {
        super(StoneBlockProperties.properties(stoneType, variant));
        this.stoneType = stoneType;
        this.variant = variant;
        this.stoneMaterial = stoneType.material();
    }

    public StoneType stoneType() {
        return stoneType;
    }

    public StoneVariant variant() {
        return variant;
    }

    public GTMaterial stoneMaterial() {
        return stoneMaterial;
    }

    public String registryId() {
        return stoneType.registryId() + "_" + variant.registrySuffix();
    }

    /** Shared baked-model id under {@code models/block/stones/}. */
    public String modelId() {
        return "block/stones/" + registryId();
    }

    @Override
    public String getDescriptionId() {
        return "block.gregtech." + registryId();
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(stoneMaterial), variant.displayName());
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (stoneType.witherProof() && entity instanceof WitherBoss) {
            return false;
        }
        return super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean silkTouch = tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
        if (variant == StoneVariant.STONE && !silkTouch) {
            Block cobble = GTBlocks.getStone(stoneType, StoneVariant.COBBLE);
            if (cobble != null) {
                return List.of(new ItemStack(cobble));
            }
        }
        return List.of(new ItemStack(this));
    }



    @Override
    public boolean isSignalSource(BlockState state) {
        return variant == StoneVariant.BRICKS_REDSTONE;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return variant == StoneVariant.BRICKS_REDSTONE ? 15 : 0;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return StoneVariantFlags.isMossy(variant);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!StoneVariantFlags.isMossy(variant)) {
            return;
        }
        if (!isBurning(level, pos)) {
            return;
        }
        BlockState replacement = switch (variant) {
            case COBBLE_MOSSY -> GTBlocks.getStoneState(stoneType, StoneVariant.COBBLE);
            case BRICKS_MOSSY -> GTBlocks.getStoneState(stoneType, StoneVariant.BRICKS);
            default -> null;
        };
        if (replacement != null) {
            level.setBlockAndUpdate(pos, replacement);
        }
    }

    private static boolean isBurning(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState adjacent = level.getBlockState(pos.relative(direction));
            if (adjacent.is(net.minecraft.world.level.block.Blocks.FIRE)
                    || adjacent.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReplaceableOreGen() {
        return variant == StoneVariant.STONE;
    }
}
