package gregtech.content.recipe;

import gregtech.api.material.*;

import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.platform.Mc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** capcellcon is a 1/9 U placeable vessel, not a second flat material item.
 * The forty variants all inherit EXTRUDER; only waxes, Plastic, Tin, TinAlloy and Aluminium inherit EXTRUDER_SIMPLE. */
public final class CapsuleCellRecipes {
    private CapsuleCellRecipes() {}
    public static int register() {
        // SourceExtruderRecipes registers native capsule forms together with the complete prefix matrix.
        return 0;
    }
    public static boolean simpleExtrusion(PortableFluidContainerSpec spec) {
        // Keep legacy/extension choices; the imported source profile supplies positive evidence only.
        return gregtech.content.data.generated.MaterialWorkability.hasProvenSimpleExtrusion(spec.material())
                || spec.id().startsWith("cell_wax")||switch(spec.id()) {
            case "cell_plastic","cell_tin","cell_tin_alloy","cell_aluminium"->true;default->false;
        };
    }
    /** RecipeMapHandlerPrefixForging.getCosts / OreDictMaterial.getWeight, at 96 GU/t. */
    public static long hotExtrusionTicks(gregtech.api.material.GTMaterial material,long units) {
        return ExtrusionWorkRules.hotExtrusionTicks(material,units);
    }

    /** RecipeMapShredder: an empty capcellcon recovers 1/9 U; a filled one is never recyclable. */
    public static int registerRecycling() {
        int count=0;
        for(var spec:PortableFluidContainerSpec.values())if(spec.shapeId().equals("cell")) {
            var output=GTItems.getStack(MaterialPrefix.dustTiny,spec.material(),1);
            if(output.isEmpty())throw new IllegalStateException("Missing capsule recycling dust: "+spec.id());
            var input=new ItemStack(Mc.item(ResourceLocation.fromNamespaceAndPath("gt6ce","fluid_"+spec.id())));
            var composition=ItemMaterialRegistry.base(input.getItem()).orElse(null);
            if(composition==null) {
                var data=new ItemComposition(null,java.util.List.of(MaterialComponent.of(spec.material(),GTValues.U9)),
                        spec.material(),"capsule-cell material",true);
                ItemMaterialRegistry.register(input.getItem(),data,ItemCompositionSource.Kind.EXPLICIT,
                        "gt6ce:capsule-cell/"+spec.id(),"PortableFluidContainerSpec");
                composition=ItemMaterialRegistry.base(input.getItem()).orElse(null);
            }
            if(composition==null
                    ||ItemMaterialRegistry.recoveryRejection(input)!=ItemMaterialRegistry.RecoveryRejection.ELIGIBLE
                    ||composition.components().size()!=1
                    ||composition.material().resolve()!=spec.material().resolve())continue;
            long work=MaterialRecoveryRules.staticShredderWork(spec.material());
            var recipe=new Recipe(new ItemStack[]{input},new ItemStack[]{output},null,null,null,null,(work+8)/9,16,0)
                    .withMaterialRecovery()
                    .withSource("recovery:capsule");
            if(RecoveryRecipeConflicts.publish(MachineRecipeMaps.Shredder,recipe)!=null)count++;
        }
        return count;
    }
}
