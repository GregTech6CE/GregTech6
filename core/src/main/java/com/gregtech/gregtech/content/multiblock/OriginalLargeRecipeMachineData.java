/* Copyright (c) 2025 GregTech-6 Team; Gregorius Techneticies.
 * LGPL-3.0-or-later. Original specialized multiblock controllers and their registered defaults. */
package com.gregtech.gregtech.content.multiblock;
import java.util.List;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
public final class OriginalLargeRecipeMachineData {
    private OriginalLargeRecipeMachineData() {}
    public enum Face { UP, DOWN, LEFT, RIGHT, FRONT }
    public record Source(int right,int up,int back,Face outputFace) {}
    public record Target(int right,int up,int back,Face receiverFace) {}
    public static List<String> structureKeys(String name) {
        return switch(name) {
            case "largecentrifuge" -> List.of("gt.tooltip.multiblock.centrifuge.1","gt.tooltip.multiblock.centrifuge.2","gt.tooltip.multiblock.centrifuge.3");
            case "largeelectrolyzer" -> List.of("gt.tooltip.multiblock.electrolyzer.1","gt.tooltip.multiblock.electrolyzer.2","gt.tooltip.multiblock.electrolyzer.3");
            case "largecoagulator" -> List.of("gt.tooltip.multiblock.coagulator.1","gt.tooltip.multiblock.coagulator.2","gt.tooltip.multiblock.coagulator.3");
            case "largeautoclave" -> List.of("gt.tooltip.multiblock.autoclave.1","gt.tooltip.multiblock.autoclave.2","gt.tooltip.multiblock.autoclave.3");
            case "largebath" -> List.of("gt.tooltip.multiblock.bath.1","gt.tooltip.multiblock.bath.2","gt.tooltip.multiblock.bath.3");
            case "largemixer" -> List.of("gt.tooltip.multiblock.mixer.1","gt.tooltip.multiblock.mixer.2","gt.tooltip.multiblock.mixer.3");
            case "largefermenter" -> List.of("gt.tooltip.multiblock.fermenter.1","gt.tooltip.multiblock.fermenter.2","gt.tooltip.multiblock.fermenter.3","gt.tooltip.multiblock.fermenter.4");
            case "largeoven" -> List.of("gt.tooltip.multiblock.oven.1","gt.tooltip.multiblock.oven.2","gt.tooltip.multiblock.oven.3","gt.tooltip.multiblock.oven.4","gt.tooltip.multiblock.oven.5");
            case "largesluice" -> List.of("gt.tooltip.multiblock.sluice.1","gt.tooltip.multiblock.sluice.2","gt.tooltip.multiblock.sluice.3","gt.tooltip.multiblock.sluice.4","gt.tooltip.multiblock.sluice.5");
            case "largecrusher" -> List.of("gt.tooltip.multiblock.crusher.1","gt.tooltip.multiblock.crusher.2","gt.tooltip.multiblock.crusher.3","gt.tooltip.multiblock.crusher.4");
            case "largeshredder" -> List.of("gt.tooltip.multiblock.shredder.1","gt.tooltip.multiblock.shredder.2","gt.tooltip.multiblock.shredder.3","gt.tooltip.multiblock.shredder.4");
            case "largesqueezer" -> List.of("gt.tooltip.multiblock.squeezer.1","gt.tooltip.multiblock.squeezer.2","gt.tooltip.multiblock.squeezer.3");
            default -> List.of();
        };
    }
    public static boolean handles(String name) {return !structureKeys(name).isEmpty();}
    public static String recipeKey(String name) {return name.equals("largeoven")?"mc.recipe.furnace":"gt.recipe."+name.substring(5);}
    public static MachineFaceMasks faces(String name) {
        var all=MachineFaceMasks.ALL_SIDES;
        int output=name.equals("largefermenter")?MachineFaceMasks.BACK:MachineFaceMasks.BOTTOM;
        return new MachineFaceMasks(all.itemInputs(),all.itemOutputs(),all.fluidInputs(),all.fluidOutputs(),
                all.energyInputs(),0,-1,output,-1,output);
    }
    public static Target output(String name,boolean items) {
        return name.equals("largefermenter")?new Target(0,items?1:0,5,Face.FRONT):new Target(0,-1,0,Face.UP);
    }
    public static List<Source> sources(String name) {
        return switch(name) {
            case "largecentrifuge","largemixer" -> List.of(new Source(0,-1,1,Face.UP),new Source(0,2,1,Face.DOWN));
            case "largeelectrolyzer" -> List.of(new Source(0,-1,1,Face.UP));
            case "largesluice" -> List.of(new Source(-2,1,5,Face.RIGHT),new Source(2,1,5,Face.LEFT));
            case "largecrusher","largeshredder","largesqueezer" -> List.of(new Source(-3,1,2,Face.RIGHT),new Source(3,1,2,Face.LEFT));
            default -> List.of(); // Original parent intentionally does not apply ordinary six-neighbor requests.
        };
    }
}
