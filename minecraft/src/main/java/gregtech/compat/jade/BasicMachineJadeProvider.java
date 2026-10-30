package gregtech.compat.jade;

import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.engine.fluid.FluidTankGT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
//?}
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;

import java.util.Locale;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** One snapshot and renderer for all recipe machines, including their bound multiblock parts. */
public enum BasicMachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "basic_machine");
    static final String DATA_KEY = "gt_machine";

    static BasicMachineBlockEntity machine(BlockEntity entity) {
        // Only use an existing binding. Inspection must not scan or form a multiblock.
        if (entity instanceof MultiblockPortBlockEntity port) entity = port.sensorTarget();
        return entity instanceof BasicMachineBlockEntity machine ? machine : null;
    }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var machine = machine(accessor.getBlockEntity());
        if (machine == null || machine.spec() == null) return;
        var spec = machine.spec();
        var control = machine.machineControl(null);
        var data = new CompoundTag();
        data.putString("unit", spec.energyType());
        data.putLongArray("energy", new long[]{spec.energyIn(), spec.energyInMin(), spec.energyInMax()});
        data.putDouble("average", machine.averageEnergyInput());
        data.putBoolean("overloaded", machine.isEnergyOverloaded());
        data.putBoolean("insufficient", machine.isEnergyInsufficient());
        // Fusion's controller counts ticks; ordinary machines count energy work.
        data.putBoolean("timed", machine instanceof gregtech.content.blockentity.machine.FusionReactorControllerBlockEntity);
        data.putBoolean("enabled", control.enabled());
        data.putBoolean("active", control.active());
        data.putLong("progress", control.progress());
        data.putLong("work", control.progressMax());
        data.putLong("recipe", machine.minimumEnergy());
        data.putInt("parallel", machine.parallelCount());
        var details = machine.recipeDetails();
        if (details != null) {
            data.putLong("basePower", details.power());
            data.putLong("baseDuration", details.duration());
        }
        data.putInt("slots", machine.inputSlots());
        int occupied = 0;
        for (int i = 0; i < machine.inputSlots(); i++)
            if (!machine.inventory().getStackInSlot(i).isEmpty()) occupied++;
        data.putInt("occupied", occupied);

        var pending = new ListTag();
        for (var item : machine.inspectionPendingItems()) pending.add(saveItem(item, accessor));
        data.put("pendingItems", pending);
        data.putInt("more", Math.max(0, machine.inspectionPendingItemCount() - pending.size()));
        var pendingFluids = new ListTag();
        for (var fluid : machine.inspectionPendingFluids())
            pendingFluids.add(saveFluid(fluid, fluid.getAmount(), fluid.getAmount(), accessor));
        data.put("pendingFluids", pendingFluids);
        data.put("inputItems", items(machine.inventory(), 0, machine.inputSlots(), accessor));
        data.put("outputItems", items(machine.inventory(), machine.inputSlots(), machine.outputSlots(), accessor));
        data.put("programItems", items(machine.autocraftingProgram(), 0, 1, accessor));
        data.put("inputFluids", fluids(machine.getTanksInput(), accessor));
        data.put("outputFluids", fluids(machine.getTanksOutput(), accessor));
        if (machine instanceof CokeOvenControllerBlockEntity coke && !control.active() && control.progressMax() == 0) {
            data.putBoolean("cokeHint", true);
            data.putBoolean("ignition", !coke.ignitionReady());
        }
        // No translated row wrappers or duplicate legacy fluid payload: the client owns layout and language.
        tag.put(DATA_KEY, data);
    }

    private static ListTag items(IItemHandler inventory, int first, int count, BlockAccessor accessor) {
        var result = new ListTag();
        for (int i = first; i < Math.min(inventory.getSlots(), first + count); i++) {
            var stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) result.add(saveItem(stack, accessor));
        }
        return result;
    }

    private static CompoundTag saveItem(ItemStack stack, BlockAccessor accessor) {
//? if >=1.21.1 {
        /*return (CompoundTag) stack.save(accessor.getLevel().registryAccess());
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    private static ListTag fluids(FluidTankGT[] tanks, BlockAccessor accessor) {
        var result = new ListTag();
        if (tanks != null) for (var tank : tanks)
            if (!tank.isEmpty()) result.add(saveFluid(tank.getFluid(), tank.getAmount(), tank.capacity(), accessor));
        return result;
    }

    private static CompoundTag saveFluid(FluidStack fluid, long amount, long capacity, BlockAccessor accessor) {
        var result = new CompoundTag();
//? if >=1.21.1 {
        /*result.put("stack", fluid.save(accessor.getLevel().registryAccess()));
*///?} else {
        result.put("stack", fluid.writeToNBT(new CompoundTag()));
//?}
        result.putLong("amount", amount);
        result.putLong("capacity", capacity);
        return result;
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(DATA_KEY)) return;
//? if >=1.21.1 {
        /*tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE);
        tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
*///?} else {
        tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_ITEM_STORAGE);
        tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE);
