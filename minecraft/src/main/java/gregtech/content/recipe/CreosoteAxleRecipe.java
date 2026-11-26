package gregtech.content.recipe;

import gregtech.content.item.FluidDisplayItemLike;
import gregtech.content.registry.GTFluids;
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * GT6's huge treated-wood axle uses {@code OD.container1000creosote}. The normal GT fluid item is
 * an infinite display/source item, so this recipe accepts only a finite item fluid handler and
 * returns that same container after draining exactly 1000 mB.
 */
public final class CreosoteAxleRecipe extends ToolShapedRecipe {
    // Recipes are built after the item/fluid registries bind. Do not construct the JEI example
    // during Loader_Tools.run(), which registers the serializer before those registries.
    private static final class IngredientHolder {
        private static final ContainerIngredient VALUE = new ContainerIngredient();
    }

    public static ContainerIngredient creosote() { return IngredientHolder.VALUE; }

    public static final RecipeSerializer<CreosoteAxleRecipe> SERIALIZER =
            CraftingRecipeCompat.wrapShapedSerializer(CreosoteAxleRecipe::new, r -> r);

    public CreosoteAxleRecipe(ShapedRecipe base) {
        super(base, false);
    }

    @Override public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        var remains = super.onGetRemainingItems(grid);
        for (int slot = 0; slot < grid.size(); slot++) {
            var original = grid.getItem(slot);
            if (creosote().test(original)) remains.set(slot, ContainerIngredient.drainOne(original));
        }
        return remains;
    }

//? if >=1.21.1 {
    /*/^* No registry-bound example is constructed during serializer registration. ^/
    public static final class ContainerIngredient implements ICustomIngredient {
        public static final MapCodec<ContainerIngredient> CODEC = MapCodec.unit(CreosoteAxleRecipe::creosote);
*///?} else {
    public static void registerIngredient() {
        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("creosote_container_1000").toLowerCase()), ContainerIngredient.SERIALIZER);
    }

    /** A real, drainable 1000 mB creosote vessel; viewers show cells and wooden buckets. */
    public static final class ContainerIngredient extends AbstractIngredient {
        private static final IIngredientSerializer<ContainerIngredient> SERIALIZER = new IIngredientSerializer<>() {
            @Override public ContainerIngredient parse(FriendlyByteBuf buffer) { return creosote(); }
            @Override public ContainerIngredient parse(JsonObject json) { return creosote(); }
            @Override public void write(FriendlyByteBuf buffer, ContainerIngredient ingredient) { }
        };

        private ContainerIngredient() {
            super(examples().map(Ingredient.ItemValue::new));
        }
//?}

        private static FluidStack required() {
            var fluid = GTFluids.stillFluid("Oil_Creosote");
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
        }

        /** Display every compatible native finite vessel, not just one representative cell. */
        private static java.util.stream.Stream<ItemStack> examples() {
            return FiniteCraftingVessels.examples(required().getFluid()).stream();
        }
        @Override public boolean test(@org.jetbrains.annotations.Nullable ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() instanceof FluidDisplayItemLike
                    || FluidDisplayBinding.hasPayload(input)) return false;
            FluidStack need = required();
            if (need.isEmpty()) return false;
            ItemStack one = input.copyWithCount(1);
            var handler = gregtech.engine.stack.StackCompat.fluidHandler(one);
            if (handler == null) return false;
            FluidStack before = gregtech.engine.stack.StackCompat.fluidContained(one);
            if (before.isEmpty() || !gregtech.engine.fluid.FluidCompat.sameFluid(before, need) || before.getAmount() < 1000)
                return false;
            FluidStack taken = handler.drain(need, gregtech.engine.fluid.FluidCompat.EXECUTE);
            if (taken.isEmpty() || !gregtech.engine.fluid.FluidCompat.sameFluid(taken, need) || taken.getAmount() != 1000)
                return false;
            FluidStack after = gregtech.engine.stack.StackCompat.fluidContained(handler.getContainer());
            // Reject creative/infinite handlers even if they claim to drain successfully.
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
            var handler = gregtech.engine.stack.StackCompat.fluidHandlerOrThrow(input.copyWithCount(1));
            FluidStack need = required();
            FluidStack drained = handler.drain(need, gregtech.engine.fluid.FluidCompat.EXECUTE);
            if (drained.isEmpty() || !gregtech.engine.fluid.FluidCompat.sameFluid(drained, need) || drained.getAmount() != 1000)
                throw new IllegalStateException("Creosote axle did not drain 1000 mB");
            return handler.getContainer();
        }

//? if >=1.21.1 {
        /*@Override public java.util.stream.Stream<ItemStack> getItems() { return examples(); }
*///?} else {
//?}
        @Override public boolean isSimple() { return false; }
//? if >=1.21.1 {
        /*@Override public IngredientType<?> getType() { return NeoRecipeSerializers.CREOSOTE_CONTAINER.get(); }
*///?} else {
        @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
        @Override public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("creosote_container_1000").toLowerCase()).toString());
            return json;
        }
//?}
    }
}
