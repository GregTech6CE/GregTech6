package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** GT6 wire handlers and MultiTileEntityWireElectric laminator recipes, resolved against block registries. */
public final class WireProcessingRecipes {
    public record Entry(RecipeMap map,Recipe recipe) {}
    private static final List<Entry> ENTRIES=new ArrayList<>();
    // MT's SOFT/FURNACE wire materials. Hard-workable metals use the quality-scaled handler cost.
    private static final Set<String> EASY=Set.of("tin","lead","copper","annealed_copper","silver","gold","aluminium","electrum","blue_alloy","electrotine_alloy");
    private WireProcessingRecipes() {}
    public static List<Entry> entries() {return Collections.unmodifiableList(ENTRIES);}
    public static int register() {
        if(!ENTRIES.isEmpty())throw new IllegalStateException("Wire recipes registered twice");
        Map<String,Map<Integer,ElectricWireBlock>> families=new LinkedHashMap<>();
        for(var entry:GTWires.allWires()) {
            var block=entry.get();families.computeIfAbsent(block.spec().id(),ignored->new TreeMap<>()).put(block.spec().size(),block);
        }
        for(var family:families.entrySet()) {
            var sizes=family.getValue();var one=sizes.get(1);var material=one.spec().material();
            if(material.has(MaterialProperty.SMITHABLE)&&!Set.of("graphene","netherite","superconductor").contains(family.getKey())) {
                var input=GTItems.getStack(MaterialPrefix.ingot,material,1);
                if(input.isEmpty())throw new IllegalStateException("Wire ingot missing: "+family.getKey());
                add(MachineRecipeMaps.Wiremill,EASY.contains(family.getKey())?16:128L*(material.getToolQuality()+1),new ItemStack(one,2),input);
            }
            for(int big=2;big<=16;big++) {
                // GT6 registers small=1 first. Keep one deterministic unpacking result instead of competing recipes.
                add(MachineRecipeMaps.Unboxinator,16,new ItemStack(one,big),new ItemStack(sizes.get(big)));
                for(int small=1;small<big;small++)if(big%small==0)
                    add(MachineRecipeMaps.Loom,32L*big*(material.getToolQuality()+1),new ItemStack(sizes.get(big)),
                            new ItemStack(sizes.get(small),big/small),new ItemStack(GTTechnological.selectorTag(big)));
            }
        }
        for(var entry:GTWires.allCables()) {
            var cable=entry.get();int size=cable.spec().size();
            if(size==16)continue; // Legacy port extension: GT6 only has insulated 1,2,4,8,12.
            int plates=switch(size){case 1,2->1;case 4->2;case 8->3;case 12->4;default->throw new IllegalStateException("Cable width "+size);};
            var wire=families.get(cable.spec().id()).get(size);
            for(var prefix:new MaterialPrefix[]{MaterialPrefix.plate,MaterialPrefix.foil}) {
                var rubber=GTItems.getStack(prefix,GTMaterialRegistry.get("Rubber"),plates*(prefix==MaterialPrefix.foil?4:1));
                if(rubber.isEmpty())throw new IllegalStateException("Missing insulation form "+prefix.getName());
                add(MachineRecipeMaps.Laminator,plates*16,new ItemStack(cable),rubber,new ItemStack(wire));
            }
        }
        for (var entry : GTSignalWires.all()) {
            var wire = entry.get();
            if (!wire.insulated()) {
                add(MachineRecipeMaps.Wiremill, 16, new ItemStack(wire, 2),
                        GTItems.getStack(MaterialPrefix.ingot, wire.material(), 1));
            } else {
                var bare = GTSignalWires.all().stream().map(r -> r.get())
                        .filter(b -> !b.insulated() && b.material() == wire.material()).findFirst().orElseThrow();
                for (var prefix : new MaterialPrefix[]{MaterialPrefix.plate, MaterialPrefix.foil}) {
                    add(MachineRecipeMaps.Laminator, 16, new ItemStack(wire),
                            GTItems.getStack(prefix, GTMaterialRegistry.get("Rubber"), prefix == MaterialPrefix.foil ? 4 : 1),
                            new ItemStack(bare));
                }
            }
        }
        return ENTRIES.size();
    }
    private static void add(RecipeMap map,long ticks,ItemStack result,ItemStack... input) {
        var recipe=new Recipe(input,new ItemStack[]{result},null,null,null,null,ticks,16,0);
        if(map.addRecipe(recipe)==null)throw new IllegalStateException("Wire recipe conflict: "+map.mNameInternal+" / "+result);
        ENTRIES.add(new Entry(map,recipe));
    }
}
