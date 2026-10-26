package gregtech.compat.jade;

import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTFluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.CORE_OBJECT_NAME;
*///?} else {
import static snownee.jade.api.Identifiers.CORE_OBJECT_NAME;
//?}

/** World fluids use the same translated name as the fluid catalog, including source/flowing water. */
public enum WorldFluidNameJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var entry = GTFluids.entryForFluid(accessor.getBlockState().getFluidState().getType());
        if (entry == null) return;
        var name = GTFluidType.describe(entry, GTFluids.sanitizePath(entry.registryName())).copy()
                .withStyle(ChatFormatting.WHITE);
        tooltip.remove(CORE_OBJECT_NAME);
        tooltip.add(0, name, CORE_OBJECT_NAME);
    }

    @Override public int getDefaultPriority() { return 1000; }
    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "world_fluid_name");
    }
}
