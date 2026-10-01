package com.gregtech.gregtech.api.material;
import com.gregtech.gregtech.data.GregTechConstants;

import com.gregtech.gregtech.content.material.Materials;
/** GT6 material family defaults. These factories do not initialize a global alias table. */
public class MaterialFactories {
    protected MaterialFactories() {}
    public static GTMaterial create(int id, String name, String localName, int color, MaterialProperty... props) {
        return GTMaterialRegistry.createMaterial(id, name, localName, color).put(props);
    }

    public static GTMaterial element(int id, String name, String symbol, int protons, int neutrons, int melt, int boil, float density, int color, MaterialProperty... props) {
        return create(id, name, name, color, props)
                .setAtomicProperties(new AtomicProperties(protons, protons, neutrons, 0))
                .put(MaterialProperty.ELEMENT, MaterialProperty.GENERATE_DUST, MaterialProperty.MELTING)
                .setTextureSet(MaterialTextureSet.FINE)
                .setStats(melt, boil, density);
    }

    public static GTMaterial metal(int id, String name, String symbol, int protons, int neutrons, int melt, int boil, float density, int color, MaterialProperty... props) {
        return element(id, name, symbol, protons, neutrons, melt, boil, density, color, props)
                .put(MaterialProperty.MELTING, MaterialProperty.METAL, MaterialProperty.INGOT, MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET, MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PARTS, MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_FOIL, MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.GENERATE_MULTIINGOT, MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_DENSEPLATE, MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL, MaterialProperty.SMITHABLE, MaterialProperty.TOOL_HEAD)
                .setTextureSet(MaterialTextureSet.METALLIC);
    }

    public static GTMaterial dust(int id, String name, int color, MaterialProperty... props) {
        return create(id, name, name, color, props)
                .put(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .setTextureSet(MaterialTextureSet.FINE);
    }

    public static GTMaterial gem(int id, String name, int color, MaterialTextureSet textureSet, MaterialProperty... props) {
        return create(id, name, name, color, props)
                .put(MaterialProperty.GEM, MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_LENS, MaterialProperty.GENERATE_PARTS, MaterialProperty.TOOL_HEAD,
                        // GT6 gems all have dust forms (diamond dust, fluorite dust, ...)
                        MaterialProperty.DUST, MaterialProperty.GENERATE_DUST)
                .setTextureSet(textureSet);
    }

    public static GTMaterial gas(int id, String name, int color, MaterialProperty... props) {
        return create(id, name, name, color, props).put(MaterialProperty.GAS, MaterialProperty.LIQUID);
    }

    public static GTMaterial ore(int id, String name, int color, MaterialProperty... props) {
        return dust(id, name, color, props)
                .put(MaterialProperty.ORE, MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING, MaterialProperty.GENERATE_DIRTY_DUST)
                .setTextureSet(MaterialTextureSet.STONE);
    }

    public static GTMaterial alloy(int id, String name, int color, MaterialProperty... props) {
        // GT6 creates every alloy with TD.G_INGOT_MACHINE, which includes DUSTS (TD.java:609) —
        // alloy dusts such as Bronze/Steel/Brass Dust are ordinary GT6 items and the chemistry
        // tables consume them. The alloy factory used to omit the dust form, so those items did not
        // exist and `OM.dust(...)`/`OP.dust.mat(...)` references silently failed.
        return create(id, name, name, color, props)
                .put(MaterialProperty.MELTING, MaterialProperty.ALLOY, MaterialProperty.METAL,
                        MaterialProperty.DUST, MaterialProperty.GENERATE_DUST,
                        MaterialProperty.INGOT, MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_PARTS, MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_FOIL, MaterialProperty.GENERATE_WIRE, MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE, MaterialProperty.GENERATE_DENSEPLATE, MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL, MaterialProperty.SMITHABLE, MaterialProperty.TOOL_HEAD)
                .setTextureSet(MaterialTextureSet.METALLIC);
    }

    public static GTMaterial wood(int id, String name, String localName, int color, MaterialProperty... props) {
        return create(id, name, localName, color, props)
                .put(MaterialProperty.WOOD, MaterialProperty.DUST, MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_FOIL, MaterialProperty.GENERATE_PLANT, MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.FLAMMABLE)
                .setTextureSet(MaterialTextureSet.WOOD)
                .setFurnaceBurnTime(GregTechConstants.TICKS_PER_SMELT / 2L)
                .setBurning(com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ashes, GregTechConstants.U9);
    }

    public static GTMaterial stone(int id, String name, String localName, int color, MaterialProperty... props) {
        return create(id, name, localName, color, props)
                .put(MaterialProperty.STONE, MaterialProperty.DUST, MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_PLANT, MaterialProperty.GENERATE_PROJECTILE)
                .setTextureSet(MaterialTextureSet.STONE);
    }

    /** GT6 {@code elec}  - electrolysis-separated gem-like material. */
    public static GTMaterial elec(int id, String name, int color, MaterialTextureSet textureSet, MaterialProperty... props) {
        return gem(id, name, color, textureSet, props).put(MaterialProperty.MAGICAL);
    }

    public static GTMaterial elec(int id, String name, int color, MaterialProperty... props) {
        return elec(id, name, color, MaterialTextureSet.SHINY, props);
    }

    /** GT6 {@code cent}  - centrifuge-separated material. */
    public static GTMaterial cent(int id, String name, int color, MaterialTextureSet textureSet, MaterialProperty... props) {
        return gem(id, name, color, textureSet, props);
    }

    /** GT6 {@code clay}  - clay-family dust. */
    public static GTMaterial clay(int id, String name, int color, MaterialProperty... props) {
        return dust(id, name, color, props).setTextureSet(MaterialTextureSet.ROUGH);
    }

}
