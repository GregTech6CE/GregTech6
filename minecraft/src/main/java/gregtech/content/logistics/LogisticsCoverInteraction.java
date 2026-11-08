package gregtech.content.logistics;

//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;

*///?} else {
//?}
import gregtech.engine.stack.StackData;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** GT6 logistics-cover placement and the item/fluid filter, priority, and quota controls. */
public final class LogisticsCoverInteraction {
    private static final String VALUE = "gt.logistics.value";
    private static final String ITEM_FILTER = "gt.filter.item";
    private static final String FLUID_FILTER = "gt.filter.fluid";

    private LogisticsCoverInteraction() {
    }

    public static int value(ItemStack cover) {
        return StackData.getInt(cover, VALUE, 0);
    }

    public static int targetStackSize(ItemStack cover) {
        return (value(cover) >> 2) & 127;
    }

    public static ItemStack itemFilter(ItemStack cover, net.minecraft.core.HolderLookup.Provider lookup) {
        var tag = StackData.read(cover);
        return tag != null && tag.contains(ITEM_FILTER)
                ? gregtech.engine.stack.NbtCompat.readItem(tag.getCompound(ITEM_FILTER), lookup)
                : ItemStack.EMPTY;
    }
//? if >=1.21.1 {
//?} else {
    public static ItemStack itemFilter(ItemStack cover) { return itemFilter(cover, null); }
//?}

    /** GT6 fluid logistics covers store a sample fluid under {@code gt.filter.fluid}. */
    public static FluidStack fluidFilter(ItemStack cover, net.minecraft.core.HolderLookup.Provider lookup) {
        if (cover == null) return FluidStack.EMPTY;
        var tag = StackData.read(cover);
        if (tag == null || !tag.contains(FLUID_FILTER)) return FluidStack.EMPTY;
        FluidStack filter = gregtech.engine.stack.NbtCompat.readFluid(tag.getCompound(FLUID_FILTER), lookup);
        return filter == null ? FluidStack.EMPTY : filter;
    }
//? if >=1.21.1 {
//?} else {
    public static FluidStack fluidFilter(ItemStack cover) { return fluidFilter(cover, null); }
//?}

    /** Matches current item and damage only; legacy metadata variants may now live in stack data. */
    public static boolean matches(ItemStack filter, ItemStack candidate) {
        return filter.isEmpty() || (!candidate.isEmpty() && filter.is(candidate.getItem())
                && filter.getDamageValue() == candidate.getDamageValue());
    }

    public static boolean setItemFilter(LogisticsCovers covers, Direction face, ItemStack template) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (template.isEmpty() || type == null || !type.filtered()
                || type.channel() != LogisticsCoverType.Channel.ITEM || !itemFilter(cover, lookup(covers)).isEmpty())
            return false;
        StackData.update(cover,
                tag -> tag.put(ITEM_FILTER, gregtech.engine.stack.NbtCompat.saveItem(
                        template.copyWithCount(1), lookup(covers))));
        covers.changed();
        return true;
    }

    public static boolean clearItemFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.ITEM)
            return false;
        StackData.remove(cover, ITEM_FILTER);
        covers.changed();
        return true;
    }

    /** A filtered fluid bus takes its sample only once; the soft hammer resets it. */
    public static boolean setFluidFilter(LogisticsCovers covers, Direction face, FluidStack sample) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (sample == null || sample.isEmpty() || type == null || !type.filtered()
                || type.channel() != LogisticsCoverType.Channel.FLUID
                || StackData.readOrEmpty(cover).contains(FLUID_FILTER))
            return false;
        StackData.update(cover,
                tag -> tag.put(FLUID_FILTER, gregtech.engine.stack.NbtCompat.saveFluid(
                        sample.copy(), lookup(covers))));
        covers.changed();
        return true;
    }

    public static boolean clearFluidFilter(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.filtered() || type.channel() != LogisticsCoverType.Channel.FLUID)
            return false;
        StackData.remove(cover, FLUID_FILTER);
        covers.changed();
        return true;
    }

    public static boolean cyclePriority(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.usesPriority())
            return false;
        int previousValue = value(cover);
        StackData.putInt(cover, VALUE, (previousValue & ~3) | ((previousValue + 1) & 3));
        covers.changed();
        return true;
    }

    public static boolean cycleTargetStackSize(LogisticsCovers covers, Direction face) {
        ItemStack cover = covers.get(face);
        var type = LogisticsCoverType.of(cover);
        if (type == null || !type.targetStackSize())
            return false;
        int previousValue = value(cover);
        int nextTargetSize = (targetStackSize(cover) + 1) % 65;
        StackData.putInt(cover, VALUE, (previousValue & 3) | (nextTargetSize << 2));
        covers.changed();
        return true;
    }

    private static void describe(Player player, ItemStack cover) {
        var type = LogisticsCoverType.of(cover);
        if (type != null && type.usesPriority())
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Priority: " + type.effectivePriority(value(cover))), false);
        if (type != null && type.targetStackSize())
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Target Stacksize: " + targetStackSize(cover)), false);
    }

    public static InteractionResult use(LogisticsCoverHost host, Level level, Player player,
                                        InteractionHand hand, Direction face) {
        BlockEntity owner = (BlockEntity) host;
        if (!player.mayBuild() || !level.mayInteract(player, owner.getBlockPos())) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        LogisticsCovers covers = host.logisticsCovers();
        LogisticsCoverType heldType = LogisticsCoverType.of(held);
        if (gregtech.content.cover.CoverPlacement.placing(player) && heldType != null) {
            if (!host.canLogistics(null)) return InteractionResult.FAIL;
            if (!level.isClientSide && covers.attach(face, held) && !player.getAbilities().instabuild)
                held.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack installed = covers.get(face);
        if (installed.isEmpty()) return InteractionResult.PASS;
        LogisticsCoverType installedType = LogisticsCoverType.of(installed);
        if (installedType == null) return InteractionResult.PASS;
        if (GTToolHelper.isMagnifyingGlass(held)) {
            if (!level.isClientSide) describe(player, installed);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean crowbar = GTToolHelper.matchesTool(held, GTToolType.CROWBAR);
        boolean screwdriver = GTToolHelper.isScrewdriver(held);
        boolean cutter = GTToolHelper.isWireCutter(held);
        boolean softHammer = GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER);
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
                setFluidFilter(covers, face, gregtech.content.cover.UtilityCoverInteraction.sample(held));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static net.minecraft.core.HolderLookup.Provider lookup(LogisticsCovers covers) {
//? if >=1.21.1 {
/*        return covers.registries();
*///?} else {
        return null;
//?}
    }
}
