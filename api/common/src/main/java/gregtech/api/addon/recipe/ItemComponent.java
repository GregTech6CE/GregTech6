/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.jspecify.annotations.Nullable;

/**
 * One item input identity. Exactly one of {@code item}, {@code tag} or a {@code material} plus
 * {@code form} pair is set. A count of 0 means any count on a removal.
 */
public record ItemComponent(@Nullable String item, @Nullable String tag, @Nullable String material,
                            @Nullable String form, int count) {
    public ItemComponent {
        if (count < 0) throw new IllegalArgumentException("count");
        boolean materialPair = material != null || form != null;
        int identities = (item == null ? 0 : 1) + (tag == null ? 0 : 1) + (materialPair ? 1 : 0);
        if (identities != 1)
            throw new IllegalArgumentException("item component needs exactly one of item, tag or material+form");
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

    public ItemComponent(@Nullable String item, @Nullable String tag, int count) {
        this(item, tag, null, null, count);
    }

    public static ItemComponent item(String item, int count) {
        return new ItemComponent(item, null, null, null, count);
    }

    public static ItemComponent tag(String tag, int count) {
        return new ItemComponent(null, tag, null, null, count);
    }

    public static ItemComponent material(String material, String form, int count) {
        return new ItemComponent(null, null, material, form, count);
    }
}
