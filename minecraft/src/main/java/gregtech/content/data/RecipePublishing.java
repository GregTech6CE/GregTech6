package gregtech.content.data;

import gregtech.compat.common.CompatCraftingPack;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * The single owner of dynamically published recipe resources.
 *
 * <p>Producers keep their own generation logic but no longer register a pack themselves: they
 * contribute rows to one of two layers, and this class publishes one pack per layer. A duplicate
 * id inside a layer fails with both owners named instead of being resolved by registration order.
 * The override layer sits above vanilla data, the generated layer below it.</p>
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class RecipePublishing {
    @FunctionalInterface
    public interface Contributor {
        void contribute(Map<ResourceLocation, byte[]> data);
    }

    private record Entry(String owner, Contributor contributor) {}

    private static final List<Entry> OVERRIDES = new ArrayList<>();
    private static final List<Entry> GENERATED = new ArrayList<>();

    static {
        // Shared producers. Platform nodes add their own before the pack event fires.
        contributeOverrides("original vanilla replacements", VanillaCraftingRecipePack::contribute);
        contributeOverrides("optional mod compatibility", CompatCraftingPack::contribute);
        contributeGenerated("basic machine manufacturing", BasicMachineRecipePack::contribute);
        contributeGenerated("multiblock manufacturing", MultiblockRecipePack::contribute);
        contributeGenerated("sensor manufacturing", SensorRecipePack::contribute);
    }

    private RecipePublishing() {}

    /** Adds a producer whose rows may replace vanilla data. */
    public static void contributeOverrides(String owner, Contributor contributor) {
        OVERRIDES.add(new Entry(owner, contributor));
    }

    /** Adds a producer whose rows sit below vanilla data. */
    public static void contributeGenerated(String owner, Contributor contributor) {
        GENERATED.add(new Entry(owner, contributor));
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        add(event, "gt6ce:recipe_overrides", "GregTech original vanilla replacements", OVERRIDES, true);
        add(event, "gt6ce:generated_recipes", "GregTech generated recipes", GENERATED, false);
    }

    private static void add(
            AddPackFindersEvent event, String id, String description, List<Entry> entries, boolean top) {
        event.addRepositorySource(output -> {
//? if >=1.21.1 {
            /*var location = new net.minecraft.server.packs.PackLocationInfo(
                    id, Component.literal(description), PackSource.BUILT_IN, java.util.Optional.empty());
            Pack pack = Pack.readMetaAndCreate(
                    location,
                    new Pack.ResourcesSupplier() {
                        public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info) {
                            return new NativeRecipePack(info, description, entries);
                        }

                        public PackResources openFull(
                                net.minecraft.server.packs.PackLocationInfo info, Pack.Metadata metadata) {
                            return new NativeRecipePack(info, description, entries);
                        }
                    },
                    PackType.SERVER_DATA,
                    new net.minecraft.server.packs.PackSelectionConfig(
                            true, top ? Pack.Position.TOP : Pack.Position.BOTTOM, true));
*///?} else {
            Pack pack = Pack.readMetaAndCreate(
                    id,
                    Component.literal(description),
                    true,
                    location -> new NativeRecipePack(id, description, entries),
                    PackType.SERVER_DATA,
                    top ? Pack.Position.TOP : Pack.Position.BOTTOM,
                    PackSource.BUILT_IN);
//?}
            if (pack != null) output.accept(pack);
        });
    }

    private static final class NativeRecipePack extends AbstractPackResources {
        private final String description;
        private final List<Entry> entries;
        private Map<ResourceLocation, byte[]> resources;
        private ResourcePrefixIndex index;
        private boolean generating;

        //? if >=1.21.1 {
        /*NativeRecipePack(net.minecraft.server.packs.PackLocationInfo info, String description, List<Entry> entries) {
            super(info);
            this.description = description;
            this.entries = List.copyOf(entries);
        *///?} else {
        NativeRecipePack(String id, String description, List<Entry> entries) {
            super(id, true);
            this.description = description;
            this.entries = List.copyOf(entries);
        //?}
        }

        private synchronized Map<ResourceLocation, byte[]> data() {
            if (resources != null) return resources;
            if (generating) throw new IllegalStateException("Recursive recipe generation: " + description);
            generating = true;
            try {
                var owned = new LinkedHashMap<ResourceLocation, String>();
                var generated = new TreeMap<ResourceLocation, byte[]>();
                for (var entry : entries) {
                    var local = new LinkedHashMap<ResourceLocation, byte[]>();
                    try {
                        entry.contributor().contribute(local);
                    } catch (RuntimeException failure) {
                        com.mojang.logging.LogUtils.getLogger().error(
                                "Failed to generate {} recipes from {}", description, entry.owner(), failure);
                        throw failure;
                    }
                    for (var row : local.entrySet()) {
                        var previous = owned.putIfAbsent(row.getKey(), entry.owner());
                        if (previous != null)
                            throw new IllegalStateException("Duplicate dynamic recipe " + row.getKey() + " from "
                                    + previous + " and " + entry.owner());
                        generated.put(row.getKey(), row.getValue());
                    }
                }
                com.mojang.logging.LogUtils.getLogger()
                        .info("[gregtech] {} datapack: {} rows", description, generated.size());
                var index = new ResourcePrefixIndex();
                for (var id : generated.keySet()) index.add(id);
                this.index = index;
                resources = generated;
                return resources;
            } finally {
                generating = false;
            }
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... path) {
            if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
//? if >=1.21.1 {
            /*return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":\""
*///?} else {
            return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":15,\"description\":\""
//?}
                    + description + "\"}}")
                    .getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
            if (type != PackType.SERVER_DATA || !recipePath(id.getPath())) return null;
            byte[] bytes = data().get(id);
            return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
        }

        @Override
        public void listResources(
                PackType type, String namespace, String path, PackResources.ResourceOutput output) {
            if (type != PackType.SERVER_DATA || !recipePath(path)) return;
            var data = data();
            for (var id : index.under(namespace, path)) {
                byte[] bytes = data.get(id);
                if (bytes != null) output.accept(id, () -> new ByteArrayInputStream(bytes));
            }
        }

        private static boolean recipePath(String path) {
//? if >=1.21.1 {
            /*String root = "recipe";
*///?} else {
            String root = "recipes";
//?}
            return path.isEmpty() || path.equals(root) || path.startsWith(root + "/");
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            if (type != PackType.SERVER_DATA) return Set.of();
            var namespaces = new TreeSet<String>();
            for (var id : data().keySet()) namespaces.add(id.getNamespace());
            return namespaces;
        }

        @Override
        public void close() {
            resources = null;
            index = null;
        }
    }
}
