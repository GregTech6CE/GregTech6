package com.gregtech.gregtech.recipe;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
/** Native datapack/network codecs for the original tool-kind and pattern-index contract. */
public final class GTToolRecipeSerializers {
 private GTToolRecipeSerializers(){}
 private static final Codec<GTToolType> TYPE=Codec.STRING.xmap(GTToolType::byId,GTToolType::id);
 private static final ResourceLocation CODEC_ID=ResourceLocation.fromNamespaceAndPath("gregtech","tools/codec");
 public static final ResourceLocation ASSEMBLY_ID=ResourceLocation.fromNamespaceAndPath("gregtech","tool_assembly");
 public static final ResourceLocation CRAFTING_ID=ResourceLocation.fromNamespaceAndPath("gregtech","tool_crafting");
 public static final ResourceLocation HEAD_ID=ResourceLocation.fromNamespaceAndPath("gregtech","tool_head");
 public static final RecipeSerializer<GTToolAssemblyRecipe> ASSEMBLY=serializer(TYPE.fieldOf("tool").xmap(type->new GTToolAssemblyRecipe(CODEC_ID,type),GTToolAssemblyRecipe::toolType));
 private static final MapCodec<GTToolPatternRecipe> CRAFTING_CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
  TYPE.fieldOf("tool").forGetter(GTToolPatternRecipe::toolType),Codec.INT.optionalFieldOf("pattern",0).forGetter(GTToolPatternRecipe::patternIndex)
 ).apply(instance,(type,index)->type==GTToolType.FLINT_AND_TINDER?new GTFlintAndTinderRecipe(CODEC_ID):new GTToolCraftingRecipe(CODEC_ID,type,index)));
 public static final RecipeSerializer<GTToolPatternRecipe> CRAFTING=serializer(CRAFTING_CODEC);
 private static final MapCodec<GTToolHeadRecipe> HEAD_CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
  TYPE.fieldOf("tool").forGetter(GTToolHeadRecipe::toolType),Codec.INT.optionalFieldOf("pattern",0).forGetter(GTToolHeadRecipe::patternIndex)
 ).apply(instance,(type,index)->new GTToolHeadRecipe(CODEC_ID,type,index)));
 public static final RecipeSerializer<GTToolHeadRecipe> HEAD=serializer(HEAD_CODEC);
 private static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec){return new RecipeSerializer<>(){public MapCodec<T> codec(){return codec;}public StreamCodec<RegistryFriendlyByteBuf,T> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(codec.codec());}};}
}
