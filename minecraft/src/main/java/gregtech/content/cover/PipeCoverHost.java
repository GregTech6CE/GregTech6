package gregtech.content.cover;

import gregtech.content.cover.CoverUtilityBehaviors.FilterAction;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Cover clicks handled by a pipe after its connection tools and before its own interactions. */
public interface PipeCoverHost extends CoverStore.Hooks {
    boolean supportsFilterCover(String behavior);

    default void wakeAfterFilterChange() {}

    @Override
    default boolean consumesTool(ItemStack tool) {
        return PanelCoverHost.blockConsumesTool(this, tool)
                || GTToolHelper.matchesTool(tool, GTToolType.CROWBAR)
                || GTToolHelper.isScrewdriver(tool)
                || GTToolHelper.isSoftHammer(tool)
                || GTToolHelper.isMagnifyingGlass(tool);
    }

    default void syncCovers() {
        var owner = coverOwner();
        owner.setChanged();
        var level = owner.getLevel();
        if (level != null) level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
    }

    default boolean clickFilterCover(Direction side, ItemStack held) {
        var level = coverOwner().getLevel();
        if (level == null || held == null || held.isEmpty()) return false;
        ItemStack cover = getCover(side);
        String behavior = CoverItems.behavior(cover);
        if (behavior == null || !supportsFilterCover(behavior)) return false;

        if (CoverUtilityBehaviors.FILTER_FLUID.equals(behavior)) {
            var sample = UtilityCoverInteraction.sample(held);
            if (level.isClientSide)
                return CoverUtilityBehaviors.canSetFluidFilter(cover, sample, level.registryAccess());
            if (!CoverUtilityBehaviors.setFluidFilter(cover, sample, level.registryAccess())) return false;
        } else {
            if (level.isClientSide) return CoverUtilityBehaviors.canSetItemFilter(cover, held, level.registryAccess());
            if (!CoverUtilityBehaviors.setItemFilter(cover, held, level.registryAccess())) return false;
        }
        syncCovers();
        return true;
    }

    default boolean configureFilterCover(Direction side, FilterAction action) {
        ItemStack cover = getCover(side);
        String behavior = CoverItems.behavior(cover);
        if (behavior == null || !supportsFilterCover(behavior)) return false;
        var level = coverOwner().getLevel();
        if (level != null && level.isClientSide) return true;
        action.apply(cover);
        syncCovers();
        wakeAfterFilterChange();
        return true;
    }

    default InteractionResult usePipeCover(Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult panel = PanelCoverInteraction.use(this, player, hand, hit, false);
        if (panel != InteractionResult.PASS) return panel;
        var level = coverOwner().getLevel();
        if (level == null) return InteractionResult.PASS;
        Direction side = hit.getDirection();
        ItemStack held = player.getItemInHand(hand);

        if (CoverPlacement.placing(player) && CoverItems.isCover(held)) {
            if (!level.isClientSide && attachCover(side, held) && !player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (GTToolHelper.matchesTool(held, GTToolType.CROWBAR)) {
            if (!level.isClientSide) {
                ItemStack removed = removeCover(side);
                if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty()) return InteractionResult.PASS;
        if (GTToolHelper.matchesTool(held, GTToolType.SCREWDRIVER)
                && configureFilterCover(side, FilterAction.TOGGLE_MODE))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER) && configureFilterCover(side, FilterAction.CLEAR))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (consumesTool(held) || !clickFilterCover(side, held)) return InteractionResult.PASS;
        if (!level.isClientSide) wakeAfterFilterChange();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
