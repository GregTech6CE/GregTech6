/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities 14500..14515 and MultiTileEntityBatteryLU8..8192. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
import java.util.ArrayList;

/** The existing Energium gem identities become the rechargeable crystals, as in GT6's ore targets. */
public record EnergyCrystalSpec(boolean cyan, int tier) implements PortableBatterySpec {
    public EnergyCrystalSpec {
        if (tier < 0 || tier > 5) throw new IllegalArgumentException("Crystal tier: " + tier);
    }
    public static final List<MaterialPrefix> FORMS = List.of(MaterialPrefix.gemChipped, MaterialPrefix.gemFlawed,
            MaterialPrefix.gem, MaterialPrefix.gemFlawless, MaterialPrefix.gemExquisite, MaterialPrefix.gemLegendary);
    public MaterialPrefix prefix() { return FORMS.get(tier); }
    public GTMaterial material() { return GTMaterialRegistry.get(cyan ? "EnergiumCyan" : "EnergiumRed"); }
    public String id() { return prefix().getItemId(material()); }
    public long voltage() { return 8L << (tier * 2); }
    public long capacity() { return voltage() * (cyan ? 800000L : 400000L); }
    public long minimumPacket() { return tier == 0 ? 1 : voltage() / 2; }
    public long maximumPacket() { return voltage() * 2; }
    public int inset() { return 6 - tier; }
    public int height() { return 4 + tier * 2; }
    public int scale() { return 127; }
    public String texture() { return "block/machines/batteries/lu/" + voltage(); }
    public int display(long energy) { return energy <= 0 ? 0 : energy >= capacity() ? 127 : 1 + (int)(energy * 126 / capacity()); }
    public int color() { return cyan ? 0x00FFFF : 0xFF0000; }
    public GregTechTags.Tag energyType() { return GregTechTags.Energy.LU; }
    public boolean barIndicator() { return false; }
    public int tint(long energy) {
        int brightness = 113 + display(energy);
        return cyan ? (brightness << 8) | brightness : brightness << 16;
    }
    public static EnergyCrystalSpec find(MaterialPrefix prefix, GTMaterial material) {
        int tier = FORMS.indexOf(prefix);
        if (tier < 0) return null;
        return switch (material.getName()) {
            case "EnergiumRed" -> new EnergyCrystalSpec(false, tier);
            case "EnergiumCyan" -> new EnergyCrystalSpec(true, tier);
            default -> null;
        };
    }
    public static List<EnergyCrystalSpec> all() {
        var result = new ArrayList<EnergyCrystalSpec>();
        for (boolean cyan : new boolean[]{false, true}) for (int tier = 0; tier < 6; tier++) result.add(new EnergyCrystalSpec(cyan, tier));
        return List.copyOf(result);
    }
}
