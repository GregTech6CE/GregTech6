package com.gregtech.gregtech.data.generated;
import com.gregtech.gregtech.content.food.*;import net.minecraft.world.item.*;import net.minecraft.resources.ResourceLocation;import java.util.*;
/** Platform binding of the single shared source statistic catalog. */
public final class GTFoodStatsGen {
 private GTFoodStatsGen(){} private static final List<String> MISSING_IDS=new ArrayList<>();
 public static final List<GTFoodStats.Row> ROWS=build();public static final List<String> MISSING=List.copyOf(MISSING_IDS);public static final List<String> SKIPPED=FoodStatisticRows.SKIPPED;
 private static List<GTFoodStats.Row> build(){List<GTFoodStats.Row> rows=new ArrayList<>();for(var r:FoodStatisticRows.ROWS){Item item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(r.itemId()));if(item==null||item==Items.AIR){MISSING_IDS.add(r.itemId());continue;}rows.add(new GTFoodStats.Row(item,r.alcohol(),r.caffeine(),r.dehydration(),r.sugar(),r.fat(),r.radiation()));}return List.copyOf(rows);}
}
