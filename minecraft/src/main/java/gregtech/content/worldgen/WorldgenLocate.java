package gregtech.content.worldgen;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.platform.Mc;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Locates known blocks or prospects unexplored terrain without requesting chunk generation. */
final class WorldgenLocate {
    private static final int DEFAULT_RADIUS = 256;
    private static final int MAX_RADIUS = 512;
    private static final Set<MinecraftServer> SEARCHING = ConcurrentHashMap.newKeySet();

    enum Kind {
        ORE,
        LAYER,
        BEDROCK_ORE
    }

    record Target(Kind kind, String name, Set<Block> blocks, List<OreSourceCatalog.Source> sources, GTMaterial material)
            implements Predicate<BlockState> {
        @Override
        public boolean test(BlockState state) {
            if (kind == Kind.LAYER) return blocks.contains(state.getBlock());
            if ((kind == Kind.BEDROCK_ORE) != OreBlock.isBedrockOre(state)) return false;
            GTMaterial ore = GTOreBlockResolver.oreMaterialOf(state.getBlock());
            return (ore != null && ore.resolve() == material) || blocks.contains(state.getBlock());
        }
    }

    record Area(BlockPos origin, int radius, int minX, int maxX, int minZ, int maxZ) {
        static Area around(ServerLevel level, BlockPos pos, int radius) {
            var border = level.getWorldBorder();
            var center = new ChunkPos(pos);
            return new Area(
                    pos,
                    radius,
                    Math.max((center.x - radius) << 4, (int) Math.ceil(border.getMinX())),
                    Math.min(((center.x + radius) << 4) + 15, (int) Math.ceil(border.getMaxX()) - 1),
                    Math.max((center.z - radius) << 4, (int) Math.ceil(border.getMinZ())),
                    Math.min(((center.z + radius) << 4) + 15, (int) Math.ceil(border.getMaxZ()) - 1));
        }

        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    record Hit(BlockPos pos, String type) {}

    private WorldgenLocate() {}

    static LiteralArgumentBuilder<CommandSourceStack> command(String literalName) {
        var locate = literal(literalName).requires(source -> source.hasPermission(2));
        for (var kind : Kind.values()) {
            var radius = argument("radius_chunks", IntegerArgumentType.integer(1, MAX_RADIUS))
                    .suggests((context, builder) ->
                            SharedSuggestionProvider.suggest(List.of("32", "64", "128", "256", "512"), builder))
                    .executes(ctx -> search(ctx, kind, IntegerArgumentType.getInteger(ctx, "radius_chunks")));
            locate.then(literal(kind.name().toLowerCase(Locale.ROOT))
                    .then(argument("material", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    names(ctx.getSource().getLevel(), kind), builder))
                            .executes(ctx -> search(ctx, kind, DEFAULT_RADIUS))
                            .then(radius)));
        }
        return locate;
    }

    private static boolean bedrock(OreSourceCatalog.Kind kind) {
        return kind == OreSourceCatalog.Kind.BEDROCK
                || kind == OreSourceCatalog.Kind.COLTAN_BEDROCK
                || kind == OreSourceCatalog.Kind.DUNGEON_BEDROCK;
    }

    private static boolean active(
            OreSourceCatalog.Source source, Kind kind, String dimension, OreGenerationSummary summary) {
        return source.dimension().equals(dimension)
                && (kind == Kind.BEDROCK_ORE) == bedrock(source.kind())
                && summary.includes(source);
    }

    private static Set<String> names(ServerLevel level, Kind kind) {
        var result = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        var summary = OreGenerationSummary.capture(level.getServer());
        String dimension = level.dimension().location().toString();
        if (kind == Kind.LAYER) {
            var active = summary.dimensions().get(dimension);
            if (active == null || !active.strata()) return result;
            for (int i = 0; i < StoneLayerField.layers().size(); i++) {
                if (GTStoneLayerFeature.layerStone(i) != null) result.add(GTStoneLayerFeature.layerMaterial(i));
                else {
                    result.add("Stone");
                    result.add("Deepslate");
                }
            }
        } else {
            for (var material : OreSourceCatalog.current().materialsWithSources())
                if (OreSourceCatalog.current().sources(material).stream()
                        .anyMatch(source -> active(source, kind, dimension, summary))) result.add(material.getName());
        }
        return result;
    }

    static String layerName(int index) {
        return GTStoneLayerFeature.layerStone(index) == null ? "Stone" : GTStoneLayerFeature.layerMaterial(index);
    }

