package gregtech.integration.jade;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.machine.ITileEntityMold;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.data.MaterialPrefix;
import gregtech.blockentity.machine.MoldBasinBlockEntity;
import gregtech.blockentity.machine.MoldBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Temperature, fluid/solid state, and content for molds and mold basins. */
public enum MoldJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "mold");
    private static final String NBT_TEMPERATURE = "gt_mold_temp";
    private static final String NBT_MATERIAL = "gt_mold_material";
    private static final String NBT_AMOUNT = "gt_mold_amount";
    private static final String NBT_STATE = "gt_mold_state";
    private static final String NBT_SOLID_OUTPUT = "gt_mold_solid_output";
    private static final String NBT_MAX_TEMP = "gt_mold_max_temp";
    private static final String NBT_MOLD_OUTPUT = "gt_mold_output";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        ITileEntityMold mold;
        long maxTemp;
        boolean solidified = false;
        if (accessor.getBlockEntity() instanceof MoldBlockEntity be) {
            mold = be;
            maxTemp = be.spec().meltDownTemperatureK();
            tag.putLong(NBT_TEMPERATURE, be.getTemperature());
            solidified = be.isContentSolidified();
        } else if (accessor.getBlockEntity() instanceof MoldBasinBlockEntity be) {
            mold = be;
            maxTemp = be.spec().meltDownTemperatureK();
            tag.putLong(NBT_TEMPERATURE, be.getTemperature());
        } else {
            return;
        }

        tag.putLong(NBT_MAX_TEMP, maxTemp);

        GTMaterial material = mold.getMoldContentMaterial();
        long amount = mold.getMoldContentAmount();
        if (material != null && material.isValid() && amount > 0) {
            tag.putString(NBT_MATERIAL, material.getDisplayNameFallback());
            tag.putString(NBT_MATERIAL + "_key", material.getTranslationKey());
            tag.putLong(NBT_AMOUNT, amount);
            String state;
            if (solidified) {
                state = "solidified";
            } else if (getTemperature(accessor) >= material.getMeltingPoint()) {
                state = "molten";
            } else {
                state = "solid";
            }
            tag.putString(NBT_STATE, state);
        }

        // Check for solidified output
        ItemStack solidOutput = ItemStack.EMPTY;
        String moldOutputName = null;
        if (accessor.getBlockEntity() instanceof MoldBlockEntity be) {
            solidOutput = be.getSolidOutput();
            Object prefix = be.getMoldRecipePrefix();
            if (prefix instanceof MaterialPrefix mp) {
                moldOutputName = mp.getDisplayName();
                // The pinned original billet tab is English even in its Chinese language file.
                tag.putString(NBT_MOLD_OUTPUT + "_key", mp == MaterialPrefix.billet
                        ? "jade.gt6ce.mold.shape.billet" : "itemGroup.gt6ce." + mp.getRegistryName());
            } else if (prefix instanceof BlockMaterialPrefix bp) {
                moldOutputName = bp.getDisplayName();
                tag.putString(NBT_MOLD_OUTPUT + "_key", "itemGroup.gt6ce." + bp.getRegistryName());
            }
        } else if (accessor.getBlockEntity() instanceof MoldBasinBlockEntity be) {
            solidOutput = be.getSolidOutput();
        }
        if (!solidOutput.isEmpty()) {
//? if >=1.21.1 {
            /*tag.put(NBT_SOLID_OUTPUT, solidOutput.save(accessor.getLevel().registryAccess()));
*///?} else {
            tag.put(NBT_SOLID_OUTPUT, solidOutput.save(new CompoundTag()));
//?}
        }
        if (moldOutputName != null) {
            tag.putString(NBT_MOLD_OUTPUT, moldOutputName);
        }
    }

    private static long getTemperature(BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MoldBlockEntity be) return be.getTemperature();
        if (accessor.getBlockEntity() instanceof MoldBasinBlockEntity be) return be.getTemperature();
        return 0;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_TEMPERATURE, CompoundTag.TAG_LONG)) {
            return;
        }
        tooltip.add(Component.translatable("jade.gt6ce.mold.temperature",
                data.getLong(NBT_TEMPERATURE), data.getLong(NBT_MAX_TEMP)));

        if (data.contains(NBT_MATERIAL)) {
            String state = data.getString(NBT_STATE);
            tooltip.add(Component.translatable("jade.gt6ce.mold.content",
                    JadeText.translated(data, NBT_MATERIAL + "_key", NBT_MATERIAL),
                    format(data.getLong(NBT_AMOUNT)),
                    Component.translatable("jade.gt6ce.mold.state." + state)));
        }

        if (data.contains(NBT_SOLID_OUTPUT, CompoundTag.TAG_COMPOUND)) {
//? if >=1.21.1 {
            /*ItemStack output = ItemStack.parseOptional(accessor.getLevel().registryAccess(),data.getCompound(NBT_SOLID_OUTPUT));
*///?} else {
            ItemStack output = ItemStack.of(data.getCompound(NBT_SOLID_OUTPUT));
//?}
            if (!output.isEmpty()) {
                tooltip.add(Component.translatable("jade.gt6ce.mold.solid_output",
                        output.getHoverName(), output.getCount()));
            }
        }

        if (data.contains(NBT_MOLD_OUTPUT, CompoundTag.TAG_STRING)) {
            tooltip.add(Component.translatable("jade.gt6ce.mold.output",
                    JadeText.translated(data, NBT_MOLD_OUTPUT + "_key", NBT_MOLD_OUTPUT)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return Long.toString(value);
    }
}
