package gregtech.integration.kubejs;

import gregtech.api.kubejs.KubeJsLoadOrder;
import gregtech.api.kubejs.KubeJsMachineDeclarations;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.ScriptType;

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
