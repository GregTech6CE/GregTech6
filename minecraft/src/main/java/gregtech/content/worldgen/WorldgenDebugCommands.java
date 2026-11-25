package gregtech.content.worldgen;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gregtech.content.block.RockBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.Mc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
*///?} else {
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Operator commands for sampling, profiling and locating geological features. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
//?}
public final class WorldgenDebugCommands {
    private static final int MAX_SAMPLE_RADIUS = 8;
    private static final int MAX_EXPORT_SIDE = 64;
    private static final int MAX_PROFILE_WIDTH = 512;

    private WorldgenDebugCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("gt6ce")
                .requires(source -> source.hasPermission(2))
                .then(literal("worldgen")
                        .then(literal("inspect").executes(context -> inspect(context.getSource())))
                        .then(literal("profile")
                                .executes(context -> profile(context.getSource(), Axis.X, 1))
                                .then(profileAxis(Axis.X))
                                .then(profileAxis(Axis.Z)))
                        .then(literal("sample")
                                .then(argument("radius", IntegerArgumentType.integer(0, MAX_SAMPLE_RADIUS))
                                        .executes(context -> sample(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius")))))
                        .then(literal("export")
                                .then(argument("side", IntegerArgumentType.integer(1, MAX_EXPORT_SIDE))
                                        .executes(context -> WorldgenExport.start(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "side")))))
                        .then(WorldgenLocate.command())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> profileAxis(Axis axis) {
        return literal(axis.getSerializedName())
                .then(argument("width", IntegerArgumentType.integer(1, MAX_PROFILE_WIDTH))
                        .executes(context -> profile(context.getSource(), axis,
                                IntegerArgumentType.getInteger(context, "width"))));
    }

    private static boolean hasLayerPlan(ServerLevel level) {
        var active = OreGenerationSummary.ordinarySources(level, level.getChunkSource().getGenerator());
        return active != null && active.strata();
    }

    private static int inspect(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        ServerLevel level = source.getLevel();
        ChunkPos cp = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
        if (chunk == null) {
            source.sendFailure(Component.literal("Chunk is not loaded."));
            return 0;
        }
        var sample = new WorldgenOreSample();
        sample.add(chunk);
        String block = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString();
        String message = "Chunk %s | Y=%d | %s | Ores %d | Bedrock ores %d"
                .formatted(cp, pos.getY(), block, sample.ores(), sample.bedrockOres());
        source.sendSuccess(() -> Component.literal(message), false);

        if (hasLayerPlan(level)) {
            int y = pos.getY();
            int x = pos.getX();
            int z = pos.getZ();
            var field = new StoneLayerField(level.getSeed());
            var sampleArea = field.sample(x, z, 1, 1, y, y);
            var contact = new StoneLayerField.Contacts();
            sampleArea.contacts(x, y, z, contact);
            var column = field.column(x, z);
            var layers = StoneLayerField.layers();
            String current = layers.get(sampleArea.layerIndex(x, y, z)).material();
            String profile = "Province %s | Planned layer %s"
                    .formatted(column.province(y).name().toLowerCase(Locale.ROOT), current);
            var contacts = new ArrayList<String>();
            for (var face : StoneLayerField.Face.values()) {
                int distance = contact.distance(face);
                if (distance == 0 || distance > StoneLayerField.CONTACT_DEPTH) continue;
                String other = layers.get(contact.opposingLayer(face)).material();
                contacts.add("%s %s (%d)".formatted(face.name().toLowerCase(Locale.ROOT), other, distance));
            }
            if (!contacts.isEmpty()) profile += " | Contacts " + String.join(", ", contacts);
            var profileLine = Component.literal(profile);
            source.sendSuccess(() -> profileLine, false);
        }

        var active = OreGenerationSummary.ordinarySources(level, level.getChunkSource().getGenerator());
        if (active != null && active.veins()) {
            String dimension = level.dimension().location().toString();
            GTOreVeinFeature.visitPlans(level, level.getChunkSource().getGenerator(), cp, body -> {
                var center = body.center();
                var vein = center.vein();
                boolean coastal = body.placementMaxY() < vein.heights(dimension).min();
                String planned = "Planned vein %s | Center %d %d %d | Y=%d..%d%s"
                        .formatted(vein.name(), center.x(), body.centerY(), center.z(), body.placementMinY(),
                                body.placementMaxY(), coastal ? " | Coastal fallback" : "");
                source.sendSuccess(() -> Component.literal(planned), false);
                return true;
            });
        }

        var pebbles = new TreeMap<String, Component>();
        for (BlockPos rockPos : chunk.getBlockEntitiesPos()) {
            if (!(chunk.getBlockEntity(rockPos) instanceof RockBlockEntity rock)) continue;
            var state = rock.getBlockState();
            String material = rock.getMaterial();
            if (rock.hasRawOre()) {
                pebbles.put("raw/" + material, Component.translatable("commands.gt6ce.worldgen.inspect.raw", material));
            } else if (state.getValue(RockBlock.APPEARANCE) == RockBlock.Appearance.INDICATOR) {
                pebbles.put("indicator/" + material, Component.translatable("commands.gt6ce.worldgen.inspect.indicator", material));
            } else if (!rock.itemStack().isEmpty()) {
                pebbles.put(rock.itemId(), rock.itemStack().getHoverName());
            } else {
                pebbles.put(material, Component.literal(material));
            }
        }
        if (!pebbles.isEmpty()) source.sendSuccess(() -> Component.translatable(
                "commands.gt6ce.worldgen.inspect.pebbles", ComponentUtils.formatList(pebbles.values(), Component.literal(", "))), false);
        return 1;
    }

    private static int profile(CommandSourceStack source, Axis axis, int width) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        var section = new WorldgenProfile.Section(
                center.getX() - (axis == Axis.X ? width / 2 : 0),
                center.getZ() - (axis == Axis.Z ? width / 2 : 0),
                axis, width, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        var columns = new LevelChunk[width];
        LevelChunk chunk = null;
        for (int i = 0; i < width; i++) {
            int x = section.xAt(i) >> 4;
            int z = section.zAt(i) >> 4;
            if (chunk == null || chunk.getPos().x != x || chunk.getPos().z != z) {
                chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    source.sendFailure(Component.literal("Load the whole profile first. Missing chunk " + new ChunkPos(x, z)));
                    return 0;
                }
            }
            columns[i] = chunk;
        }
        try {
            var report = WorldgenProfile.write(level, section, columns, hasLayerPlan(level));
            source.sendSuccess(() -> Component.literal("Profiled %d columns | %d GT ore blocks | %d ground marks | %s"
                    .formatted(width, report.ores(), report.markers(), report.path())), false);
            return width;
        } catch (IOException error) {
            Mc.logger().warn("Could not write worldgen profile in {} at {}", level.dimension().location(), center, error);
            source.sendFailure(Component.literal("Could not write the worldgen profile. See the server log."));
            return 0;
        }
    }

    private static int sample(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        var center = new ChunkPos(BlockPos.containing(source.getPosition()));
        var sample = new WorldgenOreSample();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) sample.add(chunk);
            }
        }
        if (sample.chunks() == 0) {
            source.sendFailure(Component.literal("No chunks are loaded in the sampling area."));
            return 0;
        }
        try {
            var report = sample.write(level, center, radius);
            int diameter = radius * 2 + 1;
            source.sendSuccess(() -> Component.literal("Sampled %d/%d chunks | %d ores | %d bedrock ores | %s"
                    .formatted(sample.chunks(), diameter * diameter, sample.ores(), sample.bedrockOres(), report)), false);
            return sample.chunks();
        } catch (IOException error) {
            Mc.logger().warn("Could not export worldgen sample in {} at {}", level.dimension().location(), center, error);
            source.sendFailure(Component.literal("Could not write the worldgen sample. See the server log."));
            return 0;
        }
    }

}
