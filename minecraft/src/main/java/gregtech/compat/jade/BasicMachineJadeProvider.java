package gregtech.compat.jade;

import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Energy, inventory, and fluid tank info for basic machines. */
public enum BasicMachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "basic_machine");
    private static final String NBT_NAME = "gt_bm_name";
    private static final String DETAILS = "gt_bm_details";
    private static final String NBT_TIER = "gt_bm_tier";
    private static final String NBT_UNIT = "gt_bm_unit";
    private static final String NBT_STORED = "gt_bm_stored";
    private static final String NBT_ENERGY_IN = "gt_bm_energy_in";
    private static final String NBT_ENERGY_MIN = "gt_bm_energy_min";
    private static final String NBT_ENERGY_MAX = "gt_bm_energy_max";
    private static final String NBT_ENERGY_AVERAGE = "gt_bm_energy_average";
    private static final String NBT_RUNNING = "gt_bm_running";
    private static final String NBT_ITEMS_IN = "gt_bm_items_in";
    private static final String NBT_ITEMS_TOTAL = "gt_bm_items_total";
    private static final String NBT_FLUID_COUNT = "gt_bm_fluid_count";
    private static final String NBT_FLUID_PREFIX_IN = "gt_bm_fi_";
    private static final String NBT_FLUID_PREFIX_OUT = "gt_bm_fo_";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BasicMachineBlockEntity be)) return;

        BasicMachineSpec spec = be.spec();
        if (spec == null) return;

        // Keep recipe state and item/fluid sections with the machine provider. They must not
        // disappear when the separately configurable Omni provider is switched off in Jade.
        var inspection = new JadeInspection(accessor, DETAILS);
        OmniMachineInfo.append(inspection, be, accessor);
        inspection.write(tag);

        tag.putString(NBT_NAME, spec.id());
        tag.putString(NBT_NAME + "_key", accessor.getBlock().getDescriptionId());
        tag.putInt(NBT_TIER, spec.tier());
        tag.putString(NBT_UNIT, spec.energyType());
        tag.putLong(NBT_STORED, be.getEnergyTick());
        tag.putLong(NBT_ENERGY_IN, spec.energyIn());
        tag.putLong(NBT_ENERGY_MIN, spec.energyInMin());
        tag.putLong(NBT_ENERGY_MAX, spec.energyInMax());
        tag.putDouble(NBT_ENERGY_AVERAGE, be.averageEnergyInput());
        tag.putBoolean(NBT_RUNNING, be.isRunning());

        if (be.inventory() != null) {
            int occupied = 0;
            int inputSlots = be.inputSlots();
            for (int i = 0; i < inputSlots; i++) {
                if (!be.inventory().getStackInSlot(i).isEmpty()) occupied++;
            }
            tag.putInt(NBT_ITEMS_IN, occupied);
            tag.putInt(NBT_ITEMS_TOTAL, inputSlots);
        }

        FluidTankGT[] tanksIn = be.getTanksInput();
        FluidTankGT[] tanksOut = be.getTanksOutput();
        int fluidCount = (tanksIn != null ? tanksIn.length : 0) + (tanksOut != null ? tanksOut.length : 0);
        tag.putInt(NBT_FLUID_COUNT, fluidCount);

        if (tanksIn != null) {
            for (int i = 0; i < tanksIn.length; i++) {
                FluidStack fs = tanksIn[i].getFluid();
                if (!fs.isEmpty()) {
                    CompoundTag ft = new CompoundTag();
                    // Retain the legacy fallback for older clients; new clients resolve the stack locally.
                    ft.putString("name", fs.getDisplayName().getString());
//? if >=1.21.1 {
                    /*ft.put("stack", fs.save(accessor.getLevel().registryAccess()));
*///?} else {
                    ft.put("stack", fs.writeToNBT(new CompoundTag()));
//?}
                    ft.putLong("amount", tanksIn[i].getAmount());
                    ft.putLong("cap", tanksIn[i].getCapacity());
                    tag.put(NBT_FLUID_PREFIX_IN + i, ft);
                }
            }
        }
        if (tanksOut != null) {
            for (int i = 0; i < tanksOut.length; i++) {
                FluidStack fs = tanksOut[i].getFluid();
                if (!fs.isEmpty()) {
                    CompoundTag ft = new CompoundTag();
                    // Retain the legacy fallback for older clients; new clients resolve the stack locally.
                    ft.putString("name", fs.getDisplayName().getString());
//? if >=1.21.1 {
                    /*ft.put("stack", fs.save(accessor.getLevel().registryAccess()));
*///?} else {
                    ft.put("stack", fs.writeToNBT(new CompoundTag()));
//?}
                    ft.putLong("amount", tanksOut[i].getAmount());
                    ft.putLong("cap", tanksOut[i].getCapacity());
                    tag.put(NBT_FLUID_PREFIX_OUT + i, ft);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_NAME, CompoundTag.TAG_STRING)) return;

        // Jade already owns the translated block name, including any material/tier suffix.
        if (data.contains(NBT_ENERGY_IN, CompoundTag.TAG_LONG)) {
            long energyIn = data.getLong(NBT_ENERGY_IN);
            long energyMin = data.getLong(NBT_ENERGY_MIN);
            long energyMax = data.getLong(NBT_ENERGY_MAX);
            double average = data.getDouble(NBT_ENERGY_AVERAGE);
            String unit = data.getString(NBT_UNIT);
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.energy.in",
                    format(energyIn), unit));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.energy.range",
                    format(energyMin), format(energyMax), unit));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.energy.actual",
                    String.format(Locale.US, "%.2f", average), unit));
            if (energyIn > 0) JadeText.add(tooltip, Component.translatable("jade.gt6ce.energy.multiplier",
                    String.format(Locale.US, "%.2f", average / energyIn), format(energyIn), unit));
        }

        if (data.contains(NBT_ITEMS_TOTAL, CompoundTag.TAG_INT)) {
            int occupied = data.getInt(NBT_ITEMS_IN);
            int total = data.getInt(NBT_ITEMS_TOTAL);
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.basic_machine.items", occupied, total));
        }

        if (data.contains(DETAILS)) {
            JadeInspection.render(tooltip, accessor, DETAILS);
            return;
        }
        // Compatibility with servers still supplying the previous Omni snapshot.
        if (data.getBoolean("gt_omni_inventory") && config.get(OmniJadeProvider.INSTANCE.getUid())) return;
        int fluidCount = data.getInt(NBT_FLUID_COUNT);
        for (int i = 0; i < fluidCount; i++) {
            for (boolean isInput : new boolean[]{true, false}) {
            CompoundTag ft = data.getCompound((isInput ? NBT_FLUID_PREFIX_IN : NBT_FLUID_PREFIX_OUT) + i);
            if (!ft.isEmpty()) {
                Component fName = Component.literal(ft.getString("name"));
                if (ft.contains("stack", CompoundTag.TAG_COMPOUND)) {
//? if >=1.21.1 {
                    /*FluidStack fluid = FluidStack.parseOptional(accessor.getLevel().registryAccess(), ft.getCompound("stack"));
*///?} else {
                    FluidStack fluid = FluidStack.loadFluidStackFromNBT(ft.getCompound("stack"));
//?}
                    if (!fluid.isEmpty()) fName = fluid.getDisplayName();
                }
                long amount = ft.getLong("amount");
                long cap = ft.getLong("cap");
                int pct = gregtech.content.energy.BoilerGauge.percent(amount, cap);
                Component type = Component.translatable(isInput ? "jade.gt6ce.basic_machine.fluid_in"
                        : "jade.gt6ce.basic_machine.fluid_out");
                JadeText.add(tooltip, Component.translatable("jade.gt6ce.basic_machine.fluid",
                        type, fName, format(amount), format(cap), Integer.toString(pct)));
            }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override public int getDefaultPriority() { return 1002; }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
