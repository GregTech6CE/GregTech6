/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from TileEntityBase09PowerCell and TileEntityBase08Battery. */
package gregtech.item;

import gregtech.api.energy.item.IItemEnergy;
import gregtech.block.energy.PowerCellBlock;
import gregtech.content.energy.PowerCellSpec;
import gregtech.content.energy.ItemBatteryRules;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class PowerCellItem extends BlockItem implements IItemEnergy {
    public static final String CHARGE = "gt.charge";
    public PowerCellItem(PowerCellBlock block) { super(block, new Properties().stacksTo(block.spec().filled() ? 1 : 16)); }
    public PowerCellSpec spec() { return ((PowerCellBlock) getBlock()).spec(); }
    public long stored(ItemStack stack) {
//? if >=1.21.1 {
        /*CompoundTag tag = gregtech.platform.neoforge.StackCustomData.read(stack);
*///?} else {
        CompoundTag tag = stack.getTag();
//?}
        return tag == null || !tag.contains(CHARGE) ? spec().capacity() : spec().usableRemainder(Math.max(0, tag.getLong(CHARGE)));
    }
    public void setCharge(ItemStack stack, long value) {
        long charge = spec().usableRemainder(Math.max(0, value));
//? if >=1.21.1 {
        /*gregtech.platform.neoforge.StackCustomData.update(stack, tag -> {
            if (spec().filled()) tag.putLong(CHARGE, charge); else tag.remove(CHARGE);
        });
*///?} else {
        if (spec().filled()) stack.getOrCreateTag().putLong(CHARGE, charge);
        else if (stack.hasTag()) {
            stack.getTag().remove(CHARGE);
            if (stack.getTag().isEmpty()) stack.setTag(null);
        }
//?}
    }
    @Override public ItemStack afterEnergyTransfer(ItemStack stack) {
        return spec().filled() && stored(stack) == 0
                ? new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", spec().emptyId())), stack.getCount()) : stack;
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? super.place(context) : InteractionResult.PASS;
    }
    @Override public boolean isEnergyType(ItemStack stack, GregTechTags.Tag type) { return type == GregTechTags.Energy.EU; }
    @Override public boolean canEnergyInjection(ItemStack stack, GregTechTags.Tag type, long size) { return false; }
    @Override public long doEnergyInjection(GregTechTags.Tag type, ItemStack stack, long size, long amount, Level level, BlockPos pos, boolean execute) { return 0; }
    @Override public boolean canEnergyExtraction(ItemStack stack, GregTechTags.Tag type, long size) {
        return isEnergyType(stack, type) && stack.getCount() == 1 && size >= 1 && size <= spec().voltage();
    }
    @Override public long getEnergyCapacity(ItemStack stack, GregTechTags.Tag type) { return isEnergyType(stack, type) ? spec().capacity() : 0; }
    @Override public long getEnergyStored(ItemStack stack, GregTechTags.Tag type) { return isEnergyType(stack, type) ? stored(stack) : 0; }
    @Override public long doEnergyExtraction(GregTechTags.Tag type, ItemStack stack, long size, long amount, Level level, BlockPos pos, boolean execute) {
        if (size == Long.MIN_VALUE || amount <= 0 || !canEnergyExtraction(stack, type, Math.abs(size))) return 0;
        long packet = Math.abs(size);
        long accepted = ItemBatteryRules.extractionPackets(stored(stack), spec().voltage(), packet, amount);
        if (execute && accepted > 0) setCharge(stack, stored(stack) - accepted * packet);
        return accepted;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.player.Player player
                && slot >= 0 && slot < player.getInventory().getContainerSize() && player.getInventory().getItem(slot) == stack) {
            ItemStack replacement = afterEnergyTransfer(stack);
            if (replacement != stack) player.getInventory().setItem(slot, replacement);
        }
    }
    @Override public boolean isBarVisible(ItemStack stack) { return spec().filled(); }
    @Override public int getBarWidth(ItemStack stack) { return spec().filled() ? (int) (13 * stored(stack) / spec().capacity()) : 0; }
    @Override public int getBarColor(ItemStack stack) { return 0x55FF55; }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tip, flags);
*///?} else {
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flags) {
        super.appendHoverText(stack, level, tip, flags);
//?}
        if (spec().filled()) {
            tip.add(Component.translatable("tooltip.gt6ce.electric_tool.energy", stored(stack), spec().capacity()));
            tip.add(Component.translatable("tooltip.gt6ce.chemical_battery.packet", 1, spec().voltage()));
        }
        tip.add(Component.translatable("tooltip.gt6ce.power_cell.fuel"));
        tip.add(Component.translatable("tooltip.gt6ce.chemical_battery.place"));
    }
}
