package gregtech.compat.kubejs;

import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.addon.machine.AddonMachineContributor;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.multiblock.StructureLayout;
import gregtech.engine.kubejs.KubeJsMachineDeclarations;
import java.util.List;

/**
 * Service loader entry. This class does not mention KubeJS types, so it can be constructed
 * when KubeJS is absent. The script class is touched only after the mod is confirmed loaded.
 */
public final class KubeJsMachineContributor implements AddonMachineContributor {
    private static KubeJsMachineDeclarations declarations;

    @Override
    public String addonId() {
        return KubeJsMachineDeclarations.ADDON_ID;
    }

    @Override
    public List<RecipeTable> recipeTables() {
        return loaded().tables();
    }

    @Override
    public List<AddonMachine> addonMachines() {
        return loaded().machines();
    }

    @Override
    public List<StructureLayout> structureLayouts() {
        return loaded().layouts();
    }

    @Override
    public List<LayoutPart> layoutParts() {
        return loaded().parts();
    }

    private static KubeJsMachineDeclarations loaded() {
        if (declarations == null) {
            declarations = net.neoforged.fml.ModList.get().isLoaded("kubejs")
                    ? KubeJsMachineScripts.collect()
                    : KubeJsMachineDeclarations.empty();
        }
        return declarations;
    }
}
