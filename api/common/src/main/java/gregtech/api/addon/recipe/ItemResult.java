/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.jspecify.annotations.Nullable;

/**
 * One item output. Exactly one of {@code item}, {@code tag} or a {@code material} plus {@code form}
 * pair is set. A tag output resolves to the alphabetically first tag member when the row is loaded.
 * A count or chance of 0 means any, and is only valid on a removal.
 */
public record ItemResult(@Nullable String item, @Nullable String tag, @Nullable String material,
                         @Nullable String form, int count, int chance) {
    public ItemResult {
        if (count < 0) throw new IllegalArgumentException("count");
        if (chance < 0 || chance > MachineRecipeFields.CHANCE_CERTAIN) throw new IllegalArgumentException("chance");
        boolean materialPair = material != null || form != null;
        int identities = (item == null ? 0 : 1) + (tag == null ? 0 : 1) + (materialPair ? 1 : 0);
        if (identities != 1)
            throw new IllegalArgumentException("item result needs exactly one of item, tag or material+form");
        if (materialPair) {
            if (material == null || material.isBlank() || form == null || form.isBlank())
                throw new IllegalArgumentException("material component needs both material and form");
            MachineRecipeDocuments.checkName(material, "material");
            MachineRecipeDocuments.checkName(form, "form");
        } else if (item != null) {
            MachineRecipeDocuments.checkId(item, "item");
        } else {
            MachineRecipeDocuments.checkId(tag, "tag");
        }
    }

    public ItemResult(String item, int count, int chance) {
        this(item, null, null, null, count, chance);
    }

    public static ItemResult item(String item, int count, int chance) {
        return new ItemResult(item, null, null, null, count, chance);
    }

    public static ItemResult tag(String tag, int count, int chance) {
        return new ItemResult(null, tag, null, null, count, chance);
    }

    public static ItemResult material(String material, String form, int count, int chance) {
        return new ItemResult(null, null, material, form, count, chance);
    }
}
