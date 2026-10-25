package gregtech.jei;



import gregtech.internal.multiblock.*;

import gregtech.content.multiblock.BoilerStructure;

import gregtech.content.registry.GTMultiblocks;

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

        var boiler=new Info(new ItemStack(GTMultiblocks.findPart(17201).get()),new ItemStack(GTMultiblocks.TANK_WALL.get(),25),

                new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),9),Map.copyOf(cells),true);

        var implosionCells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        implosionCells.put(BlockPos.ZERO,MultiblockLayout.Role.ITEM_IO);

        for(var cell:gregtech.content.blockentity.machine.ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) implosionCells.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());



//? if >=1.21.1 {
/*        var implosion=new Info(new ItemStack(RecipeMachines.machine("implosion_compressor_main")),new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(),25),ItemStack.EMPTY,Map.copyOf(implosionCells),false);
*///?} else {
        var implosion=new Info(new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()),new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(),25),ItemStack.EMPTY,Map.copyOf(implosionCells),false);
//?}



        var result=new ArrayList<Info>(List.of(boiler,implosion));



//? if >=1.21.1 {
/*        for(var entry:gregtech.platform.neoforge.machine.BasicMachineRegistries.all()) {
*///?} else {
        for(var entry:gregtech.internal.machine.MachineRegistry.basicMachines()) {
//?}



            var block=entry.get();var layout=gregtech.content.multiblock.LargeMachineLayouts.cells(block.basicSpec().machineName());if(layout==null)continue;

            var diagram=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();diagram.put(BlockPos.ZERO,MultiblockLayout.Role.ITEM_IO);

            var parts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();

            for(var cell:layout) {diagram.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());if(cell.role()!=MultiblockLayout.Role.AIR)parts.merge(cell.block(),1,Integer::sum);}

            var stacks=parts.entrySet().stream().map(e->new ItemStack(e.getKey(),e.getValue())).toList();

            result.add(new Info(new ItemStack(block),stacks.get(0),stacks.size()>1?stacks.get(1):ItemStack.EMPTY,Map.copyOf(diagram),false));

        }

