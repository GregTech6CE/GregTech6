import com.gregtech.gregtech.api.machine.BurningBoxDefinitions;
import com.gregtech.gregtech.api.machine.BurningBoxFuelRules;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.BoilerCatalog;
import com.gregtech.gregtech.content.recipe.OriginalFuelRecipeRows;

/** Prints maintained core data; does not run a native boiler, burn cycle or world hazard. */
class BoilerCatalogAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : BoilerCatalog.all()) {
            System.out.println("BOILER|" + spec.id() + "|" + spec.material().getName() + "|" + spec.strong()
                    + "|" + spec.steamOutput() + "|" + spec.hardness() + "|" + spec.steamCapacity()
                    + "|" + spec.heatCapacity() + "|" + spec.heatInputRecommended());
        }
        var box = BurningBoxDefinitions.spec("burning_box_gas_dense_invar");
        var fuel = OriginalFuelRecipeRows.ROWS.stream()
                .filter(row -> row.kind().equals("gas") && row.input().equals("Hydrogen"))
                .findFirst().orElseThrow();
        long heat = BurningBoxFuelRules.recipeHeat(fuel.eut(), fuel.duration(), box.efficiency());
        System.out.println("FUEL|" + box.id() + "|rate=" + box.outputRate() + "|efficiency=" + box.efficiency()
                + "|hydrogen_batch_litres=" + fuel.amount() + "|heat_per_batch=" + heat);
    }
}
