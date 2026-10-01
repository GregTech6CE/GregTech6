package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Map;

/** The nine GT6 {@code BlockRockOres} variants generated as overworld layers and Nether quartz. */
public final class RockOreBlock extends IconSetBlock implements DenseOreBlock {
    public record Spec(String material, int harvestLevel, float hardnessMultiplier, boolean flammable) {}

    private static final Map<String, Spec> SPECS = Map.ofEntries(
            Map.entry("ore_anthracite", new Spec("Coal", 0, 0.5F, true)),
            Map.entry("ore_lignite", new Spec("Lignite", 0, 0.5F, true)),
            Map.entry("ore_salt", new Spec("NaCl", 1, 1.0F, false)),
            Map.entry("ore_rocksalt", new Spec("KCl", 1, 1.0F, false)),
            Map.entry("ore_bauxite", new Spec("Bauxite", 2, 2.0F, false)),
            Map.entry("ore_oil", new Spec("Oilshale", 1, 0.5F, true)),
            Map.entry("ore_gypsum", new Spec("Gypsum", 0, 0.5F, false)),
            Map.entry("ore_milkyquartz", new Spec("MilkyQuartz", 1, 1.0F, false)),
            Map.entry("ore_netherquartz", new Spec("NetherQuartz", 1, 1.0F, false)));

    public static Spec spec(String iconName) {
        return SPECS.get(iconName);
    }

    private final Spec spec;

    public RockOreBlock(Properties properties, String iconName, Spec spec) {
        super(properties, iconName);
        this.spec = spec;
    }

    public Spec oreSpec() {
        return spec;
    }

    public GTMaterial material() {
        return GTMaterialRegistry.get(spec.material());
    }

    /** GT6 drops two raw ores, or 2..(2N+3) with Fortune N. Silk Touch keeps the block. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            return List.of(new ItemStack(this));
        }
        int fortune = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool));
        int amount = fortune == 0 ? 2 : 2 + builder.getLevel().random.nextInt(fortune * 2 + 2);
        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material(), amount);
        return raw.isEmpty() ? List.of() : List.of(raw);
    }

    /** GT6 rock ores have a one-in-eight chance to grant one XP. */
    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos,
                          int fortuneLevel, int silkTouchLevel) {
        return silkTouchLevel == 0 && random.nextInt(8) == 0 ? 1 : 0;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                               net.minecraft.core.Direction face) {
        return spec.flammable() ? 30 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                  net.minecraft.core.Direction face) {
        return 0;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                        net.minecraft.world.level.Explosion explosion) {
        return net.minecraft.world.level.block.Blocks.STONE.getExplosionResistance();
    }
}
