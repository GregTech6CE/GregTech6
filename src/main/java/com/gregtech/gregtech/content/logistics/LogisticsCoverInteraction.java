package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** GT6 logistics-cover placement and the item/fluid filter, priority, and quota controls. */
public final class LogisticsCoverInteraction {
    private static final String VALUE = "gt.logistics.value";
    private static final String ITEM_FILTER = "gt.filter.item";
    private static final String FLUID_FILTER = "gt.filter.fluid";

    private LogisticsCoverInteraction() {}

    public static int value(ItemStack cover) {
        return cover.hasTag() ? cover.getTag().getInt(VALUE) : 0;
    }

    public static int targetStackSize(ItemStack cover) {
        return (value(cover) >> 2) & 127;
    }

    public static ItemStack itemFilter(ItemStack cover) {
        return cover.hasTag() && cover.getTag().contains(ITEM_FILTER)
                ? ItemStack.of(cover.getTag().getCompound(ITEM_FILTER)) : ItemStack.EMPTY;
    }

    /** GT6 fluid logistics covers store a sample fluid under {@code gt.filter.fluid}. */
    public static FluidStack fluidFilter(ItemStack cover) {
        if (cover == null || !cover.hasTag() || !cover.getTag().contains(FLUID_FILTER)) return FluidStack.EMPTY;
        FluidStack filter = FluidStack.loadFluidStackFromNBT(cover.getTag().getCompound(FLUID_FILTER));
        return filter == null ? FluidStack.EMPTY : filter;
    }

    /** Original filtered item covers ignore NBT but still distinguish item and damage value. */
    public static boolean matches(ItemStack filter, ItemStack candidate) {
        return filter.isEmpty() || (!candidate.isEmpty() && filter.is(candidate.getItem())
                && filter.getDamageValue() == candidate.getDamageValue());
    }

    public static boolean setItemFilter(LogisticsCovers covers, Direction face, ItemStack template) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (template.isEmpty() || type == null || !type.filtered()
                || type.channel() != LogisticsCoverType.Channel.ITEM || !itemFilter(cover).isEmpty()) return false;
        cover.getOrCreateTag().put(ITEM_FILTER, template.copyWithCount(1).save(new CompoundTag()));
        covers.changed();
        return true;
    }

    public static boolean clearItemFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.ITEM) return false;
        if (cover.hasTag()) cover.getTag().remove(ITEM_FILTER);
        covers.changed();
        return true;
    }

    /** A filtered fluid bus takes its sample only once; the soft hammer resets it. */
    public static boolean setFluidFilter(LogisticsCovers covers, Direction face, FluidStack sample) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (sample == null || sample.isEmpty() || type == null || !type.filtered()
                || type.channel() != LogisticsCoverType.Channel.FLUID
                || cover.getOrCreateTag().contains(FLUID_FILTER)) return false;
        cover.getOrCreateTag().put(FLUID_FILTER, sample.copy().writeToNBT(new CompoundTag()));
        covers.changed();
        return true;
    }

    public static boolean clearFluidFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.FLUID) return false;
        if (cover.hasTag()) cover.getTag().remove(FLUID_FILTER);
        covers.changed();
        return true;
    }

    public static boolean cyclePriority(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.usesPriority()) return false;
        int old = value(cover);
        cover.getOrCreateTag().putInt(VALUE, (old & ~3) | ((old + 1) & 3));
        covers.changed();
        return true;
    }

    public static boolean cycleTargetStackSize(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.targetStackSize()) return false;
        int old = value(cover);
        int next = (targetStackSize(cover) + 1) % 65;
        cover.getOrCreateTag().putInt(VALUE, (old & 3) | (next << 2));
        covers.changed();
        return true;
    }

    public static InteractionResult use(LogisticsCoverHost host, Level level, Player player,
                                        InteractionHand hand, Direction face) {
        BlockEntity owner = (BlockEntity) host;
        if (!player.mayBuild() || !level.mayInteract(player, owner.getBlockPos())) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        LogisticsCovers covers = host.logisticsCovers();
        LogisticsCoverType heldType = LogisticsCoverType.of(held);
        if (heldType != null) {
            if (!host.canLogistics(null)) return InteractionResult.FAIL;
            if (!level.isClientSide && covers.attach(face, held) && !player.getAbilities().instabuild)
                held.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack installed = covers.get(face);
        if (installed.isEmpty()) return InteractionResult.PASS;
        LogisticsCoverType installedType = LogisticsCoverType.of(installed);
        if (installedType == null) return InteractionResult.PASS;
        boolean crowbar = GTToolHelper.matchesTool(held, GTToolType.CROWBAR);
        boolean screwdriver = GTToolHelper.isScrewdriver(held);
        boolean cutter = GTToolHelper.isWireCutter(held);
        boolean softHammer = GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER);
        if (crowbar || screwdriver || cutter || softHammer) {
            boolean handled = crowbar || screwdriver && installedType.usesPriority()
                    || cutter && installedType.targetStackSize()
                    || softHammer && installedType.filtered();
            if (!handled) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (crowbar) {
                    ItemStack removed = covers.remove(face);
                    if (!player.addItem(removed)) player.drop(removed, false);
                } else {
                    if (screwdriver) cyclePriority(covers, face);
                    else if (cutter) cycleTargetStackSize(covers, face);
                    else if (installedType.channel() == LogisticsCoverType.Channel.FLUID)
                        clearFluidFilter(covers, face);
                    else clearItemFilter(covers, face);
                }
                GTToolHelper.damageForUse(held, 1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (installedType.channel() == LogisticsCoverType.Channel.ITEM && installedType.filtered()) {
            if (!level.isClientSide) setItemFilter(covers, face, held);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (installedType.channel() == LogisticsCoverType.Channel.FLUID && installedType.filtered()) {
            if (!level.isClientSide && !held.isEmpty()) {
                var container = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
                if (container != null) {
                    FluidStack sample = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                    setFluidFilter(covers, face, sample);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
