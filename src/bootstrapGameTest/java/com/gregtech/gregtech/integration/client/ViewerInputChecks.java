package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.emi.*;
import com.gregtech.gregtech.client.MultiblockPreviewPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.*;
import java.util.*;
/** Real screen surfaces and native input events; isolated finite feedback fixture only. */
final class ViewerInputChecks {
    static Object field(Object object,String name) throws ReflectiveOperationException {var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static void exercise(int stage,Screen screen,JsonObject receipt) {
        if(stage>3)return;
        try {
            var f=EmiWidgetInput.class.getDeclaredField("surfaces");f.setAccessible(true);
            var surfaces=(List<?>)f.get(null);
            if(surfaces.isEmpty())throw new IllegalStateException("Actual EMI widgets did not publish render coordinates");
            var surface=surfaces.get(0);var method=surface.getClass().getDeclaredMethod("widget");method.setAccessible(true);var widget=(EmiInteractiveWidget)method.invoke(surface);
            method=surface.getClass().getDeclaredMethod("area");method.setAccessible(true);var area=(com.gregtech.gregtech.jei.PreviewViewport.Bounds)method.invoke(surface);
            double x=area.left()+40,y=area.top()+25;
            if(stage<=2) {
                int initial=((Number)field(widget,"offset")).intValue();
                var scroll=new net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre(screen,x,y,-1);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(scroll);
                int max=((Number)field(widget,"max")).intValue();
                if(stage==2&&max<1)throw new IllegalStateException("Large loot source fixture must have hidden rows");
                if(!scroll.isCanceled()||((Number)field(widget,"offset")).intValue()!=Math.min(max,initial+1))throw new IllegalStateException("EMI native grid scroll did not change rows");
                // Query the actual widgets currently held by EMI's native screen.
                var page=screen.getClass().getDeclaredField("currentPage");page.setAccessible(true);
                var nativeGroup=((List<?>)page.get(screen)).get(0);var wf=nativeGroup.getClass().getDeclaredField("widgets");wf.setAccessible(true);
                var recipe=(LootEmiRecipe)field(nativeGroup,"recipe");
                var slots=((List<?>)wf.get(nativeGroup)).stream().filter(dev.emi.emi.api.widget.SlotWidget.class::isInstance).map(dev.emi.emi.api.widget.SlotWidget.class::cast).filter(s->s.getBounds().y()==40).toList();
                if(slots.isEmpty())throw new IllegalStateException("Actual EMI grid slots missing");
                var first=slots.get(0);int index=Math.min(max,initial+1)*9;
                if(!first.getStack().getEmiStacks().get(0).getKey().equals(recipe.getOutputs().get(index).getKey()))throw new IllegalStateException("EMI scrolled slot kept stale output");
                if(first.getTooltip(3,41).size()<com.gregtech.gregtech.client.LootPageCaptions.lines(recipe.group().rows().get(index)).size()+1)throw new IllegalStateException("EMI grouped tooltip missing rule attributes");
                var back=new net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre(screen,x,y,1);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(back);
                receipt.addProperty(stage==1?"emiMobGridScrolled":"emiLootGridScrolled",true);
                var mc=Minecraft.getInstance();org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),(area.left()+8)*mc.getWindow().getGuiScale(),(area.top()+8)*mc.getWindow().getGuiScale());
            } else {
                MultiblockPreviewPanel panel=null;
                for(var pf:widget.getClass().getDeclaredFields())if(pf.getType()==MultiblockPreviewPanel.class){pf.setAccessible(true);panel=(MultiblockPreviewPanel)pf.get(widget);}
                if(panel==null)throw new IllegalStateException("EMI did not use shared JEI preview panel");
                double yaw=panel.camera.yaw(),pitch=panel.camera.pitch();
                var press=new net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre(screen,x,y,0);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(press);
                var drag=new net.minecraftforge.client.event.ScreenEvent.MouseDragged.Pre(screen,x+20,y+10,0,20,10);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(drag);
                var release=new net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Pre(screen,x+200,y+200,0);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(release);
                if(!press.isCanceled()||!drag.isCanceled()||!release.isCanceled()||Math.abs(panel.camera.yaw()-yaw-14)>.01||Math.abs(panel.camera.pitch()-pitch-7)>.01)throw new IllegalStateException("EMI native preview rotation/capture mismatch");
                var panPress=new net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre(screen,x,y,1);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(panPress);
                var pan=new net.minecraftforge.client.event.ScreenEvent.MouseDragged.Pre(screen,x+8,y+4,1,8,4);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pan);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Pre(screen,x,y,1));
                if(!pan.isCanceled()||panel.camera.x()!=8||panel.camera.y()!=4)throw new IllegalStateException("EMI native preview pan mismatch");
                var zoom=new net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre(screen,x,y,2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(zoom);
                if(!zoom.isCanceled()||Math.abs(panel.camera.scale()-1.3225)>.001)throw new IllegalStateException("EMI native preview zoom mismatch");
                screen.mouseClicked(area.left()+84,area.top()+panel.buttonTop()+7-20,0);
                if(panel.layer()!=0)throw new IllegalStateException("EMI native layer button mismatch");
                screen.mouseClicked(area.left()+155,area.top()+panel.buttonTop()+7-20,0);
                if(panel.layer()!=-1||panel.camera.yaw()!=225||panel.camera.scale()!=1||panel.camera.x()!=0)throw new IllegalStateException("EMI native reset button mismatch");
                receipt.addProperty("emiNativePreviewInputChecks",5);
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(),0,0);
            }
        } catch(ReflectiveOperationException e){throw new IllegalStateException("EMI feedback fixture reflection failed",e);}
    }
}
