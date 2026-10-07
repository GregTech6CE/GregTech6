package com.gregtech.gregtech.core;
import com.gregtech.gregtech.content.cover.CanvasRules;
import java.util.*;
/** Source constants and persistent identity behavior, with independently pinned samples. */
final class CanvasSourceContracts {
    static int verify(){
        require(CanvasRules.VARIANTS.size()==16,"sixteen original colors");
        require(CanvasRules.VARIANTS.get(0).originalId()==7030&&CanvasRules.VARIANTS.get(0).rgb()==0x202020,"original black ID and RGB");
        require(CanvasRules.VARIANTS.get(15).path().equals("canvas_white")&&CanvasRules.VARIANTS.get(15).originalId()==7045,"original white end of order");
        require(CanvasRules.BLOCK_SCAN_TICKS==512&&CanvasRules.CANVAS_SCAN_TICKS==64&&CanvasRules.PRINT_TICKS==64&&CanvasRules.POWER==16&&CanvasRules.PRINT_DYE_AMOUNT==16,"source scan/print costs");
        var props=new HashMap<String,String>();props.put("axis","x");var image=CanvasRules.image("minecraft:oak_log",props);props.put("axis","z");
        require(image.properties().get("axis").equals("x"),"image snapshot cannot change with input map");
        require(image.equals(CanvasRules.image("minecraft:oak_log",Map.of("axis","x"))),"image identity uses stable names/state rather than runtime IDs");
        require(CanvasRules.image("42",Map.of())==null&&CanvasRules.image("minecraft:air",Map.of())==null,"numeric runtime ID and air rejected");return 7;
    }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
}
