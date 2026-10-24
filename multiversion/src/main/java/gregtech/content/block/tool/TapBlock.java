package gregtech.content.block.tool;

import gregtech.content.tool.FluidAttachmentSpec;
import gregtech.content.material.Materials;

/** Legacy tap/nozzle wrappers; material variants use FluidAttachmentBlock directly. */
public class TapBlock extends FluidAttachmentBlock {

    public TapBlock(boolean nozzle, Properties properties) {
//? if >=1.21.1 {
        /*super(new FluidAttachmentSpec(nozzle ? "nozzle" : "tap", nozzle ? "cap_nozzle" : "tap", nozzle ? Materials.Steel : Materials.Ceramic,
                false), properties);
*///?} else {
        super(new FluidAttachmentSpec(nozzle ? "nozzle" : "tap", nozzle ? "nozzle" : "tap", nozzle ? Materials.Steel : Materials.Ceramic, false),
                properties);
//?}
    }
}
