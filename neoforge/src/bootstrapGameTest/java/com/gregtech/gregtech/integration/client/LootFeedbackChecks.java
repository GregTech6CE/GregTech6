package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.content.loot.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import java.util.*;

/** Native entity instances and the installed event bus, not a duplicate drop implementation. */
final class LootFeedbackChecks {
    private static String path(ItemStack stack) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
    /** Recipe previews enumerate tool identities; a real player uses an assembled tool. */
    private static ItemStack usableIngredient(ItemStack sample) {
        var stack = sample.copyWithCount(1);
        if (stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem tool
                && tool.toolType().requiresHeadAssembly()) {
            com.gregtech.gregtech.api.tool.GTToolHelper.write(stack,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));
        }
        return stack;
    }
    private static List<ItemEntity> post(LivingEntity entity, boolean recent, ItemStack... stacks) {
        var drops = new ArrayList<ItemEntity>();
        for (var stack : stacks) drops.add(new ItemEntity(entity.level(), 0, 100, 0, stack));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(
            entity, entity.damageSources().generic(), drops, recent));
        return drops;
    }
    private static LivingEntity create(EntityType<? extends LivingEntity> type, ServerLevel level) {
        var entity = type.create(level); entity.setPos(0, 100, 0); entity.getRandom().setSeed(1); return entity;
    }
    static void server(net.minecraft.server.MinecraftServer server, JsonObject receipt) {
        var level = server.overworld(); int checks = 0;
        var cow = create(EntityType.COW, level);
        var iron = new ItemStack(Items.IRON_INGOT, 3);
        iron.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Preserved source drop"));
        var drops = post(cow, true, iron);
        require(drops.stream().anyMatch(e -> path(e.getItem()).equals("ingot_lead") && e.getItem().getCount() == 3), "Cow source iron->lead/count failed: " + drops.stream().map(e -> path(e.getItem()) + "=" + e.getItem().getCount()).toList()); checks++;
        require(drops.stream().anyMatch(e -> path(e.getItem()).equals("ingot_lead") && e.getItem().getHoverName().getString().equals("Preserved source drop")), "Source lead replacement lost native metadata"); checks++;
        require(drops.stream().anyMatch(e -> path(e.getItem()).equals("cow_hoof") || path(e.getItem()).equals("cow_horn")), "Cow native parts never reached actual event"); checks++;
        var baby = (net.minecraft.world.entity.animal.Cow)create(EntityType.COW, level); baby.setBaby(true);
        require(post(baby, true).isEmpty(), "Baby cow gained adult extra drops"); checks++;
        var golem = create(EntityType.IRON_GOLEM, level);
        require(post(golem, false, new ItemStack(Items.IRON_INGOT, 3)).get(0).getItem().is(Items.IRON_INGOT), "Iron golem incorrectly changed to lead"); checks++;
        var zombie = create(EntityType.ZOMBIE, level);
        var wooden = post(zombie, false, new ItemStack(Items.WOODEN_SWORD));
        require(wooden.size() == 1 && wooden.get(0).getItem().is(Items.STICK) && wooden.get(0).getItem().getCount() == 1, "Farm wooden-tool source replacement failed"); checks++;
        var stone = post(create(EntityType.ZOMBIE, level), false, new ItemStack(Items.STONE_AXE));
        require(stone.size() == 1 && stone.get(0).getItem().is(Items.STICK) && stone.get(0).getItem().getCount() == 2, "Farm stone-tool source replacement failed"); checks++;
        require(post(create(EntityType.ZOMBIE, level), true, new ItemStack(Items.STONE_AXE)).stream().anyMatch(e -> e.getItem().is(Items.STONE_AXE)), "Player-kill tool drop was removed"); checks++;
        var villager = post(create(EntityType.ZOMBIE_VILLAGER, level), true);
        require(villager.stream().anyMatch(e -> path(e.getItem()).equals("dusty_guide_book"))
            && villager.stream().anyMatch(e -> path(e.getItem()).equals("dusty_material_dictionary")), "Zombie villager books missing"); checks++;
        require(post(create(EntityType.ZOMBIE_VILLAGER, level), false).isEmpty(), "Non-player zombie villager gained books"); checks++;
        var witch = post(create(EntityType.WITCH, level), true);
        require(witch.size() == 1 && path(witch.get(0).getItem()).equals("loot_bottle"), "Player-killed witch loot bottle missing"); checks++;
        for (var type : List.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE)) {
            var entity = create(type, level); entity.setRemainingFireTicks(100);
            var cooked = post(entity, true);
            require(cooked.stream().anyMatch(e -> path(e.getItem()).startsWith("grilled_")), "Burning horse-family meat missing: " + type); checks++;
        }
        var named = (Mob)create(EntityType.COW, level); named.setPersistenceRequired();
        named.setCustomName(net.minecraft.network.chat.Component.literal("Source cow"));
        require(post(named, true).stream().anyMatch(e -> e.getItem().is(Items.NAME_TAG) && e.getItem().getHoverName().getString().equals("Source cow")), "Persistent named-mob tag missing"); checks++;
        var rows = LootViewerData.mobDrops();
        require(!rows.isEmpty() && rows.stream().allMatch(r -> !r.output().isEmpty() && !r.inputs().isEmpty()), "Unresolved actual mob viewer output"); checks++;
        var loot = LootViewerData.lootTables();
        require(!loot.isEmpty() && loot.stream().allMatch(r -> !r.output().isEmpty()), "Unresolved actual loot viewer output"); checks++;
        int crafted = 0;
        for (String name : List.of("bathing_pot", "loot_crate", "crate")) {
            var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "tools/" + name);
            var loaded = server.getRecipeManager().byKey(key).orElseThrow(() -> new IllegalStateException("Missing source crafting: " + key));
            var recipe = (com.gregtech.gregtech.recipe.ToolShapedRecipe)loaded.value();
            var ingredients = recipe.getIngredients();
            int side = name.equals("crate") ? 2 : 3;
            require(ingredients.stream().filter(i -> i != net.minecraft.world.item.crafting.Ingredient.EMPTY).allMatch(i -> i.getItems().length > 0),
                "Unresolved source crafting ingredients: " + key);
            var inputs = new ArrayList<ItemStack>();
            for (var ingredient : ingredients) { var options = ingredient.getItems(); inputs.add(options.length == 0 ? ItemStack.EMPTY : usableIngredient(options[0])); }
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(side, side, inputs);
            require(recipe.matches(grid, level), "Native source ingredients do not match: " + key);
            var output = recipe.assemble(grid, level.registryAccess());
            require(path(output).equals(name) && output.getCount() == 1, "Source crafting output differs: " + key);
            crafted++;
        }
        require(!com.gregtech.gregtech.block.LootCrateBlock.crateStack().isEmpty(), "Opening loot crates still loses the empty crate");
        require(com.gregtech.gregtech.block.LootCrateBlock.rollVanillaLoot(level, net.minecraft.core.BlockPos.ZERO, net.minecraft.util.RandomSource.create(1)).size() <= 1,
            "Loot crate returns an entire chest rather than one stack");
        receipt.addProperty("newFeedbackCraftingRowsChecked", crafted);
        receipt.addProperty("lootCrateReturnAndStackLimitChecks", 2);
        receipt.addProperty("nativeMobDropEventChecks", checks);
        receipt.addProperty("lootResolvedViewerRows", loot.size());
        receipt.addProperty("mobResolvedViewerRows", rows.size());
    }
}
