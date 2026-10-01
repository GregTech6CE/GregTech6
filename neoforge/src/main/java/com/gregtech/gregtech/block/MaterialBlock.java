package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.Collections;
import java.util.List;

/** One registered block per (block prefix, material) pair. */
public class MaterialBlock extends Block implements MaterialBlockLike {
    private final BlockMaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialBlock(Properties properties, BlockMaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

    public static BlockMaterialPrefix prefix(Block block) {
        return block instanceof MaterialBlockLike materialBlock ? materialBlock.prefix() : null;
    }

    public static GTMaterial material(Block block) {
        return block instanceof MaterialBlockLike materialBlock ? materialBlock.material() : null;
    }

    @Override
    public BlockMaterialPrefix prefix() {
        return prefix;
    }

    @Override
    public GTMaterial material() {
        return material;
    }

    @Override
    public String getDescriptionId() {
        return "block." + "gregtech" + "." + prefix.getRegistryName();
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable(getDescriptionId(), Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }



    /** Falling dust blocks (GT6 {@code blockDust}). */
    public static Block falling(Properties properties, BlockMaterialPrefix prefix, GTMaterial material) {
        return new FallingMaterialBlock(properties, prefix, material);
    }

    private static final class FallingMaterialBlock extends FallingBlock implements MaterialBlockLike {
        @Override
        protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
            return com.mojang.serialization.MapCodec.unit(this);
        }

        private final BlockMaterialPrefix prefix;
        private final GTMaterial material;

        FallingMaterialBlock(Properties properties, BlockMaterialPrefix prefix, GTMaterial material) {
            super(properties);
            this.prefix = prefix;
            this.material = material;
        }

        @Override
        public BlockMaterialPrefix prefix() {
            return prefix;
        }

        @Override
        public GTMaterial material() {
            return material;
        }

        @Override
        public String getDescriptionId() {
            return "block." + "gregtech" + "." + prefix.getRegistryName();
        }

        @Override
        public MutableComponent getName() {
            return Component.translatable(getDescriptionId(), Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }
}
