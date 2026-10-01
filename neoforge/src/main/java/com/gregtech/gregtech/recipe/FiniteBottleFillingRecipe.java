package com.gregtech.gregtech.recipe;
import com.mojang.serialization.*;import net.minecraft.core.*;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.material.Fluid;import net.neoforged.neoforge.common.crafting.*;import net.neoforged.neoforge.fluids.*;import net.neoforged.neoforge.fluids.capability.IFluidHandler;import net.minecraft.core.registries.BuiltInRegistries;
/** Source finite 1000mB filling, retaining a drained physical vessel and native codecs. */
public final class FiniteBottleFillingRecipe implements CraftingRecipe {
 private final ShapelessRecipe base;public FiniteBottleFillingRecipe(ShapelessRecipe base){this.base=base;}
 @Override public boolean matches(CraftingInput input,Level level){return base.matches(input,level);}
 @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider lookup){return base.assemble(input,lookup);}
 @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return base.getResultItem(lookup);}
 @Override public boolean canCraftInDimensions(int width,int height){return base.canCraftInDimensions(width,height);}
 @Override public String getGroup(){return base.getGroup();}@Override public CraftingBookCategory category(){return base.category();}@Override public NonNullList<Ingredient> getIngredients(){return base.getIngredients();}@Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
 @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var remains=base.getRemainingItems(input);for(int slot=0;slot<input.size();slot++){var original=input.getItem(slot);for(var ingredient:getIngredients())if(ingredient.getCustomIngredient() instanceof ContainerIngredient container&&container.test(original)){remains.set(slot,ContainerIngredient.drainOne(original));break;}}return remains;}
 public static final MapCodec<FiniteBottleFillingRecipe> CODEC=new ShapelessRecipe.Serializer().codec().xmap(FiniteBottleFillingRecipe::new,recipe->recipe.base);
 public static final RecipeSerializer<FiniteBottleFillingRecipe> SERIALIZER=new RecipeSerializer<>(){public MapCodec<FiniteBottleFillingRecipe> codec(){return CODEC;}public StreamCodec<RegistryFriendlyByteBuf,FiniteBottleFillingRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}};
 public record ContainerIngredient(String fluidKey) implements ICustomIngredient {
  public static final MapCodec<ContainerIngredient> CODEC=Codec.STRING.fieldOf("fluid").xmap(ContainerIngredient::new,ContainerIngredient::fluidKey);
  public boolean accepts(Fluid fluid){
   Fluid wanted=com.gregtech.gregtech.content.food.GTDrinks.fluidForField(fluidKey);
   if(wanted!=null&&fluid==wanted)return true;
   if(!"Lubricant".equals(fluidKey))return false;
   var holder=com.gregtech.gregtech.registry.GTFluids.still("LubRoCant");
   return holder!=null&&holder.isBound()&&fluid==holder.get();
  }
  public ItemStack example(){Fluid fluid=com.gregtech.gregtech.content.food.GTDrinks.fluidForField(fluidKey);if(fluid==null&&fluidKey.equals("Lubricant")){var holder=com.gregtech.gregtech.registry.GTFluids.still("LubRoCant");if(holder!=null&&holder.isBound())fluid=holder.get();}if(fluid==null)throw new IllegalStateException("Missing bottle fluid "+fluidKey);
   for(String id:new String[]{"fluid_cell_tin","fluid_cell_wax_magic","fluid_cell_tungsten"}){var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));if(item==Items.AIR)continue;var stack=new ItemStack(item);var handler=FluidUtil.getFluidHandler(stack).orElse(null);if(handler!=null&&handler.fill(new FluidStack(fluid,1000),IFluidHandler.FluidAction.EXECUTE)==1000)return handler.getContainer();}throw new IllegalStateException("No finite physical 1000mB vessel for "+fluidKey);}
  @Override public boolean test(ItemStack input){if(input==null||input.isEmpty()||input.getItem() instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem||com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(input))return false;var one=input.copyWithCount(1);var handler=FluidUtil.getFluidHandler(one).orElse(null);if(handler==null)return false;var before=FluidUtil.getFluidContained(one).orElse(FluidStack.EMPTY);if(before.isEmpty()||!accepts(before.getFluid())||before.getAmount()<1000)return false;var requested=before.copyWithAmount(1000);var taken=handler.drain(requested,IFluidHandler.FluidAction.EXECUTE);if(!FluidStack.isSameFluidSameComponents(taken,requested)||taken.getAmount()!=1000)return false;var after=FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);return before.getAmount()-after.getAmount()==1000;}
  static ItemStack drainOne(ItemStack input){var handler=FluidUtil.getFluidHandler(input.copyWithCount(1)).orElseThrow();var before=FluidUtil.getFluidContained(input).orElse(FluidStack.EMPTY);var drained=handler.drain(before.copyWithAmount(1000),IFluidHandler.FluidAction.EXECUTE);if(drained.getAmount()!=1000)throw new IllegalStateException("Bottle recipe failed to drain 1000mB");return handler.getContainer();}
  @Override public java.util.stream.Stream<ItemStack> getItems(){return java.util.stream.Stream.of(example());}@Override public boolean isSimple(){return false;}@Override public IngredientType<?> getType(){return NeoRecipeSerializers.FINITE_CONTAINER.get();}
 }
}
