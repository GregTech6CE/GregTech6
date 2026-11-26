package gregtech.content.recipe;

import gregtech.content.item.GTToolItem;
import gregtech.engine.material.ItemMaterialForms;
import gregtech.engine.stack.StackCompat;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
*///?} else {
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//?}

/** Original CR shapeless rows: ordinary GT tool container crafting wears and returns the tool. */
public final class ToolShapelessRecipe extends GTShapelessCraftingRecipe
        implements gregtech.engine.recipe.AutocraftableCraftingRecipe {
    private final boolean autocraftable, requireEmptyFluidContainers;
    private final FormConversionSelector formSelector;
    private final FormConversionPlacement formPlacement;
    private final String conversionInput, conversionMaterial;

    /** Read-only placement metadata for accurate recipe-viewer layout and transfer. */
    public FormConversionSelector formSelector() { return formSelector; }
    public FormConversionPlacement formPlacement() { return formPlacement; }

    @Override
    public boolean isAutocraftableByGT() {
        return autocraftable;
    }
    public ToolShapelessRecipe(ShapelessRecipe base) {
        this(base, true);
    }

    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable) {
        this(base, autocraftable, FormConversionSelector.NONE);
    }

    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector) {
        this(base, autocraftable, formSelector, "", "");
    }

    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector,
            String conversionInput, String conversionMaterial) {
        this(base, autocraftable, formSelector, conversionInput, conversionMaterial, false);
    }

    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector,
            String conversionInput, String conversionMaterial, boolean requireEmptyFluidContainers) {
        this(base, autocraftable, formSelector, conversionInput, conversionMaterial, requireEmptyFluidContainers, FormConversionPlacement.FIXED);
    }

    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector,
            String conversionInput, String conversionMaterial, boolean requireEmptyFluidContainers, FormConversionPlacement formPlacement) {
        super(expand(base));
        this.autocraftable = autocraftable;
        this.requireEmptyFluidContainers = requireEmptyFluidContainers;
        this.formSelector = formSelector;
        this.formPlacement = java.util.Objects.requireNonNull(formPlacement);
        if (conversionInput.isEmpty() != conversionMaterial.isEmpty()) throw new IllegalArgumentException("Incomplete source conversion form");
        this.conversionInput = conversionInput;
        this.conversionMaterial = conversionMaterial;
    }
    /** Shapeless rows use the same GT6 tool identities as the shaped serializer. */
    private static ShapelessRecipe expand(ShapelessRecipe base) {
        base.getIngredients().replaceAll(CraftingTools::expand);
        return base;
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        if (!super.onMatches(grid, level)) return false;
        for (var stack : grid.items()) {
            if (!stack.isEmpty() && !conversionInput.isEmpty()
                    && !ItemMaterialForms.matches(conversionInput, conversionMaterial, stack)) return false;
            if (requireEmptyFluidContainers && (gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(stack)
                    || !StackCompat.fluidContained(stack).isEmpty())) return false;
            if (stack.getItem() instanceof GTToolItem && !GTToolHelper.isUsable(stack)) return false;
            // An electric tool with no charge must not craft, exactly like the shaped serializer.
            if (stack.getItem() instanceof gregtech.content.item.ElectricToolItem electric
                    && !electric.canInteract(stack)) return false;
            if (!gregtech.content.item.behavior.BehaviorLighter.usableForCrafting(stack)) return false;
        }
        if (formSelector.variants() == 0) return true;
        return formPlacement.matches(formSelector, grid.formGridSize(), grid.formFirstSlot(), grid.occupiedCount());
    }
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return super.canCraftInDimensions(width, height) && width * height >= formSelector.minimumGridSize();
    }

    @Override
    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        var remaining = super.onGetRemainingItems(grid);
        for (int i = 0; i < grid.size(); i++) {
            var stack = grid.getItem(i);
            if (stack.getItem() instanceof gregtech.content.item.ElectricToolItem electric) {
                // Charge and wear the returned tool; the shaped row already used this rule.
                var tool = stack.copyWithCount(1);
                electric.consumeInteractionEnergy(
                        tool, gregtech.content.tool.ElectricToolWear.craftEnergy(electric), null);
                remaining.set(i, gregtech.content.tool.ElectricToolWear.broken(electric, tool) ? ItemStack.EMPTY : tool);
                continue;
            }
            int cost = GTToolHelper.craftDamage(stack);
            if (cost <= 0) continue;
            var tool = stack.copyWithCount(1);
            long damage = (long) tool.getDamageValue() + cost;
            if (damage >= tool.getMaxDamage()) remaining.set(i, ItemStack.EMPTY);
            else {
                tool.setDamageValue((int) damage);
                remaining.set(i, tool);
            }
        }
        return remaining;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

