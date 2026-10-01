package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** GT6 RecipeMapShredder material decomposition, published for known vanilla compositions. */
public final class VanillaRecoveryRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    public static List<Recipe> recipes() { return Collections.unmodifiableList(RECIPES); }
    private VanillaRecoveryRecipes() {}
    public static int register() {
        for (var item : ForgeRegistries.ITEMS.getValues().stream().sorted(Comparator.comparing(i -> ForgeRegistries.ITEMS.getKey(i).toString())).toList()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (!id.getNamespace().equals("minecraft") || id.getPath().endsWith("_ore") || id.getPath().startsWith("raw_")) continue;
            var stack = new ItemStack(item);
            if (!ItemMaterialRegistry.canRecover(stack)) continue;
            var data = ItemMaterialRegistry.get(stack).orElseThrow();
            if (data.prefix() != null && (data.prefix().getName().startsWith("dust") || data.prefix() == MaterialPrefix.oreRaw)) continue;
            var crushed = new LinkedHashMap<GTMaterial,Long>();
            for (var c : data.components()) {
                var target = c.material().getTargetPulverMaterial();
                long amount = java.math.BigInteger.valueOf(c.amount()).multiply(java.math.BigInteger.valueOf(c.material().getTargetPulverAmount())).divide(java.math.BigInteger.valueOf(GTValues.U)).longValueExact();
                if (target != null && amount > 0) crushed.merge(target.resolve(), amount, Long::sum);
            }
            var outputs = new ArrayList<ItemStack>();
            long duration = 0;
            boolean complete = !crushed.isEmpty();
            for (var c : crushed.entrySet()) {
                var dust = dust(c.getKey(), c.getValue());
                if (dust.isEmpty()) { complete = false; break; }
                outputs.add(dust);
                long work = (c.getKey().getName().contains("Quartz") ? 64L : c.getKey().hasAny(MaterialProperty.WOOD,MaterialProperty.STONE,MaterialProperty.GEM) ? 2L : 256L)
                        * Math.max(1,c.getKey().getToolQuality()+1);
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
        long unit = GTValues.U;
        if (amount < unit/72) return ItemStack.EMPTY;
        if (amount >= unit && (amount >= unit*16 || amount%unit == 0)) return pile(MaterialPrefix.dust,material,amount/unit);
        if (amount >= unit/4 && (amount >= unit*8 || amount%(unit/4) <= amount%(unit/9))) return pile(MaterialPrefix.dustSmall,material,amount/(unit/4));
        if (amount >= unit/9 && (amount >= unit || amount%(unit/9) <= amount%(unit/72))) return pile(MaterialPrefix.dustTiny,material,amount/(unit/9));
        return pile(MaterialPrefix.dustDiv72,material,amount/(unit/72));
    }
    private static ItemStack pile(MaterialPrefix prefix,GTMaterial material,long count) {
        return GTItems.getStack(prefix,material,(int)Math.min(64,count));
    }
}
