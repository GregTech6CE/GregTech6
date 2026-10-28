/* Gregorius Techneticies / GregTech-6 Team; LGPL-3.0-or-later.
 * MultiTileEntityBoilerTank.onTick2 steam transfer, source line 142. */
package gregtech.engine.energy;

/** Above 75% stored pressure, the source boiler transfers twice its nominal output. */
public final class BoilerSteamOutput {
    private BoilerSteamOutput() {}

    public static long offered(long stored, long capacity, long nominal) {
        long available = stored - capacity / 2;
        return available <= 0 ? 0 : Math.min(available, stored > capacity / 4 * 3 ? nominal * 2 : nominal);
    }
}
