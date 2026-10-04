package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.item.TechItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GT6 MultiItem categories (random tools, bottles, food, bumblebees) registered as
 * display items — behaviors (food stats, bee genetics) arrive with their systems.
 * Data comes from {@link GTMultiItemsGen} (transpiled from the GT6 sources).
 */
public final class GTMultiItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    private static final Map<String, RegistryObject<Item>> BY_ID = new LinkedHashMap<>();

    private GTMultiItems() {}

    public static Collection<RegistryObject<Item>> all() {
        return BY_ID.values();
    }

    private static final net.minecraft.world.food.FoodProperties FOOD =
            new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationMod(0.3F).build();

    /** TechItem already renders the transpiled GT6 tooltip line via its lang key. */
    private static final class MultiItem extends TechItem {
        MultiItem(String name, boolean hasTooltip, Item.Properties props) {
            super(name, props);
        }

        /**
         * §108: GT6's multi-item behaviour slot - duct tape, the five sprays, the lighters, the
         * portable/debug scanner and the matches all act through {@code onItemUseFirst}. The
         * dispatcher decides by registry id first, so the other ~1270 multi-items fall straight
         * through to {@code PASS} without allocating anything.
         */
        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useOn(
                            context.getLevel(), context.getClickedPos(), context.getClickedFace(),
                            context.getPlayer(), stack,
                            (float) (context.getClickLocation().x - context.getClickedPos().getX()),
                            (float) (context.getClickLocation().y - context.getClickedPos().getY()),
                            (float) (context.getClickLocation().z - context.getClickedPos().getZ()));
            if (context.getPlayer() != null) {
                context.getPlayer().setItemInHand(context.getHand(), outcome.stack());
            }
            return outcome.acted()?InteractionResult.SUCCESS:InteractionResult.PASS;
        }

        /** Entity half of the same dispatch (the extinguisher douses burning mobs, and so on). */
        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                      InteractionHand hand) {
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useOnEntity(target, player, stack);
            player.setItemInHand(hand,outcome.stack());
            return outcome.acted()?InteractionResult.SUCCESS:InteractionResult.PASS;
        }

        /**
         * §110: GT6's multi-item {@code onItemRightClick} slot. Of the port's ~1270 multi-items only the
         * remote activator has one ({@code Behavior_Remote:76-90}, "activate every bound coordinate of
         * this dimension"), so everything else falls straight through.
         *
         * <p><b>Falling through means calling {@code super}, not returning {@code PASS}.</b>
         * {@code Item.use}'s default implementation is what starts the eating animation for an edible
         * item, and ~60 of the port's multi-items are GT6 food ({@code GTMultiItems:94-98}). Returning
         * a bare {@code pass(held)} here would have quietly made every one of them inedible — the same
         * "what happens when the behaviour is not mine" question §108.7.3 is about.</p>
         */
        @Override
        public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player,
                                                      InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useInAir(level, player, held);
            if (!outcome.acted()) return super.use(level, player, hand);
            player.setItemInHand(hand, outcome.stack());
            return InteractionResultHolder.success(outcome.stack());
        }
    }

    public static void registerAll() {
        String[] entries = GTMultiItemsGen.ENTRIES;
        for (int i = 0; i + 3 < entries.length; i += 4) {
            String id = entries[i];
            String name = entries[i + 1];
            String category = entries[i + 2];
            final boolean hasTooltip = !entries[i + 3].isEmpty();
            // §108: GT6's own food numbers (MultiItemFood's FoodStat tables) instead of one blanket
            // placeholder. GTFoodItems.itemProperties falls back to the plain 64-stack properties for
            // the 59 rows that are display-only in GT6 as well, so nothing stops being edible by
            // accident - and `cans` uses the separate original MultiItemCans table.
            final Item.Properties props = "food".equals(category)
                    ? com.gregtech.gregtech.content.food.GTFoodItems.itemProperties(id)
                    : "cans".equals(category)
                            ? com.gregtech.gregtech.content.food.GTFoodItems.itemProperties(id)
                            : new Item.Properties().stacksTo(64);
            if ("cans".equals(category)) {
                BY_ID.put(id, ITEMS.register(id, () -> new com.gregtech.gregtech.item.CannedFoodItem(id, name, props)));
                continue;
            }
            // §103.C: GT6's drink bottles (MultiItemBottles) are the 250 mB portion of their fluid, so
            // they get a drinking item instead of the plain display item. The mapping is generated by
            // tools/extract_gt6_bottles.py; ids whose fluid the port does not have keep the plain item.
            final String bottleFluid = com.gregtech.gregtech.content.food.GTBottles.fluidKeyOf(id);
            if (bottleFluid != null && !"food".equals(category) && !"cans".equals(category)) {
                BY_ID.put(id, ITEMS.register(id,
                        () -> new com.gregtech.gregtech.item.BottleItem(bottleFluid, props)));
                continue;
            }
            BY_ID.put(id, ITEMS.register(id, () -> switch(id) {
                case "portable_scanner", "portable_cropnalyzer", "debug_scanner" ->
                    new com.gregtech.gregtech.item.ScannerItem(name,
                        com.gregtech.gregtech.content.tool.ScannerEnergyRules.forItem(id), props);
                case "radaway" -> new com.gregtech.gregtech.item.RadawayItem();
                case "geiger_counter" -> new com.gregtech.gregtech.item.GeigerCounterItem();
                case "empty_wax_pill" -> new MultiItem(name,hasTooltip,new Item.Properties().food(
                        new net.minecraft.world.food.FoodProperties.Builder().nutrition(0).saturationMod(0).alwaysEat().build()));
                // The four GT6 loot bags: right-click a block to open them and get GT's own loot
                // tables (MultiItemRandomTools:583-586, Behavior_Drop_Loot).
                case "bagged_sapling" -> new com.gregtech.gregtech.content.loot.LootBagItem(props, "gt.saplings");
                case "seed_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(props, "gt.seeds");
                case "gem_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(
                        props, "gt.flawless", "gt.gems", "gt.gems");
                case "loot_pouch" -> new com.gregtech.gregtech.content.loot.LootBagItem(props, "gt.misc");
                default -> new MultiItem(name, hasTooltip, props);
            }));
        }
        for(var dye:com.gregtech.gregtech.content.tool.PaintingRules.DYES){BY_ID.put(dye.fullId(),ITEMS.register(dye.fullId(),()->new com.gregtech.gregtech.item.PaintSprayItem(dye,new Item.Properties().stacksTo(1))));BY_ID.put(dye.usedId(),ITEMS.register(dye.usedId(),()->new com.gregtech.gregtech.item.PaintSprayItem(dye,new Item.Properties().stacksTo(1))));}
        // GT6's Dusty Guide Book (MultiItemBooks:67, addItem 32765) opens the gt.books table. The port
        // registers it by hand: it is the only MultiItemBooks entry the loot chain needs, and
        // transpile_gt6_multiitems.py has a HAND_REGISTERED set so a later "books" category never
        // emits this id twice. Its assets come from tools/generate_guide_book_assets.py.
        BY_ID.put("dusty_guide_book", ITEMS.register("dusty_guide_book",
                () -> new com.gregtech.gregtech.content.loot.LootBagItem(
                        new Item.Properties().stacksTo(64), "gt.books")));
        // GT6's Dusty Material Dictionary (MultiItemBooks:68, addItem 32766) rolls gt.matdicts, the
        // table GTLootTables builds from the registered materials (§31). Assets come from
        // tools/generate_material_dictionary_assets.py.
        BY_ID.put("dusty_material_dictionary", ITEMS.register("dusty_material_dictionary",
                () -> new com.gregtech.gregtech.content.loot.LootBagItem(
                        new Item.Properties().stacksTo(64), "gt.matdicts")));
        // GT6's Loot Bottle (MultiItemBottles:367 "Clouded Bottle", addItem 32761) opens gt.bottles —
        // the last GT table the port had deferred; assets from tools/generate_loot_bottle_assets.py.
        BY_ID.put("loot_bottle", ITEMS.register("loot_bottle",
                () -> new com.gregtech.gregtech.content.loot.LootBagItem(
                        new Item.Properties().stacksTo(64), "gt.bottles")));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
