package gregtech.mixin;

import gregtech.internal.multiblock.tracking.StructureLevelData;
import gregtech.internal.multiblock.tracking.StructureWatchIndex;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Keeps structure tracking state on the server world instead of a static level-keyed map. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelStructureDataMixin implements StructureLevelData {
    @Unique
    private StructureWatchIndex.WorldData gt$structureData;

    @Override
    public StructureWatchIndex.WorldData gt$structureData() {
        return gt$structureData;
    }

    @Override
    public void gt$structureData(StructureWatchIndex.WorldData data) {
        gt$structureData = data;
    }
}
