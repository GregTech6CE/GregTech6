package gregtech.content.recipe;

import gregtech.content.item.GTToolItem;
import gregtech.engine.stack.StackCompat;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
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

/** Normal shaped crafting, with usable GT tools returned with the specified GT6 durability cost. */
public class ToolShapedRecipe extends GTShapedCraftingRecipe implements gregtech.engine.recipe.AutocraftableCraftingRecipe {
    private final boolean autocraftable;
    private final String constructionColor;
    private final boolean allowMirror;
    private final boolean requireEmptyFluidContainers;

    @Override
    public boolean isAutocraftableByGT() {
        return autocraftable;
    }

    public ToolShapedRecipe(ShapedRecipe base) {
        this(base, true);
    }

    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror) {
        this(base, allowMirror, false);
    }

    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers) {
        this(base, allowMirror, requireEmptyFluidContainers, true);
    }

    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers, boolean autocraftable) {
        this(base, allowMirror, requireEmptyFluidContainers, autocraftable, "");
    }

    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers, boolean autocraftable,
            String constructionColor) {
        this(base, allowMirror, requireEmptyFluidContainers, autocraftable, constructionColor,
                !(base instanceof ToolShapedRecipe));
    }

    /** {@code expandTools} stays off for a base that already carries expanded GT tool ingredients. */
    private ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers, boolean autocraftable,
            String constructionColor, boolean expandTools) {
        super(base);
        if (expandTools) getIngredients().replaceAll(CraftingTools::expand);
        this.allowMirror = allowMirror;
        this.requireEmptyFluidContainers = requireEmptyFluidContainers;
        this.autocraftable = autocraftable;
        this.constructionColor = constructionColor;
        displayConstructionColor();
    }

    private static boolean coloredConstruction(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && (item.getBlock() instanceof gregtech.content.block.misc.FixedConstructionColor
                    || item.getBlock().defaultBlockState().hasProperty(gregtech.content.block.misc.ColoredConstructionBlock.COLOR));
    }

    private void displayConstructionColor() {
        if (constructionColor.isEmpty()) return;
        var dye = net.minecraft.world.item.DyeColor.byName(constructionColor, null);
        if (dye == null) throw new IllegalArgumentException("Unknown construction color "+constructionColor);
        // Do not resolve tag ingredients here: recipe parsing precedes native tag binding.
        // Peeking at all getItems() arrays would cache empty screw tags for this reload.
        getIngredients().replaceAll(ingredient -> {
            var json = CraftingRecipeCompat.ingredientToJson(ingredient);
            if (!json.isJsonObject() || !json.getAsJsonObject().has("item")) return ingredient;
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(
                    json.getAsJsonObject().get("item").getAsString()));
            var sample = new ItemStack(item);
            return coloredConstruction(sample)
                    ? Ingredient.of(gregtech.content.block.misc.ConcreteBlock.coloredItem(((net.minecraft.world.item.BlockItem) item).getBlock(),
                    dye)) : ingredient;
        });
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        if (!(allowMirror ? super.onMatches(grid, level) : matchesUnmirrored(grid))) return false;
        if (!constructionColor.isEmpty()) {
            boolean color = false;
            for (int i = 0; i < grid.size(); i++) {
                if (coloredConstruction(grid.getItem(i))) {
                    color |= constructionColor.equals(
                            gregtech.content.block.misc.ColoredConstructionBlock.itemColor(grid.getItem(i)).getName());
                }
            }
            if (!color) return false;
        }
        return toolsUsable(grid);
    }

    /** Every GT tool in the grid has to be a usable tool of its own type. */
    protected boolean toolsUsable(CraftingGridView grid) {
        for (int slot = 0; slot < grid.size(); slot++) {
            var stack = grid.getItem(slot);
            if (!gregtech.content.item.behavior.BehaviorLighter.usableForCrafting(stack)) return false;
            if (requireEmptyFluidContainers && gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(stack)) return false;
            if (requireEmptyFluidContainers && !StackCompat.fluidContained(stack).isEmpty()) return false;
            if ((stack.getItem() instanceof GTToolItem
                            || stack.getItem() instanceof gregtech.content.item.ElectricToolItem)
                    && !GTToolHelper.matchesTool(stack, GTToolHelper.getType(stack))) return false;
        }
        return true;
    }

    private boolean matchesUnmirrored(CraftingGridView grid) {
        for (int left = 0; left <= grid.width() - getWidth(); left++) {
            for (int top = 0; top <= grid.height() - getHeight(); top++) {
                boolean ok = true;
                for (int y = 0; y < grid.height() && ok; y++) {
                    for (int x = 0; x < grid.width(); x++) {
                        int dx = x - left, dy = y - top;
                        var ingredient = dx >= 0 && dy >= 0 && dx < getWidth() && dy < getHeight()
                                ? getIngredients().get(dx + dy * getWidth()) : Ingredient.EMPTY;
                        if (!ingredient.test(grid.getItem(x + y * grid.width()))) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (ok) return true;
            }
        }
        return false;
    }

    @Override
    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        var result = super.onGetRemainingItems(grid);
        for (int slot = 0; slot < grid.size(); slot++) {
            var original = grid.getItem(slot);
            if (original.getItem() instanceof gregtech.content.item.ElectricToolItem electric) {
                var copy = original.copyWithCount(1);
                electric.consumeInteractionEnergy(
                        copy, gregtech.content.tool.ElectricToolWear.craftEnergy(electric), null);
                result.set(slot, gregtech.content.tool.ElectricToolWear.broken(electric, copy) ? ItemStack.EMPTY : copy);
                continue;
            }
            int cost = GTToolHelper.craftDamage(original);
            if (cost <= 0) continue;
            var copy = original.copyWithCount(1);
            long damage = (long) copy.getDamageValue() + cost;
            if (damage >= copy.getMaxDamage()) result.set(slot, ItemStack.EMPTY);
            else {
                copy.setDamageValue((int) damage);
                result.set(slot, copy);
            }
        }
        drainFiniteIngredients(grid, result);
        return result;
    }

    /** Return finite vessels only from the matched slots, including offsets and permitted mirrors. */
    private void drainFiniteIngredients(CraftingGridView grid, NonNullList<ItemStack> remains) {
        if (getIngredients().stream()
                .map(CraftingRecipeCompat::customIngredientOf)
                .noneMatch(i -> i instanceof FiniteBottleFillingRecipe.ContainerIngredient
                        || i instanceof QuarterFluidCraftingIngredient)) return;
        for (int left = 0; left <= grid.width() - getWidth(); left++) {
            for (int top = 0; top <= grid.height() - getHeight(); top++) {
                for (int mirror = 0; mirror <= (allowMirror ? 1 : 0); mirror++) {
                    Ingredient[] mapped = new Ingredient[grid.size()];
                    boolean matched = true;
                    for (int y = 0; y < grid.height() && matched; y++) {
                        for (int x = 0; x < grid.width(); x++) {
                            int dx = x - left, dy = y - top;
                            var ingredient = dx >= 0 && dy >= 0 && dx < getWidth() && dy < getHeight()
                                    ? getIngredients().get((mirror == 1 ? getWidth() - 1 - dx : dx)
                                            + dy * getWidth()) : Ingredient.EMPTY;
                            int slot = x + y * grid.width();
                            mapped[slot] = ingredient;
                            if (!ingredient.test(grid.getItem(slot))) {
                                matched = false;
                                break;
                            }
                        }
                    }
                    if (!matched) continue;
                    for (int slot = 0; slot < mapped.length; slot++) {
                        var custom = CraftingRecipeCompat.customIngredientOf(mapped[slot]);
                        if (custom instanceof FiniteBottleFillingRecipe.ContainerIngredient) {
                            remains.set(slot, FiniteBottleFillingRecipe.ContainerIngredient.drainOne(grid.getItem(slot)));
                        } else if (custom instanceof QuarterFluidCraftingIngredient quarter) {
                            remains.set(slot, quarter.drainOne(grid.getItem(slot)));
                        }
                    }
                    return;
                }
            }
        }
    }

    public String constructionColor() {
        return constructionColor;
    }

    /** GT6's {@code CR.MIR} for this row: false for every row the original did not opt in to. */
    public boolean allowMirror() {
        return allowMirror;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

//? if >=1.21.1 {
    /*public static final MapCodec<ToolShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            new ShapedRecipe.Serializer().codec().forGetter((ToolShapedRecipe recipe) -> recipe),
            Codec.BOOL.optionalFieldOf("allow_mirror", false).forGetter((ToolShapedRecipe recipe) -> recipe.allowMirror),
            Codec.BOOL.optionalFieldOf("require_empty_fluid_containers",
                    false).forGetter((ToolShapedRecipe recipe) -> recipe.requireEmptyFluidContainers),
            Codec.BOOL.optionalFieldOf("gregtech_autocraftable", true).forGetter((ToolShapedRecipe recipe) -> recipe.autocraftable),
            Codec.STRING.optionalFieldOf("construction_color", "").forGetter((ToolShapedRecipe recipe) -> recipe.constructionColor)
    ).apply(instance, ToolShapedRecipe::new));

    public static final RecipeSerializer<ToolShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ToolShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolShapedRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };
*///?} else {
    public static final RecipeSerializer<ToolShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

        @Override
        public ToolShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ToolShapedRecipe(vanilla.fromJson(id, json), json.has("allow_mirror") && json.get("allow_mirror").getAsBoolean(),
                    json.has("require_empty_fluid_containers") && json.get("require_empty_fluid_containers").getAsBoolean(),
                    !json.has("gregtech_autocraftable") || json.get("gregtech_autocraftable").getAsBoolean(),
                    json.has("construction_color") ? json.get("construction_color").getAsString() : "");
        }

        @Override
        public ToolShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ToolShapedRecipe(vanilla.fromNetwork(id, buffer), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readUtf(), false); // The packet already contains the authoritative resolved ingredients.
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ToolShapedRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
            buffer.writeBoolean(recipe.allowMirror);
            buffer.writeBoolean(recipe.requireEmptyFluidContainers);
            buffer.writeBoolean(recipe.autocraftable);
            buffer.writeUtf(recipe.constructionColor);
        }
    };
//?}
}
