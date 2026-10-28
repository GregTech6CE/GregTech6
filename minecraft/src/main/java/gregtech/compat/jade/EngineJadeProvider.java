package gregtech.compat.jade;

import gregtech.api.machine.*;
import gregtech.engine.machine.*;
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

        if (be instanceof KineticDieselEngineBlockEntity diesel) {
            tag.putBoolean(NBT_ENABLED, !diesel.isStopped());
            tag.putBoolean("gt_diesel_emitting", diesel.isEmitting());
            tag.putLong("gt_diesel_fuel_capacity", diesel.fuelTank().capacity());
            tag.putLong("gt_diesel_exhaust_capacity", diesel.exhaustTank().capacity());
            tag.putString("gt_diesel_front", accessor.getBlockState().getValue(EngineBlock.FACING).getName());
            tag.putString("gt_diesel_rear", accessor.getBlockState().getValue(EngineBlock.FACING).getOpposite().getName());
            var fuel = diesel.fuelTank().getFluid();
            var exhaust = diesel.exhaustTank().getFluid();
//? if >=1.21.1 {
            /*if (!fuel.isEmpty()) tag.put("gt_diesel_fuel", fuel.save(accessor.getLevel().registryAccess()));
            if (!exhaust.isEmpty()) tag.put("gt_diesel_exhaust", exhaust.save(accessor.getLevel().registryAccess()));
*///?} else {
            if (!fuel.isEmpty()) tag.put("gt_diesel_fuel", fuel.writeToNBT(new CompoundTag()));
            if (!exhaust.isEmpty()) tag.put("gt_diesel_exhaust", exhaust.writeToNBT(new CompoundTag()));
//?}
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
                    if (se.spec() != null) tag.putDouble("gt_eng_warmup_end", se.spec().minimumRunningFill());
                    tag.putLong(NBT_OUT_RATE, control.running() && se.isEnabled() && !se.isShutdown() && se.spec() != null
                            ? se.spec().outputRate() * (se.pressureState() + 1) / 16 : 0);
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
        Component name = JadeText.translated(data, NBT_NAME + "_key", NBT_NAME);
        long inRate = data.getLong(NBT_IN_RATE);
        String inUnit = data.getString(NBT_IN_UNIT);
        long outRate = data.getLong(NBT_OUT_RATE);
        long stored = data.getLong(NBT_STORED);
        long capacity = data.getLong(NBT_CAPACITY);
        int eff = data.getInt(NBT_EFF);

        if ("steam".equals(type)) {
            appendSteam(tooltip, accessor, data, stored, capacity, outRate);
            return;
        }
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.title", name));
        if ("diesel".equals(type)) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.diesel.output", format(outRate),
                    format(data.getBoolean("gt_diesel_emitting") ? outRate : 0), format(stored)));
            for (String tank : new String[]{"fuel", "exhaust"}) {
//? if >=1.21.1 {
                /*var fluid = net.neoforged.neoforge.fluids.FluidStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound("gt_diesel_" + tank));
*///?} else {
                var fluid = net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(data.getCompound("gt_diesel_" + tank));
//?}
                Component content = fluid.isEmpty() ? Component.translatable("jade.gt6ce.engine.diesel.empty")
                        : fluid.getDisplayName().copy().append(" " + format(fluid.getAmount()) + " L");
                JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.diesel." + tank, content,
                        format(data.getLong("gt_diesel_" + tank + "_capacity"))));
            }
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.diesel.faces",
                    JadeText.direction(data.getString("gt_diesel_front")), JadeText.direction(data.getString("gt_diesel_rear"))));
            JadeText.add(tooltip, Component.translatable(!data.getBoolean(NBT_ENABLED) ? "jade.gt6ce.engine.status.off"
                    : data.getBoolean("gt_diesel_emitting") ? "jade.gt6ce.engine.diesel.running" : "jade.gt6ce.engine.diesel.waiting"));
            return;
        }
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.in", format(inRate), Component.literal(inUnit)));

        int pct = capacity > 0 ? (int) Math.min(100, stored * 100 / capacity) : 0;
        boolean inputBuffer = "electric".equals(type) || "flux".equals(type);
        JadeText.add(tooltip, Component.translatable(inputBuffer ? "jade.gt6ce.engine.out_input_buffer" : "jade.gt6ce.engine.out",
                format(outRate), format(stored), format(capacity), pct, inUnit));

        if (eff > 0) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.engine.eff", formatEfficiency(eff)));
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
                                    long stored, long capacity, long output) {
//? if >=1.21.1 {
        /*tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
*///?} else {
        tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE);
//?}
        JadeText.add(tooltip, Component.translatable("gt.lang.energy.output").append(": " + format(output) + " ")
                .append(gregtech.client.EnergyText.unit("KU")).append("/t"));
        int percent = gregtech.content.energy.BoilerGauge.percent(stored, capacity);
        if (accessor.showDetails()) JadeText.detail(tooltip, "jade.gt6ce.engine.ku_storage",
                format(stored), format(capacity), percent);
        if (data.contains("gt_eng_tank_steam")) {
            var view = FluidView.readDefault(data.getCompound("gt_eng_tank_steam"));
            if (view != null) tooltip.add(JadeBars.fluid(view, view.ratio <= 0
                    ? Component.translatable("fluid.gt6ce.steam").append(" 0L")
                    : view.overrideText != null ? view.overrideText
                    : Component.translatable("jade.fluid", view.fluidName, view.current)));
        }
        boolean venting = data.getBoolean(NBT_SHUTDOWN) && stored > 0;
        boolean running = data.getBoolean("gt_eng_working") && !venting && data.getBoolean(NBT_ENABLED);
        String state = venting ? "jade.gt6ce.engine.overload_protection"
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

    private static String formatEfficiency(int efficiency) {
        long value = Math.abs(efficiency);
        long whole = value / 100;
        long frac = value % 100;
        if (frac > 9) return whole + "." + frac + "%";
        if (frac > 0) return whole + ".0" + frac + "%";
        return whole + ".00%";
    }
}
