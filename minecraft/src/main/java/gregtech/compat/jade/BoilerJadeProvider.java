package gregtech.compat.jade;

import gregtech.engine.machine.BoilerSpec;
import gregtech.content.energy.BoilerGauge;
import gregtech.content.energy.BoilerHazards;
import gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import net.minecraft.ChatFormatting;
import gregtech.content.block.machine.BoilerTankBlock;
import gregtech.content.blockentity.machine.BoilerTankBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.FluidView;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE;
*///?} else {
import static snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE;
//?}

/** Steam boiler: water, steam, heat, calcification, and hazard warnings. */
public enum BoilerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "steam_boiler");
    private static final String STEAM_AMT   = "gt_steam_amt";
    private static final String STEAM_CAP   = "gt_steam_cap";
    private static final String WATER_AMT   = "gt_water_amt";
    private static final String WATER_CAP   = "gt_water_cap";
    private static final String HEAT        = "gt_heat";
    private static final String MAX_HEAT    = "gt_max_heat";
    private static final String EFFICIENCY  = "gt_eff";
    private static final String WATER_VIEW  = "gt_boiler_water_view";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof OriginalLargeBoilerControllerBlockEntity large) {
            long steam = large.steamAmount();
            tag.putLong(STEAM_AMT, steam);
            tag.putLong(STEAM_CAP, large.variant().steamCapacity());
            tag.putLong(WATER_AMT, large.waterAmount());
            tag.putLong(WATER_CAP, large.directFluidHandler().getTankCapacity(0));
            var water = large.directFluidHandler().getFluidInTank(0);
            tag.put(WATER_VIEW, FluidView.writeDefault(JadeFluidObject.of(water.getFluid(), large.waterAmount()),
                    large.directFluidHandler().getTankCapacity(0)));
            tag.putLong(HEAT, large.storedHeat());
            tag.putLong(MAX_HEAT, large.heatCapacity());
            tag.putShort(EFFICIENCY, (short) large.efficiency());
            tag.putInt("gt_pressure", large.barometerValue());
            // Large boilers use a different blast scale from single-block boilers.
            tag.putFloat("gt_explosion_power", (float) (2 + Math.max(1, Math.sqrt(Math.max(0, steam)) / 1000.0)));
            tag.putFloat("gt_descaling_damage", BoilerHazards.descalingDamage(
                    large.storedHeat(), steam, large.efficiency(), large.barometerValue()));
            tag.putBoolean("gt_boiler_formed", large.isStructureOk());
            return;
        }
        if (!(accessor.getBlockEntity() instanceof BoilerTankBlockEntity boiler)) return;
        var steam = boiler.steamTank();
        var water = boiler.waterTank();
        BoilerSpec spec = boiler.spec();
        if (steam != null) {
            tag.putLong(STEAM_AMT, steam.getAmount());
            tag.putLong(STEAM_CAP, steam.getCapacity());
            tag.putLong("gt_steam_offer", gregtech.engine.energy.BoilerSteamOutput.offered(
                    steam.getAmount(), steam.capacity(), spec == null ? 0 : spec.steamOutput()));
        }
        if (water != null) {
            tag.putLong(WATER_AMT, water.getAmount());
            tag.putLong(WATER_CAP, water.getCapacity());
            tag.put(WATER_VIEW, FluidView.writeDefault(JadeFluidObject.of(water.getFluid().getFluid(), water.getAmount()),
                    water.getCapacity()));
        }
        tag.putLong(HEAT, boiler.storedHeat());
        if (spec != null) tag.putLong(MAX_HEAT, spec.heatCapacity());
        tag.putShort(EFFICIENCY, boiler.efficiency());
        tag.putInt("gt_pressure",boiler.barometerValue());
        tag.putFloat("gt_explosion_power",boiler.explosionPower());
        tag.putFloat("gt_contact_damage",boiler.contactDamage());
        tag.putFloat("gt_descaling_damage",boiler.descalingDamage());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(STEAM_CAP)) return;
        // Run after Jade's fluid provider: replace its pair of tank rows with water + pressure.
        // Rebuild the water row explicitly so two empty tanks cannot be merged into one by Jade.
        tooltip.remove(UNIVERSAL_FLUID_STORAGE);

        long steamAmt = data.getLong(STEAM_AMT);
        long steamCap = data.getLong(STEAM_CAP);
        int steamPct = BoilerGauge.percent(steamAmt, steamCap);
        String state = data.contains("gt_boiler_formed") && !data.getBoolean("gt_boiler_formed")
                ? "incomplete" : BoilerGauge.state(steamAmt, steamCap, data.getLong(WATER_AMT), data.getLong(HEAT));
        Component label = Component.translatable("jade.gt6ce.boiler.gauge",
                Component.translatable("jade.gt6ce.boiler.state." + state), steamPct);
        if (BoilerGauge.highPressure(steamAmt, steamCap)) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.boiler.warn_high_pressure").withStyle(ChatFormatting.RED));
        }
        JadeText.detail(tooltip, "jade.gt6ce.boiler.steam", steamAmt, steamCap, steamPct);
        if (data.contains("gt_steam_offer")) JadeText.add(tooltip, Component.translatable("gt.lang.energy.output").append(": " + data.getLong("gt_steam_offer") + " L/t"));

        long waterAmt = data.getLong(WATER_AMT);

        long heat = data.getLong(HEAT);
        long maxHeat = data.contains(MAX_HEAT) ? data.getLong(MAX_HEAT) : 0;
        if (maxHeat > 0) {
            JadeText.detail(tooltip, "jade.gt6ce.boiler.heat", heat, maxHeat);
        } else {
            JadeText.detail(tooltip, "jade.gt6ce.boiler.heat_nocap", heat);
        }

        short eff = data.getShort(EFFICIENCY);
        if (eff < 10000) {
            JadeText.detail(tooltip, "jade.gt6ce.boiler.calcified", JadeText.decimal(eff / 100.0, 1));
        }

        int pressure = data.getInt("gt_pressure");
        float power = data.getFloat("gt_explosion_power");
        JadeText.detail(tooltip, "jade.gt6ce.boiler.pressure", pressure);
        JadeText.add(tooltip, Component.translatable(pressure > 4 ? "jade.gt6ce.boiler.dismantle_danger" : "jade.gt6ce.boiler.dismantle_safe",
                JadeText.decimal(power, 1), JadeText.decimal(power * 2, 1)).withStyle(pressure > 4 ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (data.contains("gt_contact_damage")) JadeText.add(tooltip, Component.translatable("jade.gt6ce.boiler.contact_damage",JadeText.decimal(data.getFloat("gt_contact_damage"), 1))
                .withStyle(data.getFloat("gt_contact_damage") > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (eff < 10000) JadeText.add(tooltip, Component.translatable(pressure > 15 ? "jade.gt6ce.boiler.descaling_explosion"
                : "jade.gt6ce.boiler.descaling_damage",JadeText.decimal(data.getFloat("gt_descaling_damage"), 1))
                .withStyle(pressure > 15 || data.getFloat("gt_descaling_damage") > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));

        if (steamPct >= 100) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.boiler.warn_overpressure").withStyle(ChatFormatting.RED));
        }
        if (maxHeat > 0 && heat > maxHeat) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.boiler.warn_overheat").withStyle(ChatFormatting.RED));
        }
        if (waterAmt <= 0 && heat > 0) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.boiler.warn_dry").withStyle(ChatFormatting.RED));
        }
        if (data.contains(WATER_VIEW)) {
            var water = FluidView.readDefault(data.getCompound(WATER_VIEW));
            if (water != null) {
                var helper = IElementHelper.get();
                Component waterLabel = water.overrideText != null ? water.overrideText
                        : Component.translatable("jade.fluid", water.fluidName, water.current);
//? if >=1.21.1 {
                /*var box = BoxStyle.getNestedBox();
*///?} else {
                var box = BoxStyle.DEFAULT;
//?}
                tooltip.add(helper.progress(water.ratio, waterLabel, helper.progressStyle().overlay(water.overlay), box, true));
            }
        }
        tooltip.add(new BoilerGaugeElement(BoilerGauge.fill(steamAmt, steamCap), label, "running".equals(state)));
    }

    @Override public int getDefaultPriority() { return 1001; }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
