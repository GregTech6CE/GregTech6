package gregtech.engine.machine.crucible;

import gregtech.api.material.GTValues;
import gregtech.content.material.Materials;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.fluid.MoltenMaterialUnits;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import java.util.List;

/** Container transfers are claimed on both sides so BucketItem cannot also place world fluid. */
public final class CrucibleContainerInteraction {
    private CrucibleContainerInteraction() {}

    public static boolean handles(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET) || stack.is(Items.BUCKET)
                || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.POWDER_SNOW_BUCKET) || stack.getItem() instanceof MobBucketItem
                || FluidUtil.getFluidHandler(stack).isPresent();
    }

//? if >=1.21.1 {
    /*public static InteractionResult interact(SmeltingCrucibleEntity crucible, Player player,
*///?} else {
    public static InteractionResult interact(SmeltingCrucibleBlockEntity crucible, Player player,
//?}
            InteractionHand hand, BlockPos hitPos) {
        var held = player.getItemInHand(hand);
        if (!handles(held)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        var handler = new MoltenFluidHandler(crucible);
        boolean honey = held.is(Items.HONEY_BOTTLE), snow = held.is(Items.POWDER_SNOW_BUCKET);
        boolean milk = held.is(Items.MILK_BUCKET);
        boolean mob = held.getItem() instanceof MobBucketItem;
        boolean water = held.is(Items.WATER_BUCKET) || mob;
        boolean lava = held.is(Items.LAVA_BUCKET);
        if (!(honey || snow || milk || water || lava)) {
            return FluidUtil.interactWithFluidHandler(player, hand, handler)
                    ? InteractionResult.CONSUME : InteractionResult.FAIL;
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
        if (!player.getAbilities().instabuild)
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player,
                    new ItemStack(honey ? Items.GLASS_BOTTLE : Items.BUCKET)));
        return InteractionResult.CONSUME;
    }
}
