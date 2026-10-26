package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/** {@code GT6CEStartupEvents.machines(event => ...)} */
public final class GT6CEStartupEvents {
    public static final EventGroup GROUP = EventGroup.of("GT6CEStartupEvents");
    public static final EventHandler MACHINES = GROUP.startup("machines", () -> GT6CEMachinesEventJS.class);

    private GT6CEStartupEvents() {}
}
