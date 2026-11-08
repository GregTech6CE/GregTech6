/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Fixed display and definition ownership, without recipe or world-generation sources. */
public record MaterialInfo(int id, String translationKey, String displayName, int color, String definedBy) {
    public MaterialInfo {
        if (id <= 0 || translationKey == null || translationKey.isBlank() || displayName == null || displayName.isBlank()
                || definedBy == null || definedBy.isBlank() || (color & 0xFF000000) != 0)
            throw new IllegalArgumentException("Invalid material information");
    }
}
