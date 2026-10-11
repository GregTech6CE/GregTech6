package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SourceFilterTests {
    private static final BlockPos P=new BlockPos(3,3,3);
    @GameTest(template="test_blueprint_empty") public static void mainFaceBypassAndDualRouting(GameTestHelper h){
        var state=GTMiscBlocks.FILTER_ITEMS.get().defaultBlockState().setValue(FilterBlock.FACING,Direction.UP).setValue(FilterBlock.SECONDARY,Direction.EAST).setValue(FilterBlock.SECONDARY_SET,true);
        h.setBlock(P,state);h.setBlock(P.above(),Blocks.CHEST);h.setBlock(P.east(),Blocks.CHEST);
        var filter=(FilterBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(P));filter.setTemplate(0,new ItemStack(Items.APPLE));
        var normal=filter.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.SOUTH).orElseThrow(IllegalStateException::new);
        var bypass=filter.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        h.assertTrue(normal.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"non-main face enforces whitelist");
        h.assertTrue(bypass.insertItem(0,new ItemStack(Items.DIAMOND,3),false).isEmpty(),"main face bypasses whitelist and accesses secondary neighbor");
        h.assertTrue(normal.getStackInSlot(0).isEmpty()&&bypass.getStackInSlot(0).getCount()==3,"distinct neighboring inventories");
        filter.toggleMode();h.assertTrue(bypass.extractItem(0,3,false).getCount()==3,"main face also bypasses extraction filtering");
        for(var direction:Direction.values()){
            var legacy=state.setValue(FilterBlock.FACING,direction).setValue(FilterBlock.SECONDARY_SET,false);
            h.assertTrue(FilterBlock.secondary(legacy)==direction.getOpposite(),"old block states retain opposite-neighbor routing: "+direction);
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void itemTemplateNbtWildcard(GameTestHelper h){
        h.setBlock(new BlockPos(1,2,1),GTMiscBlocks.FILTER_ITEMS.get());
        var filter=(FilterBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,2,1)));
        var plain=new ItemStack(Items.APPLE);var tagged=plain.copy();tagged.getOrCreateTag().putString("batch","a");
        filter.setTemplate(0,plain);h.assertTrue(filter.permitsItem(tagged),"untagged source template ignores candidate NBT");
        filter.setTemplate(0,tagged);h.assertTrue(!filter.permitsItem(plain)&&filter.permitsItem(tagged),"tagged source template compares NBT");
        var different=tagged.copy();different.getOrCreateTag().putString("batch","b");h.assertTrue(!filter.permitsItem(different),"different tagged item rejected");
        filter.toggleMode();h.assertTrue(filter.permitsItem(different)&&!filter.permitsItem(tagged),"blacklist reverses exact rule");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void prefixSelectionAndFluidPassthrough(GameTestHelper h){
        h.setBlock(P,GTMiscBlocks.FILTER_OREDICT.get());var filter=(FilterBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(P));
        var steel=com.gregtech.gregtech.content.material.Materials.Steel;var copper=com.gregtech.gregtech.content.material.Materials.Copper;
        var plate=GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.plate,steel,1);
        h.assertTrue(!filter.permitsItem(plate),"unconfigured source prefix blocks items");filter.toggleMode();
        h.assertTrue(!filter.permitsItem(plate),"unconfigured prefix still blocks items in blacklist mode");filter.clearFilter();filter.setTemplate(0,plate);
        h.assertTrue(filter.permitsItem(GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.plate,copper,1)),"prefix accepts same form of another material");
        h.assertTrue(!filter.permitsItem(GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.ingot,steel,1)),"shared material does not imply shared prefix");
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        h.assertTrue(filter.permitsFluid(water)&&filter.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.SOUTH).isPresent(),"source prefix filter permits fluid capability and all fluids");
        var player=h.makeMockPlayer();var menu=new com.gregtech.gregtech.client.gui.FilterMenu(1,player.getInventory(),filter);
        var client=new com.gregtech.gregtech.client.gui.FilterMenu(1,player.getInventory(),true);
        h.assertTrue(menu.slots.size()==37&&client.slots.size()==37&&menu.getSlot(1).y==50,"server/client single-template layout agrees");
        menu.setCarried(plate.copyWithCount(17));menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==17&&filter.templates().getItem(0).getCount()==1,"prefix selection is a non-consuming ghost");
        menu.setCarried(new ItemStack(Items.APPLE));menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(filter.selectedPrefix().equals("plate"),"unrecognized item cannot overwrite valid prefix");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void toolsClearAndSavedFilters(GameTestHelper h){
        h.setBlock(P,GTMiscBlocks.FILTER_ITEMS.get());var abs=h.absolutePos(P);var filter=(FilterBlockEntity)h.getLevel().getBlockEntity(abs);
        filter.setTemplate(0,new ItemStack(Items.APPLE));filter.toggleMode();
        var saved=filter.saveWithoutMetadata();filter.clearFilter();h.assertTrue(!filter.blacklist()&&filter.templates().isEmpty(),"clear resets mode and ghosts");filter.load(saved);
        h.assertTrue(filter.blacklist()&&!filter.permitsItem(new ItemStack(Items.APPLE)),"saved templates and mode reload");
        var state=h.getLevel().getBlockState(abs);var monkey=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        var player=h.makeMockPlayer();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,monkey);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).add(0,0,.5),Direction.SOUTH,abs,false);
        com.gregtech.gregtech.api.tool.ToolInteractions.use(state,h.getLevel(),abs,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(h.getLevel().getBlockState(abs).getValue(FilterBlock.SECONDARY_SET),"choosing unchanged secondary direction explicitly migrates old auto-facing state");
        var drops=net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs),h.getLevel(),abs,filter);
        h.assertTrue(drops.size()==1&&drops.get(0).getTag().getCompound("BlockEntityTag").getBoolean("gt.blacklist"),"breaking retains filter configuration, no ghost cargo drops");h.succeed();
    }
    @GameTest(template="test_empty") public static void filterManufacturingRoutesLoad(GameTestHelper h){
        for(String name:new String[]{"blank_cover","item_filter","fluid_filter","filter_items","filter_fluids","filter_items_fluids","filter_oredict","filter_items_reset","filter_fluids_reset","filter_items_fluids_reset","filter_oredict_reset"})
            h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","logistics/"+name)).isPresent(),"filter manufacturing/reset recipe loaded: "+name);
        var casing=GTBlocks.getStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.casingMachine,com.gregtech.gregtech.content.material.Materials.SteelGalvanized);
        h.assertTrue(com.gregtech.gregtech.data.MachineRecipeMaps.Welder.mRecipeList.stream().anyMatch(r->r.mOutputs.length>0&&r.mOutputs[0].is(casing.getItem())),"galvanized casing upstream is craftable");h.succeed();
    }
}
