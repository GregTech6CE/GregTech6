package com.gregtech.gregtech.api.recipe;
import net.minecraft.resources.ResourceLocation;
public final class MachineGuiLayout {
 public record Point(int x,int y){}
 private MachineGuiLayout(){}
 public static Point item(boolean input,int index,int count,int fluids){var p=MachineGuiCoordinates.item(input,index,count,fluids);return new Point(p.x(),p.y());}
 public static Point fluid(boolean input,int index){var p=MachineGuiCoordinates.fluid(input,index);return new Point(p.x(),p.y());}
 public static ResourceLocation texture(RecipeMap map){return ResourceLocation.fromNamespaceAndPath("gregtech",MachineGuiCoordinates.texture(map==null?null:map.mGUIPath));}
}
