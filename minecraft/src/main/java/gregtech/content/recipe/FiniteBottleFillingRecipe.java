package gregtech.content.recipe;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.stack.StackCompat;
//? if >=1.21.1 {
/*import com.mojang.serialization.*;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
/^* Source finite 1000mB filling, retaining a drained physical vessel and native codecs. ^/
public final class FiniteBottleFillingRecipe implements CraftingRecipe {
    private final ShapelessRecipe base;

    public FiniteBottleFillingRecipe(ShapelessRecipe base) {
        this.base = base;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return base.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return base.assemble(input, lookup);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return base.getResultItem(lookup);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return base.canCraftInDimensions(width, height);
    }

    @Override
    public String getGroup() {
        return base.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return base.category();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return base.getIngredients();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remains = base.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            var original = input.getItem(slot);
            for (var ingredient : getIngredients())
                if (ingredient.getCustomIngredient() instanceof ContainerIngredient container
                        && container.test(original)) {
                    remains.set(slot, ContainerIngredient.drainOne(original));
                    break;
                }
        }
        return remains;
    }

    public static final MapCodec<FiniteBottleFillingRecipe> CODEC = new ShapelessRecipe.Serializer()
            .codec().xmap(FiniteBottleFillingRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<FiniteBottleFillingRecipe> SERIALIZER = new RecipeSerializer<>() {
        public MapCodec<FiniteBottleFillingRecipe> codec() {
            return CODEC;
        }

        public StreamCodec<RegistryFriendlyByteBuf, FiniteBottleFillingRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };

    public record ContainerIngredient(String fluidKey, boolean allowLubricantBottle) implements ICustomIngredient {
        public ContainerIngredient(String fluidKey) {
            this(fluidKey, false);
        }

        public ContainerIngredient {
            if (allowLubricantBottle && !"Lubricant".equals(fluidKey))
                throw new IllegalArgumentException("250mB bottle alternative is only valid for industrial lubricant");
        }

        public static final MapCodec<ContainerIngredient> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.fieldOf("fluid").forGetter(ContainerIngredient::fluidKey),
                        Codec.BOOL.optionalFieldOf("allow_lubricant_bottle", false)
                                .forGetter(ContainerIngredient::allowLubricantBottle))
                        .apply(instance, ContainerIngredient::new));

        public boolean accepts(Fluid fluid) {
            Fluid wanted = gregtech.content.food.GTDrinks.fluidForField(fluidKey);
            if (wanted != null && fluid == wanted) return true;
            if (!"Lubricant".equals(fluidKey)) return false;
            var holder = gregtech.content.registry.GTFluids.still("Lubricant");
            return holder != null && holder.isBound() && fluid == holder.get();
        }

        public ItemStack example() {
            return FiniteCraftingVessels.examples(acceptedFluids(fluidKey)).get(0);
        }

        @Override
        public boolean test(ItemStack input) {
            if (input == null || input.isEmpty()
                    || input.getItem() instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem
                    || gregtech.engine.fluid.FluidDisplayBinding.hasPayload(input)) return false;
            if (allowLubricantBottle && input.getItem() instanceof gregtech.content.item.BottleItem bottle)
                return accepts(bottle.fluid());
            var one = input.copyWithCount(1);
            var handler = StackCompat.fluidHandler(one);
            if (handler == null) return false;
            var before = StackCompat.fluidContained(one);
            if (before.isEmpty() || !accepts(before.getFluid()) || before.getAmount() < 1000) return false;
            var requested = before.copyWithAmount(1000);
            var taken = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
            if (!StackCompat.sameFluid(taken, requested) || taken.getAmount() != 1000) return false;
            var after = StackCompat.fluidContained(handler.getContainer());
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
            if (input.getItem() instanceof gregtech.content.item.BottleItem)
                return gregtech.content.item.BottleItem.emptyBottle();
            var handler = StackCompat.fluidHandlerOrThrow(input.copyWithCount(1));
            var before = StackCompat.fluidContained(input);
            var drained = handler.drain(before.copyWithAmount(1000), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1000) throw new IllegalStateException("Bottle recipe failed to drain 1000mB");
            return handler.getContainer();
        }

        @Override
        public java.util.stream.Stream<ItemStack> getItems() {
            var vessels = FiniteCraftingVessels.examples(acceptedFluids(fluidKey)).stream();
            return allowLubricantBottle ? java.util.stream.Stream.concat(vessels,
                    java.util.stream.Stream.of(new ItemStack(BuiltInRegistries.ITEM.get(
                            ResourceLocation.fromNamespaceAndPath("gt6ce", "lubricant_bottle"))))) : vessels;
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IngredientType<?> getType() {
            return NeoRecipeSerializers.FINITE_CONTAINER.get();
        }
    }
*///?} else {

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.content.food.GTDrinks;
import gregtech.content.item.FluidItem;
import gregtech.content.registry.GTFluids;
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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * GT6 MultiItemBottles: one finite 1000 mB fluid container and one to four empty
 * bottles yield that many filled bottles. The whole 1000 mB is consumed regardless of
 * bottle count; the drained vessel returns in the crafting grid.
 */
public final class FiniteBottleFillingRecipe extends ShapelessRecipe {
    public static ContainerIngredient container(String fluidKey) {
        return new ContainerIngredient(fluidKey);
    }

    public static final RecipeSerializer<FiniteBottleFillingRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

