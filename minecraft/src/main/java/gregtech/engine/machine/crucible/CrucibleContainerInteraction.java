package gregtech.engine.machine.crucible;

import gregtech.api.material.GTValues;
import gregtech.content.material.Materials;
import gregtech.engine.fluid.FluidContainerInteraction;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.fluid.MoltenMaterialUnits;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.content.item.FluidDisplayItemLike;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import java.util.List;

/** Container transfers are claimed on both sides so BucketItem cannot also place world fluid. */
public final class CrucibleContainerInteraction {
    private CrucibleContainerInteraction() {}

    /** Recognizes real containers as well as creative fluid display carriers. */
    public static boolean handles(ItemStack stack) {
        return stack.getItem() instanceof FluidDisplayItemLike
                || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET) || stack.is(Items.BUCKET)
                || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.POWDER_SNOW_BUCKET) || stack.getItem() instanceof MobBucketItem
                || FluidUtil.getFluidHandler(stack).isPresent();
    }

    /** Player-aware variant kept for callers; display carriers pour in both Creative and Survival. */
    public static boolean handles(Player player, ItemStack stack) {
        return handles(stack);
    }

public static InteractionResult interact(SmeltingCrucibleBlockEntity crucible, Player player,
            InteractionHand hand, BlockPos hitPos) {
        var held = player.getItemInHand(hand);
        if (!handles(player, held)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        var handler = new MoltenFluidHandler(crucible);
        boolean honey = held.is(Items.HONEY_BOTTLE), snow = held.is(Items.POWDER_SNOW_BUCKET);
        boolean milk = held.is(Items.MILK_BUCKET);
        boolean mob = held.getItem() instanceof MobBucketItem;
        boolean water = held.is(Items.WATER_BUCKET) || mob;
        boolean lava = held.is(Items.LAVA_BUCKET);
        if (!(honey || snow || milk || water || lava)) {
            return transfer(player, hand, handler);
        }
        boolean accepted;
        if (snow) {
            accepted = crucible.addMaterialStacks(
                    List.of(CrucibleMaterialStack.of(Materials.Snow, GTValues.U)), 272);
        } else if (honey || milk) {
            var material = honey ? Materials.Honey : Materials.Milk;
            int amount = honey ? 250 : 1000;
            var ratio = MoltenMaterialUnits.BUCKET;
            accepted = crucible.fillMoltenMaterial(material, amount, 300, false, ratio) == amount;
            if (accepted) crucible.fillMoltenMaterial(material, amount, 300, true, ratio);
        } else {
            var fluid = new FluidStack(lava ? Fluids.LAVA : Fluids.WATER, 1000);
            accepted = handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == 1000;
            if (accepted) handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        }
        if (!accepted) return InteractionResult.FAIL;
        // Preserve vanilla bucket entity data, names and bucket-spawn behavior without placing water.
        if (held.getItem() instanceof MobBucketItem bucket)
            bucket.checkExtraContent(player, player.level(), held, hitPos.above());
        var sound = honey ? SoundEvents.BOTTLE_EMPTY : snow ? SoundEvents.BUCKET_EMPTY_POWDER_SNOW
                : mob ? SoundEvents.BUCKET_EMPTY_FISH : lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        player.level().playSound(null, hitPos, sound, SoundSource.BLOCKS, 1, 1);
        player.level().gameEvent(player, GameEvent.FLUID_PLACE, hitPos);
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        player.setItemInHand(hand, FluidContainerInteraction.exchangeContainer(
                held, player, new ItemStack(honey ? Items.GLASS_BOTTLE : Items.BUCKET)));
        FluidContainerInteraction.syncInventory(player);
        return InteractionResult.CONSUME;
    }

    /**
     * Real containers exchange like a survival bucket in both game modes. A display item only pours
     * out and stays in hand, so it never turns into the fluid it stands for.
     */
    private static InteractionResult transfer(Player player, InteractionHand hand, IFluidHandler blockHandler) {
        var held = player.getItemInHand(hand);
        if (held.getItem() instanceof FluidDisplayItemLike) {
            var poured = FluidUtil.tryEmptyContainer(held, blockHandler, Integer.MAX_VALUE, player, true);
            if (!poured.isSuccess()) return InteractionResult.FAIL;
            FluidContainerInteraction.applyResult(player, hand, held, poured.getResult(), true);
            return InteractionResult.CONSUME;
        }
        var filled = FluidUtil.tryFillContainer(held, blockHandler, Integer.MAX_VALUE, player, true);
        if (filled.isSuccess()) {
            FluidContainerInteraction.applyResult(player, hand, held, filled.getResult(), false);
            return InteractionResult.CONSUME;
        }
        var emptied = FluidUtil.tryEmptyContainer(held, blockHandler, Integer.MAX_VALUE, player, true);
        if (emptied.isSuccess()) {
            FluidContainerInteraction.applyResult(player, hand, held, emptied.getResult(), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
