package gregtech.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.fluid.JadeFluidObject;
import net.minecraft.world.phys.Vec2;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/** Bounded, read-only inspection rows. Names are resolved in the viewing client's language. */
final class JadeInspection {
    private static final int MAX_ROWS = 96;
    private final BlockAccessor accessor;
    private final String dataKey;
    private final ListTag rows = new ListTag();
    private boolean groupedInventory;
    record Label(String key) {}

    JadeInspection(BlockAccessor accessor) { this(accessor, "gt_omni"); }
    JadeInspection(BlockAccessor accessor, String dataKey) { this.accessor = accessor; this.dataKey = dataKey; }
    static Label label(String suffix) { return new Label("jade.gt6ce.omni." + suffix); }

    CompoundTag line(String suffix, Object... arguments) {
        return translated("jade.gt6ce.omni." + suffix, arguments);
    }

    CompoundTag translated(String key, Object... arguments) {
        if (rows.size() >= MAX_ROWS) return null;
        var row = new CompoundTag();
        row.putString("key", key);
        var args = new ListTag();
        for (Object argument : arguments) {
            var arg = new CompoundTag();
            if (argument instanceof Label label) arg.putString("key", label.key());
            else if (argument instanceof gregtech.content.cover.CoverDisplayInfo.Text text) arg.putString("key", text.key());
            else if (argument instanceof ItemStack item) {
//? if >=1.21.1 {
                /*if (!item.isEmpty()) arg.put("item", item.save(accessor.getLevel().registryAccess()));
*///?} else {
                if (!item.isEmpty()) arg.put("item", item.save(new CompoundTag()));
//?}
            } else if (argument instanceof FluidStack fluid) {
//? if >=1.21.1 {
                /*if (!fluid.isEmpty()) arg.put("fluid", fluid.save(accessor.getLevel().registryAccess()));
*///?} else {
                if (!fluid.isEmpty()) arg.put("fluid", fluid.writeToNBT(new CompoundTag()));
//?}
            } else arg.putString("text", String.valueOf(argument));
            args.add(arg);
        }
        row.put("args", args);
        rows.add(row);
        return row;
    }

    void item(String role, int slot, ItemStack item) {
        if (item == null || item.isEmpty()) return;
        var row = line("item", label(role), slot + 1, item, item.getCount());
        group(row, "items", role);
    }

    void inventory(String role, IItemHandler inventory, int first, int count) {
        if (inventory == null) return;
        for (int i = first; i < Math.min(inventory.getSlots(), first + count) && rows.size() < MAX_ROWS; i++)
            item(role, i - first, inventory.getStackInSlot(i));
    }

    void fluid(String role, int slot, FluidStack fluid, long amount, long capacity) {
        if (fluid == null || fluid.isEmpty()) return;
        var row = "pending".equals(role) ? line("pending_fluid", slot + 1, fluid, amount)
                : line("fluid", label(role), slot + 1, fluid, amount, capacity);
        group(row, "fluids", role);
    }

    private static void group(CompoundTag row, String kind, String role) {
        if (row == null) return;
        row.putString("kind", kind);
        row.putString("role", role);
    }

    void groupedInventory() { groupedInventory = true; }

    void tanks(String role, IFluidHandler tanks) {
        if (tanks == null) return;
        for (int i = 0; i < Math.min(16, tanks.getTanks()); i++) {
            var fluid = tanks.getFluidInTank(i);
            fluid(role, i, fluid, fluid.getAmount(), tanks.getTankCapacity(i));
        }
    }

    void tanks(String role, gregtech.engine.fluid.FluidTankGT[] tanks) {
        if (tanks == null) return;
        for (int i = 0; i < Math.min(16, tanks.length); i++)
            fluid(role, i, tanks[i].getFluid(), tanks[i].getAmount(), tanks[i].capacity());
    }

    void state(boolean enabled, boolean active) { line("state", label(!enabled ? "disabled" : active ? "working" : "waiting")); }
    void flag(String suffix, boolean value) { line(suffix, label(value ? "yes" : "no")); }
    void write(CompoundTag tag) {
        if (!rows.isEmpty()) tag.put(dataKey, rows);
        if (groupedInventory) tag.putBoolean(dataKey + "_inventory", true);
    }

    static void render(ITooltip tooltip, BlockAccessor accessor) {
        render(tooltip, accessor, "gt_omni");
    }

