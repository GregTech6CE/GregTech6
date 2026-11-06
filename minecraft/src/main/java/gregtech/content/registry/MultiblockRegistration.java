package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.engine.multiblock.ControllerDeclaration;
import gregtech.engine.multiblock.tracking.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit declarations consumed by registration, matching, previews and tools. */
public final class MultiblockRegistration {
    private MultiblockRegistration() {}

    private record Entry(ControllerDeclaration<?> declaration, Supplier<? extends Block> block) {}

    private static final Map<ResourceLocation, Entry> DECLARATIONS = new LinkedHashMap<>();
    private static final Map<Block, ControllerDeclaration<?>> BY_BLOCK = new IdentityHashMap<>();
    private static final Map<Block, TrackedMultiblock> CONTROLLERS = new LinkedHashMap<>();
    private static boolean validated;

    public static <B extends Block, H extends Supplier<B>> H register(
            ControllerDeclaration<B> declaration, BiFunction<String, Supplier<B>, H> deferredRegister) {
        var id = declaration.id();
        if (validated || DECLARATIONS.containsKey(id))
            throw new IllegalStateException("Duplicate or late controller declaration: " + id);
        H handle = deferredRegister.apply(id.getPath(), declaration.block());
        DECLARATIONS.put(id, new Entry(declaration, handle));
        return handle;
    }

    /** Entity membership is derived from declarations, never a second list of block variants. */
    public static Block[] blocksForEntityType(String namespace, String path) {
        var id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        var blocks = DECLARATIONS.values().stream()
                .filter(entry -> entry.declaration().binding() != null
                        && entry.declaration().binding().entityType().equals(id))
                .map(entry -> entry.block().get())
                .toArray(Block[]::new);
        if (blocks.length == 0) throw new IllegalStateException("No controllers for entity type " + id);
        return blocks;
    }

