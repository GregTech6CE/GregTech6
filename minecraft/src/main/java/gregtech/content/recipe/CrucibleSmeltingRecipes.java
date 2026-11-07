package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.util.OM;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's crucible smelting recipes (original {@code RecipeMapCrucible#getRecipeFor} and
 * {@code #getNEIRecipes}).
 * <p>
 * In GT6 {@code RM.CrucibleSmelting} is a {@code RecipeMapCrucible}: instead of stored recipes it
 * derives, for any item carrying material data,
 * <pre>
 *   input form of material M  →  OM.ingotOrDust(M.mTargetSmelting, converted amount)
 *   special value (temperature) = M.mMeltingPoint
 * </pre>
 * and {@code getNEIRecipes} lists those forms in the recipe viewer. The smelting crucible machine
 * is registered for both crucible maps
 * ({@code RM.CrucibleSmelting.mRecipeMachineList.addAll(RM.CrucibleAlloying.mRecipeMachineList)},
 * {@code Loader_MultiTileEntities:294}), so its tab is what tells a player at which temperature a
 * material melts and what it turns into — using the actual raw-ore crushing payload before smelting, including the iron chain, where
 * {@code Pig Iron → Wrought Iron} needs 2011 K.
 * </p>
 * <p>
 * The entries are display-only: the crucible block entity performs the same conversion locally from
 * {@link GTMaterial#getTargetSmeltingMaterial()} (see {@code SmeltingCrucibleBlockEntity}), exactly
 * like the original tile entity, which never consults the map either. They must not be executable,
 * or a machine would "melt" items without any heat.
 * </p>
 */
public final class CrucibleSmeltingRecipes {
    private CrucibleSmeltingRecipes() {}

    /** Item forms GT6's {@code getNEIRecipes} lists for every meltable material. */
    private static final MaterialPrefix[] FORMS = {
        MaterialPrefix.dust,
        MaterialPrefix.crushed,
        MaterialPrefix.crushedPurified,
        MaterialPrefix.crushedCentrifuged,
        MaterialPrefix.chunk,
        MaterialPrefix.rubble,
        MaterialPrefix.pebbles,
        MaterialPrefix.cluster,
        MaterialPrefix.cleanGravel,
        MaterialPrefix.dirtyGravel,
        MaterialPrefix.crystalline,
        MaterialPrefix.reduced,
    };
    /** Forms GT6 only lists when the input material is not its own smelting target. */
    private static final MaterialPrefix[] SOLID_FORMS = {
        MaterialPrefix.ingot, MaterialPrefix.gem,
    };
    /** Block forms: {@code blockDust} always, the two solid blocks only for transformed materials. */
    private static final String[] BLOCK_FORM_NAMES = {"blockDust"};

    private static final String[] SOLID_BLOCK_FORM_NAMES = {"blockIngot", "blockGem"};

    private static final TreeSet<String> MISSING = new TreeSet<>();

    /** Forms that could not be resolved to an item (recorded, never invented). */
    public static TreeSet<String> missingContent() {
        return new TreeSet<>(MISSING);
    }

    /** Registers the display recipes and returns how many were added. */
    public static int register() {
        MISSING.clear();
        int count = 0;
        for (GTMaterial raw : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || !material.has(MaterialProperty.MELTING)) continue;
            GTMaterial target = material.getTargetSmeltingMaterial();
            if (target == null || !target.isValid()) continue;

            // GT6 lists the processing forms always and the solid forms only for the materials that
            // are actually transformed into their smelting target (Pig Iron → Wrought Iron, ores, …).
            boolean transformed = target != material;
            List<MaterialPrefix> forms = new ArrayList<>(List.of(FORMS));
            if (transformed) forms.addAll(List.of(SOLID_FORMS));
            List<BlockMaterialPrefix> blockForms = blockForms(BLOCK_FORM_NAMES);
            if (transformed) blockForms.addAll(blockForms(SOLID_BLOCK_FORM_NAMES));

            for (MaterialPrefix prefix : forms) {
                ItemStack input = GTItems.getStack(prefix, material, 1);
                if (input.isEmpty()) continue;
                var preview = gregtech.engine.machine.crucible.CrucibleInputRules.smeltingPreview(material, prefix);
                if (preview == null) continue;
                count += display(prefix.getName(), input, preview);
            }
            for (BlockMaterialPrefix prefix : blockForms) {
                ItemStack input = GTBlocks.getStack(prefix, material);
                if (input.isEmpty() && "blockDust".equals(prefix.getName()))
                    input = MaterialUnification.vanillaFormStack(prefix.getName(), material);
                if (input.isEmpty()) continue;
                var preview = gregtech.engine.machine.crucible.CrucibleInputRules.smeltingPreview(
                        material, prefix.getMaterialWeight());
                if (preview == null) continue;
                count += display(prefix.getName(), input, preview);
            }
        }
        return count;
    }

    /** Resolve deferred block prefixes after the prefix registry has bootstrapped. */
    private static List<BlockMaterialPrefix> blockForms(String[] names) {
        List<BlockMaterialPrefix> forms = new ArrayList<>(names.length);
        for (String name : names) {
            var prefix = gregtech.engine.prefix.BlockPrefixRegistry.byName(name);
            if (prefix != null) forms.add(prefix);
        }
        return forms;
    }

    /** One display row; a missing target form is recorded instead of invented. */
    private static int display(
            String form, ItemStack input, gregtech.engine.machine.crucible.CrucibleInputRules.SmeltingPreview preview) {
        ItemStack output = OM.ingotOrDust(preview.material(), preview.amount());
        if (output.isEmpty()) {
            MISSING.add(form + " of " + preview.material().getName());
            return 0;
        }
        return MachineRecipeMaps.CrucibleSmelting.addFakeRecipe(
                                false,
                                new ItemStack[] {input},
                                new ItemStack[] {output},
                                null,
                                null,
                                null,
                                null,
                                0,
                                0,
                                preview.temperatureK())
                        == null
                ? 0
                : 1;
    }
}
