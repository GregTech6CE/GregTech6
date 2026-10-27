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
import net.minecraft.world.phys.Vec2;
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
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

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
        data.putBoolean("enabled", control.enabled());
        data.putBoolean("active", control.active());
        data.putLong("progress", control.progress());
        data.putLong("work", control.progressMax());
        data.putLong("recipe", machine.minimumEnergy());
        data.putInt("parallel", machine.parallelCount());
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
            if (!tank.isEmpty()) result.add(saveFluid(tank.getFluid(), tank.getAmount(), tank.getCapacity(), accessor));
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
        if (rates.length == 3) {
            JadeText.detail(tooltip, "jade.gt6ce.energy.in", format(rates[0]), unit);
            JadeText.detail(tooltip, "jade.gt6ce.energy.range", format(rates[1]), format(rates[2]), unit);
            JadeText.detail(tooltip, "jade.gt6ce.energy.actual", JadeText.decimal(average, 2), unit);
            if (rates[0] > 0) JadeText.detail(tooltip, "jade.gt6ce.energy.multiplier",
                    JadeText.decimal(average / rates[0], 2), format(rates[0]), unit);
        }
        JadeText.detail(tooltip, "jade.gt6ce.basic_machine.items", data.getInt("occupied"), data.getInt("slots"));
        line(tooltip, "state", JadeText.status("jade.gt6ce.omni." +
                (!data.getBoolean("enabled") ? "disabled" : data.getBoolean("active") ? "working" : "waiting")));
        long progress = data.getLong("progress"), work = data.getLong("work");
        if (work > 0) line(tooltip, "progress", progress, work, gregtech.content.energy.BoilerGauge.percent(progress, work));
        if (data.getLong("recipe") > 0) line(tooltip, "recipe_energy", data.getLong("recipe"), unit, data.getInt("parallel"));
        for (String role : new String[]{"pending", "input", "output", "program"}) {
            renderItems(tooltip, accessor, role, data.getList(role + "Items", 10));
            if (role.equals("pending") && data.getInt("more") > 0) line(tooltip, "more_pending", data.getInt("more"));
            renderFluids(tooltip, accessor, role, data.getList(role + "Fluids", 10));
        }
        if (data.getBoolean("ignition")) line(tooltip, "needs_ignition");
        if (data.getBoolean("cokeHint")) line(tooltip, "coke_recipe_hint");
    }

    private static void heading(ITooltip tooltip, String kind, String role) {
        var color = switch (role) {
            case "pending" -> net.minecraft.ChatFormatting.GOLD;
            case "input" -> net.minecraft.ChatFormatting.AQUA;
            case "output" -> net.minecraft.ChatFormatting.GREEN;
            default -> net.minecraft.ChatFormatting.WHITE;
        };
        tooltip.add(Component.translatable("jade.gt6ce.omni.group_" + kind,
                Component.translatable("jade.gt6ce.omni." + role)).withStyle(color));
    }

    private static void renderItems(ITooltip tooltip, BlockAccessor accessor, String role, ListTag items) {
        if (items.isEmpty()) return;
        heading(tooltip, "items", role);
        for (int i = 0; i < items.size(); i++) {
//? if >=1.21.1 {
            /*var stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(), items.getCompound(i));
*///?} else {
            var stack = ItemStack.of(items.getCompound(i));
//?}
            var icon = IElementHelper.get().item(stack);
            icon.message(Component.translatable("jade.gt6ce.omni.item_count", stack.getHoverName(), stack.getCount()).getString());
            if (i % 8 == 0) tooltip.add(icon); else tooltip.append(icon);
        }
    }

    private static void renderFluids(ITooltip tooltip, BlockAccessor accessor, String role, ListTag fluids) {
        if (fluids.isEmpty()) return;
        heading(tooltip, "fluids", role);
        for (int i = 0; i < fluids.size(); i++) {
            var row = fluids.getCompound(i);
//? if >=1.21.1 {
            /*var fluid = FluidStack.parseOptional(accessor.getLevel().registryAccess(), row.getCompound("stack"));
*///?} else {
            var fluid = FluidStack.loadFluidStackFromNBT(row.getCompound("stack"));
//?}
            if (fluid.isEmpty()) continue;
            var text = role.equals("pending")
                    ? Component.translatable("jade.gt6ce.omni.pending_fluid", i + 1, fluid.getDisplayName(), row.getLong("amount"))
                    : Component.translatable("jade.gt6ce.omni.fluid", Component.translatable("jade.gt6ce.omni." + role),
                            i + 1, fluid.getDisplayName(), row.getLong("amount"), row.getLong("capacity"));
            var icon = IElementHelper.get().fluid(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount())).size(new Vec2(16, 16));
            JadeText.add(tooltip, icon, text);
        }
    }

    private static void line(ITooltip tooltip, String key, Object... args) {
        JadeText.detail(tooltip, "jade.gt6ce.omni." + key, args);
    }
    private static String format(long value) { return String.format(Locale.US, "%,d", value); }
    @Override public ResourceLocation getUid() { return UID; }
    @Override public int getDefaultPriority() { return 1002; }
}
