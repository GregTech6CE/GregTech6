import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import com.gregtech.gregtech.data.MachineRecipeMapDefinitions;
import java.util.Set;

/** Observes maintained core catalogs/costs. Does not load Minecraft recipes or perform processing. */
class DynamicRecipeAudit {
    public static void main(String[] args) {
        var maps = MachineRecipeMapDefinitions.all();
        System.out.println("MAPS|" + maps.size() + "|needs_outputs|"
                + maps.stream().filter(map -> map.mNeedsOutputs).count());
        for (var map : new com.gregtech.gregtech.api.recipe.RecipeMapSpec[] {
                MachineRecipeMapDefinitions.Bath, MachineRecipeMapDefinitions.Unboxinator}) {
            System.out.println("MAP|" + map.mNameLocal + "|" + map.mInputItemsCount + "|"
                    + map.mOutputItemsCount + "|" + map.mInputFluidCount + "|"
                    + map.mOutputFluidCount + "|" + map.mNeedsOutputs);
        }
        var machine = BasicMachineOriginalParams.find("largecentrifuge", 1);
        long sourceWorkBudget = machine.energyInputMax() * 600;
        long singleWork = 512L * 2304;
        long sourceBatch = Math.max(1, Math.min(machine.parallel(), sourceWorkBudget / singleWork));
        System.out.println("BATCH|largecentrifuge|" + machine.parallel() + "|" + sourceWorkBudget
                + "|" + singleWork + "|source-work-limit|" + sourceBatch);
        var cost = MachineWorkCost.calculate(512, 2304, 8, true,
                LargeMachineProcessingRules.efficiency("largecentrifuge"), 512, 4096, true);
        System.out.println("COST|eight-input-example|" + cost.minimumPower() + "|" + cost.totalWork());

        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        var forms = Set.of("crushedCentrifuged", "dust", "dustTiny");
        MaterialItemDefinitions.all().stream()
                .filter(row -> forms.contains(row.prefix().getName())
                        && Set.of("Cobalt", "Cobalt60").contains(row.material().getName()))
                .map(row -> "ITEM_DEFINITION|" + row.prefix().getName() + "|"
                        + row.material().getName() + "|" + row.itemId())
                .sorted().forEach(System.out::println);
    }
}
