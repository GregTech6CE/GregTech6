/*
 * Adapted from GregTech-6 Team's RecipeMapAutocrafting (2024), LGPL-3.0-or-later.
 */
package gregtech.content.recipe;

import gregtech.engine.recipe.AutocraftableCraftingRecipe;
import gregtech.engine.recipe.Recipe;
import gregtech.content.data.CraftingBlueprintData;
import gregtech.content.data.UsbDataCable;
import gregtech.content.data.UsbDataMedia;
import gregtech.content.tool.CraftingToolDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

public final class AutocraftingRecipes {

    private AutocraftingRecipes() {
    }
//? if >=1.21.1 {
    /*public static final TagKey<Item> BLUEPRINTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6ce",
            "autocrafter/blueprints"));
    public static final TagKey<Item> INFINITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6ce",
            "autocrafter/infinite"));
*///?} else {
    public static final TagKey<Item> BLUEPRINTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6ce", "autocrafter/blueprints"));
    public static final TagKey<Item> INFINITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6ce", "autocrafter/infinite"));
//?}

    private static final class Cache {
//? if >=1.21.1 {
        /*final RecipeHolder<CraftingRecipe> anchor;
        final List<RecipeHolder<CraftingRecipe>> allowed, recent = new ArrayList<>();
*///?} else {
        final CraftingRecipe anchor;
        final List<CraftingRecipe> allowed, recent = new ArrayList<>();
//?}
        final Map<ResourceLocation, Boolean> permissions = new HashMap<>();
//? if >=1.21.1 {
        /*Cache(List<RecipeHolder<CraftingRecipe>> recipes) {
*///?} else {
        Cache(List<CraftingRecipe> recipes) {
//?}
            anchor = recipes.isEmpty() ? null : recipes.get(0);
            allowed = new ArrayList<>();
            for (var holder : recipes) {
//? if >=1.21.1 {
                /*var recipe = holder.value();
*///?} else {
                var recipe = holder;
//?}
                if (!(recipe instanceof AutocraftableCraftingRecipe gt) || gt.isAutocraftableByGT())
                allowed.add(holder);
            }
        }
    }
    private static final Map<RecipeManager, Cache> CACHES = new WeakHashMap<>();

    private static String id(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("gt6ce") ? id.getPath() : "";
    }

    public static boolean isProgram(ItemStack stack) {
        return !stack.isEmpty() && (id(stack).equals("blueprint") || UsbDataMedia.stickTier(stack) >= 1
                || UsbDataMedia.cableTier(stack) >= 1 || stack.is(BLUEPRINTS));
    }

    private static boolean infinite(ItemStack stack) {
        return CraftingToolDefinitions.infinite(id(stack)) || stack.is(INFINITE);
    }

    public static ItemStack[] blueprint(Level level, BlockEntity machine, ItemStack program) {
        if (!isProgram(program)) return new ItemStack[0];
        var root = CraftingBlueprintData.itemData(program);
        if (id(program).equals("blueprint")
                || root.contains(AutocraftingRules.BLUEPRINT_KEY, 10)
                || root.contains(AutocraftingRules.LEGACY_KEY, 9))
            return CraftingBlueprintData.read(level, root);
        if (UsbDataMedia.cableTier(program) >= 1) {
            var data = UsbDataCable.readAdjacentMatching(machine, program, 1, file -> CraftingBlueprintData.read(level, file).length > 0);
            return CraftingBlueprintData.read(level, data);
        }
        // USB 1.0 and its source higher-tier substitutes keep the grid in gt.usb.data.
        var data = root.contains("gt.usb.data", 10) ? root.getCompound("gt.usb.data") : null;
        return CraftingBlueprintData.read(level, data);
    }

