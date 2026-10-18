package com.gregtech.gregtech.recipe;
import com.gregtech.gregtech.content.tool.ElectricToolAssembly;import com.gregtech.gregtech.api.material.*;
import com.mojang.serialization.*;import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.*;
/** Original concrete material/battery recipe, transmitted by stable identities and rebuilt with native components. */
public final class PoweredToolAssemblyRecipe extends ToolShapedRecipe {
 private final ElectricToolAssembly spec;private final GTMaterial material;private final ResourceLocation battery;
 public PoweredToolAssemblyRecipe(ShapedRecipe base,ElectricToolAssembly spec,GTMaterial material,ItemStack battery){super(base,false);this.spec=spec;this.material=material;this.battery=BuiltInRegistries.ITEM.getKey(battery.getItem());}
 public static final MapCodec<PoweredToolAssemblyRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.STRING.fieldOf("tool").forGetter(r->r.spec.id),Codec.STRING.fieldOf("material").forGetter(r->r.material.getName()),ResourceLocation.CODEC.fieldOf("battery").forGetter(r->r.battery)
 ).apply(i,(tool,material,battery)->{
  var spec=java.util.Arrays.stream(ElectricToolAssembly.values()).filter(s->s.id.equals(tool)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown electric tool "+tool));
  var recipe=spec.recipe(GTMaterialRegistry.get(material),new ItemStack(BuiltInRegistries.ITEM.get(battery)),"");
  if(!(recipe instanceof PoweredToolAssemblyRecipe powered))throw new IllegalArgumentException("Invalid electric assembly "+tool+"/"+material+"/"+battery);return powered;
 }));
 public static final RecipeSerializer<PoweredToolAssemblyRecipe> SERIALIZER=new RecipeSerializer<>(){
  public MapCodec<PoweredToolAssemblyRecipe> codec(){return CODEC;}
  public StreamCodec<RegistryFriendlyByteBuf,PoweredToolAssemblyRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
 };
 @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
}
