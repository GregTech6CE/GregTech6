package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.plant.BaleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

/** Small server-side checks inside the single EMI client launch, never a separate world runner. */
final class CatalogFeedbackSmoke {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gregtech", path); }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    static void server(MinecraftServer server, JsonObject receipt) {
        int aliases = LegacyContentAliases.ALIASES.size();
        for (int i = 0; i < com.gregtech.gregtech.content.storage.SafeLockRules.KEY_MATERIALS.size(); i++) {
            var material = com.gregtech.gregtech.content.storage.SafeLockRules.KEY_MATERIALS.get(i);
            var key = GTDungeonKeys.byIndex(i);
            require(key == BuiltInRegistries.ITEM.get(id(material + "_key")), "dungeon uses duplicate key " + material);
            require(BuiltInRegistries.ITEM.get(id("key_" + material)) instanceof com.gregtech.gregtech.item.GTDungeonKeyItem,
                    "old key lost its behavior " + material);
        }
        var level = server.overworld();
        int height = level.getSeaLevel() + 36;
        var origin = new com.gregtech.gregtech.worldgen.center.NativeOriginWorld(level, new ChunkPos(0, 0), height);
        for (int meta = 0; meta < 3; meta++) {
            var pos = new BlockPos(3 + meta, height, 3);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            require(origin.setBlock(pos.getX(), pos.getY(), pos.getZ(),
                    new com.gregtech.gregtech.worldgen.center.OriginWorld.Block("source:sands"), meta, 3), "origin sand placement failed");
            String expected = com.gregtech.gregtech.block.BlackSandDefinitions.IDS.get(meta);
            require(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(id(expected)), "origin still places dust " + meta);
        }
        var fresh = (BaleBlock) BuiltInRegistries.BLOCK.get(id("bale_grass"));
        var moldy = (BaleBlock) BuiltInRegistries.BLOCK.get(id("bale_grass_moldy"));
        // The source Nexus plains quadrant is dry; retain the original random gates.
        var pos = new BlockPos(-40, height, 40);
        level.getChunkAt(pos);
        for (var side : Direction.values()) level.setBlock(pos.relative(side), Blocks.AIR.defaultBlockState(), 3);
        level.setWeatherParameters(6000, 0, false, false);
        level.setBlock(pos, fresh.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 3);
        require(level.getBlockTicks().hasScheduledTick(pos, fresh), "fresh bale does not schedule drying");
        var random = net.minecraft.util.RandomSource.create(0xBA1E);
        for (int n = 0; n < 1000 && level.getBlockState(pos).is(fresh); n++) fresh.tick(level.getBlockState(pos), level, pos, random);
        require(level.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(id("bale_grass_dry"))), "dry fresh bale does not dry");
        require(level.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "bale drying loses orientation");
        level.setBlock(pos, fresh.defaultBlockState(), 3);
        level.setBlock(pos.east(), Blocks.WATER.defaultBlockState(), 3);
        for (int n = 0; n < 1000 && level.getBlockState(pos).is(fresh); n++) fresh.tick(level.getBlockState(pos), level, pos, random);
        require(level.getBlockState(pos).is(moldy), "wet fresh bale does not become moldy");
        for (int n = 0; n < 1000 && level.getBlockState(pos).is(moldy); n++) moldy.tick(level.getBlockState(pos), level, pos, random);
        require(level.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(id("bale_grass_rotten"))), "wet moldy bale does not rot");
        receipt.addProperty("catalogAliasesChecked", aliases);
        receipt.addProperty("canonicalDungeonKeys", 10);
        receipt.addProperty("originBlackSandVariants", 3);
        receipt.addProperty("baleDryWetRotAndScheduling", true);
        // Old ingredient IDs must resolve directly to the sole canonical item.
        var ingredient = com.gregtech.gregtech.data.MachineRecipeIngredients.resolve("item:gregtech:boiler_wall",
                com.gregtech.gregtech.content.material.Materials.StainlessSteel, 1);
        require(ingredient.equals(java.util.Map.of("item", "gregtech:tank_wall")), "canonical wall recipe lost");
        for (var row : java.util.stream.Stream.concat(com.gregtech.gregtech.data.OriginalSurvivalRecipes.ROWS.stream(),
                com.gregtech.gregtech.data.UtilityCraftingRows.ROWS.stream()).toList()) {
            var recipe=server.getRecipeManager().byKey(id("survival_repairs/" + row.id()));
            require(recipe.isPresent(), "missing source crafting recipe " + row.id());
            for (var input : recipe.orElseThrow().value().getIngredients())
                require(input == net.minecraft.world.item.crafting.Ingredient.EMPTY || java.util.Arrays.stream(input.getItems()).anyMatch(s -> !s.isEmpty() && !s.is(net.minecraft.world.item.Items.BARRIER)),
                        "empty crafting ingredient in " + row.id());
        }
        checkRecipeTags(server, receipt);
        require(com.gregtech.gregtech.registry.GTStorageContainers.LOCKERS.size()==60,"incomplete locker metalset");
        for(var holder:com.gregtech.gregtech.registry.GTStorageContainers.LOCKERS) {
            var locker=holder.get();
            require(locker.material()!=null,"uncolored locker");
            require(locker.newBlockEntity(new BlockPos(0,height,0),locker.defaultBlockState()).getType().isValid(locker.defaultBlockState()),"locker entity rejects material variant");
        }
        require(!server.getRecipeManager().byKey(id("fusion/fusion_reactor_main")).isPresent(),"duplicate fusion crafting still loaded");
        var bath=com.gregtech.gregtech.data.MachineRecipeMaps.Bath;
        require(com.gregtech.gregtech.api.recipe.MachineGuiCoordinates.item(true,0,bath.mInputItemsCount,bath.mInputFluidCount).y()==16,"Bath input does not align with texture");
        require(com.gregtech.gregtech.api.recipe.MachineGuiCoordinates.item(false,0,bath.mOutputItemsCount,bath.mOutputFluidCount).y()==16,"Bath output does not align with texture");
        require(server.getRecipeManager().byKey(id("technology/plastic_lighter_empty")).isPresent(),"missing original plastic lighter recipe");
        var omitted = new com.google.gson.JsonArray();
        com.gregtech.gregtech.content.recipe.UtilityProcessingRecipes.UNAVAILABLE_OPTIONAL_FORMS.forEach(omitted::add);
        receipt.add("unavailableOptionalMaterialForms", omitted);
        receipt.addProperty("utilityCraftingRows",com.gregtech.gregtech.data.UtilityCraftingRows.ROWS.size());
        receipt.addProperty("lockerMaterials",60);
        receipt.addProperty("bathSlotY",16);
        receipt.addProperty("utilityProcessingRows",com.gregtech.gregtech.content.recipe.UtilityProcessingRecipes.INSTANCE.entries().size());
        require(server.getRecipeManager().byKey(id("bales/bale_grass_pack")).isPresent(), "missing grass bale recipe");
        for (var wood : com.gregtech.gregtech.content.plant.WoodSlabCatalog.WOODS) {
            var slab = BuiltInRegistries.BLOCK.get(id("slab_" + wood));
            require(slab instanceof com.gregtech.gregtech.api.block.FaceGridSlab, "no nine-grid wood slab " + wood);
            require(server.getRecipeManager().byKey(id("catalog_repairs/slab_" + wood)).isPresent(), "missing wood slab recipe " + wood);
        }
        for(var alias : LegacyContentAliases.ALIASES.entrySet()) {
            require(BuiltInRegistries.ITEM.get(id(alias.getValue())) != net.minecraft.world.item.Items.AIR,"alias points to missing target " + alias);
            require(BuiltInRegistries.ITEM.get(id(alias.getKey())) == BuiltInRegistries.ITEM.get(id(alias.getValue())), "duplicate identity " + alias);
            require(!BuiltInRegistries.ITEM.keySet().contains(id(alias.getKey())), "old item still registered " + alias);
        }
        receipt.addProperty("sourceRepairCraftingRows", com.gregtech.gregtech.data.OriginalSurvivalRecipes.ROWS.size());
        var cut = com.gregtech.gregtech.data.MachineRecipeMaps.Cutter.mRecipeList.stream()
                .filter(r -> r.mOutputs.length > 0 && java.util.Set.of("glass_clear_slab","glass_glow_slab","cfoam_slab")
                        .contains(BuiltInRegistries.ITEM.getKey(r.mOutputs[0].getItem()).getPath())).toList();
        require(cut.size() == 16 * 3 * com.gregtech.gregtech.content.recipe.SawingCoolants.variants(false).size(), "missing colored slab coolant recipes " + cut.size());
        for (var recipe : cut) {
            require(recipe.mExactItemInputs, "slab cutter ignores color");
            require(com.gregtech.gregtech.block.misc.ColoredConstructionBlock.itemColor(recipe.mInputs[0])
                    == com.gregtech.gregtech.block.misc.ColoredConstructionBlock.itemColor(recipe.mOutputs[0]), "slab cutter changes color");
        }
        receipt.addProperty("exactColoredSlabCuttingRecipes", cut.size());
        receipt.addProperty("woodSlabFamilies", com.gregtech.gregtech.content.plant.WoodSlabCatalog.WOODS.size());
        receipt.addProperty("canonicalWallRecipeAlternatives", true);
        com.mojang.logging.LogUtils.getLogger().info("CATALOG_FEEDBACK_SERVER_SUCCESS aliases={} keys=10 sands=3 baleDryWetRot=true", aliases);
    }
    private static void checkRecipeTags(MinecraftServer server, JsonObject receipt) {
        var failures = new java.util.ArrayList<String>();
        for (String name : java.util.List.of("forge:chests/wooden", "gregtech:bumble_hives", "gregtech:plant_pot_mud",
                "gregtech:clay_balls", "gregtech:quicksilver", "minecraft:music_discs", "forge:slimeballs",
                "gregtech:ingot/brick", "forge:sandstone", "forge:plates/osmium", "forge:plates/ad", "forge:plates/bedrock_hsla_alloy")) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(name));
            if (BuiltInRegistries.ITEM.getTag(tag).map(v -> v.size() == 0).orElse(true)) failures.add(name);
        }
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("forge:dyes/"+color.getName()));
            if (BuiltInRegistries.ITEM.getTag(tag).map(v -> v.size() == 0).orElse(true)) failures.add(tag.location().toString());
        }
        for (var item : BuiltInRegistries.ITEM) {
            var path=BuiltInRegistries.ITEM.getKey(item).getPath();
            if(item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof com.gregtech.gregtech.api.energy.WireMaterialLike wire) {
                String form=(wire.spec().insulated()?"cable_gt":"wire_gt")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,id(form+"/"+com.gregtech.gregtech.api.material.MaterialEquivalence.materialName(wire.spec().material())));
                require(new net.minecraft.world.item.ItemStack(item).is(tag),"missing wire tag "+tag.location());
            }
            if (path.startsWith("axle_") || path.startsWith("log_") || path.startsWith("planks_")) {
                var stack=new net.minecraft.world.item.ItemStack(item);
                require(stack.getTags().noneMatch(t -> t.location().getPath().startsWith("ores/")), "false ore tag: "+path);
            }
        }
        int imprints=0, spikes=0, tapes=0;
        for(var holder:server.getRecipeManager().getRecipes()) {
            var path=holder.id().getPath();
            if(!holder.id().getNamespace().equals("gregtech") || !(path.startsWith("clay_molds/") || path.startsWith("catalog_repairs/spike_") || path.contains("tape")))continue;
            for(var input:holder.value().getIngredients())
                if(input!=net.minecraft.world.item.crafting.Ingredient.EMPTY && java.util.Arrays.stream(input.getItems()).noneMatch(s->!s.isEmpty()&&!s.is(net.minecraft.world.item.Items.BARRIER))) failures.add(holder.id().toString());
            if(path.startsWith("clay_molds/imprint_"))imprints++;
            if(path.startsWith("catalog_repairs/spike_"))spikes++;
            if(path.startsWith("catalog_repairs/") && path.contains("tape"))tapes++;
            if(path.startsWith("catalog_repairs/spike_")) require(holder.value().getResultItem(server.registryAccess()).has(net.minecraft.core.component.DataComponents.BLOCK_STATE), "spike output lost block state");
        }
        for(var raw:com.gregtech.gregtech.content.recipe.ClayMoldCatalog.RAW) {
            var input=com.gregtech.gregtech.content.recipe.ClayMoldRecipes.input(raw);
            for(var type:java.util.List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING,net.minecraft.world.item.crafting.RecipeType.BLASTING))
                if(server.getRecipeManager().getRecipes().stream().noneMatch(h->h.value().getType()==type && h.value().getIngredients().stream().anyMatch(i->i.test(input))
                    && com.gregtech.gregtech.block.machine.MoldItemData.shape(h.value().getResultItem(server.registryAccess()))==raw.shape()))failures.add(raw.id()+" missing shaped "+type);
            if(server.getRecipeManager().byKey(id("clay_molds/reclaim_"+raw.id())).isEmpty())failures.add(raw.id()+" reclaim");
            if(server.getRecipeManager().getRecipes().stream().noneMatch(h->h.value().getType()==net.minecraft.world.item.crafting.RecipeType.CRAFTING
                    && h.value().getResultItem(server.registryAccess()).is(input.getItem())))failures.add(raw.id()+" forming");
        }
        require(spikes==20 && tapes==6,"missing feedback recipes: spikes="+spikes+" tapes="+tapes);
        require(failures.isEmpty(),"recipe tag/firing failures: "+failures);
        require(imprints>0,"all clay imprints disappeared");
        receipt.addProperty("validClayImprints",imprints);
        receipt.addProperty("clayMoldFiringAndReclaim",com.gregtech.gregtech.content.recipe.ClayMoldCatalog.RAW.size());
        receipt.addProperty("reportedEmptyTagsResolved",true);
    }
}
