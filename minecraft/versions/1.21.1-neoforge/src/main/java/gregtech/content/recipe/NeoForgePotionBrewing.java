package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.engine.stack.StackCompat;
import gregtech.mixin.PotionBrewingAccessor;
import gregtech.mixin.PotionBrewingMixAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** NeoForge-side registration and probing of the vanilla brewing table. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class NeoForgePotionBrewing {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Item[] FALLBACK_CONTAINERS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

    private NeoForgePotionBrewing() {}

    @SubscribeEvent
    public static void registerExtraIngredients(RegisterBrewingRecipesEvent event) {
        for (var route : PotionBrewingRoutes.all()) {
            var base = BuiltInRegistries.POTION.getHolder(
                    ResourceLocation.fromNamespaceAndPath("minecraft", route.basePotion()));
            var result = BuiltInRegistries.POTION.getHolder(
                    ResourceLocation.fromNamespaceAndPath("minecraft", route.resultPotion()));
            if (base.isEmpty() || result.isEmpty() || route.ingredient().isEmpty()) continue;
            event.getBuilder().addRecipe(new IBrewingRecipe() {
                @Override public boolean isInput(ItemStack input) {
                    if (!(input.getItem() instanceof PotionItem)) return false;
                    return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion()
                            .map(holder -> holder.value() == base.get().value()).orElse(false);
                }

                @Override public boolean isIngredient(ItemStack ingredient) {
                    return ItemStack.isSameItemSameComponents(ingredient, route.ingredient());
                }

                @Override public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                    if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
                    return PotionContents.createItemStack(input.getItem(), result.get());
                }
            });
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        var level = event.getServer().overworld();
        if (level != null) probe(level.potionBrewing());
    }

    public static void probe(PotionBrewing brewing) {
        var containers = containers(brewing);
        var reagents = reagents(brewing);
        var rows = new ArrayList<PotionBrewingRecipes.Row>();
        for (Holder<Potion> potion : BuiltInRegistries.POTION.holders().toList()) {
            for (ItemStack container : containers) {
                ItemStack input = PotionContents.createItemStack(container.getItem(), potion);
                for (ItemStack reagent : reagents) {
                    addRow(rows, input, reagent, output(brewing, input, reagent));
                }
            }
        }
        addCustomRecipes(brewing, rows);
        PotionBrewingRecipes.rebuild(rows);
    }

    private static List<ItemStack> containers(PotionBrewing brewing) {
        var accessor = (PotionBrewingAccessor) brewing;
        var stacks = new ArrayList<ItemStack>();
        for (Ingredient ingredient : accessor.gregtech$containers())
            for (ItemStack stack : ingredient.getItems())
                if (stack.getItem() instanceof PotionItem) stacks.add(stack);
        var distinct = PotionBrewingRecipes.distinct(stacks);
        if (!distinct.isEmpty()) return distinct;
        var fallback = new ArrayList<ItemStack>();
        for (Item container : FALLBACK_CONTAINERS) fallback.add(new ItemStack(container));
        return fallback;
    }

    private static List<ItemStack> reagents(PotionBrewing brewing) {
        var accessor = (PotionBrewingAccessor) brewing;
        var stacks = new ArrayList<ItemStack>();
        for (Object mix : accessor.gregtech$potionMixes())
            for (ItemStack stack : ((PotionBrewingMixAccessor) mix).gregtech$ingredient().getItems())
                stacks.add(stack);
        for (Object mix : accessor.gregtech$containerMixes())
            for (ItemStack stack : ((PotionBrewingMixAccessor) mix).gregtech$ingredient().getItems())
                stacks.add(stack);
        for (IBrewingRecipe recipe : brewing.getRecipes())
            if (recipe instanceof BrewingRecipe custom)
                for (ItemStack stack : custom.getIngredient().getItems()) stacks.add(stack);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (validIngredient(brewing, stack)) stacks.add(stack);
        }
        return PotionBrewingRecipes.distinct(stacks);
    }

    private static boolean validIngredient(PotionBrewing brewing, ItemStack stack) {
        try {
            return brewing.isIngredient(stack);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("[gregtech] Brewing ingredient check failed for {}", stack, failure);
            return false;
        }
    }

    private static ItemStack output(PotionBrewing brewing, ItemStack input, ItemStack reagent) {
        try {
            return brewing.mix(reagent, input);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("[gregtech] Brewing mix failed for {} + {}", input, reagent, failure);
            return ItemStack.EMPTY;
        }
    }

    private static void addRow(
            List<PotionBrewingRecipes.Row> rows, ItemStack input, ItemStack reagent, ItemStack output) {
        // The ordinary input slot rejects potion items, so a potion reagent cannot be matched.
        if (reagent.getItem() instanceof PotionItem) return;
        if (output.isEmpty() || StackCompat.sameItem(output, input)) return;
        rows.add(new PotionBrewingRecipes.Row(input.copyWithCount(1), reagent.copyWithCount(1), output.copy()));
    }

    private static void addCustomRecipes(PotionBrewing brewing, List<PotionBrewingRecipes.Row> rows) {
        var unsupported = new HashSet<Class<?>>();
        for (IBrewingRecipe recipe : brewing.getRecipes()) {
            if (!(recipe instanceof BrewingRecipe custom)) {
                if (unsupported.add(recipe.getClass()))
                    LOGGER.debug(
                            "[gregtech] Cannot enumerate brewing recipe class {}",
                            recipe.getClass().getName());
                continue;
            }
            ItemStack output = custom.getOutput();
            if (output.isEmpty()) continue;
            for (ItemStack input : custom.getInput().getItems()) {
                if (!(input.getItem() instanceof PotionItem)) continue;
                for (ItemStack reagent : custom.getIngredient().getItems()) {
                    addRow(rows, input, reagent, output);
                }
            }
        }
    }
}
