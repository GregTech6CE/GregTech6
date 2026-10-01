package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.Map;

/** GT6 {@code BlockSands}: three falling black sands that keep their block when mined. */
public final class BlackSandBlock extends SandBlock {
    public record Spec(String material, int dustColor) {}

    private static final Map<String, Spec> SPECS = Map.of(
            "sand_magnetite", new Spec("Magnetite", 0x1E1E1E),
            "sand_basalt_magnetite", new Spec("BasalticMineralSand", 0x283228),
            "sand_granite_magnetite", new Spec("GraniticMineralSand", 0x283C3C));

    public static Spec spec(String iconName) {
        return SPECS.get(iconName);
    }

    private final Spec spec;

    public BlackSandBlock(Properties properties, Spec spec) {
        super(spec.dustColor(), properties);
        this.spec = spec;
    }

    public GTMaterial material() {
        return GTMaterialRegistry.get(spec.material());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                        net.minecraft.world.level.Explosion explosion) {
        return net.minecraft.world.level.block.Blocks.SAND.getExplosionResistance();
    }
}
