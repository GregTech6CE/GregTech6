/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityCryoDistillationTower / original controller17111. */
package com.gregtech.gregtech.blockentity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Original cold-energy tower: full80-part structure, real recipes and species-based rear outlets. */
public class CryoDistillationControllerBlockEntity extends DistillationTowerControllerBlockEntity {
    public CryoDistillationControllerBlockEntity(BlockPos pos,BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.CRYO_DISTILLATION.get(),pos,state);
        setSpec(com.gregtech.gregtech.block.machine.CryoDistillationControllerBlock.makeSpec());
    }
    @Override protected long inputMinimum() {return 1;}
    @Override protected boolean cryogenic() {return true;}
    @Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        // Preserve old fluids verbatim. The former HU buffer is not cold energy or a consumed recipe.
        if(!tag.contains("gt.tanks_input0") && tag.contains("gt.input")) {
            tag=tag.copy();tag.put("gt.tanks_input0",tag.getCompound("gt.input").copy());
            for(int i=0;i<3;i++) if(tag.contains("gt.output"+i))
                tag.put("gt.tanks_output"+i,tag.getCompound("gt.output"+i).copy());
            tag.remove("gt.heat");tag.remove("gt.progress");tag.remove("gt.max_progress");
        }
        super.loadAdditional(tag,lookup);
    }
}
