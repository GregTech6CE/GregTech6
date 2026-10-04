package com.gregtech.gregtech.block.tool;
import com.gregtech.gregtech.content.tool.FluidAttachmentSpec;
import com.gregtech.gregtech.content.material.Materials;
/** Existing IDs remain aliases of the original ceramic tap and steel nozzle. */
public class TapBlock extends FluidAttachmentBlock {
    public TapBlock(boolean nozzle,Properties properties){
        super(new FluidAttachmentSpec(nozzle?"nozzle":"tap",nozzle?"nozzle":"tap",nozzle?Materials.Steel:Materials.Ceramic,false),properties);
    }
}