//? if >=1.21.1 {
/*        for (var entry : gregtech.platform.neoforge.machine.FuelGeneratorRegistration.all()) {
*///?} else {
        for (var entry : gregtech.content.registry.FuelGeneratorRegistration.all()) {
//?}

//? if >=1.21.1 {
/*            if (!entry.get().declaration().multi()) continue;
*///?} else {
            if (!entry.isPresent() || !entry.get().declaration().multi()) continue;
//?}

            var layout = gregtech.content.multiblock.LargeMachineLayouts.cells(entry.get().declaration().casing().machineType());

            if (layout == null) continue;

            var diagram = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();

            diagram.put(BlockPos.ZERO, MultiblockLayout.Role.FLUID_INPUT);

            var parts = new LinkedHashMap<net.minecraft.world.level.block.Block, Integer>();

            for (var cell : layout) {

                diagram.put(cell.at(BlockPos.ZERO, Direction.NORTH), cell.role());

                if (cell.role() != MultiblockLayout.Role.AIR) parts.merge(cell.block(), 1, Integer::sum);

            }

            var stacks = parts.entrySet().stream().map(e -> new ItemStack(e.getKey(), e.getValue())).toList();

            result.add(new Info(new ItemStack(entry.get()), stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0),

                    stacks.size() > 1 ? stacks.get(1) : ItemStack.EMPTY, Map.copyOf(diagram), false));

        }

        var gas=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        gas.put(BlockPos.ZERO,MultiblockLayout.Role.FLUID_INPUT);

        for(var cell:gregtech.content.multiblock.TurbineStructure.LAYOUT.cells())gas.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());

        for(var entry:gregtech.content.multiblock.GasTurbineDefinitions.blocks())

            result.add(new Info(new ItemStack(entry.get()),new ItemStack(gregtech.content.multiblock.GasTurbineDefinitions.grade(entry.get()).wall(),35),ItemStack.EMPTY,Map.copyOf(gas),false));

        for (int grade = 0; grade < 4; grade++) {

            var original = gregtech.content.multiblock.LargeMachineParts.block(17231 + grade);

            result.add(new Info(new ItemStack(original),

                    new ItemStack(gregtech.content.multiblock.GasTurbineDefinitions.grade(original).wall(), 35),

                    ItemStack.EMPTY, Map.copyOf(gas), false));

        }

        var exchanger=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        exchanger.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);

        for(var cell:gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)

            exchanger.put(new BlockPos(cell.right(),cell.up(),cell.back()),MultiblockLayout.Role.valueOf(cell.role().name()));

        result.add(new Info(new ItemStack(GTMultiblocks.HEAT_EXCHANGER_MAIN.get()),new ItemStack(gregtech.content.multiblock.LargeMachineParts.block(18024),9),new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),8),Map.copyOf(exchanger),false));



        var drill=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        drill.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);

        for(var cell:gregtech.content.multiblock.SpecialStructures.drillCells())

            drill.put(new BlockPos(cell.dx(),cell.dy(),cell.dz()),cell.cell().role());

        result.add(new Info(new ItemStack(GTMultiblocks.findPart(17999).get()),new ItemStack(gregtech.content.multiblock.LargeMachineParts.block(18026),35),new ItemStack(GTMultiblocks.DENSE_TITANIUM_WALL.get(),9),Map.copyOf(drill),false));

        var rod=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        rod.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);

        var rodLayout=gregtech.content.multiblock.SpecialStructures.lightningLayout(true);

        for(var cell:rodLayout.cells())rod.put(new BlockPos(cell.right(),cell.up(),cell.back()),MultiblockLayout.Role.valueOf(cell.role().name()));

        rod.put(new BlockPos(0,5,0),MultiblockLayout.Role.CASING);

        result.add(new Info(new ItemStack(GTMultiblocks.findPart(17998).get()),new ItemStack(GTMultiblocks.TUNGSTEN_WALL.get(),26),new ItemStack(GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get(),18),Map.copyOf(rod),false,new ItemStack(GTMultiblocks.LIGHTNING_ROD.get())));

        for(var entry:gregtech.content.multiblock.ControllerStructureLayouts.all().entrySet()) {

            var diagram=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

            diagram.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);

            var counts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();

            entry.getValue().forEach((pos, part) -> {

                MultiblockLayout.Role role = part == net.minecraft.world.level.block.Blocks.AIR

                        ? MultiblockLayout.Role.AIR

                        : entry.getKey() instanceof gregtech.content.block.machine.AxialGeneratorBlock axial

                        ? gregtech.content.blockentity.machine.AxialGeneratorBlockEntity.role(pos, axial.grade().steam())

                        : entry.getKey() instanceof gregtech.content.block.machine.LargeCrucibleControllerBlock

                        ? pos.getY() == 0 ? MultiblockLayout.Role.ENERGY_INPUT

                        : pos.getY() == 1 ? MultiblockLayout.Role.CRUCIBLE : MultiblockLayout.Role.ITEM_FLUID_IO

                        : entry.getKey() instanceof gregtech.content.block.machine.VonDaGraaggControllerBlock

                        && pos.getY() <= 1 ? MultiblockLayout.Role.ENERGY_INPUT

                        : MultiblockLayout.Role.CASING;

                diagram.put(pos, role);

                if (role != MultiblockLayout.Role.AIR) counts.merge(part, 1, Integer::sum);

            });

            var parts=counts.entrySet().stream().map(e->new ItemStack(e.getKey(),e.getValue())).toList();

            result.add(new Info(new ItemStack(entry.getKey()),parts.get(0),parts.size()>1?parts.get(1):ItemStack.EMPTY,Map.copyOf(diagram),false));

        }

        var logistics = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();

        logistics.put(BlockPos.ZERO, MultiblockLayout.Role.CASING);

        var logisticsCells = gregtech.content.multiblock.SpecialStructures.logisticsCells(Direction.NORTH);

        var logisticsParts = new LinkedHashMap<net.minecraft.world.level.block.Block, Integer>();

        for (var cell : logisticsCells) {
            logistics.put(new BlockPos(cell.dx(), cell.dy(), cell.dz()), cell.cell().role());
            var part = gregtech.content.multiblock.SpecialStructures.previewBlock(cell.cell());
            if (part != net.minecraft.world.level.block.Blocks.AIR) logisticsParts.merge(part, 1, Integer::sum);
        }

        var logisticsStacks = logisticsParts.entrySet().stream().map(e -> new ItemStack(e.getKey(), e.getValue())).toList();

        result.add(new Info(new ItemStack(GTMultiblocks.LOGISTICS_CORE.get()), logisticsStacks.get(0),
                logisticsStacks.size() > 1 ? logisticsStacks.get(1) : ItemStack.EMPTY, Map.copyOf(logistics), false));

        var fusion=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();

        fusion.put(BlockPos.ZERO,MultiblockLayout.Role.CASING);

        for(var cell:gregtech.content.multiblock.FusionStructure.CELLS)

            fusion.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());



