package gregtech.integration.kubejs;

import gregtech.api.addon.AddonMachine;
import gregtech.api.addon.AddonMachineContributor;
import gregtech.api.addon.LayoutPart;
import gregtech.api.addon.RecipeTable;
import gregtech.api.addon.StructureLayout;
import gregtech.internal.kubejs.KubeJsMachineDeclarations;

import java.util.List;

/**
 * Service loader entry. This class does not mention KubeJS types, so it can be constructed
 * when KubeJS is absent. The script class is touched only after the mod is confirmed loaded.
 */
public final class KubeJsMachineContributor implements AddonMachineContributor {
    private static KubeJsMachineDeclarations declarations;

    @Override
    public String addonId() { return KubeJsMachineDeclarations.ADDON_ID; }

    @Override
    public List<RecipeTable> recipeTables() { return loaded().tables(); }

    @Override
    public List<AddonMachine> addonMachines() { return loaded().machines(); }

    @Override
    public List<StructureLayout> structureLayouts() { return loaded().layouts(); }

    @Override
    public List<LayoutPart> layoutParts() { return loaded().parts(); }

    private static KubeJsMachineDeclarations loaded() {
        if (declarations == null) {
            declarations = net.minecraftforge.fml.ModList.get().isLoaded("kubejs")
                    ? KubeJsMachineScripts.collect()
                    : KubeJsMachineDeclarations.empty();
        }
        return declarations;
    }
}
