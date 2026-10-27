package gregtech.content.recipe;

import gregtech.content.food.GTDrinks;
import gregtech.content.item.BottleItem;
import gregtech.engine.fluid.FluidDisplayBinding;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21.1 {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/^* The original container250 torch ingredient consumes one finite quarter-bucket portion. ^/
public final class QuarterFluidCraftingIngredient implements ICustomIngredient {
    public static final MapCodec<QuarterFluidCraftingIngredient> CODEC = Codec.STRING
            .xmap(QuarterFluidCraftingIngredient::new, value -> value.fluidKey).fieldOf("fluid");
*///?} else {
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** The original container250 torch ingredient consumes one finite quarter-bucket portion. */
public final class QuarterFluidCraftingIngredient extends AbstractIngredient {
    private static final IIngredientSerializer<QuarterFluidCraftingIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override public QuarterFluidCraftingIngredient parse(FriendlyByteBuf buffer) {
            return new QuarterFluidCraftingIngredient(buffer.readUtf());
        }
        @Override public QuarterFluidCraftingIngredient parse(JsonObject json) {
            return new QuarterFluidCraftingIngredient(json.get("fluid").getAsString());
        }
        @Override public void write(FriendlyByteBuf buffer, QuarterFluidCraftingIngredient ingredient) {
            buffer.writeUtf(ingredient.fluidKey);
        }
    };
    public static void registerIngredient() {
        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath("gt6ce", "finite_fluid_container_250"), SERIALIZER);
    }
//?}
    private final String fluidKey;

    public QuarterFluidCraftingIngredient(String fluidKey) { this.fluidKey = fluidKey; }

    @Override public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty() || FluidDisplayBinding.hasPayload(stack)) return false;
//? if >=1.21.1 {
        /*if (stack.getItem() instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem) return false;
*///?} else {
        if (stack.getItem() instanceof gregtech.content.item.FluidItem) return false;
//?}
        var wanted = GTDrinks.fluidForField(fluidKey);
        if (wanted == null) return false;
        if (stack.getItem() instanceof BottleItem bottle) return bottle.fluid() == wanted;
//? if >=1.21.1 {
        /*var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);
*///?} else {
        var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).resolve().orElse(null);
//?}
        if (handler == null) return false;
        var before = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
        if (before.isEmpty() || before.getFluid() != wanted || before.getAmount() != 250) return false;
        var requested = new FluidStack(wanted, 250);
        var taken = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
//? if >=1.21.1 {
        /*if (!FluidStack.isSameFluidSameComponents(taken, requested) || taken.getAmount() != 250) return false;
*///?} else {
        if (!taken.isFluidEqual(requested) || taken.getAmount() != 250) return false;
//?}
        var after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
        return before.getAmount() - after.getAmount() == 250;
    }

    public ItemStack drainOne(ItemStack stack) {
        if (!test(stack)) throw new IllegalStateException("Invalid finite 250 mB crafting ingredient");
        if (stack.getItem() instanceof BottleItem) return BottleItem.emptyBottle();
//? if >=1.21.1 {
        /*var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElseThrow();
*///?} else {
        var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).resolve().orElseThrow();
//?}
        var drained = handler.drain(new FluidStack(GTDrinks.fluidForField(fluidKey), 250), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != 250) throw new IllegalStateException("Torch recipe did not drain 250 mB");
        return handler.getContainer();
    }

    private java.util.stream.Stream<ItemStack> examples() {
        var fluid = GTDrinks.fluidForField(fluidKey);
        var bottles = java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
                .filter(item -> item instanceof BottleItem bottle && fluid != null && bottle.fluid() == fluid)
                .map(ItemStack::new);
        return java.util.stream.Stream.concat(bottles, FiniteCraftingVessels.examples(250, fluid).stream());
    }
    @Override public boolean isSimple() { return false; }
//? if >=1.21.1 {
    /*@Override public java.util.stream.Stream<ItemStack> getItems() { return examples(); }
    @Override public IngredientType<?> getType() { return NeoRecipeSerializers.QUARTER_CONTAINER.get(); }
*///?} else {
    @Override public ItemStack[] getItems() { return examples().toArray(ItemStack[]::new); }
    @Override public boolean isEmpty() { return false; }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
    @Override public JsonElement toJson() {
        var json = new JsonObject();
        json.addProperty("type", "gt6ce:finite_fluid_container_250");
        json.addProperty("fluid", fluidKey);
        return json;
    }
//?}
}
