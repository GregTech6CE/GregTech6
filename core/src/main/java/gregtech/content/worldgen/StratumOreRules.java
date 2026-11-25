package gregtech.content.worldgen;

import static gregtech.content.worldgen.StoneLayerField.CONTACT_DEPTH;
import static gregtech.content.worldgen.StoneLayerField.CORE_DEPTH;

import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.StoneLayerField.Contacts;
import gregtech.content.worldgen.StoneLayerField.Face;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordered ore rolls for layers and their contacts. The caller checks block placement. */
public final class StratumOreRules {
    private static final Face[] FACES = Face.values();
    private static final long CONTACT_SALT = 0xA8C4E25713BC690DL;
    private static final long LAYER_SALT = 0xD77CABC48B9063F1L;
    private static final long ORE_STEP = 0x9E3779B97F4A7C15L;

    public record Ore(GTMaterial material, boolean indicators, int chanceDiv, int minY, int maxY, String blockId) {
        private boolean contains(int y) {
            return y >= minY && y <= maxY;
        }
    }

    @FunctionalInterface
    public interface Placer {
        boolean place(Ore ore, boolean small);
    }

    private record Contact(String first, String second) {}

    private record ContactOres(List<Ore> vertical, List<Ore> lateral) {}

    private record ContactTable(ContactOres[] pairs, HeightRange[] heights) {
        boolean occursAt(int rock, int y) {
            HeightRange range = heights[rock];
            return range != null && y >= range.min() && y <= range.max();
        }
    }

    private static final class Catalog {
        private static final List<List<Ore>> LAYERS = StoneLayerField.layers().stream()
                .map(layer -> resolve(layer.ores()))
                .toList();
        private static final ContactTable CONTACTS = resolveContacts();
    }

    private StratumOreRules() {}

    public static List<Ore> layerOres(int rock) {
        return Catalog.LAYERS.get(rock);
    }

    public static Ore place(int rock, int y, Contacts contacts, long positionSeed, Placer placer) {
        if (Catalog.CONTACTS.occursAt(rock, y) && contacts.nearest() <= CONTACT_DEPTH) {
            Ore contact = placeContact(rock, y, contacts, positionSeed ^ CONTACT_SALT, placer);
            if (contact != null) return contact;
        }
        return placeOres(layerOres(rock), y, contacts, 0, positionSeed ^ LAYER_SALT, placer);
    }

    private static Ore placeContact(int rock, int y, Contacts contacts, long seed, Placer placer) {
        int closest = CONTACT_DEPTH + 1;
        List<Ore> selected = List.of();
        int count = Catalog.LAYERS.size();
        for (Face face : FACES) {
            int distance = contacts.distance(face);
            if (distance == 0 || distance >= closest) continue;
            int other = contacts.opposingLayer(face);
            int first = face == Face.ABOVE ? other : rock;
            int second = face == Face.ABOVE ? rock : other;
            ContactOres entry = Catalog.CONTACTS.pairs()[first * count + second];
            if (entry == null) continue;
            var candidates = face.vertical() ? entry.vertical() : entry.lateral();
            if (!occursAt(candidates, y)) continue;
            selected = candidates;
            closest = distance;
            if (closest == 1) break;
        }
        return placeOres(selected, y, contacts, closest, seed, placer);
    }

    private static boolean occursAt(List<Ore> ores, int y) {
        for (Ore ore : ores) if (ore.contains(y)) return true;
        return false;
    }

    private static Ore placeOres(
            List<Ore> ores, int y, Contacts contacts, int contactDistance, long seed, Placer placer) {
        for (int index = 0; index < ores.size(); index++) {
            Ore ore = ores.get(index);
            if (!ore.contains(y) || WorldgenHash.roll(WorldgenHash.mix(seed + index * ORE_STEP), ore.chanceDiv()) != 0)
                continue;
            boolean poor = ore.blockId().isEmpty()
                    && (contactDistance == 0
                            ? contacts.nearest() <= CORE_DEPTH
                            : contactDistance > 1 || y == ore.minY() || y == ore.maxY());
            if (placer.place(ore, poor)) return ore;
        }
        return null;
    }

    private static ContactTable resolveContacts() {
        Map<Contact, List<Ore>> vertical = new HashMap<>();
        Map<Contact, List<Ore>> lateral = new HashMap<>();
        Map<String, HeightRange> heights = new HashMap<>();
        for (var def : GTStoneLayersGen.CONTACTS) {
            var ores = resolve(def.ores());
            if (ores.isEmpty()) continue;
            for (Ore ore : ores) {
                var range = new HeightRange(ore.minY(), ore.maxY());
                heights.merge(def.materialA(), range, StratumOreRules::enclosing);
                heights.merge(def.materialB(), range, StratumOreRules::enclosing);
            }
            var forward = new Contact(def.materialA(), def.materialB());
            vertical.computeIfAbsent(forward, ignored -> new ArrayList<>()).addAll(ores);
            if (!def.bothSides()) continue;
            var reverse = new Contact(def.materialB(), def.materialA());
            vertical.computeIfAbsent(reverse, ignored -> new ArrayList<>()).addAll(ores);
            lateral.computeIfAbsent(forward, ignored -> new ArrayList<>()).addAll(ores);
            lateral.computeIfAbsent(reverse, ignored -> new ArrayList<>()).addAll(ores);
        }
        var layers = StoneLayerField.layers();
        int count = layers.size();
        var table = new ContactOres[count * count];
        for (int first = 0; first < count; first++) {
            for (int second = 0; second < count; second++) {
                var key = new Contact(
                        layers.get(first).material(), layers.get(second).material());
                var upDown = vertical.getOrDefault(key, List.of());
                var side = lateral.getOrDefault(key, List.of());
                if (!upDown.isEmpty() || !side.isEmpty()) table[first * count + second] = new ContactOres(upDown, side);
            }
        }
        return new ContactTable(
                table,
                layers.stream().map(layer -> heights.get(layer.material())).toArray(HeightRange[]::new));
    }

    private static HeightRange enclosing(HeightRange a, HeightRange b) {
        return new HeightRange(Math.min(a.min(), b.min()), Math.max(a.max(), b.max()));
    }

    private static List<Ore> resolve(List<GTStoneLayersGen.OreDef> defs) {
        var ores = new ArrayList<Ore>(defs.size());
        for (var def : defs) {
            var material = GTMaterialRegistry.get(def.material());
            if (!material.isValid()) continue;
            ores.add(new Ore(
                    material,
                    def.indicators(),
                    Math.max(1, def.chanceDiv()),
                    MineralWorldgenRules.stratumOreMinY(def),
                    MineralWorldgenRules.stratumOreMaxY(def),
                    def.blockId()));
        }
        return List.copyOf(ores);
    }
}
