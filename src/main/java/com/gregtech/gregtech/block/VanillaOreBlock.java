package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/** The sixteen GT6 {@code BlockVanillaOresA} variants, each an ordinary 2U stone ore. */
public final class VanillaOreBlock extends IconSetBlock {
    /** In GT6 metadata order: material, required pickaxe level, stone-hardness multiplier, burn, XP. */
    public record Spec(String icon, String material, int harvestLevel, float hardnessMultiplier,
                       int burnLevel, int minXp, int maxXp) {}

    public static final List<Spec> SPECS = List.of(
            new Spec("ore_sulfur", "Sulfur", 0, 0.5F, 30, 0, 2),
            new Spec("ore_apatite", "Apatite", 0, 0.5F, 30, 0, 2),
            new Spec("ore_ruby", "Ruby", 2, 1.5F, 0, 3, 7),
            new Spec("ore_amber", "Amber", 1, 1.0F, 0, 3, 7),
            new Spec("ore_amethyst", "Amethyst", 2, 1.0F, 0, 3, 7),
            new Spec("ore_galena", "Galena", 1, 1.0F, 0, 2, 5),
            new Spec("ore_tetrahedrite", "Tetrahedrite", 1, 1.0F, 0, 2, 5),
            new Spec("ore_cassiterite", "Cassiterite", 1, 1.0F, 0, 2, 5),
            new Spec("ore_sheldonite", "Cooperite", 2, 1.5F, 0, 2, 5),
            new Spec("ore_pentlandite", "Pentlandite", 1, 1.0F, 0, 2, 5),
            new Spec("ore_scheelite", "Scheelite", 2, 1.5F, 0, 2, 5),
            new Spec("ore_rutile", "Rutile", 2, 1.5F, 0, 2, 5),
            new Spec("ore_bastnasite", "Bastnasite", 2, 1.5F, 0, 2, 5),
            new Spec("ore_graphite", "Graphite", 0, 0.5F, 30, 2, 5),
            new Spec("ore_pitchblende", "Pitchblende", 3, 2.0F, 0, 2, 5),
            new Spec("ore_borax", "Borax", 0, 0.5F, 0, 2, 5));

    public static Spec spec(String iconName) {
        for (Spec spec : SPECS) if (spec.icon().equals(iconName)) return spec;
        return null;
    }

    private final Spec spec;

    public VanillaOreBlock(Properties properties, String iconName, Spec spec) {
        super(properties, iconName);
        this.spec = spec;
    }

    public Spec oreSpec() { return spec; }
    public GTMaterial material() { return GTMaterialRegistry.get(spec.material()); }

    /** GT6: one raw ore, or 1..Fortune+1 raw ores; Silk Touch keeps the 2U block. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0)
            return List.of(new ItemStack(this));
        int fortune = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool));
        int amount = 1 + builder.getLevel().random.nextInt(fortune + 1);
        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material(), amount);
        return raw.isEmpty() ? List.of() : List.of(raw);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos,
                          int fortuneLevel, int silkTouchLevel) {
        return silkTouchLevel > 0 ? 0 : spec.minXp() + random.nextInt(spec.maxXp() - spec.minXp() + 1);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.burnLevel();
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 0;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                        net.minecraft.world.level.Explosion explosion) {
        return Blocks.STONE.getExplosionResistance();
    }
}
