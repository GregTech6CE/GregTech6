package com.gregtech.gregtech.worldgen.center;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.item.ElectricToolItem;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.regex.Pattern;

/** Resolves the fixed original test-drawer rows; never evaluates their Java expressions. */
public final class NativeOriginItems {
    private NativeOriginItems() {}
    private static final Pattern TOOL=Pattern.compile("getToolWithStats\\(ToolsGT\\.([A-Z_]+)\\s*,\\s*(?:1,\\s*)?MT\\.([\\w.]+),\\s*MT\\.([\\w.]+)");
    private static final Pattern VANILLA=Pattern.compile("ST.make\\((?:Items|Blocks)\\.(\\w+)\\s*,\\s*(\\d+)");
    private static final Pattern FORM=Pattern.compile("OP\\.(\\w+)\\.mat\\(MT\\.(\\w+),\\s*(\\d+)\\)");
    private static ResourceLocation id(String name) { return ResourceLocation.parse(name); }
    private static String material(String name) { return switch(name) {
        case "Al" -> "Aluminium"; case "WOODS.Spruce" -> "Spruce";
        case "WOODS.Scorched" -> "WoodScorched"; default -> name;
    }; }
    public static ItemStack stack(String row) {
        var tool=TOOL.matcher(row);
        if(tool.find()) {
            var head=GTMaterialRegistry.get(material(tool.group(2)));
            var handle=GTMaterialRegistry.get(material(tool.group(3)));
            if(head==null || handle==null || !head.isValid() || !handle.isValid()) return ItemStack.EMPTY;
            String original=tool.group(1);
            for(var type:GTToolType.values()) if(type.name().replace("_","").equals(original.replace("_","")))
                return GTToolItem.create(type,head,handle);
            String electric=switch(original.replaceAll("_(LV|MV|HV)$","")) {
                case "WRENCH" -> "electric_wrench"; case "DRILL" -> "electric_drill";
                case "CHAINSAW" -> "electric_chainsaw"; case "SCREWDRIVER" -> "electric_screwdriver"; default -> null;
            };
            if(electric!=null && original.endsWith("_LV") && BuiltInRegistries.ITEM.get(id("gregtech:"+electric)) instanceof ElectricToolItem item) {
                var stack=item.assembled(head,32000);
                com.gregtech.gregtech.api.tool.GTToolHelper.write(stack,head,handle);
                stack.getOrCreateTag().putLong("gt.charge",32000);
                return stack;
            }
            return ItemStack.EMPTY;
        }
        var vanilla=VANILLA.matcher(row);
        if(vanilla.find()) {
            String name=switch(vanilla.group(1)) {case "golden_pickaxe" -> "golden_pickaxe"; default -> vanilla.group(1);};
            return new ItemStack(BuiltInRegistries.ITEM.get(id("minecraft:"+name)),Integer.parseInt(vanilla.group(2)));
        }
        var form=FORM.matcher(row);
        if(form.find()) {
            var selected=GTMaterialRegistry.get(material(form.group(2)));int count=Integer.parseInt(form.group(3));
            if(selected==null || !selected.isValid())return ItemStack.EMPTY;
            if(form.group(1).equals("wireGt01") || form.group(1).equals("cableGt01")) {
                boolean insulated=form.group(1).equals("cableGt01");
                for(var entry:com.gregtech.gregtech.registry.GTWires.all()) {
                    var spec=entry.get().spec();
                    if(spec.material()==selected && spec.size()==1 && spec.insulated()==insulated)return new ItemStack(entry.get(),count);
                }
                return ItemStack.EMPTY;
            }
            var type=PrefixRegistry.byName(form.group(1));return type==null?ItemStack.EMPTY:GTItems.getStack(type,selected,count);
        }
        var battery=Pattern.compile("Battery_Lead_Acid_(LV|MV|HV)").matcher(row);
        if(battery.find()) {
            var spec=com.gregtech.gregtech.content.energy.ChemicalBatterySpec.all().stream()
                    .filter(s->s.chemistry()==com.gregtech.gregtech.content.energy.ChemicalBatterySpec.Chemistry.LEAD_ACID
                            && s.tier()==switch(battery.group(1)){case "LV"->1;case "MV"->2;default->3;}).findFirst();
            if(spec.isPresent()) {
                var item=com.gregtech.gregtech.registry.GTChemicalBatteries.item(spec.get().chemistry(),spec.get().tier());
                var stack=new ItemStack(item); item.setCharge(stack,spec.get().capacity()); return stack;
            }
        }
        var motor=Pattern.compile("MOTORS\\[(\\d+)\\]").matcher(row);
        if(motor.find()) return new ItemStack(BuiltInRegistries.ITEM.get(id("gregtech:compact_electric_motor_"+
                switch(Integer.parseInt(motor.group(1))){case 1->"lv";case 2->"mv";default->"hv";})),64);
        var selector=Pattern.compile("Circuit_Selector.getWithDamage\\(1, (\\d+)\\)").matcher(row);
        if(selector.find())return new ItemStack(com.gregtech.gregtech.registry.GTTechnological.selectorTag(Integer.parseInt(selector.group(1))));
        String name=row.contains("Pill_Cure_All")?"cure_all":row.contains("Thermometer_Quicksilver")?"quicksilver_thermometer":
                row.contains("Brain_Tape")?"braintech_aerospace_advanced_reinforced_duct_tape_fal_84":row.contains("Dynamite_Strong")?"strong_dynamite":
                row.contains("Tool_Chunk_Remover")?"chunk_eraser":row.contains("Tool_Worldgen_Debugger")?"worldgen_debug_wand":
                row.contains("Tool_Cheat")?"debug_scanner":row.contains("Tool_Remote_Activator")?"remote_activator":null;
        if(name!=null) return new ItemStack(BuiltInRegistries.ITEM.get(id("gregtech:"+name)),row.contains("64")?64:1);
        // Source NI rows and integrations absent from this installation remain empty slots.
        return ItemStack.EMPTY;
    }
}
