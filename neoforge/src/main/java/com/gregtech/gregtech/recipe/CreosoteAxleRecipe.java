package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.registry.GTFluids;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Original non-mirrored treated huge axle: drain a real 1000 mB vessel and retain its container. */
public final class CreosoteAxleRecipe extends ToolShapedRecipe {
    private final ShapedRecipe base;
    public CreosoteAxleRecipe(ShapedRecipe base) { super(base, false); this.base = base; }

    public static final MapCodec<CreosoteAxleRecipe> CODEC = new ShapedRecipe.Serializer().codec()
            .xmap(CreosoteAxleRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<CreosoteAxleRecipe> SERIALIZER = new RecipeSerializer<>() {
        public MapCodec<CreosoteAxleRecipe> codec() { return CODEC; }
        public StreamCodec<RegistryFriendlyByteBuf, CreosoteAxleRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };
    private static final ContainerIngredient CREOSOTE = new ContainerIngredient();
    public static ContainerIngredient creosote() { return CREOSOTE; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remains = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            var original = input.getItem(slot);
            if (creosote().test(original)) remains.set(slot, ContainerIngredient.drainOne(original));
        }
        return remains;
    }

    /** No registry-bound example is constructed during serializer registration. */
    public static final class ContainerIngredient implements ICustomIngredient {
        public static final MapCodec<ContainerIngredient> CODEC = MapCodec.unit(CreosoteAxleRecipe::creosote);
        private static FluidStack required() {
            var holder = GTFluids.still("Oil_Creosote");
            return holder != null && holder.isBound() ? new FluidStack(holder.get(), 1000) : FluidStack.EMPTY;
        }
        private static boolean sameFluid(FluidStack a, FluidStack b) {
            return !a.isEmpty() && !b.isEmpty() && a.getFluid() == b.getFluid();
        }
        private static ItemStack example() {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", "fluid_cell_tin"));
            if (item == Items.AIR) throw new IllegalStateException("Creosote axle requires the finite tin cell");
            var handler = FluidUtil.getFluidHandler(new ItemStack(item)).orElse(null);
            var fluid = required();
            if (handler != null && !fluid.isEmpty() && handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 1000)
                return handler.getContainer();
            throw new IllegalStateException("Could not fill finite creosote cell");
        }
        @Override public boolean test(ItemStack input) {
            if (input == null || input.isEmpty()
                    || input.getItem() instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem
                    || FluidDisplayBinding.hasPayload(input)) return false;
            var need = required();
            if (need.isEmpty()) return false;
            var one = input.copyWithCount(1);
            var handler = FluidUtil.getFluidHandler(one).orElse(null);
            if (handler == null) return false;
            var before = FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);
            if (!sameFluid(before, need) || before.getAmount() < 1000) return false;
            var taken = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!sameFluid(taken, need) || taken.getAmount() != 1000) return false;
            var after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
            return before.getAmount() - after.getAmount() == 1000;
        }
        static ItemStack drainOne(ItemStack input) {
            var handler = FluidUtil.getFluidHandler(input.copyWithCount(1)).orElseThrow();
            var need = required();
            var drained = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!sameFluid(drained, need) || drained.getAmount() != 1000)
                throw new IllegalStateException("Creosote axle did not drain 1000 mB");
            return handler.getContainer();
        }
        @Override public java.util.stream.Stream<ItemStack> getItems() { return java.util.stream.Stream.of(example()); }
        @Override public boolean isSimple() { return false; }
        @Override public IngredientType<?> getType() { return NeoRecipeSerializers.CREOSOTE_CONTAINER.get(); }
    }
}
