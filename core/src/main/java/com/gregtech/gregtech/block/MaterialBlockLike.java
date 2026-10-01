package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;

public interface MaterialBlockLike {
    BlockMaterialPrefix prefix();
    GTMaterial material();
}
