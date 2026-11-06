package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.SpikeBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTSpikes {
    private GTSpikes() {}

    private static final java.util.Map<String, RegisteredBlock<SpikeBlock>> ALL = new java.util.LinkedHashMap<>();

    public static SpikeBlock get(SpikeBlock.Family family, SpikeBlock.Mode mode, boolean secondary) {
        return ALL.get(id(family, mode, secondary)).get();
    }

    public static String id(SpikeBlock.Family family, SpikeBlock.Mode mode, boolean secondary) {
        String material = switch (family) {
            case METAL -> secondary ? "lead" : "copper";
            case STEEL -> secondary ? "red_steel" : "blue_steel";
            case SHARP -> secondary ? "titanium" : "steel";
            case FANCY -> secondary ? "silver" : "gold";
            case SUPER -> secondary ? "adamantium" : "tungstensteel";
        };
        String form = switch (mode) {
            case WALL -> "spike_";
            case OMNI -> "spike_ball_";
            case FALLING -> "spike_ball_falling_";
        };
        return form + family.name().toLowerCase(java.util.Locale.ROOT) + "_" + material;
    }

    public static java.util.Collection<RegisteredBlock<SpikeBlock>> all() {
        return java.util.List.copyOf(ALL.values());
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var family : SpikeBlock.Family.values())
            for (boolean secondary : new boolean[] {false, true})
                for (var mode : SpikeBlock.Mode.values()) {
                    String id = id(family, mode, secondary);
                    var b = registrar.block(
                            REGISTRY_NAMESPACE,
                            id,
                            () -> new SpikeBlock(
                                    family,
                                    mode,
                                    secondary,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(30, 5)
                                            .requiresCorrectToolForDrops()
                                            .sound(SoundType.METAL)),
                            block -> new gregtech.content.item.SpikeBlockItem(block, new Item.Properties()));
                    ALL.put(id, b);
                }
    }
}
