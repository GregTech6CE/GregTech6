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
        int aliases = 0;
        for (var entry : LegacyContentAliases.ALIASES.entrySet()) {
            if (!BuiltInRegistries.ITEM.containsKey(id(entry.getKey()))) continue;
            require(BuiltInRegistries.ITEM.containsKey(id(entry.getValue())), "missing alias target " + entry);
            var old = BuiltInRegistries.ITEM.get(id(entry.getKey()));
            require(LegacyContentBinding.canonicalItem(old) == BuiltInRegistries.ITEM.get(id(entry.getValue())), "alias mismatch " + entry);
            if (BuiltInRegistries.BLOCK.containsKey(id(entry.getKey()))) {
                var a = BuiltInRegistries.BLOCK.get(id(entry.getKey()));
                var b = BuiltInRegistries.BLOCK.get(id(entry.getValue()));
                require(LegacyContentBinding.samePart(a, b), "old structural part not accepted " + entry);
                require(!LegacyContentBinding.samePart(a, Blocks.STONE), "alias accepts unrelated block " + entry);
            }
            aliases++;
        }
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
        // Machine recipes may name an old wall; accept either identity with the
        // canonical item first rather than requiring a hidden unobtainable part.
        var ingredient = com.gregtech.gregtech.data.MachineRecipeIngredients.resolve("item:gregtech:boiler_wall",
                com.gregtech.gregtech.content.material.Materials.StainlessSteel, 1);
        require(ingredient instanceof java.util.List<?> options && options.size() == 2
                && options.getFirst().equals(java.util.Map.of("item", "gregtech:tank_wall"))
                && options.contains(java.util.Map.of("item", "gregtech:boiler_wall")), "canonical/legacy recipe alternatives lost");
        receipt.addProperty("canonicalWallRecipeAlternatives", true);
        com.mojang.logging.LogUtils.getLogger().info("CATALOG_FEEDBACK_SERVER_SUCCESS aliases={} keys=10 sands=3 baleDryWetRot=true", aliases);
    }
}
