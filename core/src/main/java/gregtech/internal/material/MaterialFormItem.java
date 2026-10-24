package gregtech.internal.material;

import gregtech.content.data.MaterialPrefix;

/** Registry-backed material identity shared by ordinary items and native ArrowItem subclasses. */
public interface MaterialFormItem {
    MaterialPrefix getPrefix();
    GTMaterial getMaterial();

    default int getTintColor() {
        return 0xFF000000 | (getMaterial().getColor() & 0xFFFFFF);
    }
}
