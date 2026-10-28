package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.engine.prefix.BlockMaterialPrefix;

public interface MaterialBlockLike {
    BlockMaterialPrefix prefix();

    GTMaterial material();
}
