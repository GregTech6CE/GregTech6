package gregtech.content.material;

import gregtech.api.mod.ModData;
import gregtech.engine.material.GTMaterialRegistry;

/** One-time material registry setup shared by test classes in the same JVM. */
public final class MaterialTestFixture {
    private static boolean initialized;

    private MaterialTestFixture() {}

    public static synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            ModData.bindPresence(id -> false);
        } catch (IllegalStateException alreadyBound) {
            // Another test class in this JVM bound the loader query first.
        }
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        initialized = true;
    }
}
