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
        int itemChecks=0,fluidChecks=0,powerChecks=0,actualPowerChecks=0;
        var deliveries=new JsonArray();receipt.add("longDistanceActualPowerDelivery",deliveries);
        var thermal=new JsonArray();receipt.add("longDistanceThermalBoundaries",thermal);
        for(var facing:Direction.values()){
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
            level.setBlock(middle.relative(facing.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP),block("long_dist_wire_lead").defaultBlockState(),3);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,3,false)==3,"unconnected material branch incorrectly breaks line");
            level.setBlock(middle.relative(facing.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP),Blocks.AIR.defaultBlockState(),3);powerChecks+=4;

            var storage=start.relative(facing.getOpposite(),5);level.setBlock(storage,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(storage,block("battery_box_ev").defaultBlockState().setValue(com.gregtech.gregtech.block.energy.EnergyNodeBlock.FACING,facing.getOpposite()),3);
            var batteryBox=(com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity)level.getBlockEntity(storage);
            require(batteryBox.installBattery(new ItemStack(BuiltInRegistries.ITEM.get(id("battery_lithium_cobalt_ev")))),"actual EV battery installation");
            batteryBox.batteryEnergy().tick(level.getGameTime(),null,null);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,2,false)==2&&batteryBox.stored()==0,"power simulation mutated real battery box");
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,2,true)==2&&batteryBox.stored()==3968,"actual positive packets or 64 EU loss differs");
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,-2048,1,true)==1&&batteryBox.stored()==5952,"actual signed packet delivery differs");
            var control=com.gregtech.gregtech.api.machine.MachineControl.find(transformer,null);require(control!=null,"source on/off interface missing");control.setEnabled(false);
            require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,facing,2048,1,true)==0&&batteryBox.stored()==5952,"disabled source transmits");
            transformer.rescan();require(transformer.isStopped(),"soft reset toggles stopped state");control.setEnabled(true);
            transformer.tickActivity();require(level.getBlockState(start).getValue(LongDistanceTransformerBlock.ACTIVITY)==2,"real activity does not display blinking state");
            for(int tick=0;tick<64;tick++)transformer.tickActivity();require(level.getBlockState(start).getValue(LongDistanceTransformerBlock.ACTIVITY)==0,"64 inactive ticks do not clear display");
            actualPowerChecks+=6;
            var delivery=new JsonObject();delivery.addProperty("facing",facing.getName());delivery.addProperty("inputEU",6144);delivery.addProperty("receivedEU",batteryBox.stored());delivery.addProperty("packets",3);delivery.addProperty("lostEU",192);deliveries.add(delivery);

            for(int i=0;i<=5;i++)level.setBlock(start.relative(facing.getOpposite(),i),Blocks.AIR.defaultBlockState(),3);
        }
        endpoints(server,receipt);
        forksAndDistance(level,receipt);
        receipt.addProperty("longDistanceTypedLines",typed);receipt.addProperty("longDistanceDrops",drops);
        receipt.addProperty("longDistanceItemChecks",itemChecks);receipt.addProperty("longDistanceFluidChecks",fluidChecks);receipt.addProperty("longDistancePowerChecks",powerChecks);receipt.addProperty("longDistanceActualPowerChecks",actualPowerChecks);
    }

    private static final java.util.List<String> ENDPOINTS=java.util.List.of("long_dist_endpoint_item","long_dist_endpoint_fluid","long_dist_transformer_ulv","long_dist_transformer_lv","long_dist_transformer_mv","long_dist_transformer_zpm","long_dist_transformer_uv");
    private static void endpoints(MinecraftServer server,JsonObject receipt){
        var level=server.overworld();var pos=new BlockPos(220,236,220);
        var actor=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("ff5f7dcb-286c-4275-b499-3438c68ce90c"),"LongEndpointCheckpoint"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(223,236,223,0,0);actor.getInventory().clearContent();
        int turns=0,soft=0,inspect=0,placements=0,data=0,unconnected=0;
        for(var name:ENDPOINTS){
            var b=block(name);var stack=new ItemStack(b);require(stack.getMaxStackSize()==16,"source endpoint stack limit "+name);
            var state=b.defaultBlockState();level.setBlock(pos,state,3);
            var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wrench);
            for(var direction:Direction.values()){
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),direction,pos,false);
                state=level.getBlockState(pos);var target=(com.gregtech.gregtech.api.tool.ToolInteractionTarget)b;
                var spec=target.toolInteraction(state,wrench);require(spec!=null&&spec.allows(state,direction),"missing native rotation spec "+name+" "+direction);
                if(b instanceof LongDistEndpointBlock endpoint)endpoint.interact(state,level,pos,actor,net.minecraft.world.InteractionHand.MAIN_HAND,hit);else ((LongDistanceTransformerBlock)b).interact(state,level,pos,actor,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                state=level.getBlockState(pos);require(state.getValue(spec.facing())==direction&&spec.activeFaces(state)==1<<direction.ordinal(),"wrench did not rotate native block and overlay declaration "+name+" "+direction);turns++;
            }
            require(wrench.getDamageValue()==600,"six source facing clicks must cost 600 durability "+name);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
            for(float pitch:new float[]{65,-65,64.99f,-64.99f}){
                actor.setXRot(pitch);var context=new net.minecraft.world.item.context.BlockPlaceContext(actor,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
                var placed=b.getStateForPlacement(context);Direction expected=pitch>=65?Direction.UP:pitch<=-65?Direction.DOWN:context.getHorizontalDirection().getOpposite();
                require(placed.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)==expected,"wrong source placement pitch "+name+" "+pitch);placements++;
            }
            actor.setXRot(0);
            for(var type:java.util.List.of(com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER,com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS)){
                var tool=com.gregtech.gregtech.item.GTToolItem.create(type,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
                if(b instanceof LongDistEndpointBlock endpoint)endpoint.interact(state,level,pos,actor,net.minecraft.world.InteractionHand.MAIN_HAND,hit);else ((LongDistanceTransformerBlock)b).interact(state,level,pos,actor,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                require(tool.getDamageValue()==(type==com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER?100:1),"wrong source tool-click cost "+name+" "+type);
                var control=com.gregtech.gregtech.api.machine.MachineControl.find(level.getBlockEntity(pos),null);require(control!=null&&control.enabled(),"soft reset toggles endpoint off "+name);
                if(type==com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER)soft++;else inspect++;
            }
            var original=level.getBlockEntity(pos);com.gregtech.gregtech.api.machine.MachineControl.find(original,null).setEnabled(false);
            if(original instanceof LongDistanceTransformerBlockEntity transformer){
                transformer.setStopped(false);require(transformer.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,state.getValue(LongDistanceTransformerBlock.FACING),transformer.voltage()*3,1,true)==0&&level.getBlockState(pos).is(b),"unconnected source incorrectly overcharges "+name);transformer.setStopped(true);unconnected++;
            }
            var saved=original.saveWithFullMetadata(level.registryAccess());
            var copy=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved,level.registryAccess());
            require(copy!=null&&!com.gregtech.gregtech.api.machine.MachineControl.find(copy,null).enabled(),"native stopped-state data round trip "+name);data++;
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        actor.getInventory().clearContent();
        receipt.addProperty("longDistanceEndpointWrenchChecks",turns);receipt.addProperty("longDistanceEndpointPlacementChecks",placements);receipt.addProperty("longDistanceEndpointSoftHammerChecks",soft);receipt.addProperty("longDistanceEndpointInspectChecks",inspect);receipt.addProperty("longDistanceEndpointNativeDataChecks",data);
        receipt.addProperty("longDistanceUnconnectedPowerChecks",unconnected);
    }
    private static void forksAndDistance(ServerLevel level,JsonObject receipt){
        var source=new BlockPos(1040,235,1040);var line=block("long_dist_pipe_item");var endpoint=block("long_dist_endpoint_item");
        build(level,source,Direction.WEST,false,line);
        var middle=source.east(2);var receiver=middle.north();var sender=middle.south();
        level.setBlock(receiver,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.SOUTH),3);
        level.setBlock(middle.north(2),Blocks.CHEST.defaultBlockState(),3);
        level.setBlock(source.east(5),Blocks.CHEST.defaultBlockState(),3);
        level.setBlock(sender,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.SOUTH),3);
        var first=items(level,source,Direction.UP);var second=items(level,sender,Direction.EAST);
        require(first!=null&&first.insertItem(0,new ItemStack(Items.DIAMOND,3),false).isEmpty(),"fork rejects reachable receiver");
        require(items(level,middle.north(2),Direction.SOUTH).getStackInSlot(0).getCount()==3,"source BFS did not choose nearest front-facing receiver");
        require(second!=null&&second.getSlots()==0,"competing sender stole claimed receiver");
        ((LongDistEndpointBlockEntity)level.getBlockEntity(receiver)).rescan();require(first.getSlots()>0,"receiver soft reset destroyed incoming ownership");
        level.setBlock(source,Blocks.AIR.defaultBlockState(),3);require(second.insertItem(0,new ItemStack(Items.DIAMOND,2),false).isEmpty(),"removed sender did not release receiver");
        require(items(level,middle.north(2),Direction.SOUTH).getStackInSlot(0).getCount()==5,"claim replacement lost item content");
        receipt.addProperty("longDistanceForkOwnershipChecks",6);

        source=new BlockPos(512,235,512);int length=40;
        level.setBlock(source,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST),3);
        for(int i=1;i<=length;i++)level.setBlock(source.east(i),line.defaultBlockState(),3);
        var remote=source.east(length+1);level.setBlock(remote,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST),3);
        level.setBlock(remote.east(),Blocks.CHEST.defaultBlockState(),3);
        for(var side:Direction.values())require(items(level,source,side)!=null&&items(level,source,side).getSlots()>0,"source pipeline rejects query side "+side);
        var pipe=items(level,source,Direction.EAST);require(pipe.insertItem(0,new ItemStack(Items.EMERALD,17),false).isEmpty(),"cross-chunk actual item insertion failed");
        require(items(level,remote.east(),Direction.WEST).getStackInSlot(0).getCount()==17,"cross-chunk delivery lost quantity");
        require(pipe.extractItem(0,7,false).getCount()==7&&items(level,remote.east(),Direction.WEST).getStackInSlot(0).getCount()==10,"cross-chunk extraction lost quantity");
        receipt.addProperty("longDistanceLoadedCrossChunkChecks",9);receipt.addProperty("longDistanceLoadedCrossChunkLineLength",length);
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

        var colors=new JsonObject();int endpoints=0,states=0;
        for(var name:ENDPOINTS){
            var b=block(name);var stack=new ItemStack(b);OriginFeedbackChecks.checkModel(mc,stack,false);
            int expected=b instanceof LongDistEndpointBlock endpoint?endpoint.material().getColor():((LongDistanceTransformerBlock)b).material().getColor();
            int actual=mc.getItemColors().getColor(stack,0);require((actual&0xffffff)==(expected&0xffffff),"endpoint item RGB "+name);require((mc.getItemColors().getColor(stack,1)&0xffffff)==0xffffff,"endpoint overlay tinted "+name);
            require((actual>>>24)==255,"Neo endpoint RGB has no opaque alpha "+name);
            var tooltip=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);require(tooltip.stream().noneMatch(c->c.getString().contains("tooltip.gregtech.")||c.getString().contains("material.gregtech.")),"endpoint tooltip language "+name);
            colors.addProperty(name,String.format(java.util.Locale.ROOT,"%06x",actual&0xffffff));endpoints++;
            for(var face:Direction.values())for(int activity=0;activity<(b instanceof LongDistanceTransformerBlock?4:1);activity++){
                var state=b.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,face);if(b instanceof LongDistanceTransformerBlock)state=state.setValue(LongDistanceTransformerBlock.ACTIVITY,activity);
                require((mc.getBlockColors().getColor(state,mc.level,mc.player.blockPosition(),0)&0xffffff)==(expected&0xffffff),"endpoint block RGB "+name+" "+face);
                var model=mc.getBlockRenderer().getBlockModel(state);var quads=model.getQuads(state,face,net.minecraft.util.RandomSource.create(0),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,net.minecraft.client.renderer.RenderType.cutout());
                require(quads.stream().anyMatch(q->q.getTintIndex()==0&&q.getSprite().contents().name().getPath().endsWith("colored/front")),"missing colored front on rotated endpoint "+name+" "+face);
                String texture=activity==0?"overlay":activity==1?"overlay_active":activity==2?"overlay_blinking":"overlay_unloaded";
                require(quads.stream().anyMatch(q->q.getTintIndex()==-1&&q.getSprite().contents().name().getPath().endsWith(texture+"/front")),"missing source display front "+name+" "+face+" "+activity);states++;
            }
        }
        receipt.add("longDistanceEndpointRGB",colors);receipt.addProperty("longDistanceEndpointModels",endpoints);receipt.addProperty("longDistanceEndpointFacingDisplayStates",states);

        receipt.addProperty("longDistanceModels",models);receipt.addProperty("longDistanceTooltips",tips);
    }
    static void render(net.minecraft.client.gui.GuiGraphics graphics,net.minecraft.client.Minecraft mc){
        graphics.fill(5,5,330,101,0xe0101010);int index=0;
        for(var spec:LongDistanceCatalog.LINES){int x=10+(index%11)*28,y=22+(index/11)*36;
            graphics.renderItem(new ItemStack(block(spec.id())),x,y);graphics.drawString(mc.font,Integer.toString(spec.sourceMeta()),x,y+17,0xffffff);index++;}
        graphics.drawString(mc.font,"GT6: 16 wires / 5 pipelines",10,9,0xffffff);
        graphics.fill(5,110,330,180,0xe0101010);graphics.drawString(mc.font,"GT6: Pt / W pipeline + EV - UV endpoints",10,114,0xffffff);
        for(int i=0;i<ENDPOINTS.size();i++){int x=10+i*43;graphics.renderItem(new ItemStack(block(ENDPOINTS.get(i))),x,137);graphics.drawString(mc.font,new String[]{"Pt","W","EV","IV","LuV","ZPM","UV"}[i],x,158,0xffffff);}
    }
}
