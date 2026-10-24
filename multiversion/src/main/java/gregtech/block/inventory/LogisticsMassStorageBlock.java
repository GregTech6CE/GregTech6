package gregtech.block.inventory;

import gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity;
import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** GT6's black logistics casing with ordinary Mass Storage front controls and storage semantics. */
public final class LogisticsMassStorageBlock extends MassStorageBlock {
    private final GTMaterial constructionMaterial;

    public LogisticsMassStorageBlock(GTMaterial constructionMaterial, Properties properties) {
        super(Materials.Black, properties);
        this.constructionMaterial = constructionMaterial;
    }

    /** GT6 uses black visual casing but keeps the recipe metal's wrench tier. */
    public GTMaterial constructionMaterial() {
        return constructionMaterial;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsMassStorageBlockEntity(pos, state);
    }
}
