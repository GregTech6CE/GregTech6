package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** GT6 RecipeMapShredder decomposition for vanilla and explicitly audited native compositions. */
public final class VanillaRecoveryRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    public static List<Recipe> recipes() { return Collections.unmodifiableList(RECIPES); }
    private VanillaRecoveryRecipes() {}
    public static int register() {
        for (var item : ForgeRegistries.ITEMS.getValues().stream().sorted(Comparator.comparing(i -> ForgeRegistries.ITEMS.getKey(i).toString())).toList()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (!id.getNamespace().equals("minecraft") && !PanelMaterialRegistration.recoveryItems().contains(item) && !TransportMaterialRegistration.recoveryItems().contains(item)) continue;
            if (id.getPath().endsWith("_ore") || id.getPath().startsWith("raw_")) continue;
            var stack = new ItemStack(item);
            if (!ItemMaterialRegistry.canRecover(stack)) continue;
            var data = ItemMaterialRegistry.get(stack).orElseThrow();
            if (data.prefix() != null && (data.prefix().getName().startsWith("dust") || data.prefix() == MaterialPrefix.oreRaw)) continue;
            var crushed = new LinkedHashMap<GTMaterial,Long>();
            for (var c : data.components()) {
                var target = c.material().getTargetPulverMaterial();
                long amount = MaterialRecoveryRules.pulverizedAmount(c.material(),c.amount());
                if (target != null && amount > 0) crushed.merge(target.resolve(), amount, Long::sum);
            }
            var outputs = new ArrayList<ItemStack>();
            long duration = 0;
            boolean complete = !crushed.isEmpty();
            for (var c : crushed.entrySet()) {
                var dust = dust(c.getKey(), c.getValue());
                if (dust.isEmpty()) { complete = false; break; }
                outputs.add(dust);
                long work = MaterialRecoveryRules.shredderWork(c.getKey());
                duration += (c.getValue()*work+GTValues.U-1)/GTValues.U;
            }
            if (!complete || outputs.size() > MachineRecipeMaps.Shredder.mOutputItemsCount) continue;
            if (outputs.size() == 1 && ItemStack.isSameItemSameTags(stack,outputs.get(0)) && outputs.get(0).getCount() == 1) continue;
            var recipe = new Recipe(new ItemStack[]{stack}, outputs.toArray(ItemStack[]::new), null,null,null,null,Math.max(1,duration),16,0).withMaterialRecovery();
            // Existing explicit GT6 processing recipes keep priority and their special yields.
            if (MachineRecipeMaps.Shredder.addRecipe(recipe) != null) RECIPES.add(recipe);
        }
        return RECIPES.size();
    }
    /** OM.dust pile choice; floor any unrepresentable fraction, never manufacture extra mass. */
    private static ItemStack dust(GTMaterial material,long amount) {
        var pile=MaterialRecoveryRules.dust(amount);
        return pile==null?ItemStack.EMPTY:GTItems.getStack(pile.prefix(),material,pile.count());
    }
}
