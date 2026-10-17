package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Opt-in checks against REMI's actual post-reload index and asynchronous search. */
final class RemiGroupingSmoke {
    private static final String ROOT = "com.evandev.remi.";
    private static int stage;
    private static int frames;
    private static int before, expandedMembers;
    private static JsonObject result;
    private static List<EmiStack> index;
    private static final String[] REQUIRED = {"tool_head_pickaxe", "tool_head_shovel", "tool_head_axe",
            "block_ingot", "block_solid", "crate_gt64_ingot", "safe", "key_safe",
            "wire_01", "wire_02", "wire_16", "cable_01", "cable_02", "cable_12",
            "pipe_tiny", "pipe_medium", "pipe_quadruple", "pipe_nonuple",
            "item_pipe_medium", "item_pipe_restrictive_medium", "unit", "minecart_wheels", "bullet_gt_small", "arrow_gt_wood",
            "functional_bumblebee_drone", "functional_bumblebee_scanned_queen", "functional_small_food_can",
            "functional_covers", "functional_panel_wood", "functional_track", "functional_drum",
            "functional_bush", "functional_material_bush", "functional_smelting_crucible",
            "functional_engine_steam", "functional_engine_steam_strong", "functional_reactor_rod_nuclear",
            "functional_burning_box_solid", "functional_burning_box_solid_dense", "functional_burning_box_gas",
            "functional_burning_box_gas_dense", "functional_burning_box_fluidbed", "functional_burning_box_fluidbed_dense",
            "functional_steam_boiler", "functional_strong_steam_boiler", "functional_scaffold", "functional_hopper",
            "fluid_item", "functional_stone_stone", "functional_stone_stone_slab",
            "functional_stone_smooth", "functional_stone_smooth_slab",
            "functional_stone_bricks_redstone", "functional_stone_bricks_redstone_slab",
            "decor_asphalt", "decor_cfoam", "decor_cfoam_fresh", "decor_cfoam_slab",
            "decor_concrete", "decor_concrete_reinforced", "decor_glass_clear", "decor_glass_glow", "decor_glass_glow_slab",
            "functional_integrated_circuit", "functional_loot_chest", "functional_glowtus",
            "functional_extruder_shape", "functional_low_heat_extruder_shape", "functional_sensor",
            "functional_fluid_spring", "functional_bookshelf", "functional_bars", "functional_bottle_crate", "functional_beam",
            "functional_auto_igniter", "functional_auto_hammer", "functional_charging_crafting_table",
            "functional_co2_laser", "functional_laser_absorber", "functional_clay_molds", "functional_canvas",
            "functional_single_use", "functional_drinks", "functional_foods", "functional_ice_cream",
            "functional_book", "functional_large_book", "glass_panes", "real_fluids",
            "functional_burning_box_liquid", "functional_burning_box_liquid_dense", "functional_long_dist_wire",
            "functional_long_dist_transformer", "functional_electromagnet", "functional_energy_storage", "functional_transformer"};

