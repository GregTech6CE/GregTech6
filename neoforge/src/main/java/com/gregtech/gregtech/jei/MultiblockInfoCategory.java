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
public final class MultiblockInfoCategory extends MultiblockInfoData implements IRecipeCategory<MultiblockInfoCategory.Info> {

    public static final RecipeType<Info> TYPE=RecipeType.create("gregtech","multiblock_assembly",Info.class);
    private final IDrawable background,icon,slot;
    private static final class View { final PreviewCamera camera = new PreviewCamera(); int layer = -1; }
    private final Map<Info, View> views = new IdentityHashMap<>();
    private final Map<Info, Map<BlockPos, ItemStack>> models = new IdentityHashMap<>();
    private Map<BlockPos, ItemStack> cachedPreviewBlocks(Info info) {
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
        StructurePreview.draw(graphics,cachedPreviewBlocks(info),view.camera,view.layer<0?null:levels.get(view.layer));
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

}
