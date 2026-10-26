package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
//?}
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * GT6 {@code BlockCrystalOres}: the twelve Nether crystal variants are dense ores, not
 * decorative icon blocks. Each drops two raw ores without Fortune and gives 3–6 XP.
 */
public final class CrystalOreBlock extends IconSetBlock implements DenseOreBlock {
    private final String materialName;

    public CrystalOreBlock(Properties properties, String iconName, String materialName) {
        super(properties, iconName);
        this.materialName = materialName;
    }

    public GTMaterial material() {
        return GTMaterialRegistry.get(materialName);
    }

    /** GT6 {@code BlockCrystalOres.getDrops}: Fortune N yields 2..(2N+3) raw ores. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
//? if >=1.21.1 {
        /*if (gregtech.content.worldgen.OreHarvest.from(builder).silkTouch()) {
*///?} else {
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
//?}
            return List.of(new ItemStack(this));
        }
//? if >=1.21.1 {
        /*int fortune = Math.max(0, gregtech.content.worldgen.OreHarvest.from(builder).fortune());
*///?} else {
        int fortune = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool));
//?}
        int amount = fortune == 0 ? 2 : 2 + builder.getLevel().random.nextInt(fortune * 2 + 2);
        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material(), amount);
        return raw.isEmpty() ? List.of() : List.of(raw);
    }

    /** GT6 {@code BlockCrystalOres.getExpDrop}: 3–6 XP, independent of Fortune. */
    @Override
//? if >=1.21.1 {
    /*public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                          net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                          net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        RandomSource random = level.getRandom();
        int silkTouchLevel = gregtech.content.worldgen.OreHarvest.from(level, tool).silkTouch() ? 1 : 0;
*///?} else {
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos,
                          int fortuneLevel, int silkTouchLevel) {
//?}
        return silkTouchLevel > 0 ? 0 : 3 + random.nextInt(4);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                        net.minecraft.world.level.Explosion explosion) {
        // Original delegates both hardness and blast resistance to vanilla glowstone.
        return net.minecraft.world.level.block.Blocks.GLOWSTONE.getExplosionResistance();
    }
}
