import com.gregtech.gregtech.api.recipe.MachineWorkCost;

/** Observes maintained core arithmetic, not Minecraft ticks, inventory or saved-world behavior. */
class BasicMachineExecutionAudit {
    public static void main(String[] args) {
        observe("overshoot", 90, 100, 32);
        observe("exact", 68, 100, 32);
        observe("long-boundary", Long.MAX_VALUE - 2, Long.MAX_VALUE, 32);
        var ordinary = MachineWorkCost.calculate(32, 20, 4, false, 10000, 128, 512, false);
        var timed = MachineWorkCost.calculate(1, 20, 16, false, 10000, 1, 16, true, true);
        var overflow = MachineWorkCost.calculate(4096, Long.MAX_VALUE, 64, true, 2500, 512, 4096, true);
        System.out.println("COST|ordinary|" + ordinary.minimumPower() + "|" + ordinary.totalWork());
        System.out.println("COST|TU|" + timed.minimumPower() + "|" + timed.totalWork());
        System.out.println("COST|overflow-rejected|" + (overflow == null));
    }

    private static void observe(String name, long completed, long total, long supplied) {
        System.out.println("ADVANCE|" + name + "|" + completed + "|" + total + "|" + supplied
                + "|" + MachineWorkCost.advance(completed, total, supplied));
    }
}
