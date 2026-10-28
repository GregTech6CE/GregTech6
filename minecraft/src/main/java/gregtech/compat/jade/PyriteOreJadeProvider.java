package gregtech.compat.jade;

import gregtech.content.block.OreBlock;
import gregtech.content.material.generated.OreMaterials;
import gregtech.engine.prefix.BlockMaterialPrefix;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.CORE_OBJECT_NAME;
*///?} else {
import static snownee.jade.api.Identifiers.CORE_OBJECT_NAME;
//?}

/** Fool's gold changes only the observed ore name, never its material or recipe identity. */
public enum PyriteOreJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Nullable
    public static Component displayName(BlockState state) {
        if (!(state.getBlock() instanceof OreBlock ore) || ore.material() != OreMaterials.Pyrite
                || !BuiltInRegistries.BLOCK.getKey(ore).getNamespace().equals("gt6ce")) return null;
        if (ore.prefix() == BlockMaterialPrefix.oreSmall)
            return Component.translatable("jade.gt6ce.pyrite.small_ore");
        return ore.prefix() == BlockMaterialPrefix.ore
                ? Component.translatable("jade.gt6ce.pyrite.ore") : null;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Component name = displayName(accessor.getBlockState());
        if (name == null) return;
        tooltip.remove(CORE_OBJECT_NAME);
        tooltip.add(0, name.copy().withStyle(ChatFormatting.WHITE), CORE_OBJECT_NAME);
    }

    @Override public int getDefaultPriority() { return 1000; }
    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "pyrite_ore_name");
    }
}
