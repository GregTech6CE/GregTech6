package gregtech.api.kubejs;

/**
 * KubeJS 6 and 7 both load startup scripts from the KubeJS mod constructor, and register
 * plugin event groups before that reload. GregTech reads addon contributors later, while
 * blocks are being registered, so a listener added by a startup script is already installed.
 */
public final class KubeJsLoadOrder {
    public static final String NOT_LOADED =
            "KubeJS startup scripts have not loaded. GT6CE reads machine declarations when blocks are registered, which is after the KubeJS constructor. If KubeJS is installed and this still happens, its startup scripts did not run.";

    private KubeJsLoadOrder() {}

    public static void requireScriptsLoaded(boolean scriptsLoaded) {
        if (!scriptsLoaded) throw new IllegalStateException(NOT_LOADED);
    }
}
