package gregtech.engine.multiblock;

import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.engine.multiblock.tracking.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Required entity type and structure factory for one controller family. */
public record ControllerBinding<E extends BlockEntity & TrackedMultiblock>(
        String structureName,
        ResourceLocation entityType,
        Class<E> controllerClass,
        Function<E, StructureDefinition.Declarative> structure,
        Supplier<LargeMachineLayouts.Layout> layout) {
    public ControllerBinding {
        if (structureName == null || structureName.isBlank())
            throw new IllegalArgumentException("Missing structure name");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(controllerClass, "controllerClass");
        Objects.requireNonNull(structure, "structure");
    }

    public StructureDefinition.Declarative bind(BlockEntity entity) {
        if (!controllerClass.isInstance(entity))
            throw new IllegalStateException("Structure '" + structureName + "' requires " + controllerClass.getName()
                    + ", got " + entity.getClass().getName());
        return Objects.requireNonNull(
                structure.apply(controllerClass.cast(entity)), "Missing structure " + structureName);
    }
}
