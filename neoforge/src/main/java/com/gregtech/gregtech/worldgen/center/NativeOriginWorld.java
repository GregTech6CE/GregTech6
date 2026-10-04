package com.gregtech.gregtech.worldgen.center;

import com.gregtech.gregtech.block.misc.CFoamSlabBlock;
import com.gregtech.gregtech.block.misc.ConcreteBlock;
import com.gregtech.gregtech.block.misc.RoadStripeRailBlock;
import com.gregtech.gregtech.block.stone.StoneVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Native effects for GT6's complete origin layouts. Never writes or forces loads outside this chunk. */
public final class NativeOriginWorld implements OriginWorld {
    private record StateKey(String id, int metadata, int side) {}
    private final WorldGenLevel level;
    private final ChunkPos chunk;
    private final int height;
    private final Map<StateKey, BlockState> states = new HashMap<>();
    private final Map<String, Boolean> waterBiomes = new HashMap<>();
    private final Map<Long, OriginWorld.Biome> biomeSamples = new HashMap<>();

    public NativeOriginWorld(WorldGenLevel level, ChunkPos chunk, int height) {
        this.level = level; this.chunk = chunk; this.height = height;
    }
    @Override public int minY() { return level.getMinBuildHeight(); }
    @Override public boolean canWrite(int x, int y, int z) {
        return (x >> 4) == chunk.x && (z >> 4) == chunk.z && !level.isOutsideBuildHeight(y);
    }
    @Override public OriginWorld.Block getBlock(int x, int y, int z) {
        if (level.isOutsideBuildHeight(y) || !level.hasChunk(x >> 4, z >> 4)) return OriginSupport.NB;
        BlockPos pos = new BlockPos(x,y,z);
        BlockState state = level.getBlockState(pos);
        return new OriginWorld.Block(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), -1,
                state.isSolidRender(level,pos), !state.getFluidState().isEmpty(), state.is(BlockTags.LOGS),
                state.is(BlockTags.LEAVES), state.getFluidState().is(FluidTags.WATER));
    }
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private BlockState state(StateKey key) {
        String blockId = key.side >= 0 ? "gregtech:cfoam_slab" : key.id;
        if (blockId.startsWith("gregtech:stone_")) blockId += "_" + StoneVariant.byMeta(key.metadata).registrySuffix();
        if (blockId.equals("minecraft:terracotta")) blockId = "minecraft:" + DyeColor.byId(15-key.metadata).getName() + "_terracotta";
        if (blockId.equals("minecraft:sandstone")) blockId = switch(key.metadata) {
            case 1 -> "minecraft:chiseled_sandstone"; case 2 -> "minecraft:cut_sandstone"; default -> blockId;
        };
        ResourceLocation location = id(blockId);
        if (!BuiltInRegistries.BLOCK.containsKey(location)) throw new IllegalStateException("Missing original origin block: " + blockId);
        BlockState state = BuiltInRegistries.BLOCK.get(location).defaultBlockState();
        if (state.hasProperty(ConcreteBlock.COLOR)) state = state.setValue(ConcreteBlock.COLOR,DyeColor.byId(15-key.metadata));
        if (key.side >= 0) state = state.setValue(CFoamSlabBlock.FACING,Direction.from3DDataValue(key.side));
        if (state.getBlock() instanceof RoadStripeRailBlock) state = state
                .setValue(RoadStripeRailBlock.SHAPE,(key.metadata & 1) == 0 ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST)
                .setValue(RoadStripeRailBlock.REFLECTOR,(key.metadata & 8) != 0);
        if (state.is(Blocks.END_PORTAL_FRAME)) state = state
                .setValue(EndPortalFrameBlock.FACING,switch(key.metadata & 3) {
                    case 0 -> Direction.SOUTH; case 1 -> Direction.WEST; case 2 -> Direction.NORTH; default -> Direction.EAST;
                }).setValue(EndPortalFrameBlock.HAS_EYE,(key.metadata & 4) != 0);
        return state;
    }
    @Override public boolean setBlock(int x, int y, int z, OriginWorld.Block block, int metadata, int flags) {
        if (!canWrite(x,y,z)) return false;
        BlockState state = states.computeIfAbsent(new StateKey(block.id(),metadata & 15,block.slabSide()),this::state);
        // Source flags 0 suppress physics. Send clients the result without breaking incomplete neighbours.
        return level.setBlock(new BlockPos(x,y,z),state,flags == 0 ? 2 : flags);
    }
    @Override public OriginWorld.Biome getBiomeGenForCoords(int x, int z) {
        long key = ChunkPos.asLong(x >> 2,z >> 2);
        return biomeSamples.computeIfAbsent(key,ignored -> sampleBiome(x,z));
    }
    private OriginWorld.Biome sampleBiome(int x, int z) {
        var server = level.getLevel();
        // Road signs query biomes 4096 blocks away. Query the biome source, never those chunks.
        var holder = server.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                x >> 2,height >> 2,z >> 2,server.getChunkSource().randomState().sampler());
        String name = holder.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
        boolean water = holder.is(BiomeTags.IS_OCEAN) || holder.is(BiomeTags.IS_BEACH) || holder.is(BiomeTags.IS_RIVER);
        // Original CS.BIOMES_INFINITE_WATER also recognises these named mod biomes.
        String path = name.substring(name.indexOf(':')+1);
        water |= OriginBiomeNames.water(path);
        waterBiomes.put(name,water);
        String[] words = path.split("_");
        for (int i=0;i<words.length;i++) if (!words[i].isEmpty()) words[i] = words[i].substring(0,1).toUpperCase(Locale.ROOT)+words[i].substring(1);
        return new OriginWorld.Biome(name,String.join(" ",words));
    }
    @Override public boolean isInfiniteWaterBiome(String name) { return waterBiomes.getOrDefault(name,false); }
    private void onServer(Runnable action) {
        var server = level.getLevel().getServer();
        if (server.isSameThread()) action.run(); else server.execute(action);
    }
    @Override public void setSpawnLocation(int x, int y, int z) {
        onServer(() -> level.getLevel().setDefaultSpawnPos(new BlockPos(x,y,z),0));
    }
    @Override public void sign(int x, int y, int z, int side, int flags, String... lines) {
        if (!canWrite(x,y,z)) return;
        BlockPos pos = new BlockPos(x,y,z);
        level.setBlock(pos,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.from3DDataValue(side)),2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            var text = sign.getFrontText();
            for (int i=0;i<Math.min(4,lines.length);i++) text = text.setMessage(i,Component.literal(lines[i]));
            // Proto-chunk block entities have no Level yet; setText sends a live world update.
            CompoundTag tag = sign.saveWithoutMetadata(level.registryAccess());
            tag.put("front_text", net.minecraft.world.level.block.entity.SignText.DIRECT_CODEC
                    .encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),text).result()
                    .orElseThrow(() -> new IllegalStateException("Cannot encode generated road sign")));
            sign.loadWithComponents(tag,level.registryAccess());
            sign.setChanged();
        }
    }
    @Override public void beacon(int x, int y, int z, String primary, String secondary) {
        if (!setBlock(x,y,z,OriginSupport.Blocks.beacon,0,2)) return;
        if (level.getBlockEntity(new BlockPos(x,y,z)) instanceof BeaconBlockEntity beacon) {
            CompoundTag tag = beacon.saveWithoutMetadata(level.registryAccess());
            tag.putString("primary_effect","minecraft:"+primary);
            tag.putString("secondary_effect","minecraft:"+secondary);
            beacon.loadWithComponents(tag,level.registryAccess()); beacon.setChanged();
        }
    }
    @Override public void clearNonPlayerEntities(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        AABB box = new AABB(Math.max(minX,chunk.getMinBlockX()),minY,Math.max(minZ,chunk.getMinBlockZ()),
                Math.min(maxX,chunk.getMinBlockX()+16),maxY,Math.min(maxZ,chunk.getMinBlockZ()+16));
        onServer(() -> level.getLevel().getEntitiesOfClass(LivingEntity.class,box,entity -> !(entity instanceof Player)).forEach(LivingEntity::discard));
    }
}
