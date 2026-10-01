package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Predicate;

/** Texture and multiplication color are one decision. Original named PNGs are precolored;
 * material templates and shared dye textures require a color multiplier.
 */
public final class FluidVisualPolicy {
    public record Visual(ResourceLocation texture,int tint) {}
    private FluidVisualPolicy() {}
    public static Visual select(RegisteredFluids.FluidEntry entry,Predicate<ResourceLocation> exists){
        // GT6's sea/river/swamp water are their own fluids, so they have to reproduce vanilla water's
        // look by hand: vanilla's own still sprite multiplied by vanilla's own water tint, decided
        // before the resource-pack view is consulted (no GT sprite may darken them again). Without
        // this, seawater and swampwater fell through to the animated "molten" material template and
        // riverwater was drawn precoloured with a white (no-op) tint, which is what made them look
        // darker than vanilla water.
        if(GTWaterParity.isWorldWater(entry.registryName()))
            return new Visual(GTWaterParity.VANILLA_WATER_STILL_TEXTURE,GTWaterParity.VANILLA_WATER_TINT);
        if(entry.textureMode()==RegisteredFluids.FluidTextureMode.VANILLA_WATER)
            return new Visual(GTWaterParity.VANILLA_WATER_STILL_TEXTURE,GTWaterParity.VANILLA_WATER_TINT);
        if(entry.textureMode()==RegisteredFluids.FluidTextureMode.VANILLA_LAVA)
            return new Visual(ResourceLocation.withDefaultNamespace("block/lava_still"),0xFFFFFFFF);
        String name=entry.registryName();
        var dedicated=RegisteredFluids.fluidTexture(name);
        if(exists.test(dedicated))return new Visual(dedicated,explicitTint(entry));
        // Imported generated aliases can share their original, named fluid texture.
        if(name.startsWith("gas.")||name.startsWith("liquid.")){
            var named=RegisteredFluids.fluidTexture(name.substring(name.indexOf('.')+1));
            if(exists.test(named))return new Visual(named,0xFFFFFFFF);
        }
        String shared=name.startsWith("dye.watermixed.")?"dyes.water":name.startsWith("dye.flower.")?"dyes.flower":
                name.startsWith("dye.chemical.")?"dyes.chemical":name.startsWith("cfoam.")?"cfoam":null;
        if(shared!=null){
            var texture=RegisteredFluids.fluidTexture(shared);
            if(exists.test(texture))return new Visual(texture,rawTint(entry));
        }
        var kind=FluidTexturePolicy.kind(entry);
        var template=kind.standard();
        var material=material(entry);
        if(material.isValid() && (kind==FluidTexturePolicy.Kind.MOLTEN || kind==FluidTexturePolicy.Kind.PLASMA)){
            template=GregTech.id("block/material_icons/"+material.getTextureSet().folder()+
                    (kind==FluidTexturePolicy.Kind.PLASMA?"/plasma":"/molten"));
        }
        return new Visual(FluidTexturePolicy.select(template,kind,exists),rawTint(entry));
    }
    private static int explicitTint(RegisteredFluids.FluidEntry entry){
        return entry.textureMode()==RegisteredFluids.FluidTextureMode.DEDICATED && entry.tintTexture() && entry.tintArgb()!=0
                ? entry.tintArgb()|0xFF000000:0xFFFFFFFF;
    }
    public static GTMaterial material(RegisteredFluids.FluidEntry entry){
        String key=entry.materialKey();
        if(key==null)key=RegisteredFluids.boundMaterial(entry.registryName());
        if(key==null)key=entry.registryName().replaceFirst("^(liquid[._]|gas[._]|molten[._]|plasma[._])", "");
        return GTMaterialRegistry.get(key).resolve();
    }
    public static int rawTint(RegisteredFluids.FluidEntry entry){
        var material=material(entry);
        if(entry.tintArgb()!=0)return entry.tintArgb()|0xFF000000;
        if(FluidTexturePolicy.kind(entry)==FluidTexturePolicy.Kind.MOLTEN && material.isValid())return moltenTint(material);
        String name=entry.registryName();
        if(name.startsWith("dye.")||name.startsWith("cfoam."))return dyeTint(name.substring(name.lastIndexOf('.')+1));
        return material.isValid()?material.getColor()|0xFF000000:0xFFFFFFFF;
    }
    /** MT.setRGBaLiquid values, before multiplying by the original grayscale sprite. */
    public static int moltenTint(GTMaterial material){
        return switch(material.resolve().getName()){
            case "Iron" -> 0xFFFF4020;
            case "WroughtIron" -> 0xFFFF5028;
            case "Steel" -> 0xFFFF140A;
            case "HSLASteel" -> 0xFFB4501E;
            case "Iridium" -> 0xFFFF80C8;
            case "Naquadah" -> 0xFF00FF00;
            case "NaquadahEnriched" -> 0xFF40FF40;
            case "Naquadria" -> 0xFF80FF80;
            case "Stone" -> 0xFFC06040;
            case "Bedrock" -> 0xFF806040;
            default -> material.getColor()|0xFF000000;
        };
    }
    private static int dyeTint(String name){
        return switch(name.replace("_","")){
            case "black"->0xFF202020;case "red"->0xFFFF0000;case "green"->0xFF00FF00;case "brown"->0xFF604000;
            case "blue"->0xFF0000FF;case "purple"->0xFF800080;case "cyan"->0xFF00FFFF;case "lightgray"->0xFFC0C0C0;
            case "gray"->0xFF808080;case "pink"->0xFFFFC0C0;case "lime"->0xFF80FF80;case "yellow"->0xFFFFFF00;
            case "lightblue"->0xFF8080FF;case "magenta"->0xFFFF00FF;case "orange"->0xFFFF8000;default->0xFFFFFFFF;
        };
    }
}
