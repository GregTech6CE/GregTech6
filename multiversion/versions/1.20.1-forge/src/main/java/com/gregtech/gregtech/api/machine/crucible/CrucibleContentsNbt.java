package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

/** Original crucible list schema, kept at the Minecraft storage boundary. */
public final class CrucibleContentsNbt {
    private CrucibleContentsNbt() {}

    public static void saveList(String key, CompoundTag tag, List<CrucibleMaterialStack> stacks) {
        ListTag list = new ListTag();
        for (CrucibleMaterialStack stack : stacks) {
            if (stack == null || stack.material == null || stack.material == Materials.Invalid || stack.amount <= 0) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", stack.material.getId());
            entry.putLong("amount", stack.amount);
            list.add(entry);
        }
        tag.put(key, list);
    }

    public static List<CrucibleMaterialStack> loadList(String key, CompoundTag tag) {
        List<CrucibleMaterialStack> stacks = new ArrayList<>();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return stacks;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            GTMaterial material = GTMaterialRegistry.get(entry.getInt("id"));
            long amount = entry.getLong("amount");
            if (material.isValid() && amount > 0) {
                stacks.add(new CrucibleMaterialStack(material, amount));
            }
        }
        return stacks;
    }
}
