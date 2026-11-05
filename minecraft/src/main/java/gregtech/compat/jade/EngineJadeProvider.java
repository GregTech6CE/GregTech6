package gregtech.compat.jade;

import gregtech.api.machine.*;
import gregtech.engine.machine.*;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.content.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;

import java.util.Locale;

/** Jade/WTHIT overlay for engine blocks. Shows energy/fluid I/O, buffer, and status. */
public enum EngineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "engine");
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

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EngineBaseBlockEntity be)) return;
        EngineBlock block = (EngineBlock) accessor.getBlock();
        EngineType type = block.engineType();
        MachineSpec ms = block.machineSpec();

        tag.putString(NBT_TYPE, type.name().toLowerCase(Locale.ROOT));
        tag.putString(NBT_NAME, ms.id());
        tag.putString(NBT_NAME + "_key", block.getDescriptionId());
        tag.putLong(NBT_STORED, be.getEnergyStored(type == gregtech.engine.machine.EngineType.DIESEL
                ? GregTechTags.Energy.RU : GregTechTags.Energy.KU, null));
        tag.putLong(NBT_OUT_RATE, ms.outputRate());
        tag.putInt(NBT_EFF, ms.efficiency());

        if (be instanceof PistonEngineBlockEntity || be instanceof KineticRotationEngineBlockEntity) {
            var info = new JadeInspection(accessor, "gt_engine_conversion");
            if (be instanceof PistonEngineBlockEntity piston) {
                var control = piston.machineControl(null);
                info.state(control.enabled(), control.running());
            } else info.state(true, ((KineticRotationEngineBlockEntity) be).isActive());
            var input = type == EngineType.ELECTRIC ? GregTechTags.Energy.EU
                    : type == EngineType.FLUX ? GregTechTags.Energy.RF : GregTechTags.Energy.RU;
            long tick = accessor.getLevel().getGameTime();
            EnergyRateText.conversion(info, be, input, GregTechTags.Energy.KU,
                    be.conversionMeter().averageInput(tick), be.conversionMeter().averageOutput(tick));
            EnergyRateText.efficiency(info, ms.efficiency());
            info.detail("buffer", be.getEnergyStored(input, null), be.getEnergyCapacity(input, null), input.getShortName());
            info.write(tag);
            return;
        }

        if (be instanceof KineticDieselEngineBlockEntity diesel) {
            var info = new JadeInspection(accessor, "gt_engine_conversion");
            info.state(!diesel.isStopped(), diesel.isEmitting());
            info.energyRate("average_output", diesel.conversionMeter().averageOutput(accessor.getLevel().getGameTime()),
                    ms.outputRate(), ms.outputRate(), "RU");
            EnergyRateText.efficiency(info, ms.efficiency());
            info.detail("stored", diesel.getEnergyStored(GregTechTags.Energy.RU, null), "RU");
            info.fluid("input", 0, diesel.fuelTank().getFluid(), diesel.fuelTank().getAmount(), diesel.fuelTank().capacity());
            info.fluid("output", 0, diesel.exhaustTank().getFluid(), diesel.exhaustTank().getAmount(), diesel.exhaustTank().capacity());
            info.groupedInventory();
            info.write(tag);
            return;
        }

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
                tag.putLong(NBT_CAPACITY, be instanceof KineticSteamEngineBlockEntity se ? se.energyCapacity() : ms.outputRate() * 2000);
                tag.putBoolean(NBT_ENABLED, be instanceof KineticSteamEngineBlockEntity se && se.isEnabled());
                tag.putBoolean(NBT_SHUTDOWN, be instanceof KineticSteamEngineBlockEntity se && se.isShutdown());
                if (be instanceof KineticSteamEngineBlockEntity se) {
                    var control = se.machineControl(null);
                    tag.putBoolean("gt_eng_working", control.running());
                    tag.putBoolean("gt_eng_waiting_steam", se.isWaitingForSteam());
                    if (se.spec() != null) tag.putDouble("gt_eng_warmup_end", se.spec().minimumRunningFill());
                    tag.putDouble("gt_eng_average_output", se.conversionMeter().averageOutput(accessor.getLevel().getGameTime()));
                    tag.putLong("gt_eng_out_min", se.getEnergySizeOutputMin(GregTechTags.Energy.KU, null));
                    tag.putLong("gt_eng_out_max", se.getEnergySizeOutputMax(GregTechTags.Energy.KU, null));
                    writeTank(tag, "steam", se.steamTank(), "Steam");
                }
            }
            case ROTATION -> {
                RotationEngineSpec spec = block.engineSpec(RotationEngineSpec.class);
                tag.putLong(NBT_IN_RATE, spec.inputRate());
                tag.putString(NBT_IN_UNIT, "RU");
                tag.putLong(NBT_CAPACITY, ms.outputRate() * 2);
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_TYPE, CompoundTag.TAG_STRING)) return;

        if (data.contains("gt_engine_conversion")) {
            JadeInspection.render(tooltip, accessor, "gt_engine_conversion");
            return;
        }

        String type = data.getString(NBT_TYPE);
        Component name = JadeText.translated(data, NBT_NAME + "_key", NBT_NAME);
        long inRate = data.getLong(NBT_IN_RATE);
        String inUnit = data.getString(NBT_IN_UNIT);
        long outRate = data.getLong(NBT_OUT_RATE);
        long stored = data.getLong(NBT_STORED);
        long capacity = data.getLong(NBT_CAPACITY);
        int eff = data.getInt(NBT_EFF);

        if ("steam".equals(type)) {
            appendSteam(tooltip, accessor, data, stored, capacity,
                    data.contains("gt_eng_average_output") ? data.getDouble("gt_eng_average_output") : outRate);
            return;
        }
        JadeText.addLeft(tooltip, Component.translatable("jade.gt6ce.engine.title", name));
        JadeText.addLeft(tooltip, Component.translatable("jade.gt6ce.engine.in", format(inRate), Component.literal(inUnit)));

        int pct = capacity > 0 ? (int) Math.min(100, stored * 100 / capacity) : 0;
        boolean inputBuffer = "electric".equals(type) || "flux".equals(type);
        JadeText.addLeft(tooltip, Component.translatable(inputBuffer ? "jade.gt6ce.engine.out_input_buffer" : "jade.gt6ce.engine.out",
                format(outRate), format(stored), format(capacity), pct, inUnit));

        if (eff > 0 && accessor.showDetails()) {
            JadeText.addLeft(tooltip, Component.translatable("jade.gt6ce.engine.eff", EnergyRateText.percent(eff)));
        }

    }

    private static void writeTank(CompoundTag data, String key, gregtech.engine.fluid.FluidTankGT tank, String fluidName) {
        if (tank == null) return;
        var fallback = gregtech.content.registry.GTFluids.still(fluidName);
        var fluid = tank.getFluid();
        var type = fluid.isEmpty() && fallback != null ? fallback.get() : fluid.getFluid();
        data.put("gt_eng_tank_" + key, FluidView.writeDefault(
                JadeFluidObject.of(type, tank.getAmount()), tank.capacity()));
    }

    private static void appendSteam(ITooltip tooltip, BlockAccessor accessor, CompoundTag data,
                                    long stored, long capacity, double output) {
//? if >=1.21.1 {
        /*tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
*///?} else {
        tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE);
