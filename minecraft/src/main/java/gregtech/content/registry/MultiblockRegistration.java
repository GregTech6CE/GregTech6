package gregtech.content.registry;

import gregtech.engine.multiblock.StructurePreviewSource;
import gregtech.engine.multiblock.tracking.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared controller registration and structure validation; loaders only supply deferred handles. */
public final class MultiblockRegistration {
    private MultiblockRegistration() {}

    private static final Map<ResourceLocation, Supplier<? extends Block>> CANDIDATES = new LinkedHashMap<>();
    private static final Map<Block, TrackedMultiblock> CONTROLLERS = new LinkedHashMap<>();
    private static boolean validated;

    /** Used by built-in, recipe, behavior and fuel controllers on both loaders. No factory runs early. */
    public static <B extends Block, H extends Supplier<B>> H register(
            String namespace, String path, Supplier<B> factory, BiFunction<String, Supplier<B>, H> deferredRegister) {
        var id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (validated || CANDIDATES.containsKey(id))
            throw new IllegalStateException("Duplicate or late controller registration: " + id);
        H handle = deferredRegister.apply(path, factory);
        CANDIDATES.put(id, handle);
        return handle;
    }

    /** Runs after block entity types and structure predicates are ready, on either logical side. */
    public static void validate() {
        if (validated) return;
        var controllers = new LinkedHashMap<Block, TrackedMultiblock>();
        for (var entry : CANDIDATES.entrySet()) {
            Block block = entry.getValue().get();
            if (!(block instanceof StructurePreviewSource source)) continue;
            var state = block.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            if (state.hasProperty(BlockStateProperties.FACING))
                state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
            var entity = source.newBlockEntity(BlockPos.ZERO, state);
            // Recipe/behavior/fuel catalogs also contain single-block machines.
            if (!(entity instanceof TrackedMultiblock tracked)) continue;
            var definition = tracked.structureDefinition();
            if (definition != null && definition.formsImmediately()) continue;
            if (!(definition instanceof StructureDefinition.Declarative declarative))
                throw new IllegalStateException("Missing declarative structure: " + entry.getKey());
            var pattern = declarative.compile(Direction.NORTH);
            if (pattern == null || StructurePlan.minimum(pattern).isEmpty())
                throw new IllegalStateException("Missing multiblock layout: " + entry.getKey());
            controllers.put(block, tracked);
        }
        CONTROLLERS.putAll(controllers);
        validated = true;
    }

    /** World-free controller definitions, shared with previews. Never attach these instances to a level. */
    public static Map<Block, TrackedMultiblock> controllers() {
        validate();
        return Collections.unmodifiableMap(CONTROLLERS);
    }
}
