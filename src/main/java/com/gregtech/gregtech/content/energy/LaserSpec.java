package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.data.GregTechTags;

/** Loader_MultiTileEntities: 10101..105, 11101..105, 10121..125, 10151..155. */
public record LaserSpec(Kind kind, int tier) {
    private static final String[] ELECTRIC_MATERIALS={"SteelGalvanized","Aluminium","StainlessSteel","Chromium","Titanium"};
    private static final String[] FLUX_MATERIALS={"Lead","Invar","Electrum","EnderiumBase","Enderium"};
    public enum Kind { ELECTRIC, FLUX, ABSORBER, QUANTUM }
    public LaserSpec {
        if (tier < 1 || tier > 5) throw new IllegalArgumentException("Laser tier " + tier);
    }
    public long input() { return (32L << (2 * (tier - 1))) * (kind == Kind.FLUX ? 4 : 1); }
    public long output() { return 16L << (2 * (tier - 1)); }
    public GregTechTags.Tag inputType() {
        return switch (kind) { case ELECTRIC -> GregTechTags.Energy.EU; case FLUX -> GregTechTags.Energy.RF;
            case ABSORBER, QUANTUM -> GregTechTags.Energy.LU; };
    }
    public GregTechTags.Tag outputType() { return switch (kind) {
        case ELECTRIC, FLUX -> GregTechTags.Energy.LU; case ABSORBER -> GregTechTags.Energy.EU; case QUANTUM -> GregTechTags.Energy.QU; }; }
    public boolean backInputOnly() { return kind == Kind.ABSORBER || kind == Kind.QUANTUM; }
    public com.gregtech.gregtech.api.material.GTMaterial material() {
        String material = kind == Kind.QUANTUM ? "Osmiridium" :
                (kind == Kind.FLUX ? FLUX_MATERIALS : ELECTRIC_MATERIALS)[tier-1];
        return com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material);
    }
    public int tint() { return material().getColor(); }
}
