package com.gregtech.gregtech.content.logistics;


/** The GT6 1086-1099 cover family; display covers are reserved for W4. */
public enum LogisticsCoverDefinition {
    CPU_LOGIC("logistics_display_cpu_logic", Role.DISPLAY, Channel.NONE, false, false),
    CPU_CONTROL("logistics_display_cpu_control", Role.DISPLAY, Channel.NONE, false, false),
    CPU_STORAGE("logistics_display_cpu_storage", Role.DISPLAY, Channel.NONE, false, false),
    CPU_CONVERSION("logistics_display_cpu_conversion", Role.DISPLAY, Channel.NONE, false, false),
    FLUID_EXPORT("filtered_logistics_export_bus_fluid", Role.EXPORT, Channel.FLUID, true, false),
    FLUID_IMPORT("filtered_logistics_import_bus_fluid", Role.IMPORT, Channel.FLUID, true, false),
    FLUID_STORAGE("filtered_logistics_storage_bus_fluid", Role.STORAGE, Channel.FLUID, true, false),
    ITEM_EXPORT("filtered_logistics_export_bus_item", Role.EXPORT, Channel.ITEM, true, true),
    ITEM_IMPORT("filtered_logistics_import_bus_item", Role.IMPORT, Channel.ITEM, true, true),
    ITEM_STORAGE("filtered_logistics_storage_bus_item", Role.STORAGE, Channel.ITEM, true, false),
    GENERIC_EXPORT("generic_logistics_export_bus", Role.EXPORT, Channel.BOTH, false, true),
    GENERIC_IMPORT("generic_logistics_import_bus", Role.IMPORT, Channel.BOTH, false, true),
    GENERIC_STORAGE("generic_logistics_storage_bus", Role.STORAGE, Channel.BOTH, false, false),
    ITEM_DUMP("logistics_dump_bus_item", Role.DUMP, Channel.ITEM, false, false);

    public enum Role { IMPORT, EXPORT, STORAGE, DUMP, DISPLAY }
    public enum Channel { ITEM, FLUID, BOTH, NONE }

    private final String id;
    private final Role role;
    private final Channel channel;
    private final boolean filtered;
    private final boolean targetStackSize;

    LogisticsCoverDefinition(String id, Role role, Channel channel, boolean filtered, boolean targetStackSize) {
        this.id = id;
        this.role = role;
        this.channel = channel;
        this.filtered = filtered;
        this.targetStackSize = targetStackSize;
    }

    public String id() { return id; }
    public Role role() { return role; }
    public Channel channel() { return channel; }
    public boolean filtered() { return filtered; }
    public boolean targetStackSize() { return targetStackSize; }
    public boolean usesPriority() { return role != Role.DISPLAY && role != Role.DUMP; }
    public boolean routesItems() { return channel == Channel.ITEM || channel == Channel.BOTH; }
    public boolean routesFluids() { return channel == Channel.FLUID || channel == Channel.BOTH; }

    public static LogisticsCoverDefinition ofBehavior(String id) {
        if (id == null) return null;
        for (var type : values()) if (type.id.equals(id)) return type;
        return null;
    }

    /** GT6 filtered buses treat value zero as Filtered; generic buses treat it as Generic. */
    public int effectivePriority(int value) {
        int priority = value & 3;
        return priority == 0 ? (filtered ? 3 : 1) : priority;
    }
}
