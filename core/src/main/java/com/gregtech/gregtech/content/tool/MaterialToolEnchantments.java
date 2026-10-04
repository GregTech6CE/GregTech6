package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.ToolDefinition;
import com.gregtech.gregtech.content.book.GTMaterialEnchants;
import com.gregtech.gregtech.loaders.c.GTEnchantmentTable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original MultiItemTool material enchantments and ButcheryKnife's intrinsic looting. */
public final class MaterialToolEnchantments {
    private MaterialToolEnchantments() {}
    public static Map<String, Integer> of(ToolDefinition type, GTMaterial material) {
        Map<String, Integer> result = of(type.isMiningTool(),type.isWeapon(),OriginalToolFlags.of(type.name()).ranged(),material);
        if (type == ToolDefinition.BUTCHERY_KNIFE) {
            int looting = material.getToolQuality()/2+1;
            result.compute("looting", (key, existing) -> existing == null ? looting : 1+Math.max(existing,looting));
        }
        return result;
    }
    public static Map<String,Integer> of(boolean mining,boolean weapon,boolean ranged,GTMaterial material) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var row : GTEnchantmentTable.MATERIALS) {
            var resolved = GTMaterialEnchants.resolve(row.material());
            if (resolved == null || resolved.resolve() != material.resolve()) continue;
            for (var entry : row.entries()) {
                boolean applies = entry.kind().equals("Tools") && mining
                        || (entry.kind().equals("Damage") || entry.kind().equals("Weapons")) && weapon
                        || entry.kind().equals("Ranged") && ranged;
                if (applies) result.merge(entry.enchantment(), entry.level(), Math::max);
            }
        }
        return result;
    }
    /** Original ammo receives Damage and Ammo enchants, not the Weapons looting row. */
    public static Map<String,Integer> ammunition(GTMaterial material) {
        var result=new LinkedHashMap<String,Integer>();
        for(var row:GTEnchantmentTable.MATERIALS) {
            var resolved=GTMaterialEnchants.resolve(row.material());
            if(resolved==null||resolved.resolve()!=material.resolve())continue;
            for(var entry:row.entries())if(entry.kind().equals("Damage")||entry.kind().equals("Ammo"))
                result.merge(entry.enchantment(),entry.level(),Math::max);
        }
        return result;
    }
    public static String id(String name) {
        return switch (name) {
            case "disjunction", "butchery", "sharpness_multi", "smite_multi" -> "gregtech:"+name;
            case "haste", "gt_haste" -> "gregtech:gt_haste";
            default -> name.contains(":") ? name : "minecraft:"+name;
        };
    }
}
