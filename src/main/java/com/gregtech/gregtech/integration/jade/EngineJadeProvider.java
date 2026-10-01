package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Jade/WTHIT overlay for engine blocks. Shows energy/fluid I/O, buffer, and status. */
public enum EngineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "engine");
    private static final String NBT_TYPE      = "gt_eng_type";
    private static final String NBT_NAME      = "gt_eng_name";
    private static final String NBT_IN_RATE   = "gt_eng_in_rate";
    private static final String NBT_IN_UNIT   = "gt_eng_in_unit";
    private static final String NBT_OUT_RATE  = "gt_eng_out_rate";
    private static final String NBT_STORED    = "gt_eng_stored";
    private static final String NBT_CAPACITY  = "gt_eng_capacity";
    private static final String NBT_EFF       = "gt_eng_eff";
    private static final String NBT_ENABLED   = "gt_eng_enabled";
    private static final String NBT_SHUTDOWN  = "gt_eng_shutdown";
    private static final String NBT_STEAM_AMT = "gt_eng_steam_amt";
    private static final String NBT_STEAM_CAP = "gt_eng_steam_cap";
    private static final String NBT_WATER_AMT = "gt_eng_water_amt";
    private static final String NBT_WATER_CAP = "gt_eng_water_cap";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EngineBaseBlockEntity be)) return;
        EngineBlock block = (EngineBlock) accessor.getBlock();
        EngineType type = block.engineType();
        MachineSpec ms = block.machineSpec();

        tag.putString(NBT_TYPE, type.name().toLowerCase(Locale.ROOT));
        tag.putString(NBT_NAME, ms.id());
        tag.putLong(NBT_STORED, be.getEnergyStored(GregTechTags.Energy.KU, null));
        tag.putLong(NBT_OUT_RATE, ms.outputRate());
        tag.putInt(NBT_EFF, ms.efficiency());

        switch (type) {
            case ELECTRIC -> {
                ElectricEngineSpec spec = block.engineSpec(ElectricEngineSpec.class);
                tag.putLong(NBT_IN_RATE, spec.inputRate());
                tag.putString(NBT_IN_UNIT, "EU");
                tag.putLong(NBT_CAPACITY, ms.outputRate() * 2);
            }
            case FLUX -> {
                FluxEngineSpec spec = block.engineSpec(FluxEngineSpec.class);
                tag.putLong(NBT_IN_RATE, spec.inputRate());
                tag.putString(NBT_IN_UNIT, "RF");
                tag.putLong(NBT_CAPACITY, ms.outputRate() * 2);
            }
            case STEAM -> {
                tag.putLong(NBT_IN_RATE, 200);
                tag.putString(NBT_IN_UNIT, "Steam");
                tag.putLong(NBT_CAPACITY, ms.outputRate() * 1000);
                tag.putBoolean(NBT_ENABLED, be instanceof KineticSteamEngineBlockEntity se && se.isEnabled());
                tag.putBoolean(NBT_SHUTDOWN, be instanceof KineticSteamEngineBlockEntity se && se.isShutdown());
                if (be instanceof KineticSteamEngineBlockEntity se) {
                    var st = se.steamTank();
                    var wt = se.distilledWaterTank();
                    tag.putLong(NBT_STEAM_AMT, st != null ? st.getAmount() : 0);
                    tag.putLong(NBT_STEAM_CAP, st != null ? st.getCapacity() : 0);
                    tag.putLong(NBT_WATER_AMT, wt != null ? wt.getAmount() : 0);
                    tag.putLong(NBT_WATER_CAP, wt != null ? wt.getCapacity() : 0);
                }
            }
            case ROTATION -> {
                RotationEngineSpec spec = block.engineSpec(RotationEngineSpec.class);
                tag.putLong(NBT_IN_RATE, spec.inputRate());
                tag.putString(NBT_IN_UNIT, "RU");
                tag.putLong(NBT_CAPACITY, ms.outputRate() * 2);
            }
        }
        if (be instanceof PistonEngineBlockEntity piston) {
            var inputType = type == EngineType.ELECTRIC ? GregTechTags.Energy.EU : GregTechTags.Energy.RF;
            tag.putLong(NBT_IN_RATE, piston.operatingInput());
            tag.putLong(NBT_OUT_RATE, piston.operatingOutput());
            tag.putLong(NBT_STORED, piston.getEnergyStored(inputType, null));
            tag.putLong(NBT_CAPACITY, piston.getEnergyCapacity(inputType, null));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_TYPE, CompoundTag.TAG_STRING)) return;

        String type = data.getString(NBT_TYPE);
        String name = data.getString(NBT_NAME);
        long inRate = data.getLong(NBT_IN_RATE);
        String inUnit = data.getString(NBT_IN_UNIT);
        long outRate = data.getLong(NBT_OUT_RATE);
        long stored = data.getLong(NBT_STORED);
        long capacity = data.getLong(NBT_CAPACITY);
        int eff = data.getInt(NBT_EFF);

        tooltip.add(Component.translatable("jade.gregtech.engine.title", name));
        tooltip.add(Component.translatable("jade.gregtech.engine.in", format(inRate), inUnit));

        int pct = capacity > 0 ? (int) Math.min(100, stored * 100 / capacity) : 0;
        boolean inputBuffer = "electric".equals(type) || "flux".equals(type);
        tooltip.add(Component.translatable(inputBuffer ? "jade.gregtech.engine.out_input_buffer" : "jade.gregtech.engine.out",
                format(outRate), format(stored), format(capacity), pct, inUnit));

        if (eff > 0) {
            tooltip.add(Component.translatable("jade.gregtech.engine.eff", formatEfficiency(eff)));
        }

        if ("steam".equals(type)) {
            long steamAmt = data.getLong(NBT_STEAM_AMT);
            long steamCap = data.getLong(NBT_STEAM_CAP);
            if (steamCap > 0) {
                int steamPct = (int) Math.min(100, steamAmt * 100 / steamCap);
                tooltip.add(Component.translatable("jade.gregtech.engine.steam",
                        format(steamAmt), format(steamCap), steamPct));
            }
            long waterAmt = data.getLong(NBT_WATER_AMT);
            long waterCap = data.getLong(NBT_WATER_CAP);
            if (waterCap > 0) {
                int waterPct = (int) Math.min(100, waterAmt * 100 / waterCap);
                tooltip.add(Component.translatable("jade.gregtech.engine.water",
                        format(waterAmt), format(waterCap), waterPct));
            }

            if (data.getBoolean(NBT_SHUTDOWN)) {
                tooltip.add(Component.translatable("jade.gregtech.engine.status.shutdown"));
            } else if (!data.getBoolean(NBT_ENABLED)) {
                tooltip.add(Component.translatable("jade.gregtech.engine.status.off"));
            } else if (pct < 20) {
                tooltip.add(Component.translatable("jade.gregtech.engine.status.preheating", pct));
            } else {
                tooltip.add(Component.translatable("jade.gregtech.engine.status.running", pct));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String formatEfficiency(int efficiency) {
        long value = Math.abs(efficiency);
        long whole = value / 100;
        long frac = value % 100;
        if (frac > 9) return whole + "." + frac + "%";
        if (frac > 0) return whole + ".0" + frac + "%";
        return whole + ".00%";
    }
}