    public static boolean containsInput(Level level, BlockEntity machine, ItemStack program, ItemStack stack) {
        for (ItemStack cell : blueprint(level, machine, program))
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(cell, stack)) return true;
*///?} else {
        if (ItemStack.isSameItemSameTags(cell, stack)) return true;
//?}
        return false;
    }

    private static boolean permitted(Level level, Cache cache, ResourceLocation id, CraftingRecipe recipe, ItemStack[] pattern) {
        if (recipe instanceof AutocraftableCraftingRecipe gt && !gt.isAutocraftableByGT()) return false;
        if (!(recipe instanceof AutocraftableCraftingRecipe)
                && !ConfiguredRecipeRules.keepVanillaStorageBlockRecipe(id)
                && gregtech.content.recipe.FormConversionCraftingRecipeInjector.disallowsPlainRecipe(recipe,
                        level.registryAccess())) return false;
        return cache.permissions.computeIfAbsent(id, unused -> {
            if (level.getServer() == null) return true;
//? if >=1.21.1 {
            /*var location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json");
*///?} else {
            var location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipes/" + id.getPath() + ".json");
//?}
            var resource = level.getServer().getResourceManager().getResource(location);
            if (resource.isEmpty()) return true;
            try (var reader = resource.get().openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                return !json.has("gregtech_autocraftable") || json.get("gregtech_autocraftable").getAsBoolean();
            } catch (java.io.IOException | RuntimeException error) {
                return false;
            }
        });
    }

    public static Recipe find(Level level, BlockEntity machine, ItemStack program,
//? if >=1.21.1 {
            /*List<ItemStack> available, List<net.neoforged.neoforge.fluids.FluidStack> fluids) {
*///?} else {
            List<ItemStack> available, List<net.minecraftforge.fluids.FluidStack> fluids) {
//?}
        if (level == null || level.isClientSide || available.isEmpty()) return null;
        ItemStack[] pattern = blueprint(level, machine, program);
        var grouped = AutocraftingRules.inputs(Arrays.asList(pattern), ItemStack::isEmpty,
                s -> s.getItem() instanceof gregtech.content.item.GTToolItem
                || s.getItem() instanceof gregtech.content.item.ElectricToolItem,
//? if >=1.21.1 {
                /*ItemStack::isSameItemSameComponents, AutocraftingRecipes::infinite);
*///?} else {
                ItemStack::isSameItemSameTags, AutocraftingRecipes::infinite);
//?}
        if (grouped == null || grouped.isEmpty()) return null;
        var owner = new AbstractContainerMenu(null, 0) {

            public boolean stillValid(Player player) {
                return false;
            }

            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }
        };
        var grid = new TransientCraftingContainer(owner, 3, 3);
        for (int i = 0; i < pattern.length; i++) grid.setItem(i, pattern[i].copy());
        var manager = level.getRecipeManager();
        Cache cache = CACHES.get(manager);
        if (cache == null || cache.anchor == null
//? if >=1.21.1 {
                /*|| manager.byKey(cache.anchor.id()).orElse(null) != cache.anchor) {
*///?} else {
                || manager.byKey(cache.anchor.getId()).orElse(null) != cache.anchor) {
//?}
            cache = new Cache(manager.getAllRecipesFor(RecipeType.CRAFTING));
            CACHES.put(manager, cache);
        }
        CraftingRecipe chosen = null;
        for (var holder : cache.recent) {
//? if >=1.21.1 {
            /*var candidate = holder.value();
            if (candidate.matches(grid.asCraftInput(), level)
                    && permitted(level, cache, holder.id(), candidate, pattern)) {
                chosen = candidate;
                break;
            }
*///?} else {
            var candidate = holder;
            if (candidate.matches(grid, level)
                    && permitted(level, cache, candidate.getId(), candidate, pattern)) {
                chosen = candidate;
                break;
            }
//?}
        }
        if (chosen == null) for (int i = 0; i < cache.allowed.size(); i++) {
            var holder = cache.allowed.get(i);
//? if >=1.21.1 {
            /*var candidate = holder.value();
            if (!candidate.matches(grid.asCraftInput(), level)
                    || !permitted(level, cache, holder.id(), candidate, pattern)) continue;
*///?} else {
            var candidate = holder;
            if (!candidate.matches(grid, level)
                    || !permitted(level, cache, candidate.getId(), candidate, pattern)) continue;
//?}
            chosen = candidate;
            cache.recent.add(holder);
            cache.allowed.remove(i);
            break;
        }
        if (chosen == null) return null;
//? if >=1.21.1 {
        /*ItemStack product = chosen.assemble(grid.asCraftInput(), level.registryAccess());
*///?} else {
        ItemStack product = chosen.assemble(grid, level.registryAccess());
//?}
        if (product.isEmpty()) return null;
        var outputs = new ArrayList<AutocraftingRules.Amount<ItemStack>>();
        outputs.add(new AutocraftingRules.Amount<>(product, product.getCount()));
        // The source returns each plan item's container, rather than arbitrary recipe-specific leftovers.
        for (ItemStack cell : pattern) if (!cell.isEmpty()) {
            ItemStack container = cell.getCraftingRemainingItem();
            if (!container.isEmpty()) outputs.add(new AutocraftingRules.Amount<>(container, container.getCount()));
        }
//? if >=1.21.1 {
        /*var plan = AutocraftingRules.optimize(grouped, outputs, ItemStack::isSameItemSameComponents);
*///?} else {
        var plan = AutocraftingRules.optimize(grouped, outputs, ItemStack::isSameItemSameTags);
//?}
        if (plan.outputs().isEmpty()) return null;
        var inputs = new ArrayList<ItemStack>();
        var catalysts = new ArrayList<Integer>();
        for (var input : plan.inputs()) {
            if (input.count() == 0 || input.retained()) catalysts.add(inputs.size());
            inputs.add(input.sample().copyWithCount(Math.max(1, input.count())));
        }
        ItemStack[] products = plan.outputs().stream().map(v -> v.sample().copyWithCount(v.count())).toArray(ItemStack[]::new);
        Recipe result = new Recipe(inputs.toArray(ItemStack[]::new), products, null, null, null, null,
                plan.duration(), AutocraftingRules.POWER, 0);
        result.mExactItemInputs = true;
        result.mExplicitCatalystsOnly = true;
        result.withCatalystInputs(catalysts.stream().mapToInt(Integer::intValue).toArray());
        return result;
    }
}
