package gregtech.engine.material;

import gregtech.api.material.*;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Material identity in saves. New data stores the name; old numeric ids still resolve built-in materials. */
public final class StoredMaterial {
    private StoredMaterial() {}

    public static void writeName(CompoundTag tag, String key, GTMaterial material) {
        tag.remove(key);
        if (material == null || !material.isValid() || material.getId() <= 0) return;
        tag.putString(key, material.resolve().getName());
    }

    /** Always writes a string, using an empty string when there is nothing to show. */
    public static void writeDisplay(CompoundTag tag, String key, int materialId) {
        tag.remove(key);
        GTMaterial material = GTMaterialRegistry.get(materialId);
        tag.putString(key, material.isValid() ? material.getName() : "");
    }

    public static void writeStack(CompoundTag entry, GTMaterial material, long amount) {
        entry.putString("name", material.resolve().getName());
        entry.putLong("amount", amount);
    }

    public static GTMaterial read(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key)) return GTMaterialRegistry.get(-1);
        Tag value = tag.get(key);
        if (value == null) return GTMaterialRegistry.get(-1);
        if (value.getId() == Tag.TAG_STRING) {
            String name = tag.getString(key);
            return name.isEmpty() ? GTMaterialRegistry.get(-1) : GTMaterialRegistry.get(name);
        }
        if (isNumber(value.getId())) return GTMaterialRegistry.get(tag.getInt(key));
        return GTMaterialRegistry.get(-1);
    }

    public static int readId(CompoundTag tag, String key) {
        GTMaterial material = read(tag, key);
        return material.isValid() ? material.getId() : -1;
    }

    public static GTMaterial readStackMaterial(CompoundTag entry) {
        if (entry.contains("name", Tag.TAG_STRING)) return GTMaterialRegistry.get(entry.getString("name"));
        if (entry.contains("id")) {
            Tag value = entry.get("id");
            if (value != null && isNumber(value.getId())) return GTMaterialRegistry.get(entry.getInt("id"));
        }
        return GTMaterialRegistry.get(-1);
    }

    private static boolean isNumber(int type) {
        return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG;
    }
}
