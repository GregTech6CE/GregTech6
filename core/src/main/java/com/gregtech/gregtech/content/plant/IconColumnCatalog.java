package com.gregtech.gregtech.content.plant;
import java.util.*;
/** Original composite log/beam and eight bale identities. Both factories consume this table. */
public final class IconColumnCatalog {private IconColumnCatalog(){}
    public static String[][] all() {
        List<String[]> list = new ArrayList<>();
        for (String wood : new String[]{"bluemahoe", "bluespruce", "cinnamon", "coconut", "dry", "frozen",
                "hazel", "maple", "mossy", "rainbowood", "rotten", "rubber", "willow"}) {
            list.add(new String[]{"log_" + wood, "log_side_" + wood, "log_top_" + wood});
        }
        // Resin/sap/hole logs: special side texture, plain top of the same wood.
        list.add(new String[]{"log_hole_maple", "log_hole_maple", "log_top_maple"});
        list.add(new String[]{"log_hole_rainbowood", "log_hole_rainbowood", "log_top_rainbowood"});
        list.add(new String[]{"log_hole_rubber", "log_hole_rubber", "log_top_rubber"});
        list.add(new String[]{"log_sap_maple", "log_sap_maple", "log_top_maple"});
        list.add(new String[]{"log_sap_rainbowood", "log_sap_rainbowood", "log_top_rainbowood"});
        list.add(new String[]{"log_resin_rubber", "log_resin_rubber", "log_top_rubber"});
        for (String wood : new String[]{"acacia", "birch", "bluemahoe", "bluespruce", "cinnamon", "coconut",
                "darkoak", "darkwood", "greatwood", "hazel", "jungle", "maple", "oak", "rainbowood", "rubber",
                "rubberwood", "silverwood", "skyroot", "spruce", "willow", "wood"}) {
            list.add(new String[]{"beam_" + wood, "beam_side_" + wood, "beam_top_" + wood});
        }
        for (String crop : new String[]{"barley", "oat", "rice", "rye"}) {
            list.add(new String[]{"bale_" + crop, crop + "_side", crop + "_top"});
        }
        list.add(new String[]{"bale_grass", "grass_side", "grass_top"});
        for (String stage : new String[]{"dry", "moldy", "rotten"}) {
            list.add(new String[]{"bale_grass_" + stage, "grass_side_" + stage, "grass_top_" + stage});
        }
        return list.toArray(new String[0][]);
    }

}
