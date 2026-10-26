package gregtech.content.recipe;

//? if >=1.21.1 {
//?} else {
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gregtech.GregTech;
//?}
import gregtech.engine.fluid.FluidDisplayBinding;
//? if >=1.21.1 {
//?} else {
import gregtech.content.item.FluidItem;
//?}
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
import net.minecraft.world.item.Item;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
//?}

//? if >=1.21.1 {
/*/^* Original non-mirrored treated huge axle: drain a real 1000 mB vessel and retain its container. ^/
public final class CreosoteAxleRecipe extends ToolShapedRecipe {
    private final ShapedRecipe base;
    public CreosoteAxleRecipe(ShapedRecipe base) { super(base, false); this.base = base; }
*///?} else {
import java.util.stream.Stream;
//?}

//? if >=1.21.1 {
    /*public static final MapCodec<CreosoteAxleRecipe> CODEC = new ShapedRecipe.Serializer().codec()
            .xmap(CreosoteAxleRecipe::new, recipe -> recipe.base);
*///?} else {
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
//?}
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
//? if >=1.21.1 {
    /*private static final ContainerIngredient CREOSOTE = new ContainerIngredient();
    public static ContainerIngredient creosote() { return CREOSOTE; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remains = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            var original = input.getItem(slot);
*///?} else {

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
//?}
            if (creosote().test(original)) remains.set(slot, ContainerIngredient.drainOne(original));
        }
        return remains;
    }

//? if >=1.21.1 {
    /*/^* No registry-bound example is constructed during serializer registration. ^/
    public static final class ContainerIngredient implements ICustomIngredient {
        public static final MapCodec<ContainerIngredient> CODEC = MapCodec.unit(CreosoteAxleRecipe::creosote);
        private static FluidStack required() {
            var holder = GTFluids.still("Oil_Creosote");
            return holder != null && holder.isBound() ? new FluidStack(holder.get(), 1000) : FluidStack.EMPTY;
*///?} else {
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
//?}
        }
//? if >=1.21.1 {
        /*private static boolean sameFluid(FluidStack a, FluidStack b) {
            return !a.isEmpty() && !b.isEmpty() && a.getFluid() == b.getFluid();
*///?} else {

        private static FluidStack required() {
            var fluid = GTFluids.still("Oil_Creosote");
            if (fluid == null || !fluid.isPresent()) return FluidStack.EMPTY;
            return new FluidStack(fluid.get(), 1000);
//?}
        }

        private static ItemStack example() {
//? if >=1.21.1 {
            /*var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_cell_tin"));
            if (item == Items.AIR) throw new IllegalStateException("Creosote axle requires the finite tin cell");
            var handler = FluidUtil.getFluidHandler(new ItemStack(item)).orElse(null);
            var fluid = required();
*///?} else {
            Item item = ForgeRegistries.ITEMS.getValue(GregTech.id("fluid_cell_tin"));
            if (item == null || item == Items.AIR)
                throw new IllegalStateException("GT6 creosote axle requires the finite tin cell item");
            ItemStack cell = new ItemStack(item);
            var handler = FluidUtil.getFluidHandler(cell).resolve().orElse(null);
            FluidStack fluid = required();
//?}
            if (handler != null && !fluid.isEmpty() && handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 1000)
                return handler.getContainer();
//? if >=1.21.1 {
            /*throw new IllegalStateException("Could not fill finite creosote cell");
*///?} else {
            throw new IllegalStateException("Could not fill a finite 1000 mB creosote cell for the axle recipe");
//?}
        }
//? if >=1.21.1 {
        /*@Override public boolean test(ItemStack input) {
            if (input == null || input.isEmpty()
                    || input.getItem() instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem
*///?} else {

        @Override public boolean test(@Nullable ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() instanceof FluidItem
//?}
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
        /*@Override public java.util.stream.Stream<ItemStack> getItems() { return java.util.stream.Stream.of(example()); }
*///?} else {
//?}
        @Override public boolean isSimple() { return false; }
//? if >=1.21.1 {
        /*@Override public IngredientType<?> getType() { return NeoRecipeSerializers.CREOSOTE_CONTAINER.get(); }
*///?} else {
        @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
        @Override public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", GregTech.id("creosote_container_1000").toString());
            return json;
        }
//?}
    }
}
