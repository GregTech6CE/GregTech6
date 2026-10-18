package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.ElectricToolAssembly;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.*;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ChemicalBatteryTests {
    private static ChemicalBatteryBlock block(ChemicalBatterySpec.Chemistry chemistry,int tier){
        return GTChemicalBatteries.all().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.spec().chemistry()==chemistry&&b.spec().tier()==tier).findFirst().orElseThrow();
    }
    private static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry,int tier){return (ChemicalBatteryItem)block(chemistry,tier).asItem();}
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(Player p){return true;}
    },3,3);}
    @GameTest(template="test_empty")
    public static void allChemistriesHaveOriginalCapacitiesBoundsAndDrops(GameTestHelper h){
        h.assertTrue(GTChemicalBatteries.all().size()==25,"5 chemistries x 5 tiers");
        long[] multipliers={2000,4000,4000,64000,128000};int[] heights={8,11,11,11,13},insets={5,5,4,3,2};
        for(var entry:GTChemicalBatteries.all()) {
            var block=entry.get();var spec=block.spec();var state=block.defaultBlockState();
            h.assertTrue(spec.capacity()==(8L<<(spec.tier()*2))*multipliers[spec.chemistry().ordinal()],"original capacity "+spec.id());
            h.assertTrue(spec.display(0)==0&&spec.display(1)==1&&spec.display(spec.capacity()-1)==spec.scale()-1&&spec.display(spec.capacity())==spec.scale(),"GT6 display reserves empty/full marks");
            var bounds=state.getShape(h.getLevel(),h.absolutePos(BlockPos.ZERO)).bounds();
            h.assertTrue(bounds.minX==insets[spec.tier()]/16d&&bounds.maxY==heights[spec.tier()]/16d,"original bounds "+spec.id());
            h.assertTrue(state.getDestroySpeed(h.getLevel(),h.absolutePos(BlockPos.ZERO))==0.5f,"original hardness");
            h.assertTrue(h.getLevel().getServer().getLootData().getLootTable(state.getBlock().getLootTable())!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"self drop table generated");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void voltageStackLimitsAndFinalPacketMatchOriginal(GameTestHelper h){
        var item=item(ChemicalBatterySpec.Chemistry.LEAD_ACID,1);var stack=new ItemStack(item);
        h.assertTrue(stack.getMaxStackSize()==16,"empty stack size 16");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,15,1,null,null,true)==0,"LV rejects below 16 EU");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,65,1,null,null,true)==0,"LV rejects above 64 EU");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.HU,stack,32,1,null,null,true)==0,"wrong energy rejected");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,Long.MIN_VALUE,1,null,null,true)==0,"absolute-value overflow rejected");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,-32,Long.MAX_VALUE,null,null,false)==32&&item.stored(stack)==0,"simulation cap is recommended voltage in packets");
        item.doEnergyInjection(GregTechTags.Energy.EU,stack,-32,Long.MAX_VALUE,null,null,true);
        h.assertTrue(item.stored(stack)==1024&&stack.getMaxStackSize()==1,"charge and single-item limit");
        stack.setCount(2);h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU,stack,32,1,null,null,true)==0,"stacked charged items cannot transfer");stack.setCount(1);
        h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU,stack,-32,Long.MAX_VALUE,null,null,false)==32&&item.stored(stack)==1024,"extraction simulation unchanged");
        item.setCharge(stack,63999);
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,32,99,null,null,true)==1&&item.stored(stack)==64000,"last packet accepted, overflow energy discarded");
        h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,32,1,null,null,true)==0,"full battery refuses packets");
        item.setCharge(stack,16);
        h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU,stack,32,1,null,null,true)==0,"no partial extraction");
        h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU,stack,16,1,null,null,true)==1&&stack.getMaxStackSize()==16&&!stack.hasTag(),"empty canonical stack restored");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sneakPlacementSaveSyncAndActualBreakRetainCharge(GameTestHelper h){
        var item=item(ChemicalBatterySpec.Chemistry.LITHIUM_MANGANESE,1);var stack=new ItemStack(item);item.setCharge(stack,123456);
        var player=h.makeMockPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var base=new BlockPos(1,1,1);h.setBlock(base,Blocks.STONE);
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(base)).add(0,0.5,0),Direction.UP,h.absolutePos(base),false);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(!stack.useOn(context).consumesAction()&&h.getLevel().isEmptyBlock(h.absolutePos(base.above())),"ordinary right click does not place");
        player.setShiftKeyDown(true);
        h.assertTrue(stack.useOn(context).consumesAction(),"sneak places the battery");
        var pos=h.absolutePos(base.above());var be=(ChemicalBatteryBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(be!=null&&be.stored()==123456,"item charge transferred into placed battery");
        var saved=new ChemicalBatteryBlockEntity(pos,be.getBlockState());saved.load(be.saveWithoutMetadata());
        var client=new ChemicalBatteryBlockEntity(pos,be.getBlockState());client.load(be.getUpdateTag());
        h.assertTrue(saved.stored()==123456&&client.stored()==123456&&be.getUpdatePacket()!=null,"disk and client synchronization");
        h.assertTrue(!((Object)be instanceof com.gregtech.gregtech.api.energy.IEnergyBlock),"placed battery is passive, no fictional energy net output");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(item)&&item.stored(e.getItem())==123456),"real loot drop retains charge");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalRecipesConsumeCellsAndReturnCutter(GameTestHelper h){
        for(var entry:GTChemicalBatteries.all()) {
            var spec=entry.get().spec();var id=GregTech.id("chemical_batteries/"+spec.id());
            var recipe=(ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow();var grid=grid();
            int cutter=-1,cells=0;String[] expectedCells={"lead_acid_cell_filled","alkaline_button_cell_filled","nickel_cadmium_cell_filled","lithium_cobalt_cell_filled","lithium_manganese_cell_filled"};
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++) {
                var options=recipe.getIngredients().get(x+y*recipe.getWidth()).getItems();if(options.length==0)continue;
                var stack=options[0].copy();int slot=x+y*3;
                if(stack.getItem() instanceof GTToolItem){stack=GTToolHelper.displayTool(GTToolType.WIRE_CUTTER);cutter=slot;}
                if(stack.is(GTTechnological.get(expectedCells[spec.chemistry().ordinal()])))cells++;
                grid.setItem(slot,stack);
            }
            h.assertTrue(cells==new int[]{1,2,3,4,5}[spec.tier()],"original filled-cell count "+spec.id());
            h.assertTrue(recipe.matches(grid,h.getLevel())&&!recipe.allowMirror(),"unmirrored original recipe resolves "+spec.id());
            var output=recipe.assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(output.is(entry.get().asItem())&&!output.hasTag(),"uncharged battery crafted");
            var remaining=recipe.getRemainingItems(grid);
            if(spec.tier()==4)h.assertTrue(cutter<0,"GT6 EV pattern uses all nine cells and no crafting tool");
            else h.assertTrue(cutter>=0&&remaining.get(cutter).getDamageValue()==GTToolType.WIRE_CUTTER.damagePerCraft(),"cutter returned worn");
            for(int i=0;i<9;i++)if(i!=cutter)h.assertTrue(remaining.get(i).isEmpty(),"all components consumed");
            if(cutter>=0){grid.setItem(cutter,ItemStack.EMPTY);h.assertTrue(!recipe.matches(grid,h.getLevel()),"cannot omit the wire cutter");}
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realChargerAndToolAssemblyUseChemicalCapacity(GameTestHelper h){
        var pos=new BlockPos(1,1,1);h.setBlock(pos,GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());
        var charger=(com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var item=item(ChemicalBatterySpec.Chemistry.LEAD_ACID,1);
        charger.items().setStackInSlot(16,new ItemStack(item));
        h.assertTrue(charger.doEnergyInjection(GregTechTags.Energy.EU,Direction.DOWN,32,1,true)==1&&item.stored(charger.items().getStackInSlot(16))==32,"existing charger accepts the real LV chemical battery");
        for(var chemistry:ChemicalBatterySpec.Chemistry.values())for(var tool:ElectricToolAssembly.values()) {
            var battery=item(chemistry,1);var input=new ItemStack(battery);battery.setCharge(input,1234);
            var recipe=tool.recipe(Materials.Steel,input,"/test");
            h.assertTrue(recipe!=null,"all five LV chemistries accepted");
            var output=recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(tool.item().getEnergyCapacity(output,GregTechTags.Energy.EU)==battery.spec().capacity(),"correct chemistry capacity inherited");
            h.assertTrue(tool.item().getEnergyStored(output,GregTechTags.Energy.EU)==0,"GT6 assembly starts empty even with charged input");
            h.assertTrue(input.is(net.minecraft.tags.ItemTags.create(GregTech.id("rechargeable_batteries/lv"))),"machine recipe rechargeable tag");
            h.assertTrue(tool.recipe(Materials.Steel,new ItemStack(item(chemistry,2)),"/invalid")==null,"wrong voltage does not assemble LV tool");
        }
        h.succeed();
    }
}
