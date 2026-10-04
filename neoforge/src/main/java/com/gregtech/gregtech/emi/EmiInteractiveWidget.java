package com.gregtech.gregtech.emi;
import dev.emi.emi.api.widget.Widget;
/** Optional input surface; native screen events supply drag/scroll absent from EMI's Widget API. */
public abstract class EmiInteractiveWidget extends Widget {
    public boolean pressed(double x, double y, int button) { return false; }
    public boolean dragged(int button, double dx, double dy) { return false; }
    public boolean scrolled(double x, double y, double delta) { return false; }
}
