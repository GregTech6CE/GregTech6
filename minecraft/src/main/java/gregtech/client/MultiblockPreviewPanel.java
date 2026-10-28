package gregtech.client;
import gregtech.compat.jei.*;
import gregtech.util.PreviewCamera;
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
        graphics.drawString(font,Component.translatable(layer<0?"gt6ce.jei.preview.all":"gt6ce.jei.preview.layer",layer+1),6,height-24,0xff555555,false);
        var repeats=repeatNotes();
        if(!repeats.isEmpty())graphics.drawString(font,String.join("  ",repeats),6,height-36,0xff777777,false);
        graphics.drawString(font,Component.translatable("gt6ce.jei.preview.help"),6,height-12,0xff555555,false);
    }
    private List<String> repeatNotes() {
        if(!(info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item))return List.of();
        String machine=null;
        if(item.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock basic)machine=basic.basicSpec().machineName();
        else if(item.getBlock() instanceof gregtech.content.block.machine.BehaviorMachineBlock behavior)machine=behavior.accepted().declaration().casing().machineType();
        if(machine==null)return List.of();
        var layout=gregtech.content.multiblock.LargeMachineLayouts.layout(machine);
        if(layout==null||layout.repeats().isEmpty())return List.of();
        var out=new ArrayList<String>();
        for(var repeat:layout.repeats())out.add("↻ "+repeat.key()+" "+repeat.min()+"–"+(repeat.max()<0?"…":repeat.max()));
        return out;
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
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof gregtech.content.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gt6ce.jei.assembly_steam_turbine":"gt6ce.jei.assembly_dynamo"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock machine && machine.basicSpec().machineName().equals("largemassfab"))
            return List.of(Component.translatable("gt6ce.jei.assembly_matter"));
//? if >=1.21.1 {
        /*if(mouseY>=height-24 && info.controller().is(RecipeMachines.machine("fusion_reactor_main").asItem()))
*///?} else {
        if(mouseY>=height-24 && info.controller().is(gregtech.content.registry.GTMultiblocks.FUSION_REACTOR_MAIN.get().asItem()))
//?}
            return List.of(Component.translatable("gt6ce.jei.assembly_fusion"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem axialItem && axialItem.getBlock() instanceof gregtech.content.block.machine.AxialGeneratorBlock axial)
            return List.of(Component.translatable(axial.grade().steam()?"gt6ce.jei.assembly_steam_turbine":"gt6ce.jei.assembly_dynamo"));
        if(mouseY>=height-24 && info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17998).asItem()))
            return List.of(Component.translatable("gt6ce.jei.assembly_lightning"));
        if(mouseY>=height-24 && info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17999).asItem()))
            return List.of(Component.translatable("gt6ce.jei.assembly_drill"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem boilerItem
                && boilerItem.getBlock() instanceof gregtech.content.block.machine.OriginalLargeBoilerControllerBlock)
            return List.of(Component.translatable("gt6ce.jei.assembly_boiler"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem tankItem
                && tankItem.getBlock() instanceof gregtech.content.block.machine.TankControllerBlock tank
                && tank.valveSpec() != null)
            return List.of(Component.translatable("gt6ce.jei.assembly_tank"));
        if(mouseY>=height-24 && info.controller().getItem() instanceof net.minecraft.world.item.BlockItem block
                && (block.getBlock() instanceof gregtech.content.block.machine.LargeCrucibleControllerBlock
                    || block.getBlock() instanceof gregtech.content.block.machine.CokeOvenControllerBlock
                    || block.getBlock() instanceof gregtech.content.block.machine.VonDaGraaggControllerBlock))
            return List.of(Component.translatable("gt6ce.jei.assembly_legacy"));
//? if >=1.21.1 {
        /*if(mouseY>=height-24)return List.of(Component.translatable(gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller())?"gt6ce.jei.assembly_gas_turbine":info.controller().is(gregtech.content.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN.get().asItem())?"gt6ce.jei.assembly_exchanger":info.controller().is(gregtech.content.registry.GTMultiblocks.findPart(17999).get().asItem())||info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17999).asItem())?"gt6ce.jei.assembly_drill":info.controller().is(gregtech.content.registry.GTMultiblocks.findPart(17998).get().asItem())||info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17998).asItem())?"gt6ce.jei.assembly_lightning":info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock machine && gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName())!=null?"gt6ce.jei.assembly_large_machine":info.controller().is(RecipeMachines.machine("implosion_compressor_main").asItem())?"gt6ce.jei.assembly_implosion":info.boiler()?"gt6ce.jei.assembly_boiler":"gt6ce.jei.assembly_tank"));
*///?} else {
        if(mouseY>=height-24)return List.of(Component.translatable(gregtech.content.multiblock.GasTurbineDefinitions.isController(info.controller())?"gt6ce.jei.assembly_gas_turbine":info.controller().is(gregtech.content.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN.get().asItem())?"gt6ce.jei.assembly_exchanger":info.controller().is(gregtech.content.registry.GTMultiblocks.findPart(17999).get().asItem())||info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17999).asItem())?"gt6ce.jei.assembly_drill":info.controller().is(gregtech.content.registry.GTMultiblocks.findPart(17998).get().asItem())||info.controller().is(gregtech.content.multiblock.LargeMachineParts.block(17998).asItem())?"gt6ce.jei.assembly_lightning":info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock machine && gregtech.content.multiblock.LargeMachineLayouts.cells(machine.basicSpec().machineName())!=null?"gt6ce.jei.assembly_large_machine":info.controller().is(gregtech.content.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get().asItem())?"gt6ce.jei.assembly_implosion":info.boiler()?"gt6ce.jei.assembly_boiler":"gt6ce.jei.assembly_tank"));
//?}
        return List.of();
    }
    public int height() {return height;}
    public int previewBottom() {return height-46;}
    public int buttonTop() {return height-43;}
    public int layer() {return layer;}
}
