/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Presentation of RecipeMapScannerMolecular, RecipeMapPrinter and RecipeMapReplicator rules. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.generated.MaterialDataFacts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import java.util.*;

/** Bounded viewer rows over real registered items; machines still use their current medium's data. */
public final class MaterialDataViewerRecipes {
    private MaterialDataViewerRecipes() {}
    private static boolean registered;
    private static final List<Recipe> SCANS = new ArrayList<>(), PRINTS = new ArrayList<>(), REPLICATIONS = new ArrayList<>();
    public static List<Recipe> scans() { return Collections.unmodifiableList(SCANS); }
    public static List<Recipe> prints() { return Collections.unmodifiableList(PRINTS); }
    public static List<Recipe> replications() { return Collections.unmodifiableList(REPLICATIONS); }

    public static synchronized int register() {
        if (registered) return 0;
        var usb = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:usb3_stick")));
        if (usb.isEmpty()) throw new IllegalStateException("Missing molecular scanner USB medium");
        var forms = new TreeMap<GTMaterial,List<ItemStack>>(Comparator.comparingInt(GTMaterial::getId).thenComparing(GTMaterial::getName));
        for (var entry : ItemMaterialRegistry.entries().entrySet()) {
            var data = entry.getValue();
            if (!MaterialDataFacts.scannable(data.prefix())) continue;
            var material = data.material().resolve();
            if (!material.isValid() || material.getId() <= 0) continue;
            forms.computeIfAbsent(material, ignored -> new ArrayList<>()).add(new ItemStack(entry.getKey()));
        }
        for (var entry : forms.entrySet()) {
            var material = entry.getKey();
            var choices = entry.getValue();
            choices.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            var scan = GTMaterialDataRecipes.scanner(List.of(choices.get(0),usb),List.of());
            if (scan == null) throw new IllegalStateException("Registered scannable form has no scanner recipe: " + material);
            scan.withViewerInputAlternatives(0,choices);
            MachineRecipeMaps.ScannerMolecular.addFakeRecipe(false,scan); SCANS.add(scan);
        }
        var materials = new TreeSet<GTMaterial>(Comparator.comparingInt(GTMaterial::getId).thenComparing(GTMaterial::getName));
        for(var raw : GTMaterialRegistry.allMaterials()) {
            var material = raw.resolve();
            if(material.isValid() && material.getId() > 0) materials.add(material);
        }
        for(var material : materials) {
            var medium = GTMaterialDataRecipes.withMaterialData(usb,material);
            var print = GTMaterialDataRecipes.printer(List.of(new ItemStack(Items.PAPER,64),medium),List.of());
            if (print != null) { MachineRecipeMaps.Printer.addFakeRecipe(false,print); PRINTS.add(print); }
            var replication = GTMaterialDataRecipes.replicator(List.of(medium),List.of());
            if (replication != null) { MachineRecipeMaps.Replicator.addFakeRecipe(false,replication); REPLICATIONS.add(replication); }
        }
        registered = true;
        return SCANS.size()+PRINTS.size()+REPLICATIONS.size();
    }
}
