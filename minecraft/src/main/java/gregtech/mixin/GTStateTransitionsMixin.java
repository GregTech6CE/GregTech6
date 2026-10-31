package gregtech.mixin;

import com.google.common.collect.Table;
import gregtech.content.block.GTStateTransitions;
import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep native state objects, serialization and transitions; share only GT's lookup storage. */
@Mixin(StateHolder.class)
public abstract class GTStateTransitionsMixin {
    @Shadow
    private Table<Property<?>, Comparable<?>, Object> neighbours;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "populateNeighbours", at = @At("HEAD"), cancellable = true, require = 1)
    private void gt6ce$shareTransitions(Map<Map<Property<?>, Comparable<?>>, ?> states, CallbackInfo callback) {
        if (neighbours != null || !((Object) this instanceof BlockState state)) return;
        var shared = GTStateTransitions.create(state, states);
        if (shared != null) {
            neighbours = (Table) shared;
            callback.cancel();
        }
    }
}
