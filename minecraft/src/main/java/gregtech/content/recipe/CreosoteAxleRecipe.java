package gregtech.content.recipe;
import gregtech.engine.mod.GregTechIdentity;

//? if >=1.21.1 {
//?} else {
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
//?}
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.content.item.FluidDisplayItemLike;
import gregtech.content.registry.GTFluids;
//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
*///?} else {
//?}
import net.minecraft.core.NonNullList;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
*///?} else {
import net.minecraft.network.FriendlyByteBuf;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
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

//? if >=1.21.1 {
    /*public static final MapCodec<CreosoteAxleRecipe> CODEC = new ShapedRecipe.Serializer().codec()
            .xmap(CreosoteAxleRecipe::new, recipe -> recipe);
*///?}
    public static final RecipeSerializer<CreosoteAxleRecipe> SERIALIZER = new RecipeSerializer<>() {
//? if >=1.21.1 {
        /*public MapCodec<CreosoteAxleRecipe> codec() { return CODEC; }
        public StreamCodec<RegistryFriendlyByteBuf, CreosoteAxleRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
*///?} else {
        private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

        @Override public CreosoteAxleRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new CreosoteAxleRecipe(vanilla.fromJson(id, json));
        }

        @Override public CreosoteAxleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new CreosoteAxleRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, CreosoteAxleRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
//?}
        }
    };
    public CreosoteAxleRecipe(ShapedRecipe base) {
        super(base, false);
    }

    @Override public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

//? if >=1.21.1 {
    /*@Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remains = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            var original = input.getItem(slot);
*///?} else {
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> remains = super.getRemainingItems(inventory);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack original = inventory.getItem(slot);
//?}
            if (creosote().test(original)) remains.set(slot, ContainerIngredient.drainOne(original));
        }
        return remains;
    }

//? if >=1.21.1 {
    /*/^* No registry-bound example is constructed during serializer registration. ^/
    public static final class ContainerIngredient implements ICustomIngredient {
        public static final MapCodec<ContainerIngredient> CODEC = MapCodec.unit(CreosoteAxleRecipe::creosote);
        private static boolean sameFluid(FluidStack a, FluidStack b) {
            return !a.isEmpty() && !b.isEmpty() && a.getFluid() == b.getFluid();
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
//?}
        }

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
//? if >=1.21.1 {
            /*var need = required();
*///?} else {
            FluidStack need = required();
//?}
            if (need.isEmpty()) return false;
//? if >=1.21.1 {
            /*var one = input.copyWithCount(1);
            var handler = FluidUtil.getFluidHandler(one).orElse(null);
*///?} else {
            ItemStack one = input.copyWithCount(1);
            var handler = FluidUtil.getFluidHandler(one).resolve().orElse(null);
//?}
            if (handler == null) return false;
//? if >=1.21.1 {
            /*var before = FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);
            if (!sameFluid(before, need) || before.getAmount() < 1000) return false;
            var taken = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!sameFluid(taken, need) || taken.getAmount() != 1000) return false;
            var after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
*///?} else {
            FluidStack before = FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);
            if (before.isEmpty() || !before.isFluidEqual(need) || before.getAmount() < 1000) return false;
            FluidStack taken = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!taken.isFluidEqual(need) || taken.getAmount() != 1000) return false;
            FluidStack after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
            // Reject creative/infinite handlers even if they claim to drain successfully.
//?}
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
//? if >=1.21.1 {
            /*var handler = FluidUtil.getFluidHandler(input.copyWithCount(1)).orElseThrow();
            var need = required();
            var drained = handler.drain(need, IFluidHandler.FluidAction.EXECUTE);
            if (!sameFluid(drained, need) || drained.getAmount() != 1000)
                throw new IllegalStateException("Creosote axle did not drain 1000 mB");
*///?} else {
            var handler = FluidUtil.getFluidHandler(input.copyWithCount(1)).resolve().orElseThrow();
            FluidStack drained = handler.drain(required(), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1000) throw new IllegalStateException("Creosote axle did not drain 1000 mB");
//?}
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
