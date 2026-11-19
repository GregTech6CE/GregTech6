package gregtech.content.multiblock;

import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.engine.multiblock.ControllerBinding;
import gregtech.engine.multiblock.tracking.TrackedMultiblock;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native entity creation and ticking for a recipe controller, with its optional structure. */
public final class RecipeControllerFamily {
    @FunctionalInterface
    interface EntityFactory<E extends BasicMachineBlockEntity> {
        E create(BlockEntityType<?> type, BlockPos pos, BlockState state);
    }

    private final EntityFactory<?> factory;
    private final BlockEntityTicker<BlockEntity> ticker;

    @Nullable
    private final ControllerBinding<?> binding;

    private <E extends BasicMachineBlockEntity> RecipeControllerFamily(
            Class<E> entityClass,
            EntityFactory<E> factory,
            BlockEntityTicker<E> ticker,
            @Nullable ControllerBinding<?> binding) {
        this.factory = factory;
        this.ticker = (level, pos, state, entity) -> ticker.tick(level, pos, state, entityClass.cast(entity));
        this.binding = binding;
    }

    static <E extends BasicMachineBlockEntity> RecipeControllerFamily single(
            Class<E> entityClass, EntityFactory<E> factory) {
        return new RecipeControllerFamily(entityClass, factory, BasicMachineBlockEntity::serverTick, null);
    }

    static <E extends BasicMachineBlockEntity & TrackedMultiblock> RecipeControllerFamily multiblock(
            ControllerBinding<E> binding, EntityFactory<E> factory) {
        return multiblock(binding, factory, BasicMachineBlockEntity::serverTick);
    }

    static <E extends BasicMachineBlockEntity & TrackedMultiblock> RecipeControllerFamily multiblock(
            ControllerBinding<E> binding, EntityFactory<E> factory, BlockEntityTicker<E> ticker) {
        return new RecipeControllerFamily(binding.controllerClass(), factory, ticker, binding);
    }

    public BasicMachineBlockEntity createEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return factory.create(type, pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> serverTicker() {
        return ticker::tick;
    }

    @Nullable
    public ControllerBinding<?> binding() {
        return binding;
    }
}
