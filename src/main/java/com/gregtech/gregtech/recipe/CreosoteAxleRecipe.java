package com.gregtech.gregtech.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * GT6's huge treated-wood axle uses {@code OD.container1000creosote}. The normal GT fluid item is
 * an infinite display/source item, so this recipe accepts only a finite item fluid handler and
 * returns that same container after draining exactly 1000 mB.
 */
public final class CreosoteAxleRecipe extends ToolShapedRecipe {
    // Recipes are built after the Forge item/fluid registries bind. Do not construct the JEI
    // example during Loader_Tools.run(), which registers the serializer before those registries.
    private static final class IngredientHolder {
        private static final ContainerIngredient VALUE = new ContainerIngredient();
    }

    public static ContainerIngredient creosote() { return IngredientHolder.VALUE; }
    public static final RecipeSerializer<CreosoteAxleRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

        @Override public CreosoteAxleRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new CreosoteAxleRecipe(vanilla.fromJson(id, json));
        }

        @Override public CreosoteAxleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new CreosoteAxleRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, CreosoteAxleRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
    };

    public CreosoteAxleRecipe(ShapedRecipe base) {
        super(base, false);
    }

    @Override public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> remains = super.getRemainingItems(inventory);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack original = inventory.getItem(slot);
            if (creosote().test(original)) remains.set(slot, ContainerIngredient.drainOne(original));
        }
        return remains;
    }

    public static void registerIngredient() {
        CraftingHelper.register(GregTech.id("creosote_container_1000"), ContainerIngredient.SERIALIZER);
    }

    /** A real, drainable 1000 mB creosote vessel; JEI receives a filled tin cell as its example. */
    public static final class ContainerIngredient extends AbstractIngredient {
        private static final IIngredientSerializer<ContainerIngredient> SERIALIZER = new IIngredientSerializer<>() {
            @Override public ContainerIngredient parse(FriendlyByteBuf buffer) { return creosote(); }
            @Override public ContainerIngredient parse(JsonObject json) { return creosote(); }
            @Override public void write(FriendlyByteBuf buffer, ContainerIngredient ingredient) { }
        };

        private ContainerIngredient() {
            super(Stream.of(new Ingredient.ItemValue(example())));
        }

        private static FluidStack required() {
            var fluid = GTFluids.still("Oil_Creosote");
            if (fluid == null || !fluid.isPresent()) return FluidStack.EMPTY;
            return new FluidStack(fluid.get(), 1000);
        }

        private static ItemStack example() {
            Item item = ForgeRegistries.ITEMS.getValue(GregTech.id("fluid_cell_tin"));
            if (item == null || item == Items.AIR)
                throw new IllegalStateException("GT6 creosote axle requires the finite tin cell item");
            ItemStack cell = new ItemStack(item);
            var handler = FluidUtil.getFluidHandler(cell).resolve().orElse(null);
            FluidStack fluid = required();
            if (handler != null && !fluid.isEmpty() && handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 1000)
                return handler.getContainer();
            throw new IllegalStateException("Could not fill a finite 1000 mB creosote cell for the axle recipe");
        }

        @Override public boolean test(@Nullable ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() instanceof FluidItem
                    || FluidDisplayBinding.hasPayload(input)) return false;
            FluidStack need = required();
            if (need.isEmpty()) return false;
            ItemStack one = input.copyWithCount(1);
            var handler = FluidUtil.getFluidHandler(one).resolve().orElse(null);
            if (handler == null) return false;
            FluidStack before = FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);
            if (before.isEmpty() || !before.isFluidEqual(need) || before.getAmount() < 1000) return false;
            FluidStack taken = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!taken.isFluidEqual(need) || taken.getAmount() != 1000) return false;
            FluidStack after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
            // Reject creative/infinite handlers even if they claim to drain successfully.
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
            var handler = FluidUtil.getFluidHandler(input.copyWithCount(1)).resolve().orElseThrow();
            FluidStack drained = handler.drain(required(), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1000) throw new IllegalStateException("Creosote axle did not drain 1000 mB");
            return handler.getContainer();
        }

        @Override public boolean isSimple() { return false; }
        @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
        @Override public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", GregTech.id("creosote_container_1000").toString());
            return json;
        }
    }
}
