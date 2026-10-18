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
    private static final String[] REQUIRED = {"functional_locker", "ores", "functional_electrolyzer", "functional_bumble_hive", "tool_head_pickaxe", "tool_head_shovel", "tool_head_axe",
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
            "functional_bumble_combs", "functional_chemical_bottles", "functional_food_condiments",
            "functional_food_fruits", "functional_food_vegetables", "functional_food_meat", "functional_food_eggs_dairy",
            "functional_food_dough", "functional_food_bakery", "functional_food_sandwiches", "functional_food_pizzas",
            "functional_food_potatoes", "functional_animal_parts", "functional_plant_materials", "functional_medicines",
            "functional_book", "functional_large_book", "glass_panes", "real_fluids",
            "functional_burning_box_liquid", "functional_burning_box_liquid_dense", "functional_long_dist_wire",
            "functional_long_dist_transformer", "functional_electromagnet", "functional_energy_storage", "functional_transformer",
            "functional_long_dist_pipe_fluid", "functional_quantum_energizer", "functional_food_shapes", "functional_compact_electric_motor", "functional_compact_electric_piston", "functional_compact_force_field_emitter", "functional_compact_signal_emitter", "functional_spray_paint", "functional_leaves", "functional_sapling", "functional_indicator_flowers", "functional_walls", "functional_dense_walls", "functional_coils", "functional_multiblock_tanks", "functional_multiblock_dense_tanks", "functional_large_crucibles", "functional_gas_turbine_cores", "functional_steam_turbine_cores", "functional_planks", "functional_circuit_board", "functional_circuit", "functional_circuit_part", "functional_circuit_plate", "functional_circuit_wire", "functional_crystal_circuit", "functional_magic_research_paper", "functional_log_hole", "functional_grassblock", "functional_barometer_bottle", "functional_clay_blocks", "functional_clay_balls", "functional_fluid_measure", "functional_rope", "functional_dynamite", "functional_robot_arm", "functional_compass", "functional_keys", "functional_bale", "functional_usb_sticks", "functional_usb_cables", "functional_usb_hdd", "functional_repeaters", "functional_bridges", "functional_mortar", "functional_laser_emitters", "functional_batteries", "functional_filters", "functional_wall_spikes", "functional_logs", "functional_crystal_processors", "functional_loot_items", "functional_generator_cores", "functional_wood_slabs", "decor_glass_clear_slab"};

    @SuppressWarnings("unchecked")
    static boolean frame(JsonObject receipt) throws ReflectiveOperationException {
        Class<?> manager = Class.forName(ROOT + "integration.emi.StackManager");
        if (stage == 0) {
            result = receipt;
            index = (List<EmiStack>) manager.getField("indexStacks").get(null);
            List<EmiStack> displayed = displayed(manager);
            check(displayed, receipt, "all");
            // Custom gearboxes and fixed-ratio transformer gearboxes are distinct families.
            var gearboxFolder=displayed.stream().filter(s->groupId(s).endsWith("/functional_gearbox")).findFirst().orElseThrow();
            require(members(gearboxFolder).size()==13,"gearbox folder contains obsolete sprite identities");
            var transformers=displayed.stream().filter(s->groupId(s).endsWith("/functional_rotation_transformer")).findFirst().orElseThrow();
            require(members(transformers).size()==13,"transformer gearbox folder missing materials");
            var clothing=displayed.stream().filter(s->groupId(s).endsWith("/functional_clothing")).findFirst().orElseThrow();
            require(members(clothing).size()==24,"clothing folder missing suit pieces");
            receipt.addProperty("clothingFolderMembers",24);
            var holes=displayed.stream().filter(s->groupId(s).endsWith("/functional_log_hole")).findFirst().orElseThrow();
            var holeIds=members(holes).stream().map(s->s.getId().getPath()).toList();
            require(holeIds.containsAll(List.of("tapped_maple","tapped_rainbowood")),"tapped logs missing from folder");
            var mc=net.minecraft.client.Minecraft.getInstance();
            int gearboxes=0,transformerCount=0;
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                boolean transformer=id.getPath().startsWith("rotation_transformer_");
                if(!id.getNamespace().equals("gt6ce")||!transformer&&(!id.getPath().startsWith("gearbox_")||id.getPath().equals("gearbox_axle")))continue;
                var stack=new net.minecraft.world.item.ItemStack(item);
                var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                int faces=0;
                for(var face:net.minecraft.core.Direction.values()) {
                    var quads=model.getQuads(null,face,net.minecraft.util.RandomSource.create(0));
                    for(var quad:quads) {
                        String texture=quad.getSprite().contents().name().toString();
                        require(transformer?texture.startsWith("gt6ce:block/machines/transformers/transformer_rotation/")
                                :texture.equals("gt6ce:block/iconsets/gearbox"),"wrong gearbox item texture: "+id);
                        if(!transformer)require(quad.isTinted(),"uncolored gearbox item: "+id);
                    }
                    faces+=quads.size();
                }
                require(transformer?faces>=6:faces==6,"transparent gearbox item model: "+id+" faces="+faces);
                require((mc.getItemColors().getColor(stack,0)>>>24)==255,"transparent gearbox item color: "+id);
                if(transformer)transformerCount++;else gearboxes++;
            }
            require(gearboxes==13,"gearbox model count: "+gearboxes);
            require(transformerCount==13,"transformer gearbox model count: "+transformerCount);
            receipt.addProperty("transformerGearboxInventoryModels",transformerCount);
            receipt.addProperty("gearboxInventoryTexturedFaces",gearboxes*6);
            receipt.addProperty("tappedLogsFolded",true);
            var springNames = new HashSet<String>();
            for (var spring : com.gregtech.gregtech.worldgen.FluidSpringRules.SPRINGS) {
                var block = com.gregtech.gregtech.registry.GTFluidSprings.byFluid(spring.fluidId());
                String name = new net.minecraft.world.item.ItemStack(block).getHoverName().getString();
                require(springNames.add(name) && name.contains(" ("), "ambiguous spring name " + name);
            }
            receipt.addProperty("distinctFluidSpringNames", springNames.size());
            int retiredChecked=0;
            for(String retired:java.util.List.of("diggable_clay","diggable_peat","key_copper","large_boiler_main","large_crucible_main","tank_3x3","tank_5x5","bumble_hive","long_dist_pipe","long_dist_wire","fuel_rod_naquadah","battery_lv")){
                var key=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",retired);
                require(!net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().contains(key),"retired item registered "+retired);
                require(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key)==net.minecraft.world.item.Items.AIR,"retired item alias still resolves "+retired);
                retiredChecked++;
            }
            receipt.addProperty("retiredRegistryIdsRejected",retiredChecked);
            int fluidQueries = 0;
            for (var raw : index) if (raw.getId().getPath().equals("fluid_item_water") || raw.getId().getPath().equals("fluid_item_carbondioxide")) {
                require(raw.getKey() instanceof net.minecraft.world.level.material.Fluid, "fluid item has item lookup key");
                require(!dev.emi.emi.api.EmiApi.getRecipeManager().getRecipesByInput(raw).isEmpty(), "fluid item cannot find usage recipes " + raw.getId());
                fluidQueries++;
            }
            require(fluidQueries > 0, "no water fluid display checked");
            receipt.addProperty("fluidItemUsageQueries", fluidQueries);
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
            if (!id.isEmpty() && !id.startsWith("gt6ce:"))
                for (var member : members(stack)) require(!member.getId().getNamespace().equals("gt6ce"), "GT member in foreign folder " + id + " / " + member.getId());
            if (!id.startsWith("gt6ce:material_forms/")) continue;
            String form = id.substring(id.indexOf('/') + 1);
            require(found.add(form), "duplicate group " + id);
            List<EmiStack> members = members(stack);
            require(members.size() > 1, "empty/single group " + id);
            require(!(boolean) stack.getClass().getField("isExpanded").get(stack), "unexpected expanded group " + id);
            for (EmiStack member : members) {
                String item = member.getId().toString();
                foldedItems.add(member.getClass().getName() + ":" + item);
                if (form.equals("functional_crusher")) require(!item.endsWith(":crusher_wheels"), "crusher wheels grouped with machines");
                if (form.equals("functional_shredder")) require(!item.endsWith(":shredder_blades"), "shredder blades grouped with machines");
                if (form.equals("plate")) require(!item.equals("minecraft:glass_pane") && !item.equals("minecraft:paper"),
                        "vanilla pane/paper still classified as plate");
                if (form.equals("real_fluids")) require(member.getItemStack().isEmpty() && item.startsWith("gt6ce:"),
                        "item or foreign fluid mixed into real fluids " + item);
                if (form.startsWith("wire_") || form.startsWith("cable_") || form.startsWith("pipe_") || form.startsWith("item_pipe_"))
                    require(item.startsWith("gt6ce:" + form + "_"), "mixed transport specification " + id + " / " + item);
            }
            if (form.equals("functional_electric_motor")) {
                var order = members.stream().map(m -> m.getId().getPath()).toList();
                require(order.equals(List.of("electric_motor_lv","electric_motor_mv","electric_motor_hv","electric_motor_ev","electric_motor_iv")), "motors not in voltage order " + order);
                receipt.addProperty("motorVoltageOrder", String.join(",", order));
            }
            require(!form.equals("other_gt"), "obsolete catchall folder");
            if (form.equals("functional_bumble_combs")) require(members.size() == 20
                    && members.stream().anyMatch(m -> m.getId().getPath().equals("clay_comb")), "bee comb variants missing");
            if (form.equals("functional_drinks")) require(members.stream().noneMatch(m -> Set.of(
                    "mercury_bottle", "ink_bottle", "lubricant_bottle", "glue_bottle").contains(m.getId().getPath())), "industrial bottle incorrectly grouped as a drink");
            if (form.equals("functional_electrolyzer")) require(members.stream().anyMatch(m -> m.getId().getPath().contains("galvanized_steel")), "LV electrolyzer missing");
            if (form.equals("functional_locker")) require(members.size()==60,"locker group incomplete");
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
