package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.DenseOreBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.VanillaOreBlock;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.util.OM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

/** The native block branch of Loader_OreProcessing:311,345-349, not the oreRaw form listener.
 * Source permissions govern only this producer; they grant no authority to delete cooking rows.
 */
final class SourceFurnaceNativeAdapter {
    private SourceFurnaceNativeAdapter() {}

    // Startup baseline keeps these recipe identities across reload. This is passive emission data,
    // not input ownership: replacement/filter decisions must continue using their existing tables.
    private static final Map<Recipe, FurnaceSmeltingRecipes.CookingData> PASSIVE_COOKING = new IdentityHashMap<>();
    static Optional<FurnaceSmeltingRecipes.CookingData> cooking(Recipe row) {
        return Optional.ofNullable(PASSIVE_COOKING.get(row));
    }

    enum Action { SOURCE_ROW, SOURCE_NO_ROW, DEFERRED_NO_ROW, LEGACY_UNSUPPORTED }

    /** A canonical GTItems binding may be replaced by a foreign holder; the binding alone is not ownership. */
    static boolean nativeForm(MaterialPrefix prefix, GTMaterial material, ItemStack input) {
        return material != null && input != null && !input.isEmpty() && input.getCount() == 1
                && input.getItem() instanceof MaterialFormItem form && form.getPrefix() == prefix
                && form.getMaterial().resolve() == material.resolve()
                && BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce");
    }

    /** Whether the early dust-first producer remains responsible for this selected form. */
    static boolean earlyFormAllowed(MaterialPrefix prefix, GTMaterial material, ItemStack input) {
        if (material == null || !material.isValid() || input == null || input.isEmpty()) return false;
        if (nativeForm(prefix, material, input))
            return OreSourcePolicy.furnaceInput(material) && !OreSourcePolicy.usesReviewedFurnaceListener(material);
        var old = ReviewedOreSourceProfile.decision(material, ReviewedOreSourceProfile.Rule.FURNACE);
        return old.orLegacy(material.has(MaterialProperty.MELTING))
                && !(old == ReviewedOreSourceProfile.Decision.YES && FurnaceSmeltingRules.legacyAllows(material));
    }

    static boolean listenerAllows(MaterialPrefix prefix, GTMaterial material, ItemStack input) {
        return nativeForm(prefix, material, input)
                ? FurnaceSmeltingRules.allows(material) : FurnaceSmeltingRules.legacyAllows(material);
    }

    record Outcome(Action action, ItemStack input, ItemStack ingot, ItemStack gem, boolean food) {
        /** Source tries ingots first, then gems if ordinary admission fails. Never remove a collision. */
        boolean admit() {
            if (action != Action.SOURCE_ROW) return false;
            return admitOutput(ingot) || admitOutput(gem);
        }

        private boolean admitOutput(ItemStack output) {
            if (input.isEmpty() || output.isEmpty()) return false;
            // Same ordinary adapter as add_smelting(removeOthers=false), retaining the returned object.
            // Its pre-existing NEVER_FURNACE output conversion is deliberately outside this correction.
            Recipe accepted = MachineRecipeMaps.Furnace.addRecipe1(true, 16, 16, input,
                    MachineRecipeMaps.neverFurnaceOutput(output));
            if (accepted == null) return false;
            PASSIVE_COOKING.put(accepted, new FurnaceSmeltingRecipes.CookingData(0, food));
            return true;
        }
    }

    private static Outcome empty(Action action, ItemStack input) {
        return new Outcome(action, input, ItemStack.EMPTY, ItemStack.EMPTY, false);
    }

    static Outcome block(GTMaterial original, GTMaterial crushing, ItemStack input, Snapshot snapshot) {
        if (input == null || input.isEmpty()) return empty(Action.DEFERRED_NO_ROW, ItemStack.EMPTY);
        if (!snapshot.blockInput(original, input) || snapshot.decision(original) == Decision.UNKNOWN)
            return empty(Action.LEGACY_UNSUPPORTED, input);
        // The original material's permission is not the block permission: Al crushes to Alumina.
        // Like the source, honor the runtime crushing link; this does not assert an immutable link baseline.
        Decision permission = snapshot.decision(crushing);
        if (permission == Decision.UNKNOWN) return empty(Action.LEGACY_UNSUPPORTED, input);
        if (!FurnaceSmeltingRules.reviewedAllows(permission, snapshot.hidden(crushing)))
            return empty(Action.SOURCE_NO_ROW, input);
        GTMaterial target = crushing.getTargetSmeltingMaterial();
        long amount = crushing.getTargetSmeltingAmount();
        if (target == null || !target.isValid() || amount <= 0) return empty(Action.SOURCE_NO_ROW, input);
        ItemStack ingot = snapshot.ingot(target, amount), gem = snapshot.gem(target, amount);
        if (ingot.isEmpty() && gem.isEmpty()) return empty(Action.DEFERRED_NO_ROW, input);
        // Neither the ore prefix weight nor the crusher/dense multiplier scales this amount.
        return new Outcome(Action.SOURCE_ROW, input.copy(), ingot, gem, snapshot.food(target));
    }

    static class Snapshot {
        Decision decision(GTMaterial material) { return OreSourcePolicy.furnaceDecision(material); }
        boolean hidden(GTMaterial material) { return material.has(MaterialProperty.HIDDEN); }
        ItemStack ingot(GTMaterial target, long amount) { return OM.ingot(target, amount); }
        ItemStack gem(GTMaterial target, long amount) { return OM.gem(target, amount); }
        boolean food(GTMaterial target) { return MaterialWorkability.isFood(target); }

        boolean blockInput(GTMaterial material, ItemStack input) {
            if (material == null || input.getCount() != 1 || !(input.getItem() instanceof BlockItem item)
                    || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")) return false;
            var block = item.getBlock();
            if (block instanceof OreBlock ore)
                return ore.prefix() == BlockMaterialPrefix.ore && ore.material().resolve() == material.resolve()
                        && GTBlocks.getStack(BlockMaterialPrefix.ore, material).getItem() == item;
            boolean matches = block instanceof DenseOreBlock ore && ore.material().resolve() == material.resolve()
                    || block instanceof VanillaOreBlock vanilla && vanilla.material().resolve() == material.resolve();
            if (!matches) return false;
//? if >=1.21.1 {
            /*return gregtech.content.registry.GTSpecialOreBlocks.all().stream()
                    .anyMatch(holder -> holder.isBound() && holder.get() == block);
*///?} else {
            return gregtech.content.registry.GTIconSetBlocks.all().stream()
                    .anyMatch(holder -> holder.isPresent() && holder.get() == block);
//?}
        }
    }
}