//?}
        EnergyRateText.render(tooltip, Component.translatable("jade.gt6ce.omni.average_output"),
                Component.literal(EnergyRateText.amount(output)), JadeText.unit("KU"),
                data.getLong("gt_eng_out_min"), data.getLong("gt_eng_out_max"), accessor.showDetails());
        if (accessor.showDetails()) JadeText.addLeft(tooltip, Component.translatable("jade.gt6ce.engine.eff", EnergyRateText.percent(data.getInt(NBT_EFF))));
        int percent = gregtech.content.energy.BoilerGauge.percent(stored, capacity);
        if (accessor.showDetails()) JadeText.addLeft(tooltip, Component.translatable("jade.gt6ce.engine.ku_storage",
                format(stored), format(capacity), percent));
        if (data.contains("gt_eng_tank_steam")) {
            var view = JadeBars.readFluid(data.getCompound("gt_eng_tank_steam"));
            if (view != null) tooltip.add(JadeBars.fluid(view, view.ratio <= 0
                    ? Component.translatable("fluid.gt6ce.steam").append(" " + gregtech.engine.fluid.FluidUnits.compact(0))
                    : view.overrideText != null ? view.overrideText
                    : Component.translatable("jade.fluid", view.fluidName, view.current)));
        }
        boolean venting = data.getBoolean(NBT_SHUTDOWN) && stored > 0;
        boolean running = data.getBoolean("gt_eng_working") && !venting && data.getBoolean(NBT_ENABLED);
        String state = venting ? "jade.gt6ce.engine.overload_protection"
                : data.getBoolean("gt_eng_waiting_steam") ? "jade.gt6ce.engine.waiting_steam"
                : "jade.gt6ce.boiler.state." + (!data.getBoolean(NBT_ENABLED) || stored <= 0 ? "stopped"
                        : running ? "running" : "preheating");
        Component label = Component.translatable("jade.gt6ce.boiler.gauge", Component.translatable(state), percent);
        tooltip.add(new BoilerGaugeElement(gregtech.content.energy.BoilerGauge.fill(stored, capacity), label, running,
                data.contains("gt_eng_warmup_end") ? data.getDouble("gt_eng_warmup_end") : .25));
    }

    @Override public int getDefaultPriority() { return 1001; }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

}
