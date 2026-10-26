package gregtech.engine.logistics;

/** Bounded packet planning for GT6 17997, including its soft limit and overvoltage branch. */
public final class LogisticsCorePowerRules {

    private LogisticsCorePowerRules() {
    }
    public static final long MIN = 256, RECOMMENDED = 512, MAX = 1024;

    public record Plan(long accepted, long energy, boolean explode) {
    }

    public static Plan inject(long energy, long capacity, long size, long amount, boolean execute) {
        var plan = gregtech.engine.energy.DirectMachinePowerRules.inject(energy, capacity, MAX, size, amount, execute);
        return new Plan(plan.accepted(), plan.energy(), plan.explode());
    }
}
