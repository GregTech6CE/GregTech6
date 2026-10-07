package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import com.gregtech.gregtech.platform.neoforge.StackCustomData;
import net.minecraft.core.HolderLookup;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** GT6 logistics-cover placement and the item/fluid filter, priority, and quota controls. */
public final class LogisticsCoverInteraction {
    private static final String VALUE = "gt.logistics.value";
    private static final String ITEM_FILTER = "gt.filter.item";
    private static final String FLUID_FILTER = "gt.filter.fluid";

    private LogisticsCoverInteraction() {}

    public static int value(ItemStack cover) {
        return StackCustomData.read(cover).getInt(VALUE);
    }

    public static int targetStackSize(ItemStack cover) {
        return (value(cover) >> 2) & 127;
    }

    public static ItemStack itemFilter(ItemStack cover,HolderLookup.Provider lookup) {
        var tag=StackCustomData.read(cover);
        return tag.contains(ITEM_FILTER)?ItemStack.parseOptional(lookup,tag.getCompound(ITEM_FILTER)):ItemStack.EMPTY;
    }

    /** GT6 fluid logistics covers store a sample fluid under {@code gt.filter.fluid}. */
    public static FluidStack fluidFilter(ItemStack cover,HolderLookup.Provider lookup) {
        var tag=StackCustomData.read(cover);
        return tag.contains(FLUID_FILTER)?FluidStack.parseOptional(lookup,tag.getCompound(FLUID_FILTER)):FluidStack.EMPTY;
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
                || type.channel() != LogisticsCoverType.Channel.ITEM || !itemFilter(cover,covers.registries()).isEmpty()) return false;
        StackCustomData.update(cover,tag->tag.put(ITEM_FILTER,template.copyWithCount(1).save(covers.registries())));
        covers.changed();
        return true;
    }

    public static boolean clearItemFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.ITEM) return false;
        StackCustomData.update(cover,tag->tag.remove(ITEM_FILTER));
        covers.changed();
        return true;
    }

    /** A filtered fluid bus takes its sample only once; the soft hammer resets it. */
    public static boolean setFluidFilter(LogisticsCovers covers, Direction face, FluidStack sample) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (sample == null || sample.isEmpty() || type == null || !type.filtered()
                || type.channel() != LogisticsCoverType.Channel.FLUID
                || StackCustomData.read(cover).contains(FLUID_FILTER)) return false;
        StackCustomData.update(cover,tag->tag.put(FLUID_FILTER,sample.copy().save(covers.registries())));
        covers.changed();
        return true;
    }

    public static boolean clearFluidFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.FLUID) return false;
        StackCustomData.update(cover,tag->tag.remove(FLUID_FILTER));
        covers.changed();
        return true;
    }

    public static boolean cyclePriority(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.usesPriority()) return false;
        int old = value(cover);
        StackCustomData.update(cover,tag->tag.putInt(VALUE,(old & ~3) | ((old + 1) & 3)));
        covers.changed();
        return true;
    }

    public static boolean cycleTargetStackSize(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.targetStackSize()) return false;
        int old = value(cover);
        int next = (targetStackSize(cover) + 1) % 65;
        StackCustomData.update(cover,tag->tag.putInt(VALUE,(old & 3) | (next << 2)));
        covers.changed();
        return true;
    }

    private static void describe(Player player,ItemStack cover) {
        var type=LogisticsCoverType.of(cover);
        if(type!=null&&type.usesPriority())player.displayClientMessage(net.minecraft.network.chat.Component.literal("Priority: "+type.effectivePriority(value(cover))),false);
        if(type!=null&&type.targetStackSize())player.displayClientMessage(net.minecraft.network.chat.Component.literal("Target Stacksize: "+targetStackSize(cover)),false);
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
        if (NeoToolBindings.isMagnifyingGlass(held)) {
            if (!level.isClientSide) describe(player, installed);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean crowbar = NeoToolBindings.matches(held,"crowbar");
        boolean screwdriver = NeoToolBindings.isScrewdriver(held);
        boolean cutter = NeoToolBindings.isWireCutter(held);
        boolean softHammer = NeoToolBindings.matches(held,"soft_hammer");
        if (crowbar || screwdriver || cutter || softHammer) {
            boolean handled = crowbar || screwdriver && installedType.usesPriority()
                    || cutter && installedType.targetStackSize()
                    || softHammer && installedType.filtered();
            // Every installed logistics attachment intercepts connector tools, even when it has
            // no quota/priority setting. Otherwise the underlying wire reconnects its covered face.
            if (!handled) return InteractionResult.sidedSuccess(level.isClientSide);
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
                if (!crowbar) describe(player, installed);
                NeoToolBindings.damageForUse(held, 1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (installedType.channel() == LogisticsCoverType.Channel.ITEM && installedType.filtered()) {
            if (!level.isClientSide) setItemFilter(covers, face, held);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (installedType.channel() == LogisticsCoverType.Channel.FLUID && installedType.filtered()) {
            if (!level.isClientSide && !held.isEmpty()) {
                setFluidFilter(covers, face, com.gregtech.gregtech.content.cover.UtilityCoverInteraction.sample(held));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
