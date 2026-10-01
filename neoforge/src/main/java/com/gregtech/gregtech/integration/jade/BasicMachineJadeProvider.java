package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Energy, inventory, and fluid tank info for basic machines. */
public enum BasicMachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "basic_machine");
    private static final String NBT_NAME = "gt_bm_name";
    private static final String NBT_TIER = "gt_bm_tier";
    private static final String NBT_UNIT = "gt_bm_unit";
    private static final String NBT_STORED = "gt_bm_stored";
    private static final String NBT_ENERGY_IN = "gt_bm_energy_in";
    private static final String NBT_ENERGY_MIN = "gt_bm_energy_min";
    private static final String NBT_ENERGY_MAX = "gt_bm_energy_max";
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

        tag.putString(NBT_NAME, spec.id());
        tag.putInt(NBT_TIER, spec.tier());
        tag.putString(NBT_UNIT, spec.energyType());
        tag.putLong(NBT_STORED, be.getEnergyTick());
        tag.putLong(NBT_ENERGY_IN, spec.energyIn());
        tag.putLong(NBT_ENERGY_MIN, spec.energyInMin());
        tag.putLong(NBT_ENERGY_MAX, spec.energyInMax());
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
                    ft.putString("name", fs.getDisplayName().getString());
                    ft.putInt("amount", fs.getAmount());
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
                    ft.putString("name", fs.getDisplayName().getString());
                    ft.putInt("amount", fs.getAmount());
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

        String name = data.getString(NBT_NAME);
        int tier = data.getInt(NBT_TIER);
        tooltip.add(Component.translatable("jade.gregtech.basic_machine.label", name, tier));

        if (data.contains(NBT_ENERGY_IN, CompoundTag.TAG_LONG)) {
            long energyIn = data.getLong(NBT_ENERGY_IN);
            long energyMin = data.getLong(NBT_ENERGY_MIN);
            long energyMax = data.getLong(NBT_ENERGY_MAX);
            long current = data.getLong(NBT_STORED);
            boolean running = data.getBoolean(NBT_RUNNING);
            String unit = data.getString(NBT_UNIT);
            tooltip.add(Component.translatable("jade.gregtech.energy.in",
                    format(energyIn), unit));
            tooltip.add(Component.translatable("jade.gregtech.energy.range",
                    format(energyMin), format(energyMax), unit));
            if (running) {
                tooltip.add(Component.translatable("jade.gregtech.energy.active",
                        format(current), unit));
            } else {
                tooltip.add(Component.translatable("jade.gregtech.energy.idle"));
            }
        }

        if (data.contains(NBT_ITEMS_TOTAL, CompoundTag.TAG_INT)) {
            int occupied = data.getInt(NBT_ITEMS_IN);
            int total = data.getInt(NBT_ITEMS_TOTAL);
            tooltip.add(Component.translatable("jade.gregtech.basic_machine.items", occupied, total));
        }

        int fluidCount = data.getInt(NBT_FLUID_COUNT);
        for (int i = 0; i < fluidCount; i++) {
            for (boolean isInput : new boolean[]{true, false}) {
            CompoundTag ft = data.getCompound((isInput ? NBT_FLUID_PREFIX_IN : NBT_FLUID_PREFIX_OUT) + i);
            if (!ft.isEmpty()) {
                String fName = ft.getString("name");
                int amount = ft.getInt("amount");
                long cap = ft.getLong("cap");
                int pct = cap > 0 ? (int) Math.min(100, amount * 100 / cap) : 0;
                String type = isInput ? Component.translatable("jade.gregtech.basic_machine.fluid_in").getString()
                                      : Component.translatable("jade.gregtech.basic_machine.fluid_out").getString();
                tooltip.add(Component.translatable("jade.gregtech.basic_machine.fluid",
                        type, fName, format(amount), format(cap), Integer.toString(pct)));
            }
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
}
