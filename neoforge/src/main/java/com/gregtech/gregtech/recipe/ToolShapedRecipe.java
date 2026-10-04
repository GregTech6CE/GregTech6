package com.gregtech.gregtech.recipe;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** Original mirror policy and GT tool wear, using native shaped recipe/component codecs. */
public class ToolShapedRecipe implements CraftingRecipe, com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe {
 private final boolean autocraftable;
 @Override public boolean isAutocraftableByGT(){return autocraftable;}
 private final ShapedRecipe base;
 private final boolean allowMirror,requireEmptyFluidContainers;
 public ToolShapedRecipe(ShapedRecipe base,boolean allowMirror){this(base,allowMirror,false);}
 public ToolShapedRecipe(ShapedRecipe base,boolean allowMirror,boolean requireEmptyFluidContainers){this(base,allowMirror,requireEmptyFluidContainers,true);}
 public ToolShapedRecipe(ShapedRecipe base,boolean allowMirror,boolean requireEmptyFluidContainers,boolean autocraftable){this.autocraftable=autocraftable;base.getIngredients().replaceAll(CraftingTools::expand);this.base=base;this.allowMirror=allowMirror;this.requireEmptyFluidContainers=requireEmptyFluidContainers;}
 @Override public boolean matches(CraftingInput input,Level level){
  if(!(allowMirror?base.matches(input,level):matchesUnmirrored(input)))return false;
  return toolsUsable(input);
 }
 protected boolean toolsUsable(CraftingInput input){
  for(int i=0;i<input.size();i++){var stack=input.getItem(i);
   if(requireEmptyFluidContainers&&net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack).filter(fluid->!fluid.isEmpty()).isPresent())return false;
   if((stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem||stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem)&&!com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(stack,com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack)))return false;
  }return true;
 }
 private boolean matchesUnmirrored(CraftingInput input){
  for(int left=0;left<=input.width()-base.getWidth();left++)for(int top=0;top<=input.height()-base.getHeight();top++){
   boolean ok=true;
   for(int y=0;y<input.height()&&ok;y++)for(int x=0;x<input.width();x++){
    int dx=x-left,dy=y-top;var ingredient=dx>=0&&dy>=0&&dx<base.getWidth()&&dy<base.getHeight()?base.getIngredients().get(dx+dy*base.getWidth()):Ingredient.EMPTY;
    if(!ingredient.test(input.getItem(x+y*input.width()))){ok=false;break;}
   }if(ok)return true;
  }return false;
 }
 @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
  var result=base.getRemainingItems(input);
  for(int i=0;i<input.size();i++){var original=input.getItem(i);if(original.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric){var copy=original.copyWithCount(1);electric.consumeInteractionEnergy(copy,electric.toolName().equals("Chainsaw")?200:electric.toolName().equals("Wrench")?800L*(1L<<(2*(electric.energyTier()-1))):electric.toolName().equals("Screwdriver")?200:100,null);result.set(i,com.gregtech.gregtech.content.tool.ElectricToolWear.broken(electric,copy)?ItemStack.EMPTY:copy);continue;}int cost=NeoToolBindings.craftDamage(original);if(cost<=0)continue;
   var copy=original.copyWithCount(1);long damage=(long)copy.getDamageValue()+cost;
   if(damage>=copy.getMaxDamage())result.set(i,ItemStack.EMPTY);else{copy.setDamageValue((int)damage);result.set(i,copy);}
  }
  drainFiniteIngredients(input, result);
  return result;
 }
 /** Return finite vessels only from the matched slots, including offsets and permitted mirrors. */
 private void drainFiniteIngredients(CraftingInput input, NonNullList<ItemStack> remains) {
  if (getIngredients().stream().noneMatch(i -> i.getCustomIngredient() instanceof FiniteBottleFillingRecipe.ContainerIngredient)) return;
  for (int left=0;left<=input.width()-base.getWidth();left++)
   for (int top=0;top<=input.height()-base.getHeight();top++)
    for (int mirror=0;mirror<=(allowMirror?1:0);mirror++) {
     Ingredient[] mapped=new Ingredient[input.size()];
     boolean matched=true;
     for (int y=0;y<input.height()&&matched;y++) for (int x=0;x<input.width();x++) {
      int dx=x-left,dy=y-top;
      var ingredient=dx>=0&&dy>=0&&dx<base.getWidth()&&dy<base.getHeight()
       ? getIngredients().get((mirror==1?base.getWidth()-1-dx:dx)+dy*base.getWidth()) : Ingredient.EMPTY;
      int slot=x+y*input.width();
      mapped[slot]=ingredient;
      if (!ingredient.test(input.getItem(slot))) {matched=false;break;}
     }
     if (!matched) continue;
     for (int slot=0;slot<mapped.length;slot++)
      if (mapped[slot].getCustomIngredient() instanceof FiniteBottleFillingRecipe.ContainerIngredient)
       remains.set(slot,FiniteBottleFillingRecipe.ContainerIngredient.drainOne(input.getItem(slot)));
     return;
    }
 }
 @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider lookup){return base.assemble(input,lookup);}
 @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return base.getResultItem(lookup);}
 @Override public boolean canCraftInDimensions(int width,int height){return base.canCraftInDimensions(width,height);}
 @Override public String getGroup(){return base.getGroup();}
 @Override public CraftingBookCategory category(){return base.category();}
 @Override public NonNullList<Ingredient> getIngredients(){return base.getIngredients();}
 @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
 public boolean allowMirror(){return allowMirror;}
 public static final MapCodec<ToolShapedRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
  new ShapedRecipe.Serializer().codec().forGetter((ToolShapedRecipe recipe)->recipe.base),
  Codec.BOOL.optionalFieldOf("allow_mirror",true).forGetter((ToolShapedRecipe recipe)->recipe.allowMirror),
  Codec.BOOL.optionalFieldOf("require_empty_fluid_containers",false).forGetter((ToolShapedRecipe recipe)->recipe.requireEmptyFluidContainers),
  Codec.BOOL.optionalFieldOf("gregtech_autocraftable",true).forGetter((ToolShapedRecipe recipe)->recipe.autocraftable)
 ).apply(instance,ToolShapedRecipe::new));
 public static final RecipeSerializer<ToolShapedRecipe> SERIALIZER=new RecipeSerializer<>(){
  public MapCodec<ToolShapedRecipe> codec(){return CODEC;}
  public StreamCodec<RegistryFriendlyByteBuf,ToolShapedRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
 };
}
