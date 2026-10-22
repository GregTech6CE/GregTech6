/* Adapted from GregTech-6 Team Behavior_Bucket_Simple; LGPL-3.0-or-later. */
package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.tool.WoodenBucketCatalog;
import com.gregtech.gregtech.content.tool.WoodenBucketCatalog.Band;
import com.gregtech.gregtech.content.tool.WoodenBucketCatalog.Content;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}

import java.util.List;

/** A finite 1000L bucket: only source-listed fluids fit; filled variants retain their metal band. */
public final class WoodenBucketItem extends TechItem {
    private final Band band;
    private final Content content;

    public WoodenBucketItem(Band band, Content content) {
        super("Wooden Bucket", new Item.Properties().stacksTo(1));
        this.band = band;
        this.content = content;
        ItemMaterialRegistry.register(this, new ItemComposition(null, List.of(
                MaterialComponent.of(GTMaterialRegistry.get("Wood"), 3 * GTValues.U),
                MaterialComponent.of(GTMaterialRegistry.get(band.material()), GTValues.U)),
                "GT6 MultiItemRandomTools wooden bucket", content.empty()));
    }

    public Band band() {
        return band;
    }

    public Content content() {
        return content;
    }

    private ItemStack variant(ItemStack stack, Content next) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
                WoodenBucketCatalog.itemId(band, next)));
//? if >=1.21.1 {
        /*return stack.transmuteCopy(item, 1);
*///?} else {
        ItemStack result = new ItemStack(item);
        if (stack.hasTag()) result.setTag(stack.getTag().copy());
        return result;
//?}
    }

    public static Fluid fluid(Content content) {
        if (content == WoodenBucketCatalog.water()) return Fluids.WATER;
        for (String key : content.fluids()) {
            var holder = GTFluids.still(key);
            if (holder != null) return holder.get();
        }
        return Fluids.EMPTY;
    }

    public static Content contentOf(Fluid fluid) {
        if (fluid.isSame(Fluids.WATER)) return WoodenBucketCatalog.water();
        for (Content candidate : WoodenBucketCatalog.CONTENTS)
            for (String key : candidate.fluids()) {
                var holder = GTFluids.still(key);
                if (holder != null && fluid.isSame(holder.get())) return candidate;
            }
        return null;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return !content.empty();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return content.empty() ? ItemStack.EMPTY : variant(stack, WoodenBucketCatalog.empty());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack))
            return InteractionResult.PASS;
        Level level = context.getLevel();
        var state = level.getBlockState(context.getClickedPos());
        boolean fillCauldron = content == WoodenBucketCatalog.water() && state.is(Blocks.CAULDRON);
        boolean drainCauldron = content.empty() && state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) == 3;
        if (fillCauldron || drainCauldron) {
            if (!level.isClientSide && level.mayInteract(player, context.getClickedPos())) {
                level.setBlockAndUpdate(context.getClickedPos(), fillCauldron
                        ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                        : Blocks.CAULDRON.defaultBlockState());
                ItemStack next = variant(stack, fillCauldron ? WoodenBucketCatalog.empty() : WoodenBucketCatalog.water());
                if (!player.isCreative() || drainCauldron)
                    player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, next));
                level.playSound(null, context.getClickedPos(), fillCauldron ? SoundEvents.BUCKET_EMPTY
                        : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return FluidUtil.interactWithFluidHandler(player, context.getHand(), level, context.getClickedPos(),
                context.getClickedFace()) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (content.discardOnly()) {
            if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(held);
            return InteractionResultHolder.sidedSuccess(player.isCreative() ? held
                    : variant(held, WoodenBucketCatalog.empty()), level.isClientSide);
        }
        var hit = getPlayerPOVHitResult(level, player, content.empty()
                ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), held))
            return InteractionResultHolder.fail(held);
        if (level.isClientSide) return InteractionResultHolder.success(held);
        var result = content.empty()
                ? FluidUtil.tryPickUpFluid(held.copyWithCount(1), player, level, pos, hit.getDirection())
                : FluidUtil.tryPlaceFluid(player, level, hand, pos.relative(hit.getDirection()),
                        held.copyWithCount(1), new FluidStack(fluid(content), 1000));
        if (!result.isSuccess()) return InteractionResultHolder.fail(held);
        ItemStack next = player.isCreative() && !content.empty() ? held
                : ItemUtils.createFilledResult(held, player, result.getResult());
        return InteractionResultHolder.success(next);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!content.empty() || !(target instanceof Cow cow) || cow.isBaby()) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, variant(stack, WoodenBucketCatalog.milk())));
            player.level().playSound(null, target.blockPosition(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public IFluidHandlerItem handler(ItemStack stack) {
        return new Handler(stack);
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            private final net.minecraftforge.common.util.LazyOptional<IFluidHandlerItem> handler =
                    net.minecraftforge.common.util.LazyOptional.of(() -> WoodenBucketItem.this.handler(stack));

            @Override
            public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                    net.minecraftforge.common.capabilities.Capability<T> capability, Direction side) {
                return capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM
                        ? handler.cast() : net.minecraftforge.common.util.LazyOptional.empty();
            }
        };
    }
//?}

    private final class Handler implements IFluidHandlerItem {
        private ItemStack container;
        private Content current;

        Handler(ItemStack stack) {
            container = stack;
            current = content;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            Fluid type = fluid(current);
            return tank != 0 || current.empty() || type == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(type, 1000);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty() && contentOf(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (container.getCount() != 1 || !current.empty() || resource.isEmpty() || resource.getAmount() < 1000) return 0;
            Content next = contentOf(resource.getFluid());
            if (next == null) return 0;
            if (action.execute()) {
                container = variant(container, next);
                current = next;
            }
            return 1000;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack available = getFluidInTank(0);
            return !resource.isEmpty() && !available.isEmpty() && available.getFluid().isSame(resource.getFluid())
                    ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            if (container.getCount() != 1 || maximum < 1000) return FluidStack.EMPTY;
            FluidStack available = getFluidInTank(0);
            if (!available.isEmpty() && action.execute()) {
                container = variant(container, WoodenBucketCatalog.empty());
                current = WoodenBucketCatalog.empty();
            }
            return available;
        }
    }
}
