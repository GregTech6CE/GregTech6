package gregtech.network;

import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.content.worldgen.OreSourceCatalog.Kind;
import gregtech.engine.mod.GregTechIdentity;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.1 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
*///?} else {
import java.util.function.Supplier;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
//?}

/** The ore atlas follows the same login and reload sequence as the recipes and tags. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
public record PacketSyncOreGeneration(OreGenerationSummary summary)
//? if >=1.21.1 {
/*        implements CustomPacketPayload
*///?}
{
    //? if >=1.21.1 {
    /*public static final Type<PacketSyncOreGeneration> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, "ore_generation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncOreGeneration> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PacketSyncOreGeneration::decode);

    @Override
    public Type<PacketSyncOreGeneration> type() {
        return TYPE;
    }
    *///?} else {
    public static void handle(PacketSyncOreGeneration packet, Supplier<NetworkEvent.Context> context) {
        var network = context.get();
        network.enqueueWork(packet::apply);
        network.setPacketHandled(true);
    }
    //?}

    public void apply() {
        OreGenerationSummary.acceptClientSummary(summary);
    }

    public static void encode(PacketSyncOreGeneration packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.summary().boundaryGemDivisor());
        buffer.writeVarInt(packet.summary().dimensions().size());
        packet.summary().dimensions().forEach((name, dimension) -> {
            buffer.writeResourceLocation(ResourceLocation.parse(name));
            buffer.writeVarInt(dimension.heights().min());
            buffer.writeVarInt(dimension.heights().max());
            buffer.writeBoolean(dimension.veins());
            buffer.writeBoolean(dimension.smallOres());
            buffer.writeBoolean(dimension.strata());
        });
        buffer.writeVarInt(packet.summary().bedrockDensity());
        buffer.writeVarInt(packet.summary().bedrock().size());
        packet.summary().bedrock().forEach((name, table) -> {
            buffer.writeResourceLocation(ResourceLocation.parse(name));
            buffer.writeVarInt(table.floor());
            buffer.writeVarInt(table.divisors().size());
            table.divisors().forEach((ore, divisor) -> {
                buffer.writeUtf(ore);
                buffer.writeVarInt(divisor);
            });
        });
        buffer.writeVarInt(packet.summary().disabledStratumBlocks().size());
        for (String block : packet.summary().disabledStratumBlocks()) {
            buffer.writeResourceLocation(ResourceLocation.parse(block));
        }
        buffer.writeVarInt(packet.summary().specialDeposits().size());
        packet.summary().specialDeposits().forEach((kind, heights) -> {
            buffer.writeEnum(kind);
            buffer.writeVarInt(heights.min());
            buffer.writeVarInt(heights.max());
        });
    }

    public static PacketSyncOreGeneration decode(FriendlyByteBuf buffer) {
        int divisor = buffer.readVarInt();
        if (divisor < 1) throw new IllegalArgumentException("ore generation summary");
        int count = readCount(buffer);
        var dimensions = new LinkedHashMap<String, OreGenerationSummary.Dimension>();
        for (int i = 0; i < count; i++) {
            String name = buffer.readResourceLocation().toString();
            var heights = new HeightRange(buffer.readVarInt(), buffer.readVarInt());
            dimensions.put(name, new OreGenerationSummary.Dimension(
                    heights, buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));
        }
        int density = buffer.readVarInt();
        if (density < 0) throw new IllegalArgumentException("bedrock density");
        var bedrock = new LinkedHashMap<String, OreGenerationSummary.BedrockTable>();
        int tables = readCount(buffer);
        for (int i = 0; i < tables; i++) {
            String name = buffer.readResourceLocation().toString();
            int floor = buffer.readVarInt();
            int entries = readCount(buffer);
            var divisors = new LinkedHashMap<String, Integer>();
            for (int j = 0; j < entries; j++) {
                String ore = buffer.readUtf();
                int chance = buffer.readVarInt();
                if (chance < 1) throw new IllegalArgumentException("bedrock ore divisor");
                divisors.put(ore, chance);
            }
            bedrock.put(name, new OreGenerationSummary.BedrockTable(floor, divisors));
        }
        int disabledCount = readCount(buffer);
        var disabledBlocks = new LinkedHashSet<String>();
        for (int i = 0; i < disabledCount; i++) {
            disabledBlocks.add(buffer.readResourceLocation().toString());
        }
        int siteCount = readCount(buffer);
        var special = new EnumMap<Kind, HeightRange>(Kind.class);
        for (int i = 0; i < siteCount; i++) {
            Kind kind = buffer.readEnum(Kind.class);
            var heights = new HeightRange(buffer.readVarInt(), buffer.readVarInt());
            if (!kind.specialDeposit() || heights.isEmpty() || special.putIfAbsent(kind, heights) != null)
                throw new IllegalArgumentException("invalid special ore deposit");
        }
        return new PacketSyncOreGeneration(
                new OreGenerationSummary(dimensions, divisor, bedrock, density, disabledBlocks, special));
    }

    private static int readCount(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("ore generation summary size");
        return count;
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        var players = event.getPlayerList();
        var player = event.getPlayer();
        if (player == null && players.getPlayers().isEmpty()) return;
        var packet = new PacketSyncOreGeneration(OreGenerationSummary.capture(players.getServer()));
        if (player != null) packet.send(player);
        else players.getPlayers().forEach(packet::send);
    }

    private void send(ServerPlayer player) {
        //? if >=1.21.1 {
        /*if (NetworkRegistry.hasChannel(player.connection, TYPE.id())) PacketDistributor.sendToPlayer(player, this);
        *///?} else {
        if (GTPackets.ORE_GENERATION.isRemotePresent(player.connection.connection)) {
            GTPackets.ORE_GENERATION.send(PacketDistributor.PLAYER.with(() -> player), this);
        }
        //?}
    }
}
