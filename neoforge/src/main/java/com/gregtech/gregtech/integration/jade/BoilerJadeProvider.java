package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.block.machine.BoilerTankBlock;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Steam boiler: water, steam, heat, calcification, and hazard warnings. */
public enum BoilerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "steam_boiler");
    private static final String STEAM_AMT   = "gt_steam_amt";
    private static final String STEAM_CAP   = "gt_steam_cap";
    private static final String WATER_AMT   = "gt_water_amt";
    private static final String WATER_CAP   = "gt_water_cap";
    private static final String HEAT        = "gt_heat";
    private static final String MAX_HEAT    = "gt_max_heat";
    private static final String EFFICIENCY  = "gt_eff";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BoilerTankBlockEntity boiler)) return;
        var steam = boiler.steamTank();
        var water = boiler.waterTank();
        BoilerSpec spec = boiler.spec();
        if (steam != null) {
            tag.putLong(STEAM_AMT, steam.getAmount());
            tag.putLong(STEAM_CAP, steam.getCapacity());
        }
        if (water != null) {
            tag.putLong(WATER_AMT, water.getAmount());
            tag.putLong(WATER_CAP, water.getCapacity());
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

        long steamAmt = data.getLong(STEAM_AMT);
        long steamCap = data.getLong(STEAM_CAP);
        int steamPct = steamCap > 0 ? (int) (steamAmt * 100 / steamCap) : 0;
        tooltip.add(Component.translatable("jade.gregtech.boiler.steam",
                steamAmt, steamCap, steamPct));

        long waterAmt = data.getLong(WATER_AMT);
        long waterCap = data.getLong(WATER_CAP);
        int waterPct = waterCap > 0 ? (int) (waterAmt * 100 / waterCap) : 0;
        tooltip.add(Component.translatable("jade.gregtech.boiler.water",
                waterAmt, waterCap, waterPct));

        long heat = data.getLong(HEAT);
        long maxHeat = data.contains(MAX_HEAT) ? data.getLong(MAX_HEAT) : 0;
        if (maxHeat > 0) {
            tooltip.add(Component.translatable("jade.gregtech.boiler.heat",
                    heat, maxHeat));
        } else {
            tooltip.add(Component.translatable("jade.gregtech.boiler.heat_nocap", heat));
        }

        short eff = data.getShort(EFFICIENCY);
        if (eff < 10000) {
            tooltip.add(Component.translatable("jade.gregtech.boiler.calcified",
                    String.format("%.1f", eff / 100.0)));
        }

        int pressure = data.getInt("gt_pressure");
        float power = data.getFloat("gt_explosion_power");
        tooltip.add(Component.translatable("jade.gregtech.boiler.pressure",pressure));
        tooltip.add(Component.translatable(pressure > 4 ? "jade.gregtech.boiler.dismantle_danger" : "jade.gregtech.boiler.dismantle_safe",
                power, power * 2));
        tooltip.add(Component.translatable("jade.gregtech.boiler.contact_damage",data.getFloat("gt_contact_damage")));
        if (eff < 10000) tooltip.add(Component.translatable(pressure > 15 ? "jade.gregtech.boiler.descaling_explosion"
                : "jade.gregtech.boiler.descaling_damage",data.getFloat("gt_descaling_damage")));

        if (steamPct >= 100) {
            tooltip.add(Component.translatable("jade.gregtech.boiler.warn_overpressure"));
        }
        if (maxHeat > 0 && heat >= maxHeat) {
            tooltip.add(Component.translatable("jade.gregtech.boiler.warn_overheat"));
        }
        if (waterAmt <= 0 && heat > 0) {
            tooltip.add(Component.translatable("jade.gregtech.boiler.warn_dry"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
