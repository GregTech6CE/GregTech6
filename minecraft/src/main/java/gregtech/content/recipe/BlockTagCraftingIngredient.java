package gregtech.content.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/^* Matches block items against a block tag, not a similarly named item tag. ^/
public final class BlockTagCraftingIngredient implements ICustomIngredient {
    public static final MapCodec<BlockTagCraftingIngredient> CODEC = TagKey.codec(Registries.BLOCK)
            .xmap(BlockTagCraftingIngredient::new, ingredient -> ingredient.tag).fieldOf("tag");
*///?} else {
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

/** Matches block items against a block tag, not a similarly named item tag. */
public final class BlockTagCraftingIngredient extends AbstractIngredient {
    private static final IIngredientSerializer<BlockTagCraftingIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override public BlockTagCraftingIngredient parse(FriendlyByteBuf buffer) {
            return new BlockTagCraftingIngredient(TagKey.create(Registries.BLOCK, buffer.readResourceLocation()));
        }
        @Override public BlockTagCraftingIngredient parse(JsonObject json) {
            return new BlockTagCraftingIngredient(TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse(GsonHelper.getAsString(json, "tag"))));
        }
        @Override public void write(FriendlyByteBuf buffer, BlockTagCraftingIngredient ingredient) {
            buffer.writeResourceLocation(ingredient.tag.location());
        }
    };

    public static void registerIngredient() {
        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath("gt6ce", "block_tag"), SERIALIZER);
    }
//?}
    private final TagKey<Block> tag;

    public BlockTagCraftingIngredient(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override public boolean test(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem item
                && item.getBlock().defaultBlockState().is(tag);
    }

    private java.util.stream.Stream<ItemStack> examples() {
        return java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)
                .map(holder -> new ItemStack(holder.value())).filter(stack -> !stack.isEmpty());
    }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof BlockTagCraftingIngredient ingredient && tag.equals(ingredient.tag);
    }

    @Override public int hashCode() { return tag.hashCode(); }

    @Override public boolean isSimple() { return false; }
//? if >=1.21.1 {
    /*@Override public java.util.stream.Stream<ItemStack> getItems() { return examples(); }
    @Override public IngredientType<?> getType() { return NeoRecipeSerializers.BLOCK_TAG.get(); }
*///?} else {
    @Override public ItemStack[] getItems() { return examples().toArray(ItemStack[]::new); }
    @Override public boolean isEmpty() { return examples().findAny().isEmpty(); }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
    @Override public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gt6ce:block_tag");
        json.addProperty("tag", tag.location().toString());
        return json;
    }
//?}
}
