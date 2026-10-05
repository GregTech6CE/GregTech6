package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.content.logistics.LongDistanceCatalog;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Finite actual-world native recipes and routing checks. Never shipped in a production jar. */
final class LongDistanceRuntimeSmoke {
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("gregtech",s);}
    private static Block block(String s){return BuiltInRegistries.BLOCK.get(id(s));}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Long distance: "+reason);}
    private static void build(ServerLevel level,BlockPos start,Direction facing,boolean fluid,Block line){
        Block endpoint=block(fluid?"long_dist_endpoint_fluid":"long_dist_endpoint_item");
        level.setBlock(start,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,facing),3);
        level.setBlock(start.relative(facing.getOpposite(),4),endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,facing),3);
        for(int i=1;i<=3;i++)level.setBlock(start.relative(facing.getOpposite(),i),line.defaultBlockState(),3);
    }
    private static IFluidHandler fluids(ServerLevel level,BlockPos pos,Direction side){return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,side);}
    private static net.neoforged.neoforge.items.IItemHandler items(ServerLevel level,BlockPos pos,Direction side){return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,pos,side);}
    static void server(MinecraftServer server,JsonObject receipt){
        var level=server.overworld();var start=new BlockPos(216,235,216);
        // The fixture loads its own small area; production scans never load chunks.
        for(int x=12;x<=14;x++)for(int z=12;z<=14;z++)level.getChunk(x,z);
        var crafting=new JsonArray();
        for(var name:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES)if(name.startsWith("long_distance/"))
            MachineFeedbackChecks.craft(server,name.substring(0,name.length()-5),name.substring(14,name.length()-5),crafting);
        receipt.add("longDistanceCrafting",crafting);
        int drops=0,typed=0;
        for(var spec:LongDistanceCatalog.LINES){
            var b=block(spec.id());require(b instanceof LongDistPipeBlock,"missing live line "+spec.id());
            var line=(LongDistPipeBlock)b;
            require(line.maximumTemperature()==spec.maximumTemperature()&&line.maximumVoltage()==spec.voltage(),"wrong native limits "+spec.id());
            if(spec.kind().equals("FLUID_PIPE"))require(com.gregtech.gregtech.api.material.GTMaterialRegistry.get(spec.material()).getMeltingPoint()==spec.maximumTemperature(),"source material melting value differs "+spec.id());
            require(com.gregtech.gregtech.loaders.b.OriginCreativeContents.family(b.asItem()).equals("long_distance_transport"),"wrong creative family "+spec.id());
            require(com.gregtech.gregtech.data.BlockHarvestPolicy.level(b)==3&&com.gregtech.gregtech.data.BlockHarvestPolicy.tool(b)==(line.isWire()?com.gregtech.gregtech.data.BlockHarvestPolicy.Tool.CUTTER:com.gregtech.gregtech.data.BlockHarvestPolicy.Tool.WRENCH),"wrong source harvest "+spec.id());
            level.setBlock(start,b.defaultBlockState(),3);
            var loot=Block.getDrops(b.defaultBlockState(),level,start,null);
            require(loot.size()==1&&loot.get(0).is(b.asItem())&&loot.get(0).getCount()==1,"wrong typed block drop "+spec.id());drops++;typed++;
        }
        int itemChecks=0,fluidChecks=0,powerChecks=0;
        var thermal=new JsonArray();receipt.add("longDistanceThermalBoundaries",thermal);
        for(var facing:Direction.Plane.HORIZONTAL){
            build(level,start,facing,false,block("long_dist_pipe_item"));
            var target=start.relative(facing.getOpposite(),5);level.setBlock(target,Blocks.CHEST.defaultBlockState(),3);
            var relay=items(level,start,facing);require(relay!=null&&relay.getSlots()>0,"item route absent "+facing);
            var diamonds=new ItemStack(Items.DIAMOND,3);
            require(relay.insertItem(0,diamonds,true).isEmpty()&&relay.getStackInSlot(0).isEmpty(),"item simulation modified storage");
            require(relay.insertItem(0,diamonds,false).isEmpty()&&relay.getStackInSlot(0).getCount()==3,"item forwarding lost content");
            require(relay.extractItem(0,2,false).getCount()==2&&relay.getStackInSlot(0).getCount()==1,"item extraction differs from original inventory delegate");
            var middle=start.relative(facing.getOpposite(),2);level.setBlock(middle,block("long_dist_pipe_fluid").defaultBlockState(),3);
            require(relay.getSlots()==0,"item/fluid crossover route");
            level.setBlock(middle,block("long_dist_pipe_item").defaultBlockState(),3);require(relay.getSlots()>0,"same-tick item reconnection stale");
            itemChecks+=5;
            for(var spec:LongDistanceCatalog.LINES)if(spec.kind().equals("FLUID_PIPE")){
                build(level,start,facing,true,block(spec.id()));
                // Replacing a drum with the same state keeps its previous fluid. Each case needs a fresh receiver.
                level.setBlock(target,Blocks.AIR.defaultBlockState(),3);
                level.setBlock(target,block("drum_adamantium").defaultBlockState(),3);
                var fluidRelay=fluids(level,start,facing);require(fluidRelay!=null&&fluidRelay.getTanks()>0,"fluid route absent "+spec.id());
                FluidStack below=FluidStack.EMPTY,at=FluidStack.EMPTY,above=FluidStack.EMPTY;
                for(var fluid:BuiltInRegistries.FLUID){
                    var entry=com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid);
                    if(entry==null||!entry.registryName().toLowerCase(java.util.Locale.ROOT).contains("molten"))continue;
                    if(entry.temperature()<spec.maximumTemperature()&&below.isEmpty())below=new FluidStack(fluid,250);
                    if(entry.temperature()==spec.maximumTemperature())at=new FluidStack(fluid,250);
                    if(entry.temperature()>spec.maximumTemperature()&&entry.temperature()<6000&&above.isEmpty())above=new FluidStack(fluid,250);
                }
                require(!below.isEmpty()&&!at.isEmpty()&&!above.isEmpty(),"missing independent thermal boundary fluids "+spec.id());
                require(fluidRelay.fill(below,IFluidHandler.FluidAction.SIMULATE)==250&&fluidRelay.getFluidInTank(0).isEmpty(),"cold fluid simulation differs "+spec.id()+" "+facing);
                require(fluidRelay.isFluidValid(0,at)&&fluidRelay.fill(at,IFluidHandler.FluidAction.SIMULATE)==250,"inclusive original temperature boundary blocked "+spec.id());
                require(!fluidRelay.isFluidValid(0,above)&&fluidRelay.fill(above,IFluidHandler.FluidAction.SIMULATE)==0&&fluidRelay.fill(above,IFluidHandler.FluidAction.EXECUTE)==0,"overtemperature fluid passed "+spec.id());
                require(fluidRelay.fill(at,IFluidHandler.FluidAction.EXECUTE)==250&&fluidRelay.getFluidInTank(0).getAmount()==250,"fluid delivery lost amount");
                require(fluidRelay.drain(250,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&fluidRelay.drain(at,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&fluidRelay.getFluidInTank(0).getAmount()==250,"fluid pipeline incorrectly drains receiver");
                level.setBlock(middle,block(spec.id().equals("long_dist_pipe_fluid")?"long_dist_pipe_tungsten":"long_dist_pipe_fluid").defaultBlockState(),3);
                require(fluidRelay.getTanks()==0&&fluidRelay.fill(below,IFluidHandler.FluidAction.EXECUTE)==0,"mixed materials connected");
                level.setBlock(middle,block(spec.id()).defaultBlockState(),3);require(fluidRelay.getTanks()>0,"same-tick fluid reconnection stale");
                fluidChecks+=7;
                var row=new JsonObject();row.addProperty("line",spec.id());row.addProperty("facing",facing.getName());row.addProperty("maximumK",spec.maximumTemperature());row.addProperty("boundaryFluid",BuiltInRegistries.FLUID.getKey(at.getFluid()).toString());
                thermal.add(row);
            }
            // Equal voltage does not mean equal source metadata/material.
            var sender=block("long_dist_transformer_ulv");
            level.setBlock(start,sender.defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,facing),3);
            level.setBlock(start.relative(facing.getOpposite(),4),sender.defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,facing),3);
            for(int i=1;i<=3;i++)level.setBlock(start.relative(facing.getOpposite(),i),block("long_dist_wire_ev").defaultBlockState(),3);
            var transformer=(LongDistanceTransformerBlockEntity)level.getBlockEntity(start);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,3,false)==3,"homogeneous wire simulation blocked");
            level.setBlock(middle,block("long_dist_wire_lead").defaultBlockState(),3);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,3,false)==0,"tin and lead connected despite distinct source metadata");
            level.setBlock(middle,block("long_dist_wire_ev").defaultBlockState(),3);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,3,false)==3,"wire route did not recover");
            level.setBlock(middle.above(),block("long_dist_wire_lead").defaultBlockState(),3);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,3,false)==3,"unconnected material branch incorrectly breaks line");
            level.setBlock(middle.above(),Blocks.AIR.defaultBlockState(),3);powerChecks+=4;
            for(int i=0;i<=5;i++)level.setBlock(start.relative(facing.getOpposite(),i),Blocks.AIR.defaultBlockState(),3);
        }
        receipt.addProperty("longDistanceTypedLines",typed);receipt.addProperty("longDistanceDrops",drops);
        receipt.addProperty("longDistanceItemChecks",itemChecks);receipt.addProperty("longDistanceFluidChecks",fluidChecks);receipt.addProperty("longDistancePowerChecks",powerChecks);
    }
    static void client(net.minecraft.client.Minecraft mc,JsonObject receipt){
        int models=0,tips=0;
        for(var spec:LongDistanceCatalog.LINES){
            var stack=new ItemStack(block(spec.id()));OriginFeedbackChecks.checkModel(mc,stack,false);
            require(!stack.getHoverName().getString().startsWith("block.gregtech."),"missing line language "+spec.id());
            var tooltip=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);
            require(tooltip.stream().noneMatch(c->c.getString().contains("tooltip.gregtech.long_distance")||c.getString().contains("material.gregtech.")),"missing tooltip translation "+spec.id());
            require(tooltip.size()>=(spec.kind().equals("ITEM_PIPE")?2:spec.kind().equals("WIRE")?5:3),"line has no real stats "+spec.id());models++;tips++;
        }
        receipt.addProperty("longDistanceModels",models);receipt.addProperty("longDistanceTooltips",tips);
    }
    static void render(net.minecraft.client.gui.GuiGraphics graphics,net.minecraft.client.Minecraft mc){
        graphics.fill(5,5,330,101,0xe0101010);int index=0;
        for(var spec:LongDistanceCatalog.LINES){int x=10+(index%11)*28,y=22+(index/11)*36;
            graphics.renderItem(new ItemStack(block(spec.id())),x,y);graphics.drawString(mc.font,Integer.toString(spec.sourceMeta()),x,y+17,0xffffff);index++;}
        graphics.drawString(mc.font,"GT6: 16 wires / 5 pipelines",10,9,0xffffff);
    }
}