//? if >=1.21.1 {
    /*public static final MapCodec<ToolShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            new ShapelessRecipe.Serializer().codec().forGetter((ToolShapelessRecipe recipe) -> recipe),
            Codec.BOOL.optionalFieldOf("gregtech_autocraftable",
                    true).forGetter((ToolShapelessRecipe recipe) -> recipe.autocraftable),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("gregtech_form_variants",
                    0).forGetter((ToolShapelessRecipe recipe) -> recipe.formSelector.variants()),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("gregtech_form_offset",
                    0).forGetter((ToolShapelessRecipe recipe) -> recipe.formSelector.offset()),
            Codec.STRING.optionalFieldOf("gregtech_form_input",
                    "").forGetter((ToolShapelessRecipe recipe) -> recipe.conversionInput),
            Codec.STRING.optionalFieldOf("gregtech_form_material",
                    "").forGetter((ToolShapelessRecipe recipe) -> recipe.conversionMaterial),
            Codec.BOOL.optionalFieldOf("require_empty_fluid_containers",
                    false).forGetter((ToolShapelessRecipe recipe) -> recipe.requireEmptyFluidContainers),
            FormConversionPlacement.CODEC.optionalFieldOf("gregtech_form_placement", FormConversionPlacement.FIXED)
                    .forGetter((ToolShapelessRecipe recipe) -> recipe.formPlacement)
    ).apply(instance, (base, auto, variants, offset, input, material, empty, placement) -> new ToolShapelessRecipe(base, auto,
            new FormConversionSelector(variants, offset), input, material, empty, placement)));

    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ToolShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolShapelessRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };
*///?} else {
    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

        @Override
        public ToolShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ToolShapelessRecipe(vanilla.fromJson(id, json),
                    !json.has("gregtech_autocraftable") || json.get("gregtech_autocraftable").getAsBoolean(),
                    new FormConversionSelector(json.has("gregtech_form_variants") ? json.get("gregtech_form_variants").getAsInt() : 0,
                            json.has("gregtech_form_offset") ? json.get("gregtech_form_offset").getAsInt() : 0),
                    json.has("gregtech_form_input") ? json.get("gregtech_form_input").getAsString() : "",
                    json.has("gregtech_form_material") ? json.get("gregtech_form_material").getAsString() : "",
                    json.has("require_empty_fluid_containers") && json.get("require_empty_fluid_containers").getAsBoolean(),
                    json.has("gregtech_form_placement") ? FormConversionPlacement.fromName(json.get("gregtech_form_placement").getAsString()) : FormConversionPlacement.FIXED);
        }

        @Override
        public ToolShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ToolShapelessRecipe(vanilla.fromNetwork(id, buffer), buffer.readBoolean(),
                    new FormConversionSelector(buffer.readVarInt(), buffer.readVarInt()), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean(), buffer.readEnum(FormConversionPlacement.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ToolShapelessRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
            buffer.writeBoolean(recipe.autocraftable);
            buffer.writeVarInt(recipe.formSelector.variants());
            buffer.writeVarInt(recipe.formSelector.offset());
            buffer.writeUtf(recipe.conversionInput);
            buffer.writeUtf(recipe.conversionMaterial);
            buffer.writeBoolean(recipe.requireEmptyFluidContainers);
            buffer.writeEnum(recipe.formPlacement);
        }
    };
//?}
}