//? if >=1.21.1 {
/*        result.add(new Info(new ItemStack(RecipeMachines.machine("fusion_reactor_main")),ItemStack.EMPTY,ItemStack.EMPTY,Map.copyOf(fusion),false));
*///?} else {
        result.add(new Info(new ItemStack(GTMultiblocks.FUSION_REACTOR_MAIN.get()),ItemStack.EMPTY,ItemStack.EMPTY,Map.copyOf(fusion),false));
//?}



        var canonical = new LinkedHashMap<net.minecraft.world.item.Item, Info>();

        for (var info : result) canonical.put(info.controller().getItem(), info);

        return List.copyOf(canonical.values());

    }

public static ItemStack expectedPart(Info info,BlockPos pos) {

        if(pos.equals(BlockPos.ZERO))return info.controller();



//? if >=1.21.1 {
/*        if(info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem()))
*///?} else {
        if(info.controller().is(GTMultiblocks.FUSION_REACTOR_MAIN.get().asItem()))
//?}



            for(var cell:gregtech.content.multiblock.FusionStructure.CELLS)

                if(cell.at(BlockPos.ZERO,Direction.NORTH).equals(pos))return new ItemStack(cell.block());

        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item) {

            var layout=gregtech.content.multiblock.ControllerStructureLayouts.cells(item.getBlock());

            if(layout!=null&&layout.containsKey(pos))return new ItemStack(layout.get(pos));

        }

        if(gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller()))return new ItemStack(gregtech.content.multiblock.GasTurbineDefinitions.grade(((net.minecraft.world.item.BlockItem)info.controller().getItem()).getBlock()).wall());



        if(info.controller().is(GTMultiblocks.HEAT_EXCHANGER_MAIN.get().asItem())) {
            for (var cell : gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
                if (new BlockPos(cell.right(), cell.up(), cell.back()).equals(pos))
                    return new ItemStack(gregtech.content.multiblock.LargeMachineParts.block(cell.part()));
            return ItemStack.EMPTY;
        }

        if(info.controller().is(GTMultiblocks.findPart(17999).get().asItem())) {
            for (var cell : gregtech.content.multiblock.SpecialStructures.drillCells())
                if (new BlockPos(cell.dx(), cell.dy(), cell.dz()).equals(pos))
                    return new ItemStack(gregtech.content.multiblock.SpecialStructures.previewBlock(cell.cell()));
            return ItemStack.EMPTY;
        }

        if(info.controller().is(GTMultiblocks.findPart(17998).get().asItem())) {
            if (pos.equals(new BlockPos(0, 5, 0))) return new ItemStack(gregtech.content.multiblock.LargeMachineParts.block(18104));
            boolean original = info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item
                    && item.getBlock() instanceof gregtech.content.block.machine.OriginalLightningRodControllerBlock;
            for (var cell : gregtech.content.multiblock.SpecialStructures.lightningLayout(original).cells())
                if (new BlockPos(cell.right(), cell.up(), cell.back()).equals(pos))
                    return new ItemStack(gregtech.content.multiblock.SpecialStructures.previewBlock(cell));
            return ItemStack.EMPTY;
        }

        if(info.controller().is(GTMultiblocks.LOGISTICS_CORE.get().asItem())) {
            for (var cell : gregtech.content.multiblock.SpecialStructures.logisticsCells(Direction.NORTH))
                if (new BlockPos(cell.dx(), cell.dy(), cell.dz()).equals(pos))
                    return new ItemStack(gregtech.content.multiblock.SpecialStructures.previewBlock(cell.cell()));
            return ItemStack.EMPTY;
        }

        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof gregtech.content.block.machine.FuelGeneratorBlock generator) {

            var cells=gregtech.content.multiblock.LargeMachineLayouts.cells(generator.declaration().casing().machineType());

            if(cells!=null)for(var cell:cells)if(cell.at(BlockPos.ZERO,Direction.NORTH).equals(pos))return new ItemStack(cell.block());

        }


        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock machine) {

            var cells=gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName());

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
