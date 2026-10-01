package com.gregtech.gregtech.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.content.food.GTDrinks;
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluid;
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
 * GT6 MultiItemBottles:129-396: one finite 1000 mB fluid container and one to four empty
 * bottles yield that many filled bottles. The whole 1000 mB is consumed regardless of
 * bottle count; the drained vessel returns in the crafting grid.
 */
public final class FiniteBottleFillingRecipe extends ShapelessRecipe {
    public static ContainerIngredient container(String fluidKey) { return new ContainerIngredient(fluidKey); }

    public static final RecipeSerializer<FiniteBottleFillingRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

        @Override public FiniteBottleFillingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new FiniteBottleFillingRecipe(vanilla.fromJson(id, json));
        }

        @Override public FiniteBottleFillingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new FiniteBottleFillingRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, FiniteBottleFillingRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
    };

    public FiniteBottleFillingRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.category(),
                base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> remains = super.getRemainingItems(inventory);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack original = inventory.getItem(slot);
            for (Ingredient ingredient : getIngredients()) {
                if (ingredient instanceof ContainerIngredient container && container.test(original)) {
                    remains.set(slot, ContainerIngredient.drainOne(original));
                    break;
                }
            }
        }
        return remains;
    }

    public static void registerIngredient() {
        CraftingHelper.register(GregTech.id("finite_fluid_container_1000"), ContainerIngredient.SERIALIZER);
    }

    /** Accepts only a drainable finite 1000 mB item, never the creative/display fluid item. */
    public static final class ContainerIngredient extends AbstractIngredient {
        private static final IIngredientSerializer<ContainerIngredient> SERIALIZER = new IIngredientSerializer<>() {
            @Override public ContainerIngredient parse(FriendlyByteBuf buffer) { return container(buffer.readUtf()); }
            @Override public ContainerIngredient parse(JsonObject json) { return container(json.get("fluid").getAsString()); }
            @Override public void write(FriendlyByteBuf buffer, ContainerIngredient ingredient) {
                buffer.writeUtf(ingredient.fluidKey);
            }
        };

        private final String fluidKey;

        private ContainerIngredient(String fluidKey) {
            super(Stream.of(new Ingredient.ItemValue(example(fluidKey))));
            this.fluidKey = fluidKey;
        }

        private boolean accepts(Fluid fluid) {
            Fluid expected = GTDrinks.fluidForField(fluidKey);
            if (expected != null && fluid == expected) return true;
            if (!"Lubricant".equals(fluidKey)) return false;
            // GT6's lubricant bottle lists both FL.Lubricant and FL.LubRoCant.
            var rotary = GTFluids.still("LubRoCant");
            return rotary != null && rotary.isPresent() && fluid == rotary.get();
        }

        private static ItemStack example(String fluidKey) {
            Fluid fluid = GTDrinks.fluidForField(fluidKey);
            if (fluid == null)
                throw new IllegalStateException("GT6 bottle filling lacks fluid " + fluidKey);
            // Rainbow sap is MAGIC in GT6. Both tin and gold reject magic; the magic-wax
            // cell accepts it. Pick the first physical vessel whose real handler fills.
            for (String cellId : new String[]{"fluid_cell_tin", "fluid_cell_wax_magic",
                    "fluid_cell_tungsten"}) {
                Item item = ForgeRegistries.ITEMS.getValue(GregTech.id(cellId));
                if (item == null || item == Items.AIR) continue;
                ItemStack cell = new ItemStack(item);
                var handler = FluidUtil.getFluidHandler(cell).resolve().orElse(null);
                if (handler != null && handler.fill(new FluidStack(fluid, 1000),
                        IFluidHandler.FluidAction.EXECUTE) == 1000)
                    return handler.getContainer();
            }
            throw new IllegalStateException("Could not fill a finite 1000 mB " + fluidKey + " cell");
        }

        @Override public boolean test(@Nullable ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() instanceof FluidItem
                    || FluidDisplayBinding.hasPayload(input)) return false;
            ItemStack one = input.copyWithCount(1);
            var handler = FluidUtil.getFluidHandler(one).resolve().orElse(null);
            if (handler == null) return false;
            FluidStack before = FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);
            if (before.isEmpty() || !accepts(before.getFluid()) || before.getAmount() < 1000) return false;
            FluidStack requested = new FluidStack(before.getFluid(), 1000);
            FluidStack taken = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
            if (!taken.isFluidEqual(requested) || taken.getAmount() != 1000) return false;
            FluidStack after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
            var handler = FluidUtil.getFluidHandler(input.copyWithCount(1)).resolve().orElseThrow();
            FluidStack before = FluidUtil.getFluidContained(input).orElse(FluidStack.EMPTY);
            FluidStack drained = handler.drain(new FluidStack(before.getFluid(), 1000),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1000)
                throw new IllegalStateException("Bottle filling recipe did not drain 1000 mB");
            return handler.getContainer();
        }

        @Override public boolean isSimple() { return false; }
        @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
        @Override public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", GregTech.id("finite_fluid_container_1000").toString());
            json.addProperty("fluid", fluidKey);
            return json;
        }
    }
}
