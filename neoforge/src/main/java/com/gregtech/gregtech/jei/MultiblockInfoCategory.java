package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.content.multiblock.BoilerStructure;
import com.gregtech.gregtech.registry.GTMultiblocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Assembly diagrams are derived from the same geometry used by the controllers. */
public final class MultiblockInfoCategory implements IRecipeCategory<MultiblockInfoCategory.Info> {
    public record Info(ItemStack controller, ItemStack wall, ItemStack base, Map<BlockPos,MultiblockLayout.Role> cells, boolean boiler, ItemStack extra) {
        public Info(ItemStack controller,ItemStack wall,ItemStack base,Map<BlockPos,MultiblockLayout.Role> cells,boolean boiler) {this(controller,wall,base,cells,boiler,ItemStack.EMPTY);}
    }
    public static final RecipeType<Info> TYPE=RecipeType.create("gregtech","multiblock_assembly",Info.class);
    private final IDrawable background,icon,slot;
    private static final class View { final PreviewCamera camera = new PreviewCamera(); int layer = -1; }
    private final Map<Info, View> views = new IdentityHashMap<>();
    private final Map<Info, Map<BlockPos, ItemStack>> models = new IdentityHashMap<>();
    private Map<BlockPos, ItemStack> previewBlocks(Info info) {
        return models.computeIfAbsent(info, key -> {
            var result = new LinkedHashMap<BlockPos, ItemStack>();
            key.cells().forEach((pos, role) -> {
                if (role != MultiblockLayout.Role.AIR) result.put(pos, expectedPart(key, pos));
            });
            return Map.copyOf(result);
        });
    }
    public MultiblockInfoCategory(IGuiHelper helper) {
        background=helper.createBlankDrawable(176,174);
        icon=helper.createDrawableItemStack(new ItemStack(GTMultiblocks.LARGE_BOILER_MAIN.get()));
        slot=helper.getSlotDrawable();
    }
    public static List<Info> recipes() {
        var cells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var cell:BoilerStructure.LAYOUT.cells()) cells.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        var boiler=new Info(new ItemStack(GTMultiblocks.LARGE_BOILER_MAIN.get()),new ItemStack(GTMultiblocks.BOILER_WALL.get(),25),
                new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),9),Map.copyOf(cells),true);
        var implosionCells=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        implosionCells.put(BlockPos.ZERO,MultiblockLayout.Role.ITEM_IO);
        for(var cell:com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) implosionCells.put(cell.at(BlockPos.ZERO,Direction.NORTH),cell.role());
        var implosion=new Info(new ItemStack(RecipeMachines.machine("implosion_compressor_main")),new ItemStack(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(),25),ItemStack.EMPTY,Map.copyOf(implosionCells),false);
        var result=new ArrayList<Info>(List.of(boiler,tank(3),tank(5),implosion));
        for(var entry:com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.all()) {
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
        result.add(new Info(new ItemStack(GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get()),new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024),9),new ItemStack(GTMultiblocks.HEAT_TRANSMITTER.get(),8),Map.copyOf(exchanger),false));
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
        result.add(new Info(new ItemStack(RecipeMachines.machine("fusion_reactor_main")),ItemStack.EMPTY,ItemStack.EMPTY,Map.copyOf(fusion),false));
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
    @Override public RecipeType<Info> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("gregtech.jei.assembly");}
    @Override public IDrawable getBackground() {return background;}
    @Override public IDrawable getIcon() {return icon;}
    @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, Info info, IFocusGroup focuses) {
        var view = new View();
        views.put(info, view);
        builder.addGuiEventListener(new mezz.jei.api.gui.inputs.IJeiGuiEventListener() {
            public net.minecraft.client.gui.navigation.ScreenRectangle getArea() {
                return new net.minecraft.client.gui.navigation.ScreenRectangle(PreviewViewport.LEFT, PreviewViewport.TOP,
                        PreviewViewport.RIGHT - PreviewViewport.LEFT, PreviewViewport.BOTTOM - PreviewViewport.TOP);
            }
            public boolean mouseClicked(double x,double y,int button) { return button==0 || button==1; }
            public boolean mouseReleased(double x,double y,int button) { return button==0 || button==1; }
            public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
                if(button==0) view.camera.rotate(dx*0.7,dy*0.7);
                else if(button==1) view.camera.pan(dx,dy);
                else return false;
                return true;
            }
            public boolean mouseScrolled(double x,double y,double delta) { view.camera.zoom(delta); return true; }
        });
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Info info,IFocusGroup focuses) {
        if(info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem())) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,1,1).setBackground(slot,-1,-1).addItemStack(info.controller());
            var counts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();
            for(var cell:com.gregtech.gregtech.content.multiblock.FusionStructure.CELLS)counts.merge(cell.block(),1,Integer::sum);
            int x=20;
            for(var entry:counts.entrySet()) {
                builder.addSlot(RecipeIngredientRole.INPUT,x,1).setBackground(slot,-1,-1).addItemStack(new ItemStack(entry.getKey(),entry.getValue()));x+=19;
            }
            return;
        }
        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine) {
            var layout=com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName());
            if(layout!=null) {
                var counts=new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();
                for(var cell:layout)if(cell.role()!=MultiblockLayout.Role.AIR)counts.merge(cell.block(),1,Integer::sum);
                builder.addSlot(RecipeIngredientRole.OUTPUT,1,1).setBackground(slot,-1,-1).addItemStack(info.controller());
                int x=20;for(var entry:counts.entrySet()) {builder.addSlot(RecipeIngredientRole.INPUT,x,1).setBackground(slot,-1,-1).addItemStack(new ItemStack(entry.getKey(),entry.getValue()));x+=19;}
                return;
            }
        }
        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem controllerItem) {
            var layout = com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(controllerItem.getBlock());
            if(layout != null) {
                var counts = new LinkedHashMap<net.minecraft.world.level.block.Block,Integer>();
                layout.values().forEach(part -> {
                    if(part != net.minecraft.world.level.block.Blocks.AIR) counts.merge(part, 1, Integer::sum);
                });
                builder.addSlot(RecipeIngredientRole.OUTPUT,1,1).setBackground(slot,-1,-1)
                        .addItemStack(info.controller());
                int x = 20;
                for(var entry:counts.entrySet()) {
                    builder.addSlot(RecipeIngredientRole.INPUT,x,1).setBackground(slot,-1,-1)
                            .addItemStack(new ItemStack(entry.getKey(),entry.getValue()));
                    x += 19;
                }
                return;
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT,6,1).setBackground(slot,-1,-1).addItemStack(info.controller());
        builder.addSlot(RecipeIngredientRole.INPUT,30,1).setBackground(slot,-1,-1).addItemStack(info.wall());
        if(!info.base().isEmpty()) builder.addSlot(RecipeIngredientRole.INPUT,54,1).setBackground(slot,-1,-1).addItemStack(info.base());
        if(!info.extra().isEmpty())builder.addSlot(RecipeIngredientRole.INPUT,78,1).setBackground(slot,-1,-1).addItemStack(info.extra());
    }
    private static List<Integer> levels(Info info) {return PreviewLayers.levels(info.cells().keySet());}
    private static int color(BlockPos position,MultiblockLayout.Role role) {
        if(position.equals(BlockPos.ZERO)) return 0xFFF2BE42;
        return switch(role) {
            case AIR -> 0xFFDADADA; case HEAT_INPUT, ENERGY_INPUT -> 0xFFD86F42;
            case ITEM_FLUID_INPUT, ITEM_FLUID_ENERGY_INPUT -> 0xFF3982B4;
            case ITEM_FLUID_OUTPUT -> 0xFF74AA9B;
            case ITEM_FLUID_ENERGY -> 0xFFD0A260;
            case FLUID_INPUT -> 0xFF3982B4; case FLUID_OUTPUT -> 0xFF74AA9B;
            default -> 0xFF797C85;
        };
    }
    @Override public void draw(Info info,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        var font=net.minecraft.client.Minecraft.getInstance().font;
        var view=views.computeIfAbsent(info,key->new View());
        var levels=levels(info);
        StructurePreview.draw(graphics,previewBlocks(info),view.camera,view.layer<0?null:levels.get(view.layer));
        String[] labels={"<", ">", "-", "+", "All", "R"};
        int[] positions={6,28,60,82,110,150};
        for(int i=0;i<labels.length;i++) {
            int x=positions[i],w=i==4?34:18;
            graphics.fill(x,131,x+w,145,0xFFD0D0D0);
            graphics.drawString(font,labels[i],x+4,134,0x333333,false);
        }
        graphics.drawString(font,Component.translatable(view.layer<0?"gregtech.jei.preview.all":"gregtech.jei.preview.layer",
                view.layer+1),6,150,0x555555,false);
        graphics.drawString(font,Component.translatable("gregtech.jei.preview.help"),6,162,0x555555,false);
    }
    @Override public boolean handleInput(Info info,double x,double y,com.mojang.blaze3d.platform.InputConstants.Key input) {
        if(input.getType()!=com.mojang.blaze3d.platform.InputConstants.Type.MOUSE || input.getValue()!=0 || y<131 || y>=145)return false;
        var view=views.computeIfAbsent(info,key->new View());
        int count=levels(info).size();
        if(x>=6&&x<24)view.camera.rotate(-90,0);
        else if(x>=28&&x<46)view.camera.rotate(90,0);
        else if(x>=60&&x<78&&count>0)view.layer=view.layer<0?count-1:Math.floorMod(view.layer-1,count);
        else if(x>=82&&x<100&&count>0)view.layer=(view.layer+1)%count;
        else if(x>=110&&x<144)view.layer=-1;
        else if(x>=150&&x<168){view.camera.reset();view.layer=-1;}
        else return false;
        return true;
    }
    @Override public List<Component> getTooltipStrings(Info info,IRecipeSlotsView slots,double mouseX,double mouseY) {
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gregtech.jei.assembly_steam_turbine":"gregtech.jei.assembly_dynamo"));
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine && machine.basicSpec().machineName().equals("largemassfab"))
            return List.of(Component.translatable("gregtech.jei.assembly_matter"));
        if(mouseY>=150 && info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_fusion"));
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gregtech.jei.assembly_steam_turbine":"gregtech.jei.assembly_dynamo"));
        if(mouseY>=150 && info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17998).asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_lightning"));
        if(mouseY>=150 && info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17999).asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_drill"));
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem boilerItem
                && boilerItem.getBlock() instanceof com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock)
            return List.of(Component.translatable("gregtech.jei.assembly_boiler"));
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem tankItem
                && tankItem.getBlock() instanceof com.gregtech.gregtech.block.machine.TankControllerBlock tank
                && tank.valveSpec() != null)
            return List.of(Component.translatable("gregtech.jei.assembly_tank"));
        if(mouseY>=150 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem block && com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(block.getBlock())!=null)
            return List.of(Component.translatable("gregtech.jei.assembly_legacy"));
        if(mouseY>=150)return List.of(Component.translatable(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller())?"gregtech.jei.assembly_gas_turbine":info.controller().is(GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get().asItem())?"gregtech.jei.assembly_exchanger":info.controller().is(GTMultiblocks.BEDROCK_DRILL_MAIN.get().asItem())||info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17999).asItem())?"gregtech.jei.assembly_drill":info.controller().is(GTMultiblocks.LIGHTNING_ROD_MAIN.get().asItem())||info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17998).asItem())?"gregtech.jei.assembly_lightning":info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine && com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName())!=null?"gregtech.jei.assembly_large_machine":info.controller().is(RecipeMachines.machine("implosion_compressor_main").asItem())?"gregtech.jei.assembly_implosion":info.boiler()?"gregtech.jei.assembly_boiler":"gregtech.jei.assembly_tank"));
        return List.of();
    }
    private static ItemStack expectedPart(Info info,BlockPos pos) {
        if(pos.equals(BlockPos.ZERO))return info.controller();
        if(info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem()))
            for(var cell:com.gregtech.gregtech.content.multiblock.FusionStructure.CELLS)
                if(cell.at(BlockPos.ZERO,Direction.NORTH).equals(pos))return new ItemStack(cell.block());
        if(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var layout=com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(item.getBlock());
            if(layout!=null&&layout.containsKey(pos))return new ItemStack(layout.get(pos));
        }
        if(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller()))return new ItemStack(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.grade(((net.minecraft.world.item.BlockItem)info.controller().getItem()).getBlock()).wall());
        if(info.controller().is(GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get().asItem()))return new ItemStack(pos.getY()==0||pos.getX()==0&&pos.getZ()==0?com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024):GTMultiblocks.HEAT_TRANSMITTER.get());
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
}
