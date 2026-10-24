package gregtech.content.block;
//? if >=1.21.1 {
//?} else {
import gregtech.internal.material.MaterialPresentation;
//?}

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.internal.material.GTMaterial;
import gregtech.internal.prefix.BlockMaterialPrefix;
//? if >=1.21.1 {
//?} else {
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
//?}
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
//? if >=1.21.1 {
        /*return "block." + "gt6ce" + "." + prefix.getRegistryName();
*///?} else {
        return "block." + GregTech.NAMESPACE + "." + prefix.getRegistryName();
//?}
    }

    @Override
    public MutableComponent getName() {
//? if >=1.21.1 {
        /*return Component.translatable(getDescriptionId(), Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
*///?} else {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
//?}
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }




    @Override public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                                  net.minecraft.world.level.Explosion explosion) {
        return gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material) > 0
                ? 0 : super.getExplosionResistance(state, level, pos, explosion);
    }
    @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material) <= 0;
    }
    @Override public void onBlockExploded(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                          net.minecraft.world.level.Explosion explosion) {
        float power = gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material);
        if (power > 0) gregtech.content.hazard.MaterialBlockIgnition.detonate(level, pos, power);
        else super.onBlockExploded(state, level, pos, explosion);
    }
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return gregtech.content.hazard.MaterialBlockHazards.ignitionPower(prefix, material) > 0 ? 300 : 0;
    }
    @Override public void onCaughtFire(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                      net.minecraft.core.Direction side, net.minecraft.world.entity.LivingEntity igniter) {
        gregtech.content.hazard.MaterialBlockIgnition.detonate(level, pos,
                gregtech.content.hazard.MaterialBlockHazards.ignitionPower(prefix, material));
    }
    @Override public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState previous, boolean moved) {
        super.onPlace(state, level, pos, previous, moved);
        gregtech.content.hazard.MaterialBlockIgnition.schedule(level, pos, state, this);
    }
    @Override public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                          Block neighbor, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, from, moved);
        gregtech.content.hazard.MaterialBlockIgnition.schedule(level, pos, state, this);
    }
    @Override public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!gregtech.content.hazard.MaterialBlockIgnition.tick(level, pos, this)) super.tick(state, level, pos, random);
    }

    /** Falling dust blocks (GT6 {@code blockDust}). */
    public static Block falling(Properties properties, BlockMaterialPrefix prefix, GTMaterial material) {
        return new FallingMaterialBlock(properties, prefix, material);
    }

    private static final class FallingMaterialBlock extends FallingBlock implements MaterialBlockLike {
//? if >=1.21.1 {
        /*@Override
        protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
            return com.mojang.serialization.MapCodec.unit(this);
        }

*///?} else {
//?}
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
//? if >=1.21.1 {
            /*return "block." + "gt6ce" + "." + prefix.getRegistryName();
*///?} else {
            return "block." + GregTech.NAMESPACE + "." + prefix.getRegistryName();
//?}
        }

        @Override
        public MutableComponent getName() {
//? if >=1.21.1 {
            /*return Component.translatable(getDescriptionId(), Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
*///?} else {
            return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
//?}
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }

        @Override public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                                      net.minecraft.world.level.Explosion explosion) {
            return gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material) > 0
                    ? 0 : super.getExplosionResistance(state, level, pos, explosion);
        }
        @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {
            return gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material) <= 0;
        }
        @Override public void onBlockExploded(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                              net.minecraft.world.level.Explosion explosion) {
            float power = gregtech.content.hazard.MaterialBlockHazards.chainPower(prefix, material);
            if (power > 0) gregtech.content.hazard.MaterialBlockIgnition.detonate(level, pos, power);
            else super.onBlockExploded(state, level, pos, explosion);
        }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
            return gregtech.content.hazard.MaterialBlockHazards.ignitionPower(prefix, material) > 0 ? 300 : 0;
        }
        @Override public void onCaughtFire(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                          net.minecraft.core.Direction side, net.minecraft.world.entity.LivingEntity igniter) {
            gregtech.content.hazard.MaterialBlockIgnition.detonate(level, pos,
                    gregtech.content.hazard.MaterialBlockHazards.ignitionPower(prefix, material));
        }
        @Override public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState previous, boolean moved) {
            super.onPlace(state, level, pos, previous, moved);
            gregtech.content.hazard.MaterialBlockIgnition.schedule(level, pos, state, this);
        }
        @Override public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                              Block neighbor, BlockPos from, boolean moved) {
            super.neighborChanged(state, level, pos, neighbor, from, moved);
            gregtech.content.hazard.MaterialBlockIgnition.schedule(level, pos, state, this);
        }
        @Override public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
            if (!gregtech.content.hazard.MaterialBlockIgnition.tick(level, pos, this)) super.tick(state, level, pos, random);
        }
    }
}