    private static Target target(ServerLevel level, Kind kind, String query, OreGenerationSummary summary) {
        var blocks = new HashSet<Block>();
        String dimension = level.dimension().location().toString();
        if (kind == Kind.LAYER) {
            var active = summary.dimensions().get(dimension);
            if (active == null || !active.strata()) return null;
            String name = null;
            if ("Deepslate".equalsIgnoreCase(query)) {
                blocks.add(Blocks.DEEPSLATE);
                name = "Deepslate";
            }
            for (int i = 0; i < StoneLayerField.layers().size(); i++) {
                if (!layerName(i).equalsIgnoreCase(query)) continue;
                name = layerName(i);
                var state = GTStoneLayerFeature.layerStone(i);
                blocks.add(state == null ? Blocks.STONE : state.getBlock());
            }
            return name == null ? null : new Target(kind, name, blocks, List.of(), null);
        }
        for (var material : OreSourceCatalog.current().materialsWithSources()) {
            if (!material.getName().equalsIgnoreCase(query)) continue;
            var sources = OreSourceCatalog.current().sources(material).stream()
                    .filter(source -> active(source, kind, dimension, summary))
                    .toList();
            if (sources.isEmpty()) return null;
            for (var source : sources) {
                if (source.blockId().isEmpty()) continue;
                var id = source.blockId();
                var block = Mc.block(id.contains(":") ? Mc.parseId(id) : Mc.gregtechId(id));
                if (block != null && block != Blocks.AIR) blocks.add(block);
            }
            return new Target(kind, material.getName(), blocks, sources, material.resolve());
        }
        return null;
    }

    private static int search(CommandContext<CommandSourceStack> ctx, Kind kind, int radius) {
        var source = ctx.getSource();
        var level = source.getLevel();
        var server = level.getServer();
        String query = StringArgumentType.getString(ctx, "material");
        var summary = OreGenerationSummary.capture(server);
        var target = target(level, kind, query, summary);
        if (target == null) {
            source.sendFailure(
                    message("unknown", query, level.dimension().location().toString()));
            return 0;
        }
        if (!SEARCHING.add(server)) {
            source.sendFailure(message("busy"));
            return 0;
        }
        var area = Area.around(level, BlockPos.containing(source.getPosition()), radius);
        var checked = new HashSet<Long>();
        try {
            var local = loaded(level, area, target, checked);
            if (local != null) {
                SEARCHING.remove(server);
                report(source, target, new Hit(local, "block"));
                return 1;
            }
            source.sendSuccess(() -> message("searching", target.name(), radius), false);
            CompletableFuture.supplyAsync(
                            () -> {
                                var pos = WorldgenProspecting.find(level, summary, target, area, checked);
                                return new Hit(pos, pos == null ? "missing" : "planned");
                            },
                            Util.backgroundExecutor())
                    .whenComplete((result, error) -> {
                        SEARCHING.remove(server);
                        if (!server.isRunning()) return;
                        server.execute(() -> {
                            if (error != null) {
                                Mc.logger()
                                        .warn(
                                                "Could not locate {} in {}",
                                                target.name(),
                                                level.dimension().location(),
                                                error);
                                source.sendFailure(message("error"));
                            } else if (result.pos() == null) {
                                source.sendFailure(message(result.type(), target.name(), radius));
                            } else {
                                report(source, target, result);
                            }
                        });
                    });
        } catch (RuntimeException ex) {
            SEARCHING.remove(server);
            throw ex;
        }
        return 1;
    }

    private static BlockPos loaded(ServerLevel level, Area area, Target target, Set<Long> checked) {
        var origin = area.origin();
        var center = new ChunkPos(origin);
        int reach = Math.min(area.radius(), 3);
        var cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var offset : BlockPos.spiralAround(BlockPos.ZERO, reach, Direction.EAST, Direction.SOUTH)) {
            int cx = center.x + offset.getX(), cz = center.z + offset.getZ();
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;
            checked.add(chunk.getPos().toLong());
            int minX = cx << 4, minZ = cz << 4;
            int nearX = Math.max(minX, Math.min(minX + 15, origin.getX()));
            int nearZ = Math.max(minZ, Math.min(minZ + 15, origin.getZ()));
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                int baseY = level.getSectionYFromSectionIndex(i) << 4;
                int nearY = Math.max(baseY, Math.min(baseY + 15, origin.getY()));
                if (origin.distToLowCornerSqr(nearX, nearY, nearZ) >= distance) continue;
                var section = sections[i];
                if (section.hasOnlyAir() || !section.maybeHas(target)) continue;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            cursor.set(minX + x, baseY + y, minZ + z);
                            double next = cursor.distSqr(origin);
                            if (next >= distance
                                    || !area.contains(cursor.getX(), cursor.getZ())
                                    || !target.test(section.getBlockState(x, y, z))) continue;
                            best = cursor.immutable();
                            distance = next;
                            if (distance == 0) return best;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static Component message(String suffix, Object... args) {
        return Component.translatable("commands.gt6ce.worldgen.locate." + suffix, args);
    }

    private static void report(CommandSourceStack source, Target target, Hit hit) {
        var pos = hit.pos();
        String xyz = "%d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ());
        var coordinates = Component.literal("[" + xyz + "]")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + xyz)));
        source.sendSuccess(() -> message(hit.type(), target.name(), coordinates), false);
    }
}
