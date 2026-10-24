package gregtech.integration.client;

import com.google.gson.JsonObject;
import gregtech.registry.*;
import gregtech.block.plant.BaleBlock;
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
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    static void server(MinecraftServer server, JsonObject receipt) {
        checkNamespaceIdentity(receipt);
        int canonical = gregtech.content.multiblock.SharedLargeMachineParts.DEFINITIONS.size();
        for (int i = 0; i < gregtech.content.storage.SafeLockRules.KEY_MATERIALS.size(); i++) {
            var material = gregtech.content.storage.SafeLockRules.KEY_MATERIALS.get(i);
            var key = GTDungeonKeys.byIndex(i);
            require(key == BuiltInRegistries.ITEM.get(id(material + "_key")), "dungeon uses duplicate key " + material);
        }
        var level = server.overworld();
        int height = level.getSeaLevel() + 36;
        var origin = new gregtech.worldgen.center.NativeOriginWorld(level, new ChunkPos(0, 0), height);
        for (int meta = 0; meta < 3; meta++) {
            var pos = new BlockPos(3 + meta, height, 3);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            require(origin.setBlock(pos.getX(), pos.getY(), pos.getZ(),
                    new gregtech.worldgen.center.OriginWorld.Block("source:sands"), meta, 3), "origin sand placement failed");
            String expected = gregtech.block.BlackSandDefinitions.IDS.get(meta);
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
        receipt.addProperty("canonicalPartsChecked", canonical);
        receipt.addProperty("canonicalDungeonKeys", 10);
        receipt.addProperty("originBlackSandVariants", 3);
        receipt.addProperty("baleDryWetRotAndScheduling", true);
        // Old ingredient IDs must resolve directly to the sole canonical item.
        var ingredient = gregtech.data.MachineRecipeIngredients.resolve("item:gt6ce:tank_wall",
                gregtech.content.material.Materials.StainlessSteel, 1);
        require(ingredient.equals(java.util.Map.of("item", "gt6ce:tank_wall")), "canonical wall recipe lost");
        for (var row : java.util.stream.Stream.concat(gregtech.data.OriginalSurvivalRecipes.ROWS.stream(),
                gregtech.data.UtilityCraftingRows.ROWS.stream()).toList()) {
            var recipe=server.getRecipeManager().byKey(id("survival_repairs/" + row.id()));
            require(recipe.isPresent(), "missing source crafting recipe " + row.id());
            for (var input : recipe.orElseThrow().value().getIngredients())
                require(input == net.minecraft.world.item.crafting.Ingredient.EMPTY || java.util.Arrays.stream(input.getItems()).anyMatch(s -> !s.isEmpty() && !s.is(net.minecraft.world.item.Items.BARRIER)),
                        "empty crafting ingredient in " + row.id());
        }
        checkRecipeTags(server, receipt);
        require(gregtech.registry.GTStorageContainers.LOCKERS.size()==60,"incomplete locker metalset");
        for(var holder:gregtech.registry.GTStorageContainers.LOCKERS) {
            var locker=holder.get();
            require(locker.material()!=null,"uncolored locker");
            require(locker.newBlockEntity(new BlockPos(0,height,0),locker.defaultBlockState()).getType().isValid(locker.defaultBlockState()),"locker entity rejects material variant");
        }
        require(!server.getRecipeManager().byKey(id("fusion/fusion_reactor_main")).isPresent(),"duplicate fusion crafting still loaded");
        var bath=gregtech.data.MachineRecipeMaps.Bath;
        require(gregtech.internal.recipe.MachineGuiCoordinates.item(true,0,bath.mInputItemsCount,bath.mInputFluidCount).y()==16,"Bath input does not align with texture");
        require(gregtech.internal.recipe.MachineGuiCoordinates.item(false,0,bath.mOutputItemsCount,bath.mOutputFluidCount).y()==16,"Bath output does not align with texture");
        require(server.getRecipeManager().byKey(id("technology/plastic_lighter_empty")).isPresent(),"missing original plastic lighter recipe");
        var omitted = new com.google.gson.JsonArray();
        gregtech.content.recipe.UtilityProcessingRecipes.UNAVAILABLE_OPTIONAL_FORMS.forEach(omitted::add);
        receipt.add("unavailableOptionalMaterialForms", omitted);
        receipt.addProperty("utilityCraftingRows",gregtech.data.UtilityCraftingRows.ROWS.size());
        receipt.addProperty("lockerMaterials",60);
        receipt.addProperty("bathSlotY",16);
        receipt.addProperty("utilityProcessingRows",gregtech.content.recipe.UtilityProcessingRecipes.INSTANCE.entries().size());
        require(server.getRecipeManager().byKey(id("bales/bale_grass_pack")).isPresent(), "missing grass bale recipe");
        for (var wood : gregtech.content.plant.WoodSlabCatalog.WOODS) {
            var slab = BuiltInRegistries.BLOCK.get(id("slab_" + wood));
            require(slab instanceof gregtech.internal.block.FaceGridSlab, "no nine-grid wood slab " + wood);
            require(server.getRecipeManager().byKey(id("catalog_repairs/slab_" + wood)).isPresent(), "missing wood slab recipe " + wood);
        }
        for(var part:gregtech.content.multiblock.SharedLargeMachineParts.DEFINITIONS){
            require(BuiltInRegistries.BLOCK.get(id(part.name()))==GTMultiblocks.findPart(part.originalId()).get(),"part identity mismatch "+part.name());
        }
        receipt.addProperty("sourceRepairCraftingRows", gregtech.data.OriginalSurvivalRecipes.ROWS.size());
        var cut = gregtech.data.MachineRecipeMaps.Cutter.mRecipeList.stream()
                .filter(r -> r.mOutputs.length > 0 && java.util.Set.of("glass_clear_slab","glass_glow_slab","cfoam_slab")
                        .contains(BuiltInRegistries.ITEM.getKey(r.mOutputs[0].getItem()).getPath())).toList();
        require(cut.size() == 16 * 3 * gregtech.content.recipe.SawingCoolants.variants(false).size(), "missing colored slab coolant recipes " + cut.size());
        for (var recipe : cut) {
            require(recipe.mExactItemInputs, "slab cutter ignores color");
            require(gregtech.block.misc.ColoredConstructionBlock.itemColor(recipe.mInputs[0])
                    == gregtech.block.misc.ColoredConstructionBlock.itemColor(recipe.mOutputs[0]), "slab cutter changes color");
        }
        receipt.addProperty("exactColoredSlabCuttingRecipes", cut.size());
        receipt.addProperty("woodSlabFamilies", gregtech.content.plant.WoodSlabCatalog.WOODS.size());
        receipt.addProperty("canonicalWallRecipeAlternatives", true);
        com.mojang.logging.LogUtils.getLogger().info("CATALOG_FEEDBACK_SERVER_SUCCESS canonicalParts={} keys=10 sands=3 baleDryWetRot=true", canonical);
    }
    private static void checkNamespaceIdentity(JsonObject receipt) {
        String namespace = "gt6ce";
        String expectedName = "GregTech 6 Community Edition";
        require(namespace.equals(gregtech.internal.mod.GregTechIdentity.MOD_ID)
                && namespace.equals(gregtech.internal.mod.GregTechIdentity.REGISTRY_NAMESPACE),
                "loader and content namespace differ");
        require(expectedName.equals(gregtech.internal.mod.GregTechIdentity.MOD_NAME),
                "shared mod display name differs");
        var keyId = id("copper_key");
        var blockId = id("clay_brown");
        var fluidId = id("seawater");
        require(BuiltInRegistries.ITEM.containsKey(keyId), "missing migrated copper key");
        require(BuiltInRegistries.ITEM.getKey(GTDungeonKeys.byIndex(
                gregtech.content.storage.SafeLockRules.KEY_MATERIALS.indexOf("copper"))).equals(keyId),
                "dungeon copper key uses another namespace");
        require(BuiltInRegistries.BLOCK.containsKey(blockId)
                && BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.get(blockId)).equals(blockId),
                "missing migrated clay block");
        require(BuiltInRegistries.FLUID.containsKey(fluidId)
                && BuiltInRegistries.FLUID.getKey(BuiltInRegistries.FLUID.get(fluidId)).equals(fluidId),
                "missing migrated seawater fluid");
        require(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("gregtech", keyId.getPath()))
                && !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("gregtech", blockId.getPath()))
                && !BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath("gregtech", fluidId.getPath())),
                "old content namespace remains registered");
        var container = net.neoforged.fml.ModList.get().getModContainerById(namespace)
                .orElseThrow(() -> new IllegalStateException("missing gt6ce mod container"));
        String loadedName = container.getModInfo().getDisplayName();
        require(expectedName.equals(loadedName), "loader reports another mod display name: " + loadedName);
        var owner = gregtech.data.ModReferences.GT;
        require(namespace.equals(owner.id) && expectedName.equals(owner.name) && owner.isLoaded()
                && gregtech.internal.mod.ModData.MODS.get(namespace) == owner,
                "material ownership metadata does not resolve to GT6CE");
        require("gregtech".equals(gregtech.data.ModReferences.GT5U.id)
                && "gregtech".equals(gregtech.data.ModReferences.GT6U.id),
                "external GregTech provenance IDs were replaced");
        String emiName;
        try {
            // EMI's implementation is available only at runtime, outside its compile-time API jar.
            emiName = (String) Class.forName("dev.emi.emi.EmiUtil")
                    .getMethod("getModName", String.class).invoke(null, namespace);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("cannot resolve actual EMI namespace display name", failure);
        }
        require(expectedName.equals(emiName), "EMI reports another mod name: " + emiName);
        var identity = new JsonObject();
        identity.addProperty("namespace", namespace);
        identity.addProperty("itemId", keyId.toString());
        identity.addProperty("blockId", blockId.toString());
        identity.addProperty("fluidId", fluidId.toString());
        identity.addProperty("oldRegistrySamplesAbsent", true);
        identity.addProperty("loaderDisplayName", loadedName);
        identity.addProperty("materialOwnerId", owner.id);
        identity.addProperty("materialOwnerDisplayName", owner.name);
        identity.addProperty("materialOwnerIdentityVerified", true);
        identity.addProperty("emiDisplayName", emiName);
        receipt.add("namespaceIdentity", identity);
    }
    private static void checkRecipeTags(MinecraftServer server, JsonObject receipt) {
        var failures = new java.util.ArrayList<String>();
        for (String name : java.util.List.of("forge:chests/wooden", "gt6ce:bumble_hives", "gt6ce:plant_pot_mud",
                "gt6ce:clay_balls", "gt6ce:quicksilver", "minecraft:music_discs", "forge:slimeballs",
                "gt6ce:ingot/brick", "forge:sandstone", "forge:plates/osmium", "forge:plates/ad", "forge:plates/bedrock_hsla_alloy")) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(name));
            if (BuiltInRegistries.ITEM.getTag(tag).map(v -> v.size() == 0).orElse(true)) failures.add(name);
        }
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("forge:dyes/"+color.getName()));
            if (BuiltInRegistries.ITEM.getTag(tag).map(v -> v.size() == 0).orElse(true)) failures.add(tag.location().toString());
        }
        for (var item : BuiltInRegistries.ITEM) {
            var path=BuiltInRegistries.ITEM.getKey(item).getPath();
            if(item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof gregtech.internal.energy.WireMaterialLike wire) {
                String form=(wire.spec().insulated()?"cable_gt":"wire_gt")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,id(form+"/"+gregtech.internal.material.MaterialEquivalence.materialName(wire.spec().material())));
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
            if(!holder.id().getNamespace().equals("gt6ce") || !(path.startsWith("clay_molds/") || path.startsWith("catalog_repairs/spike_") || path.contains("tape")))continue;
            for(var input:holder.value().getIngredients())
                if(input!=net.minecraft.world.item.crafting.Ingredient.EMPTY && java.util.Arrays.stream(input.getItems()).noneMatch(s->!s.isEmpty()&&!s.is(net.minecraft.world.item.Items.BARRIER))) failures.add(holder.id().toString());
            if(path.startsWith("clay_molds/imprint_"))imprints++;
            if(path.startsWith("catalog_repairs/spike_"))spikes++;
            if(path.startsWith("catalog_repairs/") && path.contains("tape"))tapes++;
            if(path.startsWith("catalog_repairs/spike_")) require(holder.value().getResultItem(server.registryAccess()).has(net.minecraft.core.component.DataComponents.BLOCK_STATE), "spike output lost block state");
        }
        for(var raw:gregtech.content.recipe.ClayMoldCatalog.RAW) {
            var input=gregtech.content.recipe.ClayMoldRecipes.input(raw);
            for(var type:java.util.List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING,net.minecraft.world.item.crafting.RecipeType.BLASTING))
                if(server.getRecipeManager().getRecipes().stream().noneMatch(h->h.value().getType()==type && h.value().getIngredients().stream().anyMatch(i->i.test(input))
                    && gregtech.block.machine.MoldItemData.shape(h.value().getResultItem(server.registryAccess()))==raw.shape()))failures.add(raw.id()+" missing shaped "+type);
            if(server.getRecipeManager().byKey(id("clay_molds/reclaim_"+raw.id())).isEmpty())failures.add(raw.id()+" reclaim");
            if(server.getRecipeManager().getRecipes().stream().noneMatch(h->h.value().getType()==net.minecraft.world.item.crafting.RecipeType.CRAFTING
                    && h.value().getResultItem(server.registryAccess()).is(input.getItem())))failures.add(raw.id()+" forming");
        }
        require(spikes==20 && tapes==6,"missing feedback recipes: spikes="+spikes+" tapes="+tapes);
        require(failures.isEmpty(),"recipe tag/firing failures: "+failures);
        require(imprints>0,"all clay imprints disappeared");
        receipt.addProperty("validClayImprints",imprints);
        receipt.addProperty("clayMoldFiringAndReclaim",gregtech.content.recipe.ClayMoldCatalog.RAW.size());
        receipt.addProperty("reportedEmptyTagsResolved",true);
    }
}
