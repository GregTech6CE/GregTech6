package com.gregtech.gregtech.emi;
import com.gregtech.gregtech.jei.PreviewViewport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
/** Coordinates come from actual widget render poses, without depending on EMI internals. */
public final class EmiWidgetInput {
    private record Surface(EmiInteractiveWidget widget, PreviewViewport.Bounds area, org.joml.Matrix4f inverse) {
        org.joml.Vector3f local(double x,double y) {return inverse.transformPosition(new org.joml.Vector3f((float)x,(float)y,0));}
        boolean contains(double x, double y) { return x >= area.left() && x < area.right() && y >= area.top() && y < area.bottom(); }
    }
    private static final List<Surface> surfaces = new ArrayList<>();
    private static Screen frameScreen, captureScreen;
    private static Surface captured;
    private static int capturedButton;
    private EmiWidgetInput() {}
    public static void beginFrame(Screen screen) { frameScreen = screen; surfaces.clear(); if (captureScreen != screen) captured = null; }
    public static void rendered(EmiInteractiveWidget widget, GuiGraphics graphics) {
        var b = widget.getBounds();
        surfaces.add(new Surface(widget, PreviewViewport.screenBounds(graphics.pose().last().pose(), b.x(), b.y(), b.x()+b.width(), b.y()+b.height()), new org.joml.Matrix4f(graphics.pose().last().pose()).invert()));
    }
    public static boolean pressed(Screen screen, double x, double y, int button) {
        captured = null;
        if (screen != frameScreen) return false;
        for (int i = surfaces.size()-1; i >= 0; i--) {
            var surface = surfaces.get(i);
            var local=surface.local(x,y);
            if (surface.contains(x,y) && surface.widget().pressed(local.x(),local.y(),button)) {
                captured=surface; captureScreen=screen; capturedButton=button; return true;
            }
        }
        return false;
    }
    public static boolean released(Screen screen, int button) {
        boolean handled = captured != null && screen == captureScreen && button == capturedButton;
        if (handled) captured = null;
        return handled;
    }
    public static boolean dragged(Screen screen, int button, double dx, double dy) {
        if(captured==null||screen!=captureScreen||button!=capturedButton)return false;
        var delta=captured.inverse().transformDirection(new org.joml.Vector3f((float)dx,(float)dy,0));
        return captured.widget().dragged(button,delta.x(),delta.y());
    }
    public static boolean scrolled(Screen screen, double x, double y, double delta) {
        if (screen != frameScreen) return false;
        for(int i=surfaces.size()-1;i>=0;i--) {var surface=surfaces.get(i);var local=surface.local(x,y);
            if(surface.contains(x,y)&&surface.widget().scrolled(local.x(),local.y(),delta))return true;
        }
        return false;
    }
}
