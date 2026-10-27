package gregtech.compat.jade;

import gregtech.content.block.machine.SmeltingCrucibleBlock;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
//?}
import gregtech.api.material.GTMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Temperature, buffered HU, and internal material content for smelting crucibles. */
public enum CrucibleJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "crucible");
    private static final String NBT_TEMPERATURE = "gt_crucible_temp";
    private static final String NBT_BUFFER = "gt_crucible_buffer";
    private static final String NBT_CONTENT = "gt_crucible_content";
    private static final String NBT_CACHE = "gt_crucible_cache";
    private static final String NBT_FILL = "gt_crucible_fill";
    private static final String NBT_MOLTEN = "gt_crucible_molten";
    private static final String NBT_WARNING = "gt_crucible_warning";
    private static final String NBT_MELT_LIMIT = "gt_crucible_melt_limit";
    private static final String NBT_DISPLAY_MAT = "gt_crucible_display_mat";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
//? if >=1.21.1 {
        /*SmeltingCrucibleEntity crucible = entity instanceof SmeltingCrucibleEntity direct ? direct
*///?} else {
        SmeltingCrucibleBlockEntity crucible = entity instanceof SmeltingCrucibleBlockEntity direct ? direct
//?}
                : entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity port
//? if >=1.21.1 {
/*// Version-specific branch.
                    ? (port.crucibleController() instanceof SmeltingCrucibleEntity owner ? owner : null) : null;
*///?} else {
// Version-specific branch.
                    ? port.crucibleController() : null;
//?}
        if (crucible == null) return;
        appendCrucibleData(tag, crucible);
    }

//? if >=1.21.1 {
    /*public static void appendCrucibleData(CompoundTag tag, SmeltingCrucibleEntity crucible) {
*///?} else {
    public static void appendCrucibleData(CompoundTag tag, SmeltingCrucibleBlockEntity crucible) {
//?}
        tag.putLong(NBT_TEMPERATURE, crucible.getTemperature());
        tag.putLong(NBT_BUFFER, crucible.getEnergyBuffer());
        var contents = new net.minecraft.nbt.ListTag();
        for (var stack : crucible.getContentView()) {
            if (stack.amount <= 0 || !stack.material.isValid()) continue;
            var row = new CompoundTag();
            row.putString("material", stack.material.getName());
            row.putLong("amount", stack.amount);
            row.putBoolean("molten", crucible.getTemperature() >= stack.material.getMeltingPoint());
            contents.add(row);
        }
        tag.put(NBT_CONTENT, contents);
        tag.putInt("gt_crucible_species", contents.size());
        long temperature = crucible.getTemperature();
        if (temperature > gregtech.util.GTEntityHelper.HEAT_THRESHOLD) {
            float damage = (temperature - gregtech.util.GTEntityHelper.ENV_TEMP) / 50.0F;
            boolean large = crucible instanceof gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
            tag.putFloat("gt_crucible_contact", large ? damage : Math.max(1, Math.min(10, damage)));
        }
        tag.putInt(NBT_FILL, crucible.getDisplayFillPercent());
        tag.putBoolean(NBT_MOLTEN, crucible.isDisplayedMolten());
        tag.putBoolean(NBT_WARNING, crucible.isMeltDownWarning());
        tag.putLong(NBT_MELT_LIMIT, crucible.getMeltDownLimitK());
        GTMaterial displayed = crucible.getDisplayedMaterial();
        if (displayed != null && displayed.isValid()) {
            tag.putString(NBT_DISPLAY_MAT, displayed.getLocalName());
        }
        ItemStack cache = crucible.getCacheStack();
        if (!cache.isEmpty()) {
//? if >=1.21.1 {
            /*tag.put(NBT_CACHE, cache.save(crucible.getLevel().registryAccess()));
*///?} else {
            tag.put(NBT_CACHE, cache.save(new CompoundTag()));
//?}
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_TEMPERATURE, CompoundTag.TAG_LONG)) {
            return;
        }
        long temperature = data.getLong(NBT_TEMPERATURE);
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.temperature", temperature)
                .append(" (" + (temperature - 273) + "°C)"));
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.omni.material_species", data.getInt("gt_crucible_species")));
        if (data.getFloat("gt_crucible_contact") > 0)
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.omni.hot_surface", data.getFloat("gt_crucible_contact"))
                    .withStyle(net.minecraft.ChatFormatting.RED));
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.buffer", format(data.getLong(NBT_BUFFER))));
        if (data.contains(NBT_MELT_LIMIT)) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.melt_limit", data.getLong(NBT_MELT_LIMIT)));
        }
        if (data.contains(NBT_FILL)) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.fill", data.getInt(NBT_FILL)));
        }
        if (data.getBoolean(NBT_WARNING)) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.warning"));
        }
        for (var entry : data.getList(NBT_CONTENT, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) entry;
            var material = gregtech.engine.material.GTMaterialRegistry.get(row.getString("material"));
            String phase = temperature >= material.getBoilingPoint() ? "gas" : row.getBoolean("molten") ? "molten" : "solid";
            var line = Component.literal(gregtech.client.MaterialTooltips.displayUnits(row.getLong("amount")) + " ")
                    .append(gregtech.engine.material.MaterialPresentation.name(material))
                    .append(" (").append(JadeText.phase(phase)).append(")");
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.content", line));
        }
        if (data.contains(NBT_CACHE, CompoundTag.TAG_COMPOUND)) {
//? if >=1.21.1 {
            /*ItemStack cache = ItemStack.parseOptional(accessor.getLevel().registryAccess(),data.getCompound(NBT_CACHE));
*///?} else {
            ItemStack cache = ItemStack.of(data.getCompound(NBT_CACHE));
//?}
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.crucible.cache",
                    cache.getHoverName(), cache.getCount()));
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
