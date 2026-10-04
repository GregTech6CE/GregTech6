package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.content.multiblock.BoilerStructure;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;


/** Runtime structure inventory shared by independent JEI and EMI viewers. */
public class MultiblockInfoData {
    protected MultiblockInfoData() {}
public record Info(ItemStack controller, ItemStack wall, ItemStack base, Map<BlockPos,MultiblockLayout.Role> cells, boolean boiler, ItemStack extra) {
        public Info(ItemStack controller,ItemStack wall,ItemStack base,Map<BlockPos,MultiblockLayout.Role> cells,boolean boiler) {this(controller,wall,base,cells,boiler,ItemStack.EMPTY);}
    }
public static List<Info> recipes() {
        var cells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var cell:BoilerStructure.LAYOUT.cells()) cells.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        var boiler=new Info(new ItemStack(GTMultiblocks.LARGE_BOILER_MAIN.get()),new ItemStack(GTMultiblocks.BOILER_WALL.get(),25),
                new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),9),Map.copyOf(cells),true);
        var implosionCells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        implosionCells.put(BlockPos.ZERO,MultiblockLayout.Role.ITEM_IO);
        for(var cell:com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) implosionCells.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        var implosion=new Info(new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()),new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(),25),ItemStack.EMPTY,Map.copyOf(implosionCells),false);
        var result=new ArrayList<Info>(List.of(boiler,tank(3),tank(5),implosion));
        for(var entry:com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines()) {
            var block=entry.get();var layout=com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(block.basicSpec().machineName());if(layout==null)continue;
            var diagram=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();diagram.put(BlockPos.ZERO,MultiblockLayout.Role.ITEM_IO);
            var parts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();
            for(var cell:layout) {diagram.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());if(cell.role()!=MultiblockLayout.Role.AIR)parts.merge(cell.block(),1,Integer::sum);}
            var stacks=parts.entrySet().stream().map(e->new ItemStack(e.getKey(),e.getValue())).toList();
            result.add(new Info(new ItemStack(block),stacks.get(0),stacks.size()>1?stacks.get(1):ItemStack.EMPTY,Map.copyOf(diagram),false));
        }
        var gas=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        gas.put(BlockPos.ZERO,MultiblockLayout.Role.FLUID_INPUT);
        for(var cell:com.gregtech.gregtech.content.multiblock.TurbineStructure.LAYOUT.cells())gas.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        for(var entry:com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks())
            result.add(new Info(new ItemStack(entry.get()),new ItemStack(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.grade(entry.get()).wall(),35),ItemStack.EMPTY,Map.copyOf(gas),false));
        for (int grade = 0; grade < 4; grade++) {
            var original = com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17231 + grade);
            result.add(new Info(new ItemStack(original),
                    new ItemStack(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.grade(original).wall(), 35),
                    ItemStack.EMPTY, Map.copyOf(gas), false));
        }
        var exchanger=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(int y=0;y<2;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)exchanger.put(new BlockPos(x,y,z),y==0?MultiblockLayout.Role.FLUID_INPUT:MultiblockLayout.Role.CASING);
        result.add(new Info(new ItemStack(GTMultiblocks.HEAT_EXCHANGER_MAIN.get()),new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024),9),new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),8),Map.copyOf(exchanger),false));
        var drill=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(int y=-5;y<=0;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            drill.put(new BlockPos(x,y,z),y==-5?MultiblockLayout.Role.CASING:com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity.role(x,y,z));
        result.add(new Info(new ItemStack(GTMultiblocks.BEDROCK_DRILL_MAIN.get()),new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18026),35),new ItemStack(GTMultiblocks.BEDROCK_DRILL_WALL.get(),9),Map.copyOf(drill),false));
        var rod=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(int y=0;y<5;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)rod.put(new BlockPos(x,y,z),MultiblockLayout.Role.CASING);
        rod.put(new BlockPos(0,5,0),MultiblockLayout.Role.CASING);
        result.add(new Info(new ItemStack(GTMultiblocks.LIGHTNING_ROD_MAIN.get()),new ItemStack(GTMultiblocks.LIGHTNING_ROD_WALL.get(),26),new ItemStack(GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get(),18),Map.copyOf(rod),false,new ItemStack(GTMultiblocks.LIGHTNING_ROD_PILLAR.get())));
        for(var entry:com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.all().entrySet()) {
            var diagram=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
            diagram.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);
            var counts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();
            entry.getValue().forEach((pos, part) -> {
                MultiblockLayout.Role role = part == net.minecraft.world.level.block.Blocks.AIR
                        ? MultiblockLayout.Role.AIR
                        : entry.getKey() instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock axial
                        ? com.gregtech.gregtech.blockentity.machine.AxialGeneratorBlockEntity.role(pos, axial.grade().steam())
                        : entry.getKey() instanceof com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock
                        ? pos.getY() == 0 ? MultiblockLayout.Role.ENERGY_INPUT
                        : pos.getY() == 1 ? MultiblockLayout.Role.CRUCIBLE : MultiblockLayout.Role.ITEM_FLUID_IO
                        : entry.getKey() instanceof com.gregtech.gregtech.block.machine.VonDaGraaggControllerBlock
                        && pos.getY() <= 1 ? MultiblockLayout.Role.ENERGY_INPUT
                        : MultiblockLayout.Role.CASING;
                diagram.put(pos, role);
                if (role != MultiblockLayout.Role.AIR) counts.merge(part, 1, Integer::sum);
            });
            var parts=counts.entrySet().stream().map(e->new ItemStack(e.getKey(),e.getValue())).toList();
            result.add(new Info(new ItemStack(entry.getKey()),parts.get(0),parts.size()>1?parts.get(1):ItemStack.EMPTY,Map.copyOf(diagram),false));
        }
        var fusion=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        fusion.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);
        for(var cell:com.gregtech.gregtech.content.multiblock.FusionStructure.CELLS)
            fusion.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        result.add(new Info(new ItemStack(GTMultiblocks.FUSION_REACTOR_MAIN.get()),ItemStack.EMPTY,ItemStack.EMPTY,Map.copyOf(fusion),false));
        return result;
    }
