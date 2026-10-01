package com.gregtech.gregtech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

/** GT6's 176-wide machine panel; shared by the real container and JEI. */
public final class MachineGuiLayout {
    public record Point(int x,int y) {}
    private MachineGuiLayout() {}
    public static Point item(boolean input,int index,int count,int totalFluids) {
        int x=input?53-index%3*18:107+index%3*18;
        int y=count<=3?(totalFluids>3?7:25):count<=6?(totalFluids>3?7:16)+index/3*18:7+index/3*18;
        return new Point(x,y);
    }
    public static Point fluid(boolean input,int index) {
        return new Point(input?53-index%3*18:107+index%3*18,63-index/3*18);
    }
    public static ResourceLocation texture(RecipeMap map) {
        String path=map==null?"default":map.mGUIPath.substring(map.mGUIPath.lastIndexOf('/')+1).replace(".png","").toLowerCase(Locale.ROOT);
        // These two legacy port maps have no corresponding original GT6 GUI.
        if(path.equals("assembler") || path.equals("chisel")) path="default";
        return ResourceLocation.fromNamespaceAndPath("gregtech","textures/gui/recipes/"+path+".png");
    }
}
