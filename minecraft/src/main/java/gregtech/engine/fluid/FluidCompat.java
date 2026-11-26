package gregtech.engine.fluid;

import gregtech.engine.stack.StackCompat;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Thin fluid helpers shared across containers, menus, and packets on 1.20.1 / 1.21.1. */
public final class FluidCompat {
    public static final IFluidHandler.FluidAction SIMULATE = IFluidHandler.FluidAction.SIMULATE;
    public static final IFluidHandler.FluidAction EXECUTE = IFluidHandler.FluidAction.EXECUTE;

    private FluidCompat() {}

    /** Copies {@code stack} with a new millibucket amount without mutating the source stack. */
    public static FluidStack copyWithAmount(FluidStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return FluidStack.EMPTY;
        FluidStack copy = stack.copy();
        copy.setAmount(amount);
        return copy;
    }

    /** Copies {@code stack} with a replacement {@link Fluid}, preserving amount and data components / NBT. */
    public static FluidStack withFluid(FluidStack stack, Fluid fluid) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
//? if >=1.21.1 {
        /*return new FluidStack(fluid.builtInRegistryHolder(), stack.getAmount(), stack.getComponentsPatch());
*///?} else {
        return new FluidStack(fluid, stack.getAmount(), stack.getTag() == null ? null : stack.getTag().copy());
//?}
    }

    /** Stable serialization key for fluid data components / NBT, used for recipe viewer subtypes. */
    public static String metadataKey(FluidStack stack) {
//? if >=1.21.1 {
        /*return String.valueOf(stack.getComponentsPatch());
*///?} else {
        return stack.hasTag() ? stack.getTag().toString() : "";
//?}
    }

    /** Creates a plain vanilla water bottle item stack. */
    public static ItemStack waterBottle() {
//? if >=1.21.1 {
        /*return PotionContents.createItemStack(Items.POTION, Potions.WATER);
*///?} else {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
//?}
    }

    /** Returns true when {@code stack} is a plain vanilla water potion bottle. */
    public static boolean isWaterPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
//? if >=1.21.1 {
        /*var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        return potion.isPresent() && potion.get().is(Potions.WATER);
*///?} else {
        return PotionUtils.getPotion(stack) == Potions.WATER;
//?}
    }

    /** Extracts the fluid carried by a vanilla {@link BucketItem}. */
    public static Fluid bucketFluid(BucketItem bucket) {
//? if >=1.21.1 {
        /*return bucket.content;
*///?} else {
        return bucket.getFluid();
//?}
    }

    /** Resolves the fluid type's bucket empty or fill sound, or {@code null} when unset. */
    public static SoundEvent bucketSound(FluidStack stack, boolean empty) {
//? if >=1.21.1 {
        /*return stack.getFluidType().getSound(stack, empty ? SoundActions.BUCKET_EMPTY : SoundActions.BUCKET_FILL);
*///?} else {
        return stack.getFluid().getFluidType().getSound(
                stack, empty ? SoundActions.BUCKET_EMPTY : SoundActions.BUCKET_FILL);
//?}
    }

    /** Same fluid and same data components / NBT tag, ignoring stack amount. */
    public static boolean sameFluid(FluidStack first, FluidStack second) {
        return StackCompat.sameFluid(first, second);
    }

    /** Same fluid, same data components / NBT tag, and identical amount. */
    public static boolean matches(FluidStack first, FluidStack second) {
//? if >=1.21.1 {
        /*return FluidStack.matches(first, second);
*///?} else {
        return first.isFluidStackIdentical(second);
//?}
    }

    /** Encodes an optional (possibly empty) {@link FluidStack} onto a menu/packet buffer. */
//? if >=1.21.1 {
    /*public static void writeFluid(net.minecraft.network.RegistryFriendlyByteBuf buf, FluidStack stack) {
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    public static FluidStack readFluid(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        return FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }
*///?} else {
    public static void writeFluid(net.minecraft.network.FriendlyByteBuf buf, FluidStack stack) {
        buf.writeFluidStack(stack);
    }

    public static FluidStack readFluid(net.minecraft.network.FriendlyByteBuf buf) {
        return buf.readFluidStack();
    }
//?}
}