private static Info tank(int size) {
        var cells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        cells.put(BlockPos.ZERO,MultiblockLayout.Role.FLUID_IO);
        HollowTankStructure.validate(BlockPos.ZERO,Direction.NORTH,size,p->true,
                p->{cells.put(p,MultiblockLayout.Role.FLUID_IO);return true;},
                p->{cells.put(p,MultiblockLayout.Role.AIR);return true;});
        int walls=(int)cells.values().stream().filter(role->role!=MultiblockLayout.Role.AIR).count()-1;
        return new Info(new ItemStack(size==3?GTMultiblocks.TANK_3X3.get():GTMultiblocks.TANK_5X5.get()),
                new ItemStack(size==3?GTMultiblocks.TANK_WALL.get():GTMultiblocks.TANK_WALL_DENSE.get(),walls),
                ItemStack.EMPTY,Map.copyOf(cells),false);
    }
public static ItemStack expectedPart(Info info,BlockPos pos) {
        if(pos.equals(BlockPos.ZERO))return info.controller();
        if(info.controller().is(GTMultiblocks.FUSION_REACTOR_MAIN.get().asItem()))
            for(var cell:com.gregtech.gregtech.content.multiblock.FusionStructure.CELLS)
                if(cell.at(BlockPos.ZERO,Direction.NORTH).equals(pos))return new ItemStack(cell.block());
        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var layout=com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(item.getBlock());
            if(layout!=null&&layout.containsKey(pos))return new ItemStack(layout.get(pos));
        }
        if(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller()))return new ItemStack(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.grade(((net.minecraft.world.item.BlockItem)info.controller().getItem()).getBlock()).wall());
        if(info.controller().is(GTMultiblocks.HEAT_EXCHANGER_MAIN.get().asItem()))return new ItemStack(pos.getY()==0||pos.getX()==0&&pos.getZ()==0?com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024):GTMultiblocks.HEAT_TRANSMITTER.get());
        if(info.controller().is(GTMultiblocks.BEDROCK_DRILL_MAIN.get().asItem()))return new ItemStack(pos.getY()==-5?net.minecraft.world.level.block.Blocks.BEDROCK:pos.getY()==-4?GTMultiblocks.BEDROCK_DRILL_WALL.get():com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18026));
        if(info.controller().is(GTMultiblocks.LIGHTNING_ROD_MAIN.get().asItem()))return new ItemStack(pos.getY()==5?GTMultiblocks.LIGHTNING_ROD_PILLAR.get():pos.getY()%2==0?GTMultiblocks.LIGHTNING_ROD_WALL.get():GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get());
        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine) {
            var cells=com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName());
            if(cells!=null)for(var cell:cells)if(cell.at(BlockPos.ZERO,Direction.NORTH).equals(pos))return new ItemStack(cell.block());
        }
        if(info.cells().get(pos)==MultiblockLayout.Role.AIR)return ItemStack.EMPTY;
        if(info.boiler() && info.cells().get(pos)==MultiblockLayout.Role.HEAT_INPUT)return info.base();
        return info.wall();
    }
    public static Map<BlockPos, ItemStack> previewBlocks(Info info) {
        var result = new LinkedHashMap<BlockPos, ItemStack>();
        info.cells().forEach((pos, role) -> {
            if (role != MultiblockLayout.Role.AIR) result.put(pos, expectedPart(info, pos).copyWithCount(1));
        });
        return Map.copyOf(result);
    }
    public static List<ItemStack> parts(Info info) {
        var counts = new LinkedHashMap<net.minecraft.world.item.Item, Integer>();
        previewBlocks(info).forEach((pos, stack) -> {
            if (!pos.equals(BlockPos.ZERO) && !stack.isEmpty()) counts.merge(stack.getItem(), 1, Integer::sum);
        });
        return counts.entrySet().stream().map(e -> new ItemStack(e.getKey(), e.getValue())).toList();
    }
}
