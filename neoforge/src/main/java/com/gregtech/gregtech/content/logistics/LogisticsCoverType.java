package com.gregtech.gregtech.content.logistics;
import com.gregtech.gregtech.content.cover.CoverItems;
import net.minecraft.world.item.ItemStack;
/** Platform ItemStack lookup over the single shared original cover definition table. */
public enum LogisticsCoverType {
    CPU_LOGIC(LogisticsCoverDefinition.CPU_LOGIC),
    CPU_CONTROL(LogisticsCoverDefinition.CPU_CONTROL),
    CPU_STORAGE(LogisticsCoverDefinition.CPU_STORAGE),
    CPU_CONVERSION(LogisticsCoverDefinition.CPU_CONVERSION),
    FLUID_EXPORT(LogisticsCoverDefinition.FLUID_EXPORT),
    FLUID_IMPORT(LogisticsCoverDefinition.FLUID_IMPORT),
    FLUID_STORAGE(LogisticsCoverDefinition.FLUID_STORAGE),
    ITEM_EXPORT(LogisticsCoverDefinition.ITEM_EXPORT),
    ITEM_IMPORT(LogisticsCoverDefinition.ITEM_IMPORT),
    ITEM_STORAGE(LogisticsCoverDefinition.ITEM_STORAGE),
    GENERIC_EXPORT(LogisticsCoverDefinition.GENERIC_EXPORT),
    GENERIC_IMPORT(LogisticsCoverDefinition.GENERIC_IMPORT),
    GENERIC_STORAGE(LogisticsCoverDefinition.GENERIC_STORAGE),
    ITEM_DUMP(LogisticsCoverDefinition.ITEM_DUMP);
    public enum Role { IMPORT, EXPORT, STORAGE, DUMP, DISPLAY }
    public enum Channel { ITEM, FLUID, BOTH, NONE }
    private final LogisticsCoverDefinition definition;
    LogisticsCoverType(LogisticsCoverDefinition definition) { this.definition=definition; }
    public String id() { return definition.id(); }
    public Role role() { return Role.valueOf(definition.role().name()); }
    public Channel channel() { return Channel.valueOf(definition.channel().name()); }
    public boolean filtered() { return definition.filtered(); }
    public boolean targetStackSize() { return definition.targetStackSize(); }
    public boolean usesPriority() { return definition.usesPriority(); }
    public boolean routesItems() { return definition.routesItems(); }
    public boolean routesFluids() { return definition.routesFluids(); }
    public int effectivePriority(int value) { return definition.effectivePriority(value); }
    public static LogisticsCoverType of(ItemStack stack) {
        var definition=LogisticsCoverDefinition.ofBehavior(CoverItems.behavior(stack));
        return definition==null?null:valueOf(definition.name());
    }
}
