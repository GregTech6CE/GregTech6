package gregtech.block;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.prefix.BlockMaterialPrefix;

public interface MaterialBlockLike {
    BlockMaterialPrefix prefix();
    GTMaterial material();
}
