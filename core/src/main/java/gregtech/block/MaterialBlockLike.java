package gregtech.block;

import gregtech.api.material.GTMaterial;
import gregtech.api.prefix.BlockMaterialPrefix;

public interface MaterialBlockLike {
    BlockMaterialPrefix prefix();
    GTMaterial material();
}
