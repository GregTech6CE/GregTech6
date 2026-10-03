package com.gregtech.gregtech.content.energy;

/** MultiTileEntityBoilerTank:165-175, 202-215, 242: source boiler hazards in HU/steam units. */
public final class BoilerHazards {
    private BoilerHazards() {}
    public static double hotEnergy(long heat,long steam) { return (double)heat + steam / 2; }
    public static float contactDamage(long heat,long steam) {
        double hot = hotEnergy(heat,steam);
        return hot > 2000 ? (float)Math.min(10.0,hot/2000.0) : 0;
    }
    public static float descalingDamage(long heat,long steam,int efficiency,int pressure) {
        double hot = hotEnergy(heat,steam);
        return efficiency < 10000 && pressure <= 15 && hot > 2000 ? (float)(hot/2000.0) : 0;
    }
    public static float explosionPower(long steam) {
        return (float)Math.max(1.0,Math.sqrt(Math.max(0,steam))/100.0);
    }
}
