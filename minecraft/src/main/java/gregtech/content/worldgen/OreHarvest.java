package gregtech.content.worldgen;

import gregtech.engine.stack.EnchantmentCompat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Fortune / silk from a block loot tool. */
public record OreHarvest(int fortune, boolean silkTouch) {
    public static OreHarvest from(LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        return from(params.getLevel(), tool);
    }

    public static OreHarvest from(LevelAccessor level, ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return new OreHarvest(0, false);
        }
        int fortune = EnchantmentCompat.fortune(tool, level);
        int silk = EnchantmentCompat.silkTouch(tool, level);
        return new OreHarvest(fortune, silk > 0);
    }
}
