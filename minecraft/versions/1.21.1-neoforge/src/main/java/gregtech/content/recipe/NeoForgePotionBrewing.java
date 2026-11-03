package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** NeoForge-side registration and probing of the vanilla brewing table. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class NeoForgePotionBrewing {
    private static final Item[] CONTAINERS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

    private NeoForgePotionBrewing() {}

    @SubscribeEvent
    public static void registerExtraIngredients(RegisterBrewingRecipesEvent event) {
        for (var route : PotionBrewingRoutes.all()) {
            var base = BuiltInRegistries.POTION.getHolder(ResourceLocation.fromNamespaceAndPath("minecraft", route.basePotion()));
            var result = BuiltInRegistries.POTION.getHolder(ResourceLocation.fromNamespaceAndPath("minecraft", route.resultPotion()));
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
        var ingredients = new ArrayList<ItemStack>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.getItem() instanceof PotionItem || !brewing.isIngredient(stack)) continue;
            ingredients.add(stack);
        }
        var rows = new ArrayList<PotionBrewingRecipes.Row>();
        for (var potion : BuiltInRegistries.POTION.holders().toList()) {
            for (Item container : CONTAINERS) {
                ItemStack input = PotionContents.createItemStack(container, potion);
                for (ItemStack ingredient : ingredients) {
                    ItemStack output = brewing.mix(ingredient, input);
                    if (output.isEmpty() || ItemStack.isSameItemSameComponents(output, input)) continue;
                    rows.add(new PotionBrewingRecipes.Row(input, ingredient.copyWithCount(1), output));
                }
            }
        }
        PotionBrewingRecipes.rebuild(rows);
    }
}
