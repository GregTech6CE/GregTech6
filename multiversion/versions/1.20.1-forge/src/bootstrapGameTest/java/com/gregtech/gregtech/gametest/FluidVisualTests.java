package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.*;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class FluidVisualTests {
    private static boolean exists(ResourceLocation id){
        return FluidVisualTests.class.getResource("/assets/"+id.getNamespace()+"/textures/"+id.getPath()+".png")!=null;
    }
    @GameTest(template="test_empty") public static void namedFluidTexturesWinAndKeepTheirPaintedColors(GameTestHelper h){
        for(String field:new String[]{"Beer","Oil_Normal","Oil_Heavy","Oil_Light","Honey","Oil_Creosote"}){
            var entry=RegisteredFluids.get(field);var texture=RegisteredFluids.fluidTexture(entry.registryName());
            h.assertTrue(exists(texture),"Original sprite available: "+field);
            var visual=FluidVisualPolicy.select(entry,FluidVisualTests::exists);
            h.assertTrue(visual.texture().equals(texture)&&visual.tint()==0xFFFFFFFF,"Dedicated PNG is not replaced or multiplied twice: "+field);
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void generatedPhasesUseDistinctFallbacksAndMaterialColors(GameTestHelper h){
        var gas=RegisteredFluids.get("GenGas_Hydrogen");var liquid=RegisteredFluids.get("GenLiquid_SulfuricAcid");
        h.assertTrue(FluidTexturePolicy.kind(gas)==FluidTexturePolicy.Kind.GAS,"Generated gas is not molten metal");
        h.assertTrue(FluidTexturePolicy.kind(liquid)==FluidTexturePolicy.Kind.LIQUID,"Generated liquid is not molten metal");
        var gasVisual=FluidVisualPolicy.select(gas,id->id.equals(FluidTexturePolicy.Kind.GAS.standard()));
        var liquidVisual=FluidVisualPolicy.select(liquid,id->id.equals(FluidTexturePolicy.Kind.LIQUID.standard()));
        h.assertTrue(gasVisual.texture().equals(FluidTexturePolicy.Kind.GAS.standard()),"Missing gas texture uses gas template");
        h.assertTrue(liquidVisual.texture().equals(FluidTexturePolicy.Kind.LIQUID.standard())&&liquidVisual.tint()==(Materials.SulfuricAcid.getColor()|0xFF000000),"Missing liquid keeps material tint");
        h.assertTrue(FluidVisualPolicy.moltenTint(Materials.Steel)==0xFFFF140A&&FluidVisualPolicy.moltenTint(Materials.Iron)==0xFFFF4020,"Original molten multipliers are not premultiplied twice");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void sharedDyeAndFoamSpritesUseOriginalPalette(GameTestHelper h){
        for(String prefix:new String[]{"Dye_Water_","Dye_Flower_","Dye_Chemical_","CFoam_Dyed_","CFoam_DyedOwned_"}){
            var red=FluidVisualPolicy.select(RegisteredFluids.get(prefix+"Red"),FluidVisualTests::exists);
            var lime=FluidVisualPolicy.select(RegisteredFluids.get(prefix+"Lime"),FluidVisualTests::exists);
            h.assertTrue(exists(red.texture())&&red.texture().equals(lime.texture()),"Colors share their original grayscale sprite: "+prefix);
            h.assertTrue(red.tint()==0xFFFF0000&&lime.tint()==0xFF80FF80,"GT6 dye palette: "+prefix);
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void visualPolicyUsesCurrentResourceView(GameTestHelper h){
        var entry=RegisteredFluids.Beer;var named=RegisteredFluids.fluidTexture(entry.registryName());
        var fallback=FluidVisualPolicy.select(entry,id->id.equals(FluidTexturePolicy.Kind.LIQUID.standard()));
        var restored=FluidVisualPolicy.select(entry,id->id.equals(named));
        h.assertTrue(!fallback.texture().equals(named)&&restored.texture().equals(named),"Restoring a resource pack restores dedicated sprite selection");
        for(var fluid:RegisteredFluids.all().values()){
            var visual=FluidVisualPolicy.select(fluid,FluidVisualTests::exists);
            h.assertTrue((visual.tint()>>>24)==255,"Opaque shader tint: "+fluid.registryName());
            // Dedicated servers do not ship the client asset index containing vanilla water/lava.
            boolean vanilla=visual.texture().equals(ResourceLocation.withDefaultNamespace("block/water_still"))
                    ||visual.texture().equals(ResourceLocation.withDefaultNamespace("block/lava_still"));
            h.assertTrue(vanilla||exists(visual.texture()),"Every registered fluid selects a present resource: "+fluid.registryName());
        }
        h.succeed();
    }
}
