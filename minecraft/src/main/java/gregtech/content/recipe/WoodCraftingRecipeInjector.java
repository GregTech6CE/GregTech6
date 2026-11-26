package gregtech.content.recipe;

import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.recipe.WoodCraftingRow.Item;
import gregtech.content.recipe.WoodCraftingRow.ItemSpec;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import gregtech.content.recipe.WoodCraftingRow.Tool;
import gregtech.content.recipe.WoodCraftingRow.WoodBlock;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTToolItems;
import gregtech.content.registry.GTWoods;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Resolves original wood recipes and holds shared diagnostics for runtime injection. */
public final class WoodCraftingRecipeInjector {
    private static final String NATURAL_PREFIX = GregTechIdentity.REGISTRY_NAMESPACE + ":wood/natural/";
    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<String> TREATED_REGISTERED = new ArrayList<>();

    private WoodCraftingRecipeInjector() {}

    public static ItemStack resolve(ItemSpec spec) {
        if (spec instanceof Item item)
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.id())));
        if (spec instanceof MaterialForm form) {
            var material = GTMaterialRegistry.get(form.material());
            return material == null || !material.isValid()
                    ? ItemStack.EMPTY
                    : GTItems.getStack(form.prefix(), material, 1);
        }
        if (spec instanceof Tool tool) {
            var item = GTToolItems.get(
                    switch (tool) {
                        case SAW -> GTToolType.SAW;
                        case FILE -> GTToolType.FILE;
                    });
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        var wood = (WoodBlock) spec;
        for (WoodSpecies species : WoodSpecies.values()) {
            if (!species.id().equals(wood.species())) continue;
            return switch (wood.form()) {
                case LOG -> new ItemStack(GTWoods.log(species));
                case PLANKS -> new ItemStack(GTWoods.planks(species));
                case STRIPPED -> new ItemStack(GTWoods.strippedLog(species));
            };
        }
        return ItemStack.EMPTY;
    }

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() {
        return REGISTERED.stream().filter(id -> !id.startsWith(NATURAL_PREFIX)).toList();
    }

    public static List<String> treatedRegisteredIds() {
        return List.copyOf(TREATED_REGISTERED);
    }

    public static List<String> skipped() {
        return List.copyOf(SKIPPED);
    }

    public static List<String> speciesIds() {
        return GTWoodRecipes.SPECIES.stream().map(GTWoodRecipes.Species::id).toList();
    }

    static void clearDiagnostics() {
        REGISTERED.clear();
        SKIPPED.clear();
        TREATED_REGISTERED.clear();
    }

    static void recordRegistered(String id, boolean treated) {
        (treated ? TREATED_REGISTERED : REGISTERED).add(id);
    }

    static void recordSkipped(String message) {
        SKIPPED.add(message);
    }

    static int totalRegisteredCount() {
        return REGISTERED.size() + TREATED_REGISTERED.size();
    }
}
