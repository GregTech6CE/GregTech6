package gregtech.compat.jade;

import gregtech.content.block.plant.BaleBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Uses the same moisture predicate as scheduled bale ageing, not a second approximation. */
public enum BaleJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    public static String hint(BaleBlock.Stage stage, Level level, BlockPos pos) {
        return switch (stage) {
            case FRESH -> level.dimension() != Level.NETHER && BaleBlock.isWet(level, pos)
                    ? "jade.gt6ce.bale.mold" : "jade.gt6ce.bale.dry";
            case MOLDY -> "jade.gt6ce.bale.rot";
            case DRY -> "jade.gt6ce.bale.stable";
            default -> "";
        };
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof BaleBlock bale)) return;
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        String key = hint(bale.stage(), level, pos);
        if (!key.isEmpty()) tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (bale.stage() == BaleBlock.Stage.FRESH && level.dimension() != Level.NETHER
                && (level.getBiome(pos).value().getBaseTemperature() < .65F
                || !level.isDay() || level.isRaining() || !level.canSeeSky(pos.above(2)))) {
            tooltip.add(Component.translatable("jade.gt6ce.bale.slow").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "bale_drying");
    }
}
