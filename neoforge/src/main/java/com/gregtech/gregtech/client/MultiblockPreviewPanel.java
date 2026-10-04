package com.gregtech.gregtech.client;
import com.gregtech.gregtech.jei.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class MultiblockPreviewPanel {
    public static final int WIDTH=176, HEIGHT=174;
    public static int preferredHeight() {return Math.max(88,Math.min(HEIGHT,Minecraft.getInstance().getWindow().getGuiScaledHeight()-100));}
    private static final int[] POSITIONS={6,28,60,82,110,150};
    private static final String[] LABELS={"<",">","-","+","All","R"};
    public final PreviewCamera camera=new PreviewCamera();
    private final Map<BlockPos,ItemStack> blocks;
    private final List<Integer> levels;
    private final MultiblockInfoData.Info info;
    private final int height;
    private int layer=-1;
    public MultiblockPreviewPanel(MultiblockInfoData.Info info) {this(info,preferredHeight());}
    public MultiblockPreviewPanel(MultiblockInfoData.Info info,int availableHeight) {height=Math.max(88,Math.min(HEIGHT,availableHeight)); this.info=info;blocks=MultiblockInfoData.previewBlocks(info);levels=PreviewLayers.levels(info.cells().keySet()); }
    public void draw(GuiGraphics graphics) {
        StructurePreview.draw(graphics,blocks,camera,layer<0?null:levels.get(layer),0,20,WIDTH,previewBottom());
        var font=Minecraft.getInstance().font;
        for(int i=0;i<POSITIONS.length;i++) { int x=POSITIONS[i],w=i==4?34:18; graphics.fill(x,buttonTop(),x+w,buttonTop()+14,0xffd0d0d0);graphics.drawString(font,LABELS[i],x+4,buttonTop()+3,0xff333333,false); }
        graphics.drawString(font,Component.translatable(layer<0?"gregtech.jei.preview.all":"gregtech.jei.preview.layer",layer+1),6,height-24,0xff555555,false);
        graphics.drawString(font,Component.translatable("gregtech.jei.preview.help"),6,height-12,0xff555555,false);
    }
    public boolean click(double x,double y,int button) {
        if(button!=0||y<buttonTop()||y>=buttonTop()+14)return false;
        int action=-1;
        for(int i=0;i<POSITIONS.length;i++) if(x>=POSITIONS[i]&&x<POSITIONS[i]+(i==4?34:18))action=i;
        switch(action) {
            case 0 -> camera.rotate(-90,0);
            case 1 -> camera.rotate(90,0);
            case 2 -> {if(!levels.isEmpty())layer=layer<0?levels.size()-1:Math.floorMod(layer-1,levels.size());}
            case 3 -> {if(!levels.isEmpty())layer=(layer+1)%levels.size();}
            case 4 -> layer=-1;
            case 5 -> {camera.reset();layer=-1;}
            default -> {return false;}
        }
        return true;
    }
    public boolean drag(int button,double dx,double dy) { if(button==0)camera.rotate(dx*.7,dy*.7);else if(button==1)camera.pan(dx,dy);else return false;return true; }
    public void scroll(double delta) {camera.zoom(delta);}
    public List<Component> tooltip(double mouseY) {
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gregtech.jei.assembly_steam_turbine":"gregtech.jei.assembly_dynamo"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine && machine.basicSpec().machineName().equals("largemassfab"))
            return List.of(Component.translatable("gregtech.jei.assembly_matter"));
        if(mouseY>=height-24 && info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_fusion"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gregtech.jei.assembly_steam_turbine":"gregtech.jei.assembly_dynamo"));
        if(mouseY>=height-24 && info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17998).asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_lightning"));
        if(mouseY>=height-24 && info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17999).asItem()))
            return List.of(Component.translatable("gregtech.jei.assembly_drill"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem boilerItem
                && boilerItem.getBlock() instanceof com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock)
            return List.of(Component.translatable("gregtech.jei.assembly_boiler"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem tankItem
                && tankItem.getBlock() instanceof com.gregtech.gregtech.block.machine.TankControllerBlock tank
                && tank.valveSpec() != null)
            return List.of(Component.translatable("gregtech.jei.assembly_tank"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem block && com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(block.getBlock())!=null)
            return List.of(Component.translatable("gregtech.jei.assembly_legacy"));
        if(mouseY>=height-24)return List.of(Component.translatable(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller())?"gregtech.jei.assembly_gas_turbine":info.controller().is(com.gregtech.gregtech.registry.GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get().asItem())?"gregtech.jei.assembly_exchanger":info.controller().is(com.gregtech.gregtech.registry.GTMultiblocks.BEDROCK_DRILL_MAIN.get().asItem())||info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17999).asItem())?"gregtech.jei.assembly_drill":info.controller().is(com.gregtech.gregtech.registry.GTMultiblocks.LIGHTNING_ROD_MAIN.get().asItem())||info.controller().is(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17998).asItem())?"gregtech.jei.assembly_lightning":info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine && com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName())!=null?"gregtech.jei.assembly_large_machine":info.controller().is(RecipeMachines.machine("implosion_compressor_main").asItem())?"gregtech.jei.assembly_implosion":info.boiler()?"gregtech.jei.assembly_boiler":"gregtech.jei.assembly_tank"));
        return List.of();
    }
    public int height() {return height;}
    public int previewBottom() {return height-46;}
    public int buttonTop() {return height-43;}
    public int layer() {return layer;}
}
