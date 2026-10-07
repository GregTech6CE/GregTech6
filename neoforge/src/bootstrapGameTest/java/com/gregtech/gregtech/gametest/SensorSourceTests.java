package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.sensor.SensorCatalog;
import com.gregtech.gregtech.block.sensor.SensorBlock;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_sensor_source")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class SensorSourceTests {
    @GameTest(template="test_empty",timeoutTicks=300)
    public static void original_boiler_builder_and_diagnostics(GameTestHelper h) {
        var world=h.getLevel();var pos=h.absolutePos(new BlockPos(7,3,7));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BoilerBuilder"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.offset(-4,0,-4)));
        for(var variant:com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs.all())for(var front:Direction.Plane.HORIZONTAL) {
            var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+variant.path()));
            var centre=pos.relative(front.getOpposite());
            world.setBlockAndUpdate(pos,block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,front));
            var boiler=(com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity)world.getBlockEntity(pos);
            var wand=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.BUILDER_WAND,
                    com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Heliodor"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            player.getInventory().clearContent();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
            player.getInventory().setItem(35,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER.get(),9));
            player.getInventory().setItem(34,new ItemStack(variant.wall(),25));
            h.assertTrue(useHeatTool(player,pos).consumesAction()&&!boiler.isStructureOk(),"Controller-local build is handled but cannot reach the full shell "+variant.path()+front);
            h.assertTrue(player.getInventory().getItem(35).getCount()==3&&player.getInventory().getItem(34).getCount()==15,
                    "First click consumes exactly six heat transmitters and ten walls in every facing");
            var part=(com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)world.getBlockEntity(centre);
            h.assertTrue(part!=null&&part.isBoundTo(pos)&&part.fluidToolHandler(Direction.DOWN,false).getTanks()==0,
                    "Incomplete boiler keeps part tool access without opening storage");
            useHeatTool(player,centre);
            useHeatTool(player,centre.east().above());
            useHeatTool(player,centre.above(2));
            h.assertTrue(boiler.isStructureOk()&&wand.getDamageValue()==4,"Four local clicks complete the source structure at one wear each");
            h.assertTrue(world.getBlockState(pos).is(block)&&world.getBlockState(centre.above()).isAir()
                    &&player.getInventory().getItem(35).isEmpty()&&player.getInventory().getItem(34).isEmpty(),
                    "Builder preserves controller and hollow centre; total consumption is nine transmitters and twenty-five walls");
            var messages=boiler.magnifyingGlassMessages();
            h.assertTrue(messages.size()==2&&messages.get(0).getString().equals("No Calcification in this Boiler")
                    &&messages.get(1).getString().equals("WARNING: NO WATER!!!"),"Source formed boiler override reports no scale and empty-water warning");
            var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10000);
            h.assertTrue(boiler.directFluidHandler().fill(water,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==10000,"Formed boiler admits actual native water");
            var data=new net.minecraft.nbt.CompoundTag();data.putInt("gt.efficiency",9876);data.putBoolean("gt.state.str",true);boiler.loadWithComponents(data,world.registryAccess());
            messages=boiler.magnifyingGlassMessages();
            h.assertTrue(messages.size()==2&&messages.get(0).getString().equals("Calcification: 1.24%")
                    &&messages.get(1).getString().startsWith("10_000 L of ")&&messages.get(1).getString().endsWith(" (Liquid)"),
                    "Source scale precision and actual water contents remain intact after state load");
            var glass=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS,
                    com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Glass"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,glass);
            h.assertTrue(useHeatTool(player,centre).consumesAction()&&glass.getDamageValue()==1,"Magnifier on a wall reaches boiler with one wear");
            world.setBlockAndUpdate(centre.above(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!boiler.isStructureOk()&&part.isBoundTo(pos),"Blocked hollow prevents formation without dropping valid bindings");
            h.assertTrue(boiler.magnifyingGlassMessages().get(0).getString().equals("Structure did not form!"),"Broken boiler does not report normal formed diagnostics");
            world.removeBlock(pos,false);
            for(var cell:com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.CHECK_ORDER)
                world.setBlockAndUpdate(centre.offset(cell.x(),cell.y(),cell.z()),Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void heat_exchanger_builder_and_magnifier(GameTestHelper h) {
        var world=h.getLevel();var pos=h.absolutePos(new BlockPos(7,2,7));
        var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:heat_exchanger_main"));
        world.setBlockAndUpdate(pos,block.defaultBlockState());
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity)world.getBlockEntity(pos);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HeatBuilder"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.offset(-3,0,-3)));
        var wand=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.BUILDER_WAND,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Heliodor"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        var glass=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Glass"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        h.assertTrue(!wand.isEmpty()&&!glass.isEmpty(),"Real registered tools exist");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var wall=com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024);
        var transmitter=com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18101);
        h.assertTrue(machine.magnifyingGlassMessages().get(0).getString().equals("Structure did not form!"),"Missing shell diagnostic");
        player.getInventory().setItem(35,new ItemStack(wall,9));
        player.getInventory().setItem(34,new ItemStack(transmitter,8));
        h.assertTrue(useHeatTool(player,pos).consumesAction(),"Builder is handled before ordinary plane copying");
        h.assertTrue(machine.isStructureOk()&&wand.getDamageValue()==1,"One source tool click builds all seventeen cells with one wear");
        h.assertTrue(player.getInventory().getItem(35).isEmpty()&&player.getInventory().getItem(34).isEmpty(),"Exactly nine walls and eight transmitters consumed");
        var messages=machine.magnifyingGlassMessages();
        h.assertTrue(messages.size()==3&&messages.get(0).getString().equals("Structure is formed already!")
                &&messages.get(1).getString().equals("Input: Empty")&&messages.get(2).getString().equals("Output: Empty"),"Formed diagnostic includes both original tanks");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,glass);
        h.assertTrue(useHeatTool(player,pos).consumesAction()&&glass.getDamageValue()==1,"Magnifier dispatch costs one durability");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var near=pos.offset(-1,0,-1);var far=pos.offset(1,0,1);var clicked=pos.north();
        world.setBlockAndUpdate(near,Blocks.AIR.defaultBlockState());world.setBlockAndUpdate(far,Blocks.AIR.defaultBlockState());
        h.assertTrue(!machine.isStructureOk(),"Broken shell is incomplete");
        var part=(com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)world.getBlockEntity(clicked);
        h.assertTrue(part.isBoundTo(pos)&&part.fluidToolHandler(Direction.NORTH,false).getTanks()==0,
                "Valid part retains tool ownership while incomplete shell cannot transport fluids");
        player.getInventory().setItem(35,new ItemStack(wall,2));
        useHeatTool(player,clicked);
        h.assertTrue(world.getBlockState(near).is(wall)&&world.getBlockState(far).isAir()&&player.getInventory().getItem(35).getCount()==1,
                "Part click only repairs the nearby hole and leaves the opposite edge two blocks away untouched");
        h.assertTrue(wand.getDamageValue()==2&&!machine.isStructureOk(),"Partial repair charges once and remains incomplete");
        useHeatTool(player,pos);
        h.assertTrue(machine.isStructureOk()&&wand.getDamageValue()==3&&player.getInventory().getItem(35).isEmpty(),"Controller click completes remaining repair");
        // With scarce material, source call order and reverse inventory order are observable.
        world.setBlockAndUpdate(pos.offset(-1,0,-1),Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(pos.offset(0,0,-1),Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(pos.offset(1,0,-1),Blocks.STONE.defaultBlockState());
        player.getInventory().setItem(35,new ItemStack(wall,1));
        useHeatTool(player,pos);
        h.assertTrue(world.getBlockState(pos.offset(-1,0,-1)).is(wall)&&world.getBlockState(pos.offset(0,0,-1)).isAir()
                &&world.getBlockState(pos.offset(1,0,-1)).is(Blocks.STONE),"First source cell wins scarce material and solid obstacle survives");
        player.getInventory().setItem(9,new ItemStack(wall,2));player.getInventory().setItem(35,new ItemStack(wall,1));
        useHeatTool(player,pos);
        h.assertTrue(player.getInventory().getItem(35).isEmpty()&&player.getInventory().getItem(9).getCount()==2,"Higher inventory slot is consumed first");
        world.setBlockAndUpdate(pos.offset(1,0,-1),Blocks.OAK_LEAVES.defaultBlockState());
        useHeatTool(player,pos);
        h.assertTrue(machine.isStructureOk()&&player.getInventory().getItem(9).getCount()==1,"Source easy replacement places into leaves at exact cell");
        // Creative builds without inventory and without wear; adventure cannot edit the shell.
        player.getInventory().setItem(9,ItemStack.EMPTY);
        world.setBlockAndUpdate(near,Blocks.AIR.defaultBlockState());
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        int wear=wand.getDamageValue();useHeatTool(player,pos);
        h.assertTrue(machine.isStructureOk()&&wand.getDamageValue()==wear,"Creative placement needs no source items and does not wear wand");
        world.setBlockAndUpdate(near,Blocks.AIR.defaultBlockState());
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.ADVENTURE);
        player.getInventory().setItem(35,new ItemStack(wall));useHeatTool(player,pos);
        h.assertTrue(world.getBlockState(near).isAir()&&player.getInventory().getItem(35).getCount()==1,"Denied placement neither consumes material nor creates a block");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        world.setBlockAndUpdate(near,wall.defaultBlockState());
        var data=new net.minecraft.nbt.CompoundTag();data.putBoolean("gt.state.str",false);
        machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.magnifyingGlassMessages().get(0).getString().equals("Structure did form just now!"),"Previously unformed diagnostic reports successful forced check");
        var cold=new com.gregtech.gregtech.api.fluid.FluidTankGT();
        cold.setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1),Long.MAX_VALUE);
        var coldData=new net.minecraft.nbt.CompoundTag();cold.writeToNBT(coldData,world.registryAccess());
        data.put("gt.cold",coldData);data.putBoolean("gt.state.str",true);machine.loadWithComponents(data,world.registryAccess());
        messages=machine.magnifyingGlassMessages();
        h.assertTrue(messages.size()==3&&messages.get(2).getString().startsWith("Output: 9_223_372_036_854_775_807 L of ")
                &&messages.get(2).getString().endsWith(" (Liquid)"),"Actual diagnostic preserves full long tank amount and source phase text");
        world.removeBlock(pos,false);
        var orphanMessages=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,glass);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked),Direction.UP,clicked,false));
        h.assertTrue(part.useMultiblockTool(context,orphanMessages)==1&&orphanMessages.size()==1
                &&orphanMessages.get(0).getString().equals("There is no Multiblock Controller for this Block."),"Orphan part reports original missing-controller diagnostic");
        h.succeed();
    }
    private static net.minecraft.world.InteractionResult useHeatTool(net.minecraft.world.entity.player.Player player,BlockPos pos) {
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
        var stack=player.getMainHandItem();
        return ((com.gregtech.gregtech.item.GTToolItem)stack.getItem()).onItemUseFirst(stack,context);
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void heat_exchanger_tool_ports_and_fixed_front(GameTestHelper h) {
        var world=h.getLevel();var pos=h.absolutePos(new BlockPos(7,2,7));
        var block=(com.gregtech.gregtech.block.machine.LargeHeatExchangerControllerBlock)BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse("gregtech:heat_exchanger_main"));
        var facing=com.gregtech.gregtech.block.machine.LargeHeatExchangerControllerBlock.FACING;
        h.assertTrue(block.defaultBlockState().getValue(facing)==Direction.DOWN,"Original default front is bottom");
        world.setBlockAndUpdate(pos,block.defaultBlockState().setValue(facing,Direction.WEST));
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity)world.getBlockEntity(pos);
        machine.onLoad();
        h.assertTrue(world.getBlockState(pos).getValue(facing)==Direction.DOWN,"Old horizontal native state is normalized on load");
        var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        for(var side:Direction.values()) {
            var spec=block.toolInteraction(block.defaultBlockState(),wrench);
            h.assertTrue(spec!=null,"Actual registered wrench receives a rotation declaration");
            h.assertTrue(spec.allows(side)==(side==Direction.DOWN)&&spec.activeFaces(block.defaultBlockState())==1,
                    "Wrench target and overlay only accept bottom "+side);
        }
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HeatWrench"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wrench);
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5),Direction.DOWN,pos,false);
        com.gregtech.gregtech.api.tool.ToolInteractions.use(world.getBlockState(pos),world,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(wrench.getDamageValue()==100,"Original valid unchanged facing click costs100 tool durability");
        var invalid=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+.5,pos.getZ()),Direction.NORTH,pos,false);
        com.gregtech.gregtech.api.tool.ToolInteractions.use(world.getBlockState(pos),world,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,invalid);
        h.assertTrue(wrench.getDamageValue()==100&&world.getBlockState(pos).getValue(facing)==Direction.DOWN,"Invalid front costs nothing and cannot rotate");
        world.setBlockAndUpdate(pos,world.getBlockState(pos).setValue(facing,Direction.WEST));
        com.gregtech.gregtech.api.tool.ToolInteractions.use(world.getBlockState(pos),world,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(wrench.getDamageValue()==200&&world.getBlockState(pos).getValue(facing)==Direction.DOWN,"Changed and unchanged valid facing clicks cost the same source wear");
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
            world.setBlockAndUpdate(pos.offset(cell.right(),cell.up(),cell.back()),com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(cell.part()).defaultBlockState());
        h.assertTrue(machine.isStructureOk(),"Source shell forms for attachment forwarding");
        var lava=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,5000);
        var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,250);
        var data=new net.minecraft.nbt.CompoundTag();
        data.put("gt.hot",lava.save(world.registryAccess()));
        data.put("gt.cold",water.save(world.registryAccess()));
        machine.loadWithComponents(data,world.registryAccess());
        var simulate=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        var tap=(com.gregtech.gregtech.block.tool.FluidAttachmentBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:tap"));
        var funnel=(com.gregtech.gregtech.block.tool.FluidAttachmentBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:fluid_funnel"));
        var tapped=tap.access(tap.attachedHandler(world,pos,Direction.NORTH));
        h.assertTrue(tapped.drain(lava,simulate).isEmpty(),"Typed drain must not skip the nonempty output to find matching input");
        h.assertTrue(tapped.drain(1000,simulate).getAmount()==250&&machine.tankAmount(1)==250,"Tap simulates only output without mutation");
        h.assertTrue(tapped.drain(1000,execute).getAmount()==250&&machine.tankAmount(0)==5000,"Output is drained first even when input is larger");
        h.assertTrue(machine.drain(1000,simulate).isEmpty(),"Automatic main drain never extracts unused fuel");
        h.assertTrue(tapped.drain(1000,simulate).getFluid()==net.minecraft.world.level.material.Fluids.LAVA&&machine.tankAmount(0)==5000,
                "Attachment falls back to hot input only after output becomes empty");
        h.assertTrue(tapped.drain(1000,execute).getAmount()==1000&&machine.tankAmount(0)==4000,"Actual tap removes hot fluid exactly once");
        h.assertTrue(funnel.access(funnel.attachedHandler(world,pos,Direction.UP)).fill(water,simulate)==0,"Funnel rejects nonfuel water");
        machine.loadWithComponents(data,world.registryAccess());
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS) {
            var partPos=pos.offset(cell.right(),cell.up(),cell.back());
            var handler=tap.access(tap.attachedHandler(world,partPos,Direction.NORTH));
            h.assertTrue(handler.drain(1000,simulate).getAmount()==250,"Every formed part delegates source tap access irrespective of pipe role "+cell);
            h.assertTrue(funnel.access(funnel.attachedHandler(world,partPos,Direction.NORTH)).fill(lava,simulate)==5000,
                    "Every formed part delegates the same recipe-gated input "+cell);
        }
        var transmitterPos=pos.above().east();
        var part=(com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)world.getBlockEntity(transmitterPos);
        h.assertTrue(world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,transmitterPos,Direction.NORTH).getTanks()==0,
                "Special attachment access does not open the transmitter's automatic fluid capability");
        var retained=tap.access(tap.attachedHandler(world,transmitterPos,Direction.NORTH));
        world.setBlockAndUpdate(pos.above().west(),Blocks.AIR.defaultBlockState());
        h.assertTrue(retained.drain(1000,execute).isEmpty()&&machine.tankAmount(1)==250,"Retained part tool handle closes immediately when shell breaks");
        world.removeBlock(pos,false);
        h.assertTrue(retained.getTanks()==0&&retained.drain(1000,execute).isEmpty(),"Removed owner is not retained by a tool handle");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void heat_exchanger_long_state_and_real_fuel(GameTestHelper h) {
        var world=h.getLevel();var pos=h.absolutePos(new BlockPos(7,2,7));
        var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:heat_exchanger_main"));
        world.setBlockAndUpdate(pos,block.defaultBlockState());
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity)world.getBlockEntity(pos);
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
            world.setBlockAndUpdate(pos.offset(cell.right(),cell.up(),cell.back()),com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(cell.part()).defaultBlockState());
        h.assertTrue(machine.isStructureOk(),"Original two-layer shell binds native parts");
        var recipe=com.gregtech.gregtech.data.FuelRecipeMaps.Hot.mRecipeList.stream().filter(x->x.mFluidInputs.length==1&&x.mFluidOutputs.length==1&&x.mEUt<0).findFirst().orElseThrow();
        var fuel=recipe.mFluidInputs[0].copy();fuel.setAmount(Math.min(163840,recipe.mFluidInputs[0].getAmount()*1000));
        int accepted=machine.fill(fuel,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        machine.tick();long used=accepted-machine.tankAmount(0),batches=used/recipe.mFluidInputs[0].getAmount();
        h.assertTrue(batches>0&&machine.tankAmount(1)==batches*recipe.mFluidOutputs[0].getAmount()
                &&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.HU,null)==batches*(-recipe.mEUt)*recipe.mDuration,
                "Actual hot recipe consumes matching fuel and conserves cold output and heat");
        var data=machine.saveWithoutMetadata(world.registryAccess());data.putLong("gt.output",500000000L);data.putShort("gt.eff",(short)6250);
        data.putString("gt.energy.emitted","ENERGY.ELECTRICITY");data.putLong("gt.energy",8_000_000_000L);
        var cold=new net.minecraft.nbt.CompoundTag();cold.put("Fluid",recipe.mFluidOutputs[0].save(world.registryAccess()));cold.putLong("Amount",5_000_000_000L);
        data.put("gt.cold",cold);machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.tankCapacity(0)==5_000_000_000L&&machine.tankCapacity(1)==Long.MAX_VALUE
                &&machine.tankAmount(1)==5_000_000_000L&&machine.settings().efficiency()==6250,
                "Native large configured capacity and long cold save do not truncate to int");
        var roundtrip=machine.saveWithoutMetadata(world.registryAccess());data=roundtrip;machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.tankAmount(1)==5_000_000_000L,"Native save/load preserves long output exactly");
        var emptyTank=new net.minecraft.nbt.CompoundTag();emptyTank.putLong("Amount",0);emptyTank.putLong("Capacity",5_000_000_000L);
        data.put("gt.hot",emptyTank);machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.tankAmount(0)==0&&machine.tankCapacity(0)==5_000_000_000L,"Empty long envelope retains capacity without decoding an absent fluid");
        h.assertTrue(machine.isEnergyEmittingTo(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP,false)
                &&!machine.isEnergyEmittingTo(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.DOWN,false)
                &&machine.getEnergySizeOutputMin(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP)==500000000L
                &&machine.getEnergySizeOutputMax(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP)==500000000L,
                "Source energy type, top emission and equal min/recommended/max");
        world.setBlockAndUpdate(pos.above().east(),Blocks.AIR.defaultBlockState());
        machine.tick();
        h.assertTrue(!machine.isStructureOk()&&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.EU,null)==7_500_000_000L,
                "Original buffered energy dissipates through all eight outlets even with broken shell /no receivers");
        var legacy=new net.minecraft.nbt.CompoundTag();legacy.putLong("gt.hu",12345L);
        legacy.put("gt.cold",recipe.mFluidOutputs[0].save(world.registryAccess()));data=legacy;machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.settings().rate()==16384&&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.HU,null)==12345L
                &&machine.tankAmount(1)==recipe.mFluidOutputs[0].getAmount(),"Old native int tank and gt.hu remain readable");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_raw_progress_and_spawner(GameTestHelper h) {
        var world=h.getLevel();
        var targetPos=h.absolutePos(new BlockPos(5,1,5));
        var controller=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:von_da_graagg_generator"));
        world.setBlockAndUpdate(targetPos,controller.defaultBlockState());
        var machine=(com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity)world.getBlockEntity(targetPos);
        var saved=machine.saveWithoutMetadata(world.registryAccess());
        saved.putInt("gt.range",255); machine.loadWithComponents(saved,world.registryAccess());
        h.assertTrue(machine.progressValue(0)==255 && machine.progressMaximum(0)==256
                && machine.gibblValue(0)==255000 && machine.gibblMaximum(0)==256000,"Source range / compression domains");
        var sensorPos=targetPos.west();
        for(var name:java.util.List.of("sensor_progressmeter","sensor_gibblometer")) {
            var block=(SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+name));
            world.setBlockAndUpdate(sensorPos,block.defaultBlockState());
            var meter=(SensorBlockEntity)world.getBlockEntity(sensorPos);meter.setInputSide(Direction.EAST);
            SensorBlockEntity.measure(world,sensorPos,block.defaultBlockState(),meter);
            h.assertTrue(meter.value()==255,"Actual sensor exposes raw255 not percent99 " + name);
        }
        world.setBlockAndUpdate(targetPos,Blocks.SPAWNER.defaultBlockState());
        var spawner=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)world.getBlockEntity(targetPos);
        var data=new net.minecraft.nbt.CompoundTag();data.putShort("Delay",(short)237);
        spawner.getSpawner().load(world,targetPos,data);
        var block=(SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:sensor_progressmeter"));
        world.setBlockAndUpdate(sensorPos,block.defaultBlockState());
        var meter=(SensorBlockEntity)world.getBlockEntity(sensorPos);meter.setInputSide(Direction.EAST);
        SensorBlockEntity.measure(world,sensorPos,block.defaultBlockState(),meter);
        h.assertTrue(meter.value()==237,"Actual vanilla countdown accessed through installed mixin");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_factories_input_and_pressure(GameTestHelper h) {
        for (var entry : SensorCatalog.ALL) {
            var block = (SensorBlock) BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+entry.id()));
            var be = (SensorBlockEntity)block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            h.assertTrue(be.kind().name().equals(entry.kind()) && be.inputSide() == Direction.SOUTH, "Actual source factory /default input " + entry.id());
        }
        var item = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:sensor_itemometer"));
        var pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,item.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pos.east());
        chest.setItem(0,new ItemStack(Items.IRON_INGOT,13));
        var sensor = (SensorBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(sensor.setInputSide(Direction.EAST) && !sensor.setInputSide(Direction.NORTH),"Independent input /front rejection");
        SensorBlockEntity.measure(h.getLevel(),pos,item.defaultBlockState(),sensor);
        h.assertTrue(sensor.value() == 13,"Actual selected east inventory, not default south");
        var saved = sensor.saveWithoutMetadata(h.getLevel().registryAccess()); sensor.resetInputSide(); sensor.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(sensor.inputSide() == Direction.EAST,"Native save/load retains independent input");
        item.toolStateChanged(h.getLevel(),pos,item.defaultBlockState());
        h.assertTrue(sensor.inputSide() == Direction.SOUTH,"Wrench display click restores opposite input");
        var old = saved.copy(); old.remove("gt.sensor_input"); sensor.loadWithComponents(old,h.getLevel().registryAccess());
        h.assertTrue(sensor.inputSide() == Direction.SOUTH,"Old native save without new key defaults behind display");

        var boilerBlock = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock boiler && boiler.variant().steamCapacity() > 1_234_567L)
                .findFirst().orElseThrow();
        var boilerPos = h.absolutePos(new BlockPos(1,1,4)); h.getLevel().setBlockAndUpdate(boilerPos,boilerBlock.defaultBlockState());
        var boiler = (com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity)h.getLevel().getBlockEntity(boilerPos);
        var steam = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("gregtech:steam"));
        var seeded = new com.gregtech.gregtech.api.fluid.FluidTankGT(boiler.variant().steamCapacity());
        seeded.setFluid(new net.neoforged.neoforge.fluids.FluidStack(steam,1_234_567));
        var tankTag = new net.minecraft.nbt.CompoundTag(); seeded.writeToNBT(tankTag,h.getLevel().registryAccess());
        var boilerSave = boiler.saveWithoutMetadata(h.getLevel().registryAccess()); boilerSave.put("gt.steam",tankTag); boiler.loadWithComponents(boilerSave,h.getLevel().registryAccess());
        h.assertTrue(boiler.gibblValue(0) == 1_234_567,"Actual source compressed steam amount");
        for (var path : java.util.List.of("sensor_gibblometer","sensor_kilogibblometer")) {
            var meterBlock = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+path));
            var meterPos = boilerPos.west(); h.getLevel().setBlockAndUpdate(meterPos,meterBlock.defaultBlockState());
            var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos); meter.setInputSide(Direction.EAST);
            SensorBlockEntity.measure(h.getLevel(),meterPos,meterBlock.defaultBlockState(),meter);
            h.assertTrue(meter.value() == (path.equals("sensor_gibblometer")?1234:1),"Actual source pressure scale " + path);
        }
        var bathBlock = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock basic
                        && basic.basicSpec().machineName().equals("bath") && basic.basicSpec().tier()==1).findFirst().orElseThrow();
        var bathPos = h.absolutePos(new BlockPos(1,1,7)); h.getLevel().setBlockAndUpdate(bathPos,bathBlock.defaultBlockState());
        var bath = (com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)h.getLevel().getBlockEntity(bathPos);
        bath.getTanksInput()[0].setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,5000));
        bath.getTanksOutput()[0].setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,7000));
        h.assertTrue(bath.gibblValue(0)==5000,"Source basic compression counts input only, excludes output");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_parts_delegate_sensor_measurements(GameTestHelper h) {
        for (var source : com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.ALL) {
            var block = (com.gregtech.gregtech.block.machine.MultiblockPortBlock) BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.parse("gregtech:" + source.path()));
            var part = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            h.assertTrue(part.getType().isValid(block.defaultBlockState()) && part.sensorTarget() == part,
                    "All45 native factory bindings, unbound measures itself " + source.path());
        }
        var block = (com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock) BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse("gregtech:stainless_steel_boiler_main_barometer"));
        var origin = h.absolutePos(new BlockPos(4,2,4));
        h.getLevel().setBlockAndUpdate(origin,block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,Direction.NORTH));
        var boiler = (com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity)h.getLevel().getBlockEntity(origin);
        for (var cell : com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs.LAYOUT.cells()) {
            var position = cell.at(origin,Direction.NORTH);
            if (position.equals(origin)) continue;
            var required = cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR ? Blocks.AIR
                    : cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT
                    ? com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER.get() : boiler.variant().wall();
            h.getLevel().setBlockAndUpdate(position,required.defaultBlockState());
        }
        var steam = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("gregtech:steam"));
        var seeded = new com.gregtech.gregtech.api.fluid.FluidTankGT(boiler.variant().steamCapacity());
        seeded.setFluid(new net.neoforged.neoforge.fluids.FluidStack(steam,1_234_567));
        var tank = new net.minecraft.nbt.CompoundTag(); seeded.writeToNBT(tank,h.getLevel().registryAccess());
        var saved = boiler.saveWithoutMetadata(h.getLevel().registryAccess()); saved.put("gt.steam",tank); boiler.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(boiler.isStructureOk(),"Real boiler binds its complete shell");
        var heatPos = origin.east().below().south();
        var heat = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) h.getLevel().getBlockEntity(heatPos);
        h.assertTrue(heat.sensorTarget() == boiler,"Source sensor unwraps an energy-only port to controller");
        var fluids = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,heatPos,Direction.EAST);
        h.assertTrue(fluids == null || fluids.getTanks() == 0,"Sensor delegation does not expose transport through heat port");
        var meterPos = heatPos.east();
        for (var name : java.util.List.of("sensor_gibblometer","sensor_kilogibblometer","sensor_fluidometer")) {
            var meterBlock = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+name));
            h.getLevel().setBlockAndUpdate(meterPos,meterBlock.defaultBlockState());
            var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos); meter.setInputSide(Direction.WEST);
            SensorBlockEntity.measure(h.getLevel(),meterPos,meterBlock.defaultBlockState(),meter);
            long expected = name.equals("sensor_gibblometer")?1234:name.equals("sensor_kilogibblometer")?1:1_234_567;
            h.assertTrue(meter.value() == expected,"Pressure /fluid reads controller, not restricted part " + name);
        }
        var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos);
        var broken = origin.west(); h.getLevel().setBlockAndUpdate(broken,Blocks.AIR.defaultBlockState());
        h.assertTrue(!boiler.isStructureOk() && heat.sensorTarget() == heat,"Invalid shell releases measurement binding");
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 0,"Released part cannot retain controller measurement");
        h.getLevel().setBlockAndUpdate(broken,boiler.variant().wall().defaultBlockState());
        h.assertTrue(boiler.isStructureOk(),"Repaired shell rebinds");
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 1_234_567,"Reformed shell restores real measurement");
        h.getLevel().setBlockAndUpdate(origin,Blocks.AIR.defaultBlockState());
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 0,"Removed controller leaves no stale measurement");
        h.succeed();
    }
}
