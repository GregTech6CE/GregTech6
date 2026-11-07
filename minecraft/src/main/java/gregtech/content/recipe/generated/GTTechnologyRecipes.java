package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.tool.CraftingToolDefinitions;
import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Original embedded RM rows in {@code MultiItemRandomTools:345-361,369,378,389,503-512}. */
public final class GTTechnologyRecipes {
    private static final String SOURCE = "gt6@3703e403/MultiItemRandomTools:";

    private GTTechnologyRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        toolBoxes(rows);
        lighters(rows);
        matches(rows);
        publish("technology item recipes", rows);
    }

    private static void toolBoxes(List<ChemRecipe> rows) {
        for (CraftingToolDefinitions.Tool tool : CraftingToolDefinitions.ALL)
            row(SOURCE + "345-361", "Boxinator", 16, 16L * tool.count())
                    .item(form(tool.prefix(), tool.material(), 1))
                    .kept(tech(tool.tip(), 1))
                    .out(tech(tool.token(), tool.count())).add(rows);
        row(SOURCE + "369", "Boxinator", 64, 64)
                .item(tech("match", 64)).item(form("plateDouble", "Paper", 1))
                .out(tech("match_box_full", 1)).add(rows);
        row(SOURCE + "378", "Unboxinator", 32, 32)
                .item(tech("match_box_full", 1))
                .out(tech("match", 64)).out(form("scrapGt", "Paper", 16)).add(rows);
    }

    private static void lighters(List<ChemRecipe> rows) {
        row(SOURCE + "389", "Canner", 16, 16)
                .item(tech("lighter_empty", 1)).fluid(nativeFluid("Butane", 100))
                .out(tech("lighter_full", 1)).add(rows);
        row(SOURCE + "389", "Canner", 64, 64)
                .item(tech("shiny_lighter_empty", 1)).fluid(nativeFluid("Butane", 1000))
                .out(tech("shiny_lighter_full", 1)).add(rows);
        row(SOURCE + "389", "Canner", 16, 16)
                .item(tech("plastic_lighter_empty", 1)).fluid(nativeFluid("Butane", 100))
                .out(tech("plastic_lighter_full", 1)).add(rows);
    }

    /**
     * Every ordinary wood against every phosphorus dust form. The source's wood group and its
     * phosphorus sentinel are both expanded, so no group is reduced to one representative.
     */
    private static void matches(List<ChemRecipe> rows) {
        Set<GTMaterial> phosphorus = new LinkedHashSet<>(MaterialGroups.Phosphorus.getReRegistrations());
        phosphorus.add(gregtech.content.data.generated.GT6Materials.Elements.P);
        for (GTMaterial wood : MaterialGroups.Wood.getReRegistrations()) {
            if (!MaterialPrefix.bolt.isValidFor(wood.resolve())) continue;
            for (GTMaterial material : phosphorus) {
                if (material == null || !MaterialPrefix.dust.isValidFor(material.resolve())) continue;
                String name = material.resolve().getName(), stick = wood.resolve().getName();
                row(SOURCE + "503-512", "Assembler", 16, 16)
                        .item(form("bolt", stick, 1)).item(form("dustSmall", name, 1))
                        .out(tech("match", 1)).add(rows);
                row(SOURCE + "503-512", "Assembler", 64, 64)
                        .item(form("bolt", stick, 4)).item(form("dust", name, 1))
                        .out(tech("match", 4)).add(rows);
            }
        }
    }
}