        @Override
        public FiniteBottleFillingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new FiniteBottleFillingRecipe(vanilla.fromJson(id, json));
        }

        @Override
        public FiniteBottleFillingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new FiniteBottleFillingRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FiniteBottleFillingRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
    };

    public FiniteBottleFillingRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.category(),
                base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
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
        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("finite_fluid_container_1000").toLowerCase()), ContainerIngredient.SERIALIZER);
    }

    /** Accepts only a drainable finite 1000 mB item, never the creative/display fluid item. */
    public static final class ContainerIngredient extends AbstractIngredient {
        private static final IIngredientSerializer<ContainerIngredient> SERIALIZER = new IIngredientSerializer<>() {
            @Override
            public ContainerIngredient parse(FriendlyByteBuf buffer) {
                return new ContainerIngredient(buffer.readUtf(), buffer.readBoolean());
            }

            @Override
            public ContainerIngredient parse(JsonObject json) {
                return new ContainerIngredient(json.get("fluid").getAsString(),
                        json.has("allow_lubricant_bottle") && json.get("allow_lubricant_bottle").getAsBoolean());
            }

            @Override
            public void write(FriendlyByteBuf buffer, ContainerIngredient ingredient) {
                buffer.writeUtf(ingredient.fluidKey);
                buffer.writeBoolean(ingredient.allowLubricantBottle);
            }
        };

        private final String fluidKey;
        private final boolean allowLubricantBottle;

        private ContainerIngredient(String fluidKey) {
            this(fluidKey, false);
        }

        private ContainerIngredient(String fluidKey, boolean allowLubricantBottle) {
            super(examples(fluidKey, allowLubricantBottle));
            if (allowLubricantBottle && !"Lubricant".equals(fluidKey))
                throw new IllegalArgumentException("250mB bottle alternative is only valid for industrial lubricant");
            this.fluidKey = fluidKey;
            this.allowLubricantBottle = allowLubricantBottle;
        }

        private static Stream<Ingredient.Value> examples(String fluidKey, boolean allowBottle) {
            Stream<Ingredient.Value> vessels = FiniteCraftingVessels.examples(acceptedFluids(fluidKey))
                    .stream().map(Ingredient.ItemValue::new);
            if (!allowBottle) return vessels;
            Item bottle = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("lubricant_bottle").toLowerCase()));
            if (bottle == null || bottle == Items.AIR) throw new IllegalStateException("Missing industrial lubricant bottle");
            return Stream.concat(vessels, Stream.of(new Ingredient.ItemValue(new ItemStack(bottle))));
        }

        private boolean accepts(Fluid fluid) {
            Fluid expected = GTDrinks.fluidForField(fluidKey);
            if (expected != null && fluid == expected) return true;
            if (!"Lubricant".equals(fluidKey)) return false;
            // GT6's lubricant bottle lists both FL.Lubricant and FL.LubRoCant.
            var rotary = GTFluids.still("Lubricant");
            return rotary != null && rotary.isPresent() && fluid == rotary.get();
        }

        @Override
        public boolean test(@Nullable ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() instanceof FluidItem
                    || FluidDisplayBinding.hasPayload(input)) return false;
            if (allowLubricantBottle && input.getItem() instanceof gregtech.content.item.BottleItem bottle)
                return accepts(bottle.fluid());
            var one = input.copyWithCount(1);
            var handler = StackCompat.fluidHandler(one);
            if (handler == null) return false;
            var before = StackCompat.fluidContained(one);
            if (before.isEmpty() || !accepts(before.getFluid()) || before.getAmount() < 1000) return false;
            var requested = new FluidStack(before.getFluid(), 1000);
            var taken = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
            if (!StackCompat.sameFluid(taken, requested) || taken.getAmount() != 1000) return false;
            var after = StackCompat.fluidContained(handler.getContainer());
            return before.getAmount() - after.getAmount() == 1000;
        }

        static ItemStack drainOne(ItemStack input) {
            if (input.getItem() instanceof gregtech.content.item.BottleItem)
                return gregtech.content.item.BottleItem.emptyBottle();
            var handler = StackCompat.fluidHandlerOrThrow(input.copyWithCount(1));
            var before = StackCompat.fluidContained(input);
            var drained = handler.drain(new FluidStack(before.getFluid(), 1000),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1000)
                throw new IllegalStateException("Bottle filling recipe did not drain 1000 mB");
            return handler.getContainer();
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IIngredientSerializer<? extends Ingredient> getSerializer() {
            return SERIALIZER;
        }

        @Override
        public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("finite_fluid_container_1000").toLowerCase()).toString());
            json.addProperty("fluid", fluidKey);
            if (allowLubricantBottle) json.addProperty("allow_lubricant_bottle", true);
            return json;
        }
    }
//?}
    /** The lubricant recipe explicitly accepts both source fluids; other keys remain exact. */
    private static net.minecraft.world.level.material.Fluid[] acceptedFluids(String fluidKey) {
        var primary = gregtech.content.food.GTDrinks.fluidForField(fluidKey);
        // Not every container1000 key is a drink row: GT6's honey container names its fluid directly.
        if (primary == null) {
            var registered = gregtech.content.registry.GTFluids.still(fluidKey);
            primary = registered != null && GTFluids.isPresent(registered) ? registered.get() : null;
        }
        if (!"Lubricant".equals(fluidKey)) return new net.minecraft.world.level.material.Fluid[]{primary};
        var alternate = gregtech.content.registry.GTFluids.still("Lubricant");
        var rotary = alternate != null && GTFluids.isPresent(alternate) ? alternate.get() : null;
        return new net.minecraft.world.level.material.Fluid[]{primary, rotary};
    }

}
