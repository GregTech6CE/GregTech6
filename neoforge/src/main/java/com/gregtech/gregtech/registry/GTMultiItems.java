package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.item.TechItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import java.util.*;
/** Complete original multi-item identities and full behavior/loot/food/bottle adapters. */
public final class GTMultiItems {
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
 private static final Set<String> PROCESSING_REMAINDERS=Set.of("plant_remains","fruit_remains","vegetable_remains","nut_remains","rubber_resin","clay_tap","clay_funnel","clay_jug","clay_measuring_pot","modeled_porcelain_cup","geiger_counter","geiger_counter_empty","empty_blueprint","blueprint");
 private static final Map<String,DeferredHolder<Item,Item>> BY_ID=new LinkedHashMap<>();
 private GTMultiItems(){}
 public static Collection<DeferredHolder<Item,Item>> all(){return Collections.unmodifiableCollection(BY_ID.values());}
 public static void register(IEventBus bus){
  if(!BY_ID.isEmpty())throw new IllegalStateException("Neo MultiItem subset registry initialized twice");
  String[] entries=GTMultiItemsGen.ENTRIES;
  for(int i=0;i+3<entries.length;i+=4){String id=entries[i],name=entries[i+1];

   // Original remainder entries have no food stat; remote uses its actual native behavior item.
   String category=entries[i+2];String bottle=com.gregtech.gregtech.content.food.GTBottles.fluidKeyOf(id);
   BY_ID.put(id,ITEMS.register(id,()-> {Item.Properties props=category.equals("food")?com.gregtech.gregtech.content.food.GTFoodItems.itemProperties(id):category.equals("cans")?com.gregtech.gregtech.content.food.GTFoodItems.itemProperties(id):new Item.Properties().stacksTo(64);if(category.equals("cans"))return new com.gregtech.gregtech.item.CannedFoodItem(id,name,props);if(id.equals("geiger_counter"))return new com.gregtech.gregtech.item.GeigerCounterItem();if(id.equals("remote_activator"))return new com.gregtech.gregtech.item.RemoteActivatorItem(name,props);if(bottle!=null&&!category.equals("food")&&!category.equals("cans"))return new com.gregtech.gregtech.item.BottleItem(bottle,props);return switch(id){case "portable_scanner", "portable_cropnalyzer", "debug_scanner" ->
    new com.gregtech.gregtech.item.ScannerItem(name,
        com.gregtech.gregtech.content.tool.ScannerEnergyRules.forItem(id), props);
case "radaway" -> new com.gregtech.gregtech.item.RadawayItem();case "empty_wax_pill" -> new com.gregtech.gregtech.item.MultiBehaviorItem(name,new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()));case "bagged_sapling" -> new com.gregtech.gregtech.content.loot.LootBagItem(props,"gt.saplings");case "seed_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(props,"gt.seeds");case "gem_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(props,"gt.flawless","gt.gems","gt.gems");case "loot_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(props,"gt.misc");default -> new com.gregtech.gregtech.item.MultiBehaviorItem(name,props);};}));
  }
        for(var dye:com.gregtech.gregtech.content.tool.PaintingRules.DYES){BY_ID.put(dye.fullId(),ITEMS.register(dye.fullId(),()->new com.gregtech.gregtech.item.PaintSprayItem(dye,new Item.Properties().stacksTo(1))));BY_ID.put(dye.usedId(),ITEMS.register(dye.usedId(),()->new com.gregtech.gregtech.item.PaintSprayItem(dye,new Item.Properties().stacksTo(1))));}
  BY_ID.put("dusty_guide_book",ITEMS.register("dusty_guide_book",()->new com.gregtech.gregtech.content.loot.LootBagItem(new Item.Properties().stacksTo(64),"gt.books")));
  BY_ID.put("dusty_material_dictionary",ITEMS.register("dusty_material_dictionary",()->new com.gregtech.gregtech.content.loot.LootBagItem(new Item.Properties().stacksTo(64),"gt.matdicts")));
  BY_ID.put("loot_bottle",ITEMS.register("loot_bottle",()->new com.gregtech.gregtech.content.loot.LootBagItem(new Item.Properties().stacksTo(64),"gt.bottles")));
  ITEMS.register(bus);
 }
}
