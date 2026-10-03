/*
 * Energy behavior adapted from GregTech-6 Team / Gregorius Techneticies,
 * MultiItemRandomTools, EnergyStat and EnergyStatDebug, LGPL-3.0-or-later.
 */
package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.content.tool.ScannerEnergyRules;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.behavior.BehaviorScanner;
import com.gregtech.gregtech.item.behavior.BehaviorCropnalyzer;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

/** Original scanner identities share a charge-only EnergyStat; debug has infinite energy. */
public final class ScannerItem extends TechItem implements IItemEnergy {
    private final ScannerEnergyRules.Spec spec;

    public ScannerItem(String name, ScannerEnergyRules.Spec spec, Properties properties) {
        super(name, properties);
        this.spec = java.util.Objects.requireNonNull(spec);
    }

    public ScannerEnergyRules.Spec spec() { return spec; }

    private long stored(ItemStack stack) {
        if (spec.debug()) return 4000000000000000000L;
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getLong(ScannerEnergyRules.ENERGY_KEY));
    }

    private void setCharge(ItemStack stack, long charge) {
        if (charge > 0) stack.getOrCreateTag().putLong(ScannerEnergyRules.ENERGY_KEY, charge);
        else if (stack.hasTag()) {
            stack.getTag().remove(ScannerEnergyRules.ENERGY_KEY);
            if (stack.getTag().isEmpty()) stack.setTag(null);
        }
    }

    /** MultiItem:392-399: discharged items stack; charged items do not. */
    @Override public int getMaxStackSize(ItemStack stack) {
        return !spec.debug() && stored(stack) > 0 ? 1 : 64;
    }

    @Override public boolean isEnergyType(ItemStack stack, GregTechTags.Tag type) {
        return spec.debug() || type == null || type == GregTechTags.Energy.EU;
    }
    @Override public long getEnergyCapacity(ItemStack stack, GregTechTags.Tag type) {
        return isEnergyType(stack, type) ? spec.capacity() : 0;
    }
    @Override public long getEnergyStored(ItemStack stack, GregTechTags.Tag type) {
        return isEnergyType(stack, type) ? stored(stack) : 0;
    }
    @Override public boolean canEnergyInjection(ItemStack stack, GregTechTags.Tag type, long size) {
        return isEnergyType(stack, type) && spec.acceptsPacket(stack.getCount(), size);
    }
    @Override public boolean canEnergyExtraction(ItemStack stack, GregTechTags.Tag type, long size) {
        return spec.debug();
    }
    @Override public long doEnergyInjection(GregTechTags.Tag type, ItemStack stack, long size, long amount,
            Level level, BlockPos pos, boolean execute) {
        if (!isEnergyType(stack, type)) return 0;
        if (spec.debug()) return amount;
        long stored = stored(stack);
        long accepted = spec.injectionPackets(stack.getCount(), stored, size, amount);
        // EnergyStat:75,128-149 stores the last oversized partial packet, without clamping it.
        if (execute && accepted > 0) setCharge(stack, stored + accepted * Math.abs(size));
        return accepted;
    }
    @Override public long doEnergyExtraction(GregTechTags.Tag type, ItemStack stack, long size, long amount,
            Level level, BlockPos pos, boolean execute) {
        return spec.debug() ? amount : 0;
    }

    /** Payment happens after gathering the scan, exactly as Behavior_Scanner/Cropnalyzer do. */
    public boolean useEnergy(ItemStack stack, long cost, Player player) {
        boolean free = spec.debug() || player.getAbilities().instabuild;
        var use = ScannerEnergyRules.use(stored(stack), cost, free);
        if (!free) {
            setCharge(stack, use.remaining());
            player.getInventory().setChanged();
        }
        return use.successful();
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide) {
            // Prediction must neither promote crop scan levels nor spend/send results twice.
            return spec.scansBlocks() || BehaviorCropnalyzer.supports(context.getLevel(), context.getClickedPos())
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        var outcome = ItemBehaviors.useOn(context.getLevel(), context.getClickedPos(),
                context.getClickedFace(), context.getPlayer(), stack, 0, 0, 0);
        return outcome.acted() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private void scanEntity(Player player, Entity entity) {
        if (player instanceof ServerPlayer)
            BehaviorScanner.scanEntity(entity, spec.scanLevel()).ifPresent(line ->
                    player.displayClientMessage(Component.literal(line), false));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            LivingEntity target, InteractionHand hand) {
        if (!spec.scansBlocks()) return InteractionResult.PASS;
        scanEntity(player, target);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!spec.scansBlocks()) return false;
        scanEntity(player, target);
        return true;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return !spec.debug() && stored(stack) > 0; }
    @Override public int getBarWidth(ItemStack stack) {
        if (spec.debug()) return 0;
        return (int) (Math.min(spec.capacity(), stored(stack)) * 13 / spec.capacity());
    }
    @Override public int getBarColor(ItemStack stack) { return 0x00CC00; }

    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (spec.debug()) tooltip.add(Component.translatable("tooltip.gregtech.scanner.infinite"));
        else tooltip.add(Component.translatable("tooltip.gregtech.scanner.energy",
                Math.min(spec.capacity(), stored(stack)), spec.capacity(), spec.voltage()));
        if (spec.scansBlocks()) tooltip.add(Component.translatable("tooltip.gregtech.scanner.blocks"));
        tooltip.add(Component.translatable("tooltip.gregtech.scanner.crops"));
    }
}

