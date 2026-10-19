import com.gregtech.gregtech.api.machine.BurningBoxDefinitions;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.recipe.OriginalFuelRecipeRows;

/** Prints maintained core definitions only; does not simulate native fuel or ash handling. */
class BurningBoxCatalogAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var entry : BurningBoxDefinitions.all()) {
            var spec = entry.spec();
            System.out.println("BOX|" + entry.fuelType() + "|" + spec.id()
                    + "|" + spec.outputRate() + "|" + spec.efficiency());
        }
        OriginalFuelRecipeRows.ROWS.stream().map(row -> row.kind()).distinct()
                .forEach(kind -> System.out.println("FUEL_KIND|" + kind));
    }
}