    static void render(ITooltip tooltip, BlockAccessor accessor, String dataKey) {
        if (accessor.getServerData().getBoolean(dataKey + "_inventory")) {
//? if >=1.21.1 {
            /*tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE);
            tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
*///?} else {
            tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_ITEM_STORAGE);
            tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE);
//?}
        }
        var rows = accessor.getServerData().getList(dataKey, 10);
        var groups = new LinkedHashMap<String, List<CompoundTag>>();
        for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
            var row = rows.getCompound(i);
            if (row.contains("kind")) {
                groups.computeIfAbsent(row.getString("role") + "/" + row.getString("kind"), key -> new ArrayList<>()).add(row);
                continue;
            }
            renderLine(tooltip, accessor, row);
        }
        // In-progress products must remain visible even when the output slots are still empty.
        for (String role : new String[]{"pending", "input", "output", "program"}) {
            for (String kind : new String[]{"items", "fluids"}) {
                var group = groups.remove(role + "/" + kind);
                if (group != null) renderGroup(tooltip, accessor, group);
            }
        }
        for (var group : groups.values()) renderGroup(tooltip, accessor, group);
    }

    private static void renderGroup(ITooltip tooltip, BlockAccessor accessor, List<CompoundTag> rows) {
        var first = rows.get(0);
        var role = first.getString("role");
        var color = switch (role) {
            case "pending", "prepared" -> net.minecraft.ChatFormatting.GOLD;
            case "input" -> net.minecraft.ChatFormatting.AQUA;
            case "output" -> net.minecraft.ChatFormatting.GREEN;
            default -> net.minecraft.ChatFormatting.WHITE;
        };
        tooltip.add(Component.translatable("jade.gt6ce.omni.group_" + first.getString("kind"),
                Component.translatable("jade.gt6ce.omni." + role)).withStyle(color));
        if ("items".equals(first.getString("kind"))) {
            for (var row : rows) {
                var args = row.getList("args", 10);
                for (int j = 0; j < args.size(); j++) {
                    var arg = args.getCompound(j);
                    if (!arg.contains("item")) continue;
                    var item = readItem(accessor, arg);
                    if (item.isEmpty()) continue;
                    JadeText.item(tooltip, item);
                }
            }
        } else for (var row : rows) renderLine(tooltip, accessor, row);
    }

    private static ItemStack readItem(BlockAccessor accessor, CompoundTag arg) {
//? if >=1.21.1 {
        /*return ItemStack.parseOptional(accessor.getLevel().registryAccess(), arg.getCompound("item"));
*///?} else {
        return ItemStack.of(arg.getCompound("item"));
//?}
    }

    private static FluidStack readFluid(BlockAccessor accessor, CompoundTag arg) {
//? if >=1.21.1 {
        /*return FluidStack.parseOptional(accessor.getLevel().registryAccess(), arg.getCompound("fluid"));
*///?} else {
        return FluidStack.loadFluidStackFromNBT(arg.getCompound("fluid"));
//?}
    }

    private static void renderLine(ITooltip tooltip, BlockAccessor accessor, CompoundTag row) {
        var args = row.getList("args", 10);
        Object[] values = new Object[args.size()];
        snownee.jade.api.ui.IElement icon = null;
        for (int j = 0; j < args.size(); j++) {
            var arg = args.getCompound(j);
            Component value = JadeText.unit(arg.getString("text"));
            if (arg.contains("key")) value = JadeText.status(arg.getString("key"));
            else if (arg.contains("item")) {
                var item = readItem(accessor, arg);
                value = item.getHoverName();
                if (!item.isEmpty() && icon == null) icon = IElementHelper.get().item(item);
            } else if (arg.contains("fluid")) {
                var fluid = readFluid(accessor, arg);
                if (!fluid.isEmpty()) {
                    value = fluid.getDisplayName();
                    if (icon == null) icon = IElementHelper.get().fluid(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount())).size(new Vec2(16, 16));
                }
            }
            values[j] = value;
        }
        var text = Component.translatable(row.getString("key"), values);
        if (row.getString("key").equals("jade.gt6ce.omni.state") && values.length == 1)
            tooltip.add((Component) values[0]);
        else if (icon == null) JadeText.detail(tooltip, row.getString("key"), values);
        else JadeText.add(tooltip, icon, text);
    }
}
