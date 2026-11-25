package gregtech.engine.fluid;

import gregtech.engine.stack.StackCompat;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
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
