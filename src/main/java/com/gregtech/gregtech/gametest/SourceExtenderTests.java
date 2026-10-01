package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.SourceExtenderBlock;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import com.gregtech.gregtech.registry.GTMiscBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SourceExtenderTests {
    private static final BlockPos CENTER=new BlockPos(3,3,3);
    @GameTest(template="test_blueprint_empty") public static void sourceItemRoutingAndCapabilitySeparation(GameTestHelper h){
        for(var spec:ExtenderSpec.values()){
            var block=GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get();
            h.setBlock(CENTER,block.defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.UP).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST));
            for(var dir:Direction.values()){
                h.setBlock(CENTER.relative(dir),Blocks.CHEST);
                ((ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER.relative(dir)))).setItem(0,new ItemStack(Items.APPLE,dir.ordinal()+1));
            }
            var be=h.getLevel().getBlockEntity(h.absolutePos(CENTER));
            for(var side:Direction.values()){
                var items=be.getCapability(ForgeCapabilities.ITEM_HANDLER,side);
                h.assertTrue(items.isPresent()==spec.items,"item capability exists only on inventory variants: "+spec.id);
                h.assertTrue(be.getCapability(ForgeCapabilities.FLUID_HANDLER,side).isPresent()==spec.fluids,"fluid capability exists only on tank variants: "+spec.id);
                if(!spec.items)continue;
                var target=spec.bridge?side.getOpposite():side==Direction.UP?Direction.EAST:Direction.UP;
                var handler=items.orElseThrow(IllegalStateException::new);
                h.assertTrue(handler.getStackInSlot(0).getCount()==target.ordinal()+1,"exact source routing on "+spec.id+" / "+side);
                var sample=handler.getStackInSlot(0);sample.setCount(64);
                h.assertTrue(handler.getStackInSlot(0).getCount()==target.ordinal()+1,"inspection is defensive");
                h.assertTrue(handler.extractItem(0,1,true).getCount()==1&&handler.getStackInSlot(0).getCount()==target.ordinal()+1,"simulation cannot consume items");
            }
            h.assertTrue(!be.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"unsided access cannot bypass source routing");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void sourceFluidRoutingAndLiveReplacement(GameTestHelper h){
        var jug=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","fluid_jug"));
        for(var spec:ExtenderSpec.values())if(spec.fluids){
            var block=GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get();
            h.setBlock(CENTER,block.defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.UP).setValue(SourceExtenderBlock.SECONDARY,Direction.WEST));
            for(var dir:Direction.values()){
                h.setBlock(CENTER.relative(dir),Blocks.AIR);
                h.setBlock(CENTER.relative(dir),jug);
                h.getLevel().getBlockEntity(h.absolutePos(CENTER.relative(dir))).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new)
                        .fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100*(dir.ordinal()+1)),FluidAction.EXECUTE);
            }
            var be=h.getLevel().getBlockEntity(h.absolutePos(CENTER));
            for(var side:Direction.values()){
                var target=spec.bridge?side.getOpposite():side==Direction.UP?Direction.WEST:Direction.UP;
                var handler=be.getCapability(ForgeCapabilities.FLUID_HANDLER,side).orElseThrow(IllegalStateException::new);
                h.assertTrue(handler.getFluidInTank(0).getAmount()==100*(target.ordinal()+1),"source fluid routing: "+spec.id+" / "+side);
                handler.drain(25,FluidAction.SIMULATE);
                h.assertTrue(handler.getFluidInTank(0).getAmount()==100*(target.ordinal()+1),"fluid simulation does not drain");
            }
            var cached=be.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            var exit=spec.bridge?Direction.DOWN:Direction.WEST;h.setBlock(CENTER.relative(exit),Blocks.AIR);
            h.assertTrue(cached.getTanks()==0&&cached.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),FluidAction.EXECUTE)==0,"cached relay cannot fill removed tank");
            h.setBlock(CENTER.relative(exit),jug);
            h.assertTrue(cached.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),FluidAction.EXECUTE)==100,"same capability resolves replacement tank");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void sourceRelayCyclesTerminate(GameTestHelper h){
        var block=GTMiscBlocks.SOURCE_EXTENDERS.get(ExtenderSpec.COMBINED).get();
        h.setBlock(CENTER,block.defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.EAST).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST));
        h.setBlock(CENTER.east(),block.defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.WEST).setValue(SourceExtenderBlock.SECONDARY,Direction.WEST));
        var be=h.getLevel().getBlockEntity(h.absolutePos(CENTER));
        var items=be.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        h.assertTrue(items.getSlots()==0&&items.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"recursive item relay terminates without loss");
        var fluids=be.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        h.assertTrue(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),FluidAction.EXECUTE)==0,"recursive tank relay terminates");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void sourceToolsAndCrafting(GameTestHelper h){
        var steel=com.gregtech.gregtech.content.material.Materials.Steel;var wood=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,steel,wood);
        var monkey=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH,steel,wood);
        for(var spec:ExtenderSpec.values()){
            var block=GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get();var state=block.defaultBlockState();
            h.assertTrue(new ItemStack(block).getMaxStackSize()==16,"source stack size 16");
            if(spec.bridge)h.assertTrue(block.toolInteraction(state,wrench)==null,"bridge has no facing adjustment");
            else{
                h.assertTrue(block.toolInteraction(state,wrench).facing()==SourceExtenderBlock.FACING&&block.toolInteraction(state,monkey).facing()==SourceExtenderBlock.SECONDARY,"tools share overlay spec and select separate faces");
                for(var side:Direction.values())h.assertTrue(block.toolInteraction(state,monkey).allows(state,side),"secondary face supports all six directions");
            }
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","logistics/"+spec.id);
            var recipe=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(block.asItem()),"source survival recipe loaded: "+spec.id);
            var shaped=(com.gregtech.gregtech.recipe.ToolShapedRecipe)recipe;
            var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
                public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
            };
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            for(int y=0;y<shaped.getHeight();y++)for(int x=0;x<shaped.getWidth();x++){
                var ingredient=shaped.getIngredients().get(x+y*shaped.getWidth());if(ingredient.isEmpty())continue;
                var input=ingredient.getItems()[0].copy();
                if(input.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)input=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolHelper.getType(input),steel,wood);
                grid.setItem(x+y*3,input);
            }
            h.assertTrue(shaped.matches(grid,h.getLevel()),"source pattern can actually be crafted: "+spec.id);
            var remainder=shaped.getRemainingItems(grid);
            for(int slot=0;slot<9;slot++)if(grid.getItem(slot).getItem() instanceof com.gregtech.gregtech.item.GTToolItem)
                h.assertTrue(!remainder.get(slot).isEmpty()&&remainder.get(slot).getDamageValue()>0,"hammer and wrench remain with wear");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void doubleFacingToolExecutionAndSavedState(GameTestHelper h){
        var block=GTMiscBlocks.SOURCE_EXTENDERS.get(ExtenderSpec.COMBINED).get();h.setBlock(CENTER,block);
        var absolute=h.absolutePos(CENTER);var player=h.makeMockPlayer();
        var material=com.gregtech.gregtech.content.material.Materials.Steel;var wood=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,material,wood);
        var monkey=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH,material,wood);
        for(var side:Direction.values()){
            var state=h.getLevel().getBlockState(absolute);var before=state.getValue(SourceExtenderBlock.SECONDARY);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wrench);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(side.getStepX()*.5,side.getStepY()*.5,side.getStepZ()*.5),side,absolute,false);
            com.gregtech.gregtech.api.tool.ToolInteractions.use(state,h.getLevel(),absolute,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            state=h.getLevel().getBlockState(absolute);
            h.assertTrue(state.getValue(SourceExtenderBlock.FACING)==side&&state.getValue(SourceExtenderBlock.SECONDARY)==before,"wrench changes only main face: "+side);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,monkey);
            com.gregtech.gregtech.api.tool.ToolInteractions.use(state,h.getLevel(),absolute,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            state=h.getLevel().getBlockState(absolute);
            h.assertTrue(state.getValue(SourceExtenderBlock.FACING)==side&&state.getValue(SourceExtenderBlock.SECONDARY)==side,"monkey wrench changes only secondary face: "+side);
            var tag=net.minecraft.nbt.NbtUtils.writeBlockState(state);
            var decoded=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),tag);
            h.assertTrue(decoded==state,"both directions survive block-state save encoding");
        }
        h.succeed();
    }
}
