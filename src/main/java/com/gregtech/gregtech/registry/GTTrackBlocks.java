package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.TrackBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GT6's {@code Loader_Rails} tracks ({@code Loader_Rails:41-72}): ten materials, each as a plain
 * track, a booster track and a detector track, with GT6's own top speed and explosion resistance.
 *
 * <p>Ids are {@code track_<material>}, {@code track_booster_<material>} and
 * {@code track_detector_<material>} rather than GT6's
 * {@code gt.block.rail[.booster|.detector].<material>}. The {@code rail_straight_*},
 * {@code rail_turned_*}, {@code rail_booster_*} and {@code rail_detector_*} assets
 * are textures for these real tracks, not separate placeable blocks. {@code rail_gt}
 * is the material rail <em>item</em>.</p>
 *
 * <p>The tracks are listed in {@code #minecraft:rails}
 * ({@code data/minecraft/tags/blocks/rails.json}); Forge's {@code getMaxSpeedWithRail} ignores any
 * rail that is not in that tag, so the material speeds would silently do nothing without it.</p>
 */
public final class GTTrackBlocks {

    /** GT6's three rail registrations per material. */
    public enum Family {
        /** {@code gt.block.rail.<material>} — "Aluminium Track". */
        STRAIGHT,
        /** {@code gt.block.rail.booster.<material>} — the powered rail. */
        BOOSTER,
        /** {@code gt.block.rail.detector.<material>} — the detector rail. */
        DETECTOR
    }

    /** One registered track: port id, GT6 parameters and the block. */
    public record Track(String id, Family family, String material, float speed, float resistance,
                        RegistryObject<Block> block) {
        /** GT6's "Aluminium Booster Track". */
        public String displayName() {
            String base = material.substring(0, 1).toUpperCase(Locale.ROOT) + material.substring(1);
            return switch (family) {
                case STRAIGHT -> base + " Track";
                case BOOSTER -> base + " Booster Track";
                case DETECTOR -> base + " Detector Track";
            };
        }
    }

    /**
     * GT6 {@code Loader_Rails:41-72}: material, top speed, explosion resistance and the rail material
     * the booster recipe uses as its "G" ingredient. Order is GT6's registration order.
     */
    private static final String[][] MATERIALS = {
            {"Aluminium", "0.20", "6", "Silver"},
            {"Bronze", "0.30", "8", "Silver"},
            {"Magnalium", "0.60", "12", "Silver"},
            {"Steel", "0.60", "12", "Gold"},
            {"StainlessSteel", "0.80", "10", "Gold"},
            {"Tungsten", "1.00", "20", "Electrum"},
            {"Titanium", "1.20", "16", "Electrum"},
            {"Tungstensteel", "1.40", "20", "Platinum"},
            {"TungstenCarbide", "1.60", "24", "Platinum"},
            {"Adamantium", "4.00", "100", "Osmium"},
    };

    private static final List<Track> TRACKS = new ArrayList<>();
    private static final Map<String, Track> BY_ID = new LinkedHashMap<>();
    private static boolean registered;

    private GTTrackBlocks() {}

    /** Everything GT6 registers, in GT6's order. */
    public static List<Track> all() { return List.copyOf(TRACKS); }

    /** One track by port id, or null. */
    public static Track get(String id) { return BY_ID.get(id); }

    /** GT6's material name for a port slug, e.g. {@code tungstensteel}. */
    public static String materialName(String slug) {
        for (String[] row : MATERIALS) {
            if (row[0].toLowerCase(Locale.ROOT).equals(slug)) return row[0];
        }
        return null;
    }

    /** The booster recipe's "G" ingredient material ({@code Loader_Rails:119-128}), or null. */
    public static String boosterMaterial(String material) {
        for (String[] row : MATERIALS) if (row[0].equals(material)) return row[3];
        return null;
    }

    /** Registers the thirty tracks; idempotent. */
    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;
        for (String[] row : MATERIALS) {
            String material = row[0];
            float speed = Float.parseFloat(row[1]);
            float resistance = Float.parseFloat(row[2]);
            String slug = material.toLowerCase(Locale.ROOT);
            add("track_" + slug, Family.STRAIGHT, material, speed, resistance);
            add("track_booster_" + slug, Family.BOOSTER, material, speed, resistance);
            add("track_detector_" + slug, Family.DETECTOR, material, speed, resistance);
        }
    }

    private static void add(String id, Family family, String material, float speed, float resistance) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .noCollission()
                // GT6 keeps the vanilla rail hardness and uses the material's explosion resistance.
                .strength(0.7F, resistance).sound(SoundType.METAL);
        RegistryObject<Block> block = GTBlocks.BLOCKS.register(id, () -> switch (family) {
            case STRAIGHT -> new TrackBlock.Straight(properties, speed);
            case BOOSTER -> new TrackBlock.Booster(properties, speed);
            case DETECTOR -> new TrackBlock.Detector(properties, speed);
        });
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        Track track = new Track(id, family, material, speed, resistance, block);
        TRACKS.add(track);
        BY_ID.put(id, track);
    }
}