    @SuppressWarnings("unchecked")
    static boolean frame(JsonObject receipt) throws ReflectiveOperationException {
        Class<?> manager = Class.forName(ROOT + "integration.emi.StackManager");
        if (stage == 0) {
            result = receipt;
            index = (List<EmiStack>) manager.getField("indexStacks").get(null);
            List<EmiStack> displayed = displayed(manager);
            check(displayed, receipt, "all");
            var springNames = new HashSet<String>();
            for (var spring : com.gregtech.gregtech.worldgen.FluidSpringRules.SPRINGS) {
                var block = com.gregtech.gregtech.registry.GTFluidSprings.byFluid(spring.fluidId());
                String name = new net.minecraft.world.item.ItemStack(block).getHoverName().getString();
                require(springNames.add(name) && name.contains(" ("), "ambiguous spring name " + name);
            }
            receipt.addProperty("distinctFluidSpringNames", springNames.size());
            receipt.addProperty("remiRawIndex", index.size());
            receipt.addProperty("remiVersion", net.neoforged.fml.ModList.get().getModContainerById("remi")
                    .orElseThrow().getModInfo().getVersion().toString());
            stage = 1;
            com.mojang.logging.LogUtils.getLogger().info("REMI_GROUPING_STAGE inventory open, all displayed={}", displayed.size());
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
        } else if (stage < 5) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            screen(minecraft, minecraft.screen);
        }
        return stage == 5;
    }

    static void screen(net.minecraft.client.Minecraft minecraft, net.minecraft.client.gui.screens.Screen screen)
            throws ReflectiveOperationException {
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) || stage < 1 || stage >= 5) return;
        Class<?> manager = Class.forName(ROOT + "integration.emi.StackManager");
        Class<?> search = Class.forName("dev.emi.emi.search.EmiSearch");
        if (stage == 1) {
            if (++frames < 10) return;
            // REMI only applies asynchronous search results to its index while a
            // real EMI search panel is active. A world HUD alone is insufficient.
            require(Class.forName("dev.emi.emi.screen.EmiScreenManager").getMethod("getSearchPanel").invoke(null) != null,
                    "inventory has no EMI search panel");
            Class<?> sidebar = Class.forName("dev.emi.emi.config.SidebarType");
            Class.forName("dev.emi.emi.screen.EmiScreenManager").getMethod("focusSearchSidebarType", sidebar)
                    .invoke(null, sidebar.getField("INDEX").get(null));
            Object widget = Class.forName("dev.emi.emi.screen.EmiScreenManager").getField("search").get(null);
            widget.getClass().getMethod("setValue", String.class).invoke(widget, "@gregtech");
            manager.getMethod("search", List.class, String.class).invoke(null, index, "@gregtech");
            stage = 2;
            frames = 0;
            com.mojang.logging.LogUtils.getLogger().info("REMI_GROUPING_STAGE search requested");
            return;
        }
        Thread worker = (Thread) search.getField("searchThread").get(null);
        if (worker != null && worker.isAlive()) return;
        List<EmiStack> displayed = displayed(manager);
        if (stage == 2 && ++frames < 30) return;
        // Use REMI's interaction path to expand then collapse a safe, proving that
        // folding does not remove its material variants from the searchable index.
        EmiStack safe = displayed.stream().filter(s -> groupId(s).endsWith("/safe")).findFirst().orElseThrow();
        Class<?> sidebar = Class.forName("dev.emi.emi.config.SidebarType");
        Object indexType = sidebar.getField("INDEX").get(null);
        var interact = manager.getMethod("onStackInteraction", EmiIngredient.class, sidebar);
        if (stage == 2) {
            check(displayed, result, "search");
            before = displayed.size();
            expandedMembers = members(safe).size();
            require(before < result.get("remiallDisplayed").getAsInt(), "search did not filter the all-items index");
            interact.invoke(null, safe, indexType);
            stage = 3;
            com.mojang.logging.LogUtils.getLogger().info("REMI_GROUPING_STAGE expanded, before={} members={} after={}", before, expandedMembers, displayed(manager).size());
        } else if (stage == 3) {
            require(displayed.size() == before + expandedMembers,
                    "safe expansion count: before=" + before + ", members=" + expandedMembers + ", after=" + displayed.size());
            interact.invoke(null, safe, indexType);
            stage = 4;
            com.mojang.logging.LogUtils.getLogger().info("REMI_GROUPING_STAGE collapsed");
        } else {
            require(displayed.size() == before, "safe did not collapse again");
            result.addProperty("remiSafeExpandCollapseMembers", expandedMembers);
            stage = 5;
            // This callback precedes inventory rendering: its framebuffer is HUD
            // only and must not be recorded as visual proof of the search panel.
            minecraft.setScreen(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<EmiStack> displayed(Class<?> manager) throws ReflectiveOperationException {
        return (List<EmiStack>) manager.getField("displayedStacks").get(null);
    }

    private static String groupId(EmiStack stack) {
        if (!stack.getClass().getSimpleName().equals("EmiGroupStack")) return "";
        try {
            Object group = stack.getClass().getField("group").get(stack);
            return group.getClass().getMethod("getId").invoke(group).toString();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static List<EmiStack> members(EmiStack stack) throws ReflectiveOperationException {
        List<EmiStack> result = new ArrayList<>();
        for (Object member : (List<?>) stack.getClass().getMethod("getItems").invoke(stack))
            result.add((EmiStack) member.getClass().getField("realStack").get(member));
        return result;
    }

    private static void check(List<EmiStack> displayed, JsonObject receipt, String phase) throws ReflectiveOperationException {
        Set<String> found = new HashSet<>(), foldedItems = new HashSet<>();
        JsonObject counts = new JsonObject();
        for (EmiStack stack : displayed) {
            String id = groupId(stack);
            if (!id.startsWith("gregtech:material_forms/")) continue;
            String form = id.substring(id.indexOf('/') + 1);
            require(found.add(form), "duplicate group " + id);
            List<EmiStack> members = members(stack);
            require(members.size() > 1, "empty/single group " + id);
            require(!(boolean) stack.getClass().getField("isExpanded").get(stack), "unexpected expanded group " + id);
            for (EmiStack member : members) {
                String item = member.getId().toString();
                foldedItems.add(member.getClass().getName() + ":" + item);
                if (form.equals("plate")) require(!item.equals("minecraft:glass_pane") && !item.equals("minecraft:paper"),
                        "vanilla pane/paper still classified as plate");
                if (form.equals("real_fluids")) require(member.getItemStack().isEmpty() && item.startsWith("gregtech:"),
                        "item or foreign fluid mixed into real fluids " + item);
                if (form.startsWith("wire_") || form.startsWith("cable_") || form.startsWith("pipe_") || form.startsWith("item_pipe_"))
                    require(item.startsWith("gregtech:" + form + "_"), "mixed transport specification " + id + " / " + item);
            }
            counts.addProperty(form, members.size());
        }
        for (String expected : REQUIRED) {
            // @gregtech correctly filters out the vanilla-only pane family.
            if (phase.equals("search") && expected.equals("glass_panes")) continue;
            require(found.contains(expected), "missing folded group in " + phase + ": " + expected);
        }
        for (EmiStack stack : displayed) if (groupId(stack).isEmpty())
            require(!foldedItems.contains(stack.getClass().getName() + ":" + stack.getId()), "folded member still displayed separately: " + stack.getId());
        receipt.add("remi" + phase + "Groups", counts);
        receipt.addProperty("remi" + phase + "Displayed", displayed.size());
        receipt.addProperty("remi" + phase + "FoldedItems", foldedItems.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
