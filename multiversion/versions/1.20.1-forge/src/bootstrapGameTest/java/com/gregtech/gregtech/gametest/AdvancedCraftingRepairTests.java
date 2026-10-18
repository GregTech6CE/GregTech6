package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.client.gui.AdvancedCraftingMenu;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.worldgen.dungeon.*;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class AdvancedCraftingRepairTests {
    private static AdvancedCraftingTableBlockEntity place(GameTestHelper h){h.setBlock(new BlockPos(1,1,1),GTMiscBlocks.ADVANCED_CRAFTING_TABLE.get());return (AdvancedCraftingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));}
    private static ItemStack tool(GTToolType type){return GTToolItem.create(type,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));}
    private static void probe(GameTestHelper h,boolean toolRecipe,ItemStack[] inputs,Runnable action){
        var manager=h.getLevel().getRecipeManager();var old=new ArrayList<Recipe<?>>(manager.getRecipes());var all=new ArrayList<Recipe<?>>(old);
        var ingredients=NonNullList.<Ingredient>create();for(var input:inputs)ingredients.add(Ingredient.of(input));
        var recipe=new ShapedRecipe(GregTech.id("repair_crafting_probe"),"",CraftingBookCategory.MISC,inputs.length,1,ingredients,new ItemStack(Items.EMERALD));
        all.add(toolRecipe?new com.gregtech.gregtech.recipe.ToolShapedRecipe(recipe):recipe);manager.replaceRecipes(all);
        try{action.run();}finally{manager.replaceRecipes(old);}
    }
    @GameTest(template="test_empty")
    public static void craftingPreservesPatternUntilStoresRunOutAndHonorsCursorCapacity(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        probe(h,false,new ItemStack[]{new ItemStack(Items.STRUCTURE_VOID),new ItemStack(Items.STRUCTURE_VOID)},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID));table.items().setStackInSlot(22,new ItemStack(Items.STRUCTURE_VOID));table.items().setStackInSlot(70,new ItemStack(Items.STRUCTURE_VOID,4));
            var menu=new AdvancedCraftingMenu(1,player.getInventory(),table);
            h.assertTrue(menu.slots.size()==72&&menu.getSlot(36).y==84&&menu.getSlot(63).y==142,"original 36 top GUI slots plus player inventory coordinates");
            menu.setCarried(new ItemStack(Items.EMERALD,64));menu.clicked(AdvancedCraftingMenu.RESULT,0,ClickType.PICKUP,player);
            h.assertTrue(table.items().getStackInSlot(70).getCount()==4,"full cursor does not consume any material");menu.setCarried(ItemStack.EMPTY);
            menu.clicked(AdvancedCraftingMenu.RESULT,0,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().getCount()==1&&table.items().getStackInSlot(70).getCount()==2&&table.items().getStackInSlot(21).getCount()==1,"one craft consumes storage first, leaves grid sample");
            menu.clicked(AdvancedCraftingMenu.RESULT,1,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().getCount()==3&&table.items().getStackInSlot(21).isEmpty()&&table.items().getStackInSlot(22).isEmpty(),"right click crafts remaining supplies including last grid pair");
            h.assertTrue(table.preview().isEmpty(),"exhausted real grid clears preview");
            menu.clicked(AdvancedCraftingMenu.RESULT,0,ClickType.PICKUP,player);h.assertTrue(menu.getCarried().getCount()==3,"cannot duplicate display output");
        });h.succeed();
    }
    @GameTest(template="test_empty")
    public static void shiftCraftingFillsOneStackOrAllAvailableInventoryWithoutSpills(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        probe(h,false,new ItemStack[]{new ItemStack(Items.STRUCTURE_VOID)},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID,64));table.items().setStackInSlot(70,new ItemStack(Items.STRUCTURE_VOID,64));
            var menu=new AdvancedCraftingMenu(1,player.getInventory(),table);
            menu.clicked(AdvancedCraftingMenu.RESULT,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(player.getInventory().getItem(0).is(Items.EMERALD)&&player.getInventory().getItem(0).getCount()==64&&player.getInventory().getItem(1).isEmpty(),"Shift-left crafts one inventory stack");
            menu.clicked(AdvancedCraftingMenu.RESULT,1,ClickType.QUICK_MOVE,player);
            h.assertTrue(player.getInventory().getItem(1).getCount()==64&&table.items().getStackInSlot(21).isEmpty()&&table.items().getStackInSlot(70).isEmpty(),"Shift-right consumes remaining stock into next inventory slot");
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID));menu.clicked(AdvancedCraftingMenu.RESULT,1,ClickType.QUICK_MOVE,player);
            h.assertTrue(table.items().getStackInSlot(21).getCount()==1,"full inventory prevents crafting and does not drop results on ground");
        });h.succeed();
    }
    @GameTest(template="test_empty")
    public static void rearInventoryShiftMergeInvalidatesCachedCraft(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        probe(h,false,new ItemStack[]{new ItemStack(Items.STRUCTURE_VOID)},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID));table.items().setStackInSlot(35,new ItemStack(Items.STRUCTURE_VOID));
            h.assertTrue(table.preview().is(Items.EMERALD),"prime crafting cache");
            var storage=table.storageMenu(1,player.getInventory());player.getInventory().setItem(9,new ItemStack(Items.STRUCTURE_VOID,2));
            storage.quickMoveStack(player,36);
            h.assertTrue(table.items().getStackInSlot(35).getCount()==3,"rear Shift-click merged into existing stack");
            h.assertTrue(table.craftOne(player).is(Items.EMERALD)&&table.items().getStackInSlot(35).getCount()==2&&table.items().getStackInSlot(21).getCount()==1,"craft uses fresh inventory after merge, loses no added supplies");
        });h.succeed();
    }
    @GameTest(template="test_empty")
    public static void toolWearAndRemaindersCommitWithoutLoss(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var hammer=tool(GTToolType.HARD_HAMMER);
        probe(h,true,new ItemStack[]{new ItemStack(Items.JIGSAW),hammer},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.JIGSAW,2));table.items().setStackInSlot(22,hammer.copy());
            int original=table.items().getStackInSlot(22).getDamageValue();table.preview();table.preview();
            h.assertTrue(table.items().getStackInSlot(22).getDamageValue()==original,"preview never damages tool");
            h.assertTrue(table.craftOne(player).is(Items.EMERALD)&&table.items().getStackInSlot(22).getDamageValue()==original+GTToolType.HARD_HAMMER.damagePerCraft(),"actual tool returned with crafting wear");
        });
        table.load(new CompoundTag());
        probe(h,false,new ItemStack[]{new ItemStack(Items.HONEY_BOTTLE)},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.HONEY_BOTTLE,2));for(int slot:AdvancedCraftingTableBlockEntity.INPUTS)table.items().setStackInSlot(slot,new ItemStack(Items.DIRT,64));
            h.assertTrue(table.preview().is(Items.EMERALD)&&table.craftOne(player).isEmpty()&&table.items().getStackInSlot(21).getCount()==2,"no room for glass bottle rejects transaction without consuming honey");
            table.items().setStackInSlot(0,ItemStack.EMPTY);
            h.assertTrue(table.craftOne(player).is(Items.EMERALD)&&table.items().getStackInSlot(0).is(Items.GLASS_BOTTLE)&&table.items().getStackInSlot(21).getCount()==1,"remainder returned to available storage");
        });h.succeed();
    }
    @GameTest(template="test_empty")
    public static void blueprintRecordsVirtualPatternAndCannotBecomeFreeMaterials(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        probe(h,false,new ItemStack[]{new ItemStack(Items.STRUCTURE_VOID)},()->{
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID));
            table.items().setStackInSlot(30,new ItemStack(Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(GregTech.id("empty_blueprint")))));
            var menu=new AdvancedCraftingMenu(1,player.getInventory(),table);menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(table.items().getStackInSlot(30).hasTag(),"blueprint written by actual GUI gesture");table.storeGrid();
            h.assertTrue(table.items().getStackInSlot(21).isEmpty()&&table.preview().is(Items.EMERALD),"blueprint recipe survives clearing physical crafting grid");
            h.assertTrue(table.craftOne(player).is(Items.EMERALD)&&table.craftOne(player).isEmpty()&&table.preview().is(Items.EMERALD),"ghost remembers recipe but cannot provide ingredients");
            table.load(table.saveWithoutMetadata());h.assertTrue(table.items().getStackInSlot(21).isEmpty()&&table.preview().is(Items.EMERALD),"save preserves blueprint, never creates physical ghost items");
        });h.succeed();
    }
    @GameTest(template="test_empty")
    public static void automationModesFilterFlushAndCapabilityLifecycle(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();
        var cap=table.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN);var handler=cap.orElseThrow(IllegalStateException::new);
        h.assertTrue(handler.getSlots()==53,"drop slot plus 16 and 36 stores");
        h.assertTrue(!(handler instanceof net.minecraftforge.items.IItemHandlerModifiable),"automation cannot bypass filters using direct slot replacement");
        table.items().setStackInSlot(0,new ItemStack(Items.DIAMOND));h.assertTrue(handler.extractItem(1,1,false).isEmpty(),"stored inputs cannot be drained by automation");
        table.items().setStackInSlot(33,new ItemStack(Items.APPLE,3));h.assertTrue(handler.extractItem(0,2,false).getCount()==2,"drop slot is extractable");
        player.setItemInHand(InteractionHand.MAIN_HAND,tool(GTToolType.SCREWDRIVER));table.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        handler=table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElseThrow(IllegalStateException::new);
        h.assertTrue(handler.insertItem(2,new ItemStack(Items.DIAMOND),false).getCount()==1&&handler.insertItem(1,new ItemStack(Items.DIAMOND),false).isEmpty(),"diversity filter directs equal items to first occupied slot");
        player.setItemInHand(InteractionHand.MAIN_HAND,tool(GTToolType.MONKEY_WRENCH));table.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(!cap.isPresent()&&table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElseThrow(IllegalStateException::new).getSlots()==37,"upper monkey wrench disables 16-slot store");
        table.toggleMode(false,false);table.items().setStackInSlot(21,new ItemStack(Items.GOLD_INGOT,2));table.flushGrid();
        handler=table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElseThrow(IllegalStateException::new);h.assertTrue(handler.getSlots()==10&&handler.extractItem(1,2,false).getCount()==2,"flush exposes grid even when both stores blocked");
        h.assertTrue((table.modes()&16)==0,"flush ends when all crafting items are drained");
        int modes=table.modes();table.load(table.saveWithoutMetadata());h.assertTrue(table.modes()==modes&&table.items().getSlots()==71,"modes and inventory persist");
        table.invalidateCaps();h.assertTrue(!table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"removed capability cannot reappear");table.reviveCaps();h.assertTrue(table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"revive recreates capabilities");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sixtyMaterialRecipesMenusAndContentsDrop(GameTestHelper h){
        var table=place(h);var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        h.assertTrue(GTMiscBlocks.ADVANCED_CRAFTING_TABLES.size()==60,"original advanced table material family");
        for(var spec:GTStorageMetals.ALL){var block=GTMiscBlocks.advancedCraftingTable(spec.material());
            var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(GregTech.id("hand/storage/advanced_crafting_table_"+spec.suffix())).orElseThrow();
            var grid=new TransientCraftingContainer(new CraftingMenu(1,player.getInventory()),3,3);
            for(int slot:new int[]{0,2,6,8})grid.setItem(slot,GTItems.getStack(MaterialPrefix.plate,spec.material(),1));for(int slot:new int[]{3,5})grid.setItem(slot,GTItems.getStack(MaterialPrefix.screw,spec.material(),1));
            grid.setItem(1,tool(GTToolType.SCREWDRIVER));grid.setItem(4,new ItemStack(Items.CRAFTING_TABLE));grid.setItem(7,new ItemStack(Items.CHEST));
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.getResultItem(h.getLevel().registryAccess()).is(block.asItem())&&block.asItem().getDefaultInstance().getMaxStackSize()==16,"original plate/screw/chest/workbench recipe and stack size "+spec.suffix());
        }
        for(Direction face:Direction.values()){
            var result=table.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
            h.assertTrue(result.consumesAction()==(face==Direction.UP||face.getAxis()==Direction.Axis.Z),"top and front/back interaction "+face);
        }
        var menu=table.storageMenu(1,player.getInventory());menu.getSlot(35).set(new ItemStack(Items.NETHERITE_INGOT,7));
        h.assertTrue(table.items().getStackInSlot(70).getCount()==7,"rear menu maps slot 35 to physical slot 70");
        table.items().setStackInSlot(16,new ItemStack(Items.NETHERITE_INGOT,2));table.items().setStackInSlot(21,new ItemStack(Items.NETHERITE_INGOT,3));
        h.getLevel().destroyBlock(pos,true);table.dropContents();
        h.runAfterDelay(1,()->{int total=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.NETHERITE_INGOT)).mapToInt(e->e.getItem().getCount()).sum();h.assertTrue(total==12,"all real contents drop once, no preview output");h.succeed();});
    }
    @GameTest(template="test_empty")
    public static void workshopCraftingSuppliesMatchOriginalSlotsAndRandomStream(GameTestHelper h){
        var origin=h.absolutePos(BlockPos.ZERO);var pos=h.absolutePos(new BlockPos(1,1,1));
        var forms=new MaterialPrefix[]{MaterialPrefix.stick,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateCurved,MaterialPrefix.screw,MaterialPrefix.ring,MaterialPrefix.gearGt,MaterialPrefix.gearGtSmall};int[] base={32,32,32,16,16,8,1,8},range={33,33,33,49,49,25,4,25};
        for(long seed=0;seed<96;seed++){
            if(h.getLevel().getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity old)old.load(new CompoundTag());h.getLevel().removeBlock(pos,false);
            var data=new GTDungeonData(h.getLevel(),origin.getX(),origin.getY(),origin.getZ(),StoneType.LIMESTONE,StoneType.SLATE,3,new byte[5][5],2,2,0,new long[GTDungeonFeature.KEY_COUNT],new ItemStack[GTDungeonFeature.KEY_COUNT],new boolean[GTDungeonFeature.KEY_COUNT],new HashSet<>(),new HashSet<>(),RandomSource.create(seed));
            WorkshopCraftingSupplies.place(data,1,1,1);var table=(AdvancedCraftingTableBlockEntity)h.getLevel().getBlockEntity(pos);var rng=RandomSource.create(seed);
            ItemStack[] expected=new ItemStack[71];Arrays.fill(expected,ItemStack.EMPTY);
            for(int i=0;i<8;i++){int count=base[i]+rng.nextInt(range[i]);int slot=rng.nextInt(16);expected[slot]=GTItems.getStack(forms[i],Materials.Steel,count);}
            String lighter=rng.nextBoolean()?"lighter_full":"lighter_empty";expected[35+rng.nextInt(36)]=new ItemStack(Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(GregTech.id(lighter))));
            table.load(table.saveWithoutMetadata());for(int slot=0;slot<71;slot++)h.assertTrue(ItemStack.matches(expected[slot],table.items().getStackInSlot(slot)),"original parts and lighter after save: "+seed+" / "+slot);
            h.assertTrue(table.getBlockState().getBlock()==GTMiscBlocks.advancedCraftingTable(Materials.StainlessSteel)&&table.getBlockState().getValue(AdvancedCraftingTableBlock.FACING)==Direction.EAST,"original 5011 east");
            h.assertTrue(data.next(100000)==rng.nextInt(100000),"original conditional random call order");
        }h.succeed();
    }
}