    /** Saved-entity loading uses the block's factory, but only for the entity type that owns it. */
    public static <E extends BlockEntity & TrackedMultiblock> BlockEntityType.BlockEntitySupplier<E> entityFactory(
            String namespace, String path) {
        var expectedType = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return (position, state) -> createEntity(expectedType, position, state);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity & TrackedMultiblock> E createEntity(
            ResourceLocation expectedType, BlockPos position, BlockState state) {
        var declaration = declaration(state.getBlock());
        var binding = declaration.binding();
        if (binding == null || !expectedType.equals(binding.entityType()))
            throw new IllegalStateException(
                    "Controller entity type mismatch: " + declaration.id() + "; cannot load as " + expectedType);
        if (!(state.getBlock() instanceof EntityBlock block))
            throw new IllegalStateException("Controller is not an entity block: " + state);
        var entity = Objects.requireNonNull(block.newBlockEntity(position, state), "Controller factory returned null");
        if (!binding.controllerClass().isInstance(entity)
                || !expectedType.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()))
                || !entity.getType().isValid(state))
            throw new IllegalStateException("Controller factory differs from its declaration: " + declaration.id());
        return (E) entity;
    }

    private static ControllerDeclaration<?> declaration(BlockEntity entity) {
        return declaration(entity.getBlockState().getBlock());
    }

    private static ControllerDeclaration<?> declaration(Block block) {
        var value = BY_BLOCK.get(block);
        if (value == null) {
            for (var entry : DECLARATIONS.values()) {
                if (entry.block().get() == block) {
                    value = entry.declaration();
                    BY_BLOCK.put(block, value);
                    break;
                }
            }
        }
        if (value == null)
            throw new IllegalStateException(
                    "Undeclared multiblock controller: " + BuiltInRegistries.BLOCK.getKey(block));
        return value;
    }

    public static StructureDefinition definition(TrackedMultiblock machine) {
        var entity = machine.trackedEntity();
        var declaration = declaration(entity);
        if (declaration.binding() != null) return declaration.binding().bind(entity);
        if (entity instanceof BehaviorMachineBlockEntity) return StructureDefinition.Immediate.INSTANCE;
        throw new IllegalStateException("Multiblock declared as a single block: " + declaration.id());
    }

    public static LargeMachineLayouts.Layout layout(TrackedMultiblock machine) {
        var declaration = declaration(machine.trackedEntity());
        var binding = declaration.binding();
        if (binding == null && machine instanceof BehaviorMachineBlockEntity) return null;
        if (binding == null || binding.layout() == null)
            throw new IllegalStateException("Controller has no bound cell layout: " + declaration.id());
        return Objects.requireNonNull(binding.layout().get(), "Missing layout for " + declaration.id());
    }

    /** Checks the actual block registry as well as declarations, so an omitted controller cannot disappear. */
    public static void validate() {
        if (validated) return;
        var controllers = new LinkedHashMap<Block, TrackedMultiblock>();
        var namespaces = new HashSet<String>();
        namespaces.add(REGISTRY_NAMESPACE);
        for (var entry : DECLARATIONS.values()) {
            var declared = entry.declaration();
            namespaces.add(declared.id().getNamespace());
            var block = entry.block().get();
            if (!declared.id().equals(BuiltInRegistries.BLOCK.getKey(block)))
                throw new IllegalStateException("Controller registered under the wrong id: " + declared.id());
            BY_BLOCK.put(block, declared);
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!namespaces.contains(id.getNamespace()) || !(block instanceof EntityBlock source)) continue;
            var declared = BY_BLOCK.get(block);
            var entity = source.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            var binding = declared == null ? null : declared.binding();
            if (!(entity instanceof TrackedMultiblock tracked)) {
                if (binding != null) throw new IllegalStateException("Declared controller is not tracked: " + id);
                continue;
            }
            if (declared == null) throw new IllegalStateException("Missing controller declaration: " + id);
            if (binding == null) {
                if (!(entity instanceof BehaviorMachineBlockEntity))
                    throw new IllegalStateException("Multiblock declared without a structure: " + id);
                continue;
            }
            if (!binding.entityType().equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()))
                    || !entity.getType().isValid(block.defaultBlockState()))
                throw new IllegalStateException(
                        "Controller entity type mismatch: " + id + "; expected " + binding.entityType());
            var definition = binding.bind(entity);
            if (definition.formsImmediately()) throw new IllegalStateException("Multiblock has no structure: " + id);
            var pattern = definition.compile(LayoutStructures.facing(entity));
            if (pattern == null || StructurePlan.minimum(pattern).isEmpty())
                throw new IllegalStateException(
                        "Empty multiblock structure: " + id + " (" + binding.structureName() + ")");
            controllers.put(block, tracked);
        }
        for (var entry : DECLARATIONS.values())
            if (entry.declaration().binding() != null
                    && !controllers.containsKey(entry.block().get()))
                throw new IllegalStateException("Controller declaration has no entity: "
                        + entry.declaration().id());
        CONTROLLERS.putAll(controllers);
        validated = true;
    }

    /** Validates an overlay against every controller sharing the named layout, before installing it. */
    public static void validatePorts(String name, gregtech.content.multiblock.SharedLargeMachineLayouts.Layout layout) {
        var cells = new ArrayList<>(LargeMachineLayouts.fromShared(layout.cells()));
        for (var repeat : layout.repeats()) cells.addAll(LargeMachineLayouts.fromShared(repeat.cells()));
        for (var entry : controllers().entrySet()) {
            if (!declaration(entry.getKey()).binding().structureName().equals(name)) continue;
            for (var cell : cells) {
                gregtech.engine.multiblock.StructurePortHandlers.validate(entry.getValue(), cell);
                for (var choice : cell.alternatives())
                    gregtech.engine.multiblock.StructurePortHandlers.validate(
                            entry.getValue(),
                            new LargeMachineLayouts.Cell(
                                    cell.right(),
                                    cell.up(),
                                    cell.back(),
                                    choice.part(),
                                    cell.role(),
                                    choice.plain(),
                                    cell.design(),
                                    List.of(),
                                    cell.ports()));
            }
        }
    }

    /** Detached definitions shared with previews; these instances never join a world. */
    public static Map<Block, TrackedMultiblock> controllers() {
        validate();
        return Collections.unmodifiableMap(CONTROLLERS);
    }
}