//?}
        var data = accessor.getServerData().getCompound(DATA_KEY);
        var rates = data.getLongArray("energy");
        String unit = data.getString("unit");
        double average = data.getDouble("average");
        if (rates.length == 3 && !unit.equalsIgnoreCase("TU")) {
            boolean overloaded = data.getBoolean("overloaded") || average > rates[2];
            boolean insufficient = !overloaded && average < rates[1] && data.getBoolean("insufficient");
            if (rates[0] > 0) {
                tooltip.add(new MachineEnergyElement(rates[1], rates[0], rates[2], average, unit, overloaded, insufficient));
                if (!overloaded && (insufficient || average > 0 && average < rates[1]))
                    tooltip.add(Component.translatable("jade.gt6ce.machine.insufficient_energy")
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
            if (accessor.showDetails()) {
                JadeText.detail(tooltip, "jade.gt6ce.energy.in", format(rates[0]), unit);
                JadeText.detail(tooltip, "jade.gt6ce.energy.range", format(rates[1]), format(rates[2]), unit);
            }
        }
        tooltip.add(JadeText.status("jade.gt6ce.omni." +
                (!data.getBoolean("enabled") ? "disabled" : data.getBoolean("active") ? "working" : "waiting")));
        long progress = data.getLong("progress"), work = data.getLong("work");
        if (work > 0) {
            // Progress is energy work, not ticks. While paused use the job's minimum power.
            double power = data.getBoolean("timed") ? 1 : Math.max(Math.max(1, data.getLong("recipe")), average);
            tooltip.add(JadeBars.time(progress, work, power));
        }
        appendRecipeDetails(tooltip, accessor, data, rates, unit, average);
        for (String role : new String[]{"pending", "input", "output", "program"}) {
            if (role.equals("pending") && (!data.getList("pendingItems", 10).isEmpty()
                    || !data.getList("pendingFluids", 10).isEmpty()))
                tooltip.add(Component.translatable("jade.gt6ce.machine.recipe_outputs").withStyle(net.minecraft.ChatFormatting.GRAY));
            renderItems(tooltip, accessor, data.getList(role + "Items", 10));
            if (role.equals("pending") && data.getInt("more") > 0) line(tooltip, "more_pending", data.getInt("more"));
            renderFluids(tooltip, accessor, data.getList(role + "Fluids", 10));
            if (role.equals("pending") && (!data.getList("pendingItems", 10).isEmpty()
                    || !data.getList("pendingFluids", 10).isEmpty()))
                tooltip.add(snownee.jade.api.ui.IElementHelper.get().spacer(0, 9));
        }
        if (data.getBoolean("ignition")) line(tooltip, "needs_ignition");
        if (data.getBoolean("cokeHint")) line(tooltip, "coke_recipe_hint");
    }

    private static void appendRecipeDetails(ITooltip tooltip, BlockAccessor accessor, CompoundTag data,
                                           long[] rates, String unit, double average) {
        long minimum = data.getLong("recipe"), work = data.getLong("work");
        if (minimum <= 0 || work <= 0) return;
        int parallel = Math.max(1, data.getInt("parallel"));
        boolean timed = unit.equalsIgnoreCase("TU");
        var line = timed
                ? Component.translatable("jade.gt6ce.machine.recipe_parallel", parallel)
                : Component.translatable("jade.gt6ce.omni.recipe_energy", format(minimum),
                        gregtech.client.EnergyText.unit(unit), parallel);
        long duration = data.getLong("baseDuration");
        if (duration > 0 && rates.length == 3 && rates[2] > 0) {
            // Retain tick rounding: the processor completes at most one batch per tick.
            // A paused job uses its minimum viable supply only to estimate future energy cost.
            double required = Math.max(1, Math.max(minimum, rates[1]));
            double power = timed ? 1 : Math.min(rates[2], Math.max(required, average));
            var supply = BigDecimal.valueOf(power);
            var ticks = BigDecimal.valueOf(work).divide(supply, 0, RoundingMode.CEILING);
            boolean running = data.getBoolean("active") && data.getBoolean("enabled")
                    && !data.getBoolean("overloaded") && (timed || average >= required && average <= rates[2]);
            double speed = running ? (double) duration * parallel / ticks.doubleValue() : 0;
            line.append(Component.translatable("jade.gt6ce.machine.effective_speed", JadeText.decimal(speed, 2)));
            JadeText.add(tooltip, line);
            if (accessor.showDetails()) {
                if (timed) {
                    JadeText.detail(tooltip, "jade.gt6ce.machine.original_time", format(duration));
                    tooltip.add(Component.translatable("jade.gt6ce.machine.no_external_energy"));
                } else {
                    JadeText.detail(tooltip, "jade.gt6ce.machine.original_recipe", format(data.getLong("basePower")),
                            gregtech.client.EnergyText.unit(unit), format(duration));
                    var energy = ticks.multiply(supply);
                    JadeText.detail(tooltip, "jade.gt6ce.machine.estimated_energy", decimal(energy),
                            gregtech.client.EnergyText.unit(unit),
                            decimal(energy.divide(BigDecimal.valueOf(parallel), 2, RoundingMode.HALF_UP)),
                            gregtech.client.EnergyText.unit(unit));
                }
            }
        } else {
            JadeText.add(tooltip, line);
        }
    }

    private static String decimal(BigDecimal value) {
        return new java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    private static void renderItems(ITooltip tooltip, BlockAccessor accessor, ListTag items) {
        if (items.isEmpty()) return;
        var summary = new JadeItemSummary();
        for (int i = 0; i < items.size(); i++) {
//? if >=1.21.1 {
            /*var stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(), items.getCompound(i));
*///?} else {
            var stack = ItemStack.of(items.getCompound(i));
//?}
            summary.add(stack);
        }
        summary.render(tooltip);
    }

    private static void renderFluids(ITooltip tooltip, BlockAccessor accessor, ListTag fluids) {
        if (fluids.isEmpty()) return;
        for (int i = 0; i < fluids.size(); i++) {
            var row = fluids.getCompound(i);
//? if >=1.21.1 {
            /*var fluid = FluidStack.parseOptional(accessor.getLevel().registryAccess(), row.getCompound("stack"));
*///?} else {
            var fluid = FluidStack.loadFluidStackFromNBT(row.getCompound("stack"));
//?}
            if (fluid.isEmpty()) continue;
            var view = JadeBars.readFluid(FluidView.writeDefault(
                    JadeFluidObject.of(fluid.getFluid(), row.getLong("amount")), row.getLong("capacity")));
            if (view != null) tooltip.add(JadeBars.fluid(view, view.overrideText != null ? view.overrideText
                    : Component.translatable("jade.fluid", view.fluidName, view.current)));
        }
    }

    private static void line(ITooltip tooltip, String key, Object... args) {
        JadeText.detail(tooltip, "jade.gt6ce.omni." + key, args);
    }
    private static String format(long value) { return String.format(Locale.US, "%,d", value); }
    @Override public ResourceLocation getUid() { return UID; }
    @Override public int getDefaultPriority() { return 1002; }
}
