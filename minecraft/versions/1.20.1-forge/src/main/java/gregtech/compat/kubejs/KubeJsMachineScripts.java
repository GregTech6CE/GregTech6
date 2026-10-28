package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import gregtech.engine.kubejs.KubeJsLoadOrder;
import gregtech.engine.kubejs.KubeJsMachineDeclarations;

/** Posts the startup machines event once, after KubeJS has loaded its startup scripts. */
final class KubeJsMachineScripts {
    private KubeJsMachineScripts() {}

    static KubeJsMachineDeclarations collect() {
        KubeJsLoadOrder.requireScriptsLoaded(KubeJS.getStartupScriptManager() != null);
        var declarations = new KubeJsMachineDeclarations();
        GT6CEStartupEvents.MACHINES.post(ScriptType.STARTUP, new GT6CEMachinesEventJS(declarations));
        declarations.seal();
        return declarations;
    }
}
