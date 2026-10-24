package gregtech.registry;
import gregtech.block.misc.*;
import gregtech.content.logistics.LongDistanceCatalog;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.*;

/** Single factory and registration list for both loaders' long-distance networks. */
public final class GTLongDistance {
    private GTLongDistance() {}
    private static final Map<String, RegisteredBlock<Block>> ALL = new LinkedHashMap<>();
    public static Block block(String id) { return Objects.requireNonNull(ALL.get(id), id).get(); }
    public static List<RegisteredBlock<Block>> all() { return List.copyOf(ALL.values()); }
    public static Block[] transformers() {
        return ALL.values().stream().map(RegisteredBlock::get).filter(block -> block instanceof LongDistanceTransformerBlock).toArray(Block[]::new);
    }
    public static void initialize() {
        if (!ALL.isEmpty()) throw new IllegalStateException("Long-distance networks registered twice");
        for (var spec : LongDistanceCatalog.ALL) {
            var block = RegisteredBlock.<Block>register(spec.id(), () -> {
                var properties = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(), spec.resistance())
                        .requiresCorrectToolForDrops().sound(spec.kind().equals("WIRE") ? SoundType.WOOL : SoundType.METAL);
                return switch (spec.kind()) {
                    case "ITEM_ENDPOINT" -> new LongDistEndpointBlock(false, properties);
                    case "FLUID_ENDPOINT" -> new LongDistEndpointBlock(true, properties);
                    case "TRANSFORMER" -> new LongDistanceTransformerBlock(spec.voltage(), properties);
                    default -> new LongDistPipeBlock(spec, properties);
                };
            }, value -> new gregtech.item.LongDistanceBlockItem(value,
                    new Item.Properties().stacksTo(spec.stackLimit()), spec));
            ALL.put(spec.id(), block);
        }
    }
}
