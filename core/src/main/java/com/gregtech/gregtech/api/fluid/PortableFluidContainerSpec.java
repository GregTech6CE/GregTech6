package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import java.util.function.Supplier;

/** GT6 portable vessels, including forty original 1000 L capsule material variants. */
public enum PortableFluidContainerSpec {
    JUG("jug", 2000, () -> Materials.Ceramic, false, false, 0),
    CUP("cup", 250, com.gregtech.gregtech.content.material.SupplementalMaterials::porcelain, false, true, 0),
    MEASURING_POT("measuring_pot", 1000, () -> Materials.Ceramic, false, false, 0),
    THERMOS("thermos", 4000, () -> Materials.Aluminium, false, true, 50),
    BAROMETER_GAS_CYLINDER("barometer_gas_cylinder", 8000, () -> Materials.Steel, true, false, 50),
    MEASURING_POT_STAINLESS("measuring_pot_stainless_steel",1000,() -> Materials.StainlessSteel,false,false,50,true),
    MEASURING_POT_TUNGSTEN("measuring_pot_tungsten",1000,() -> Materials.Tungsten,false,true,50,true),
    MEASURING_POT_CARBIDE("measuring_pot_tantalum_hafnium_carbide",1000,() -> Materials.TantalumHafniumCarbide,false,true,50,false),
    GAS_CYLINDER_STAINLESS("barometer_gas_cylinder_stainless_steel",8000,() -> Materials.StainlessSteel,true,false,50,true),
    GAS_CYLINDER_TUNGSTEN("barometer_gas_cylinder_tungsten",8000,() -> Materials.Tungsten,true,true,50,true),
    GAS_CYLINDER_CARBIDE("barometer_gas_cylinder_tantalum_hafnium_carbide",8000,() -> Materials.TantalumHafniumCarbide,true,true,50,false),
    CELL_WAX("cell_wax","Wax",10,0,false,false,false),
    CELL_WAX_BEE("cell_wax_bee","WaxBee",10,0,false,false,false),
    CELL_WAX_PLANT("cell_wax_plant","WaxPlant",10,0,false,false,false),
    CELL_WAX_PARAFFIN("cell_wax_paraffin","WaxParaffin",10,0,false,false,false),
    CELL_WAX_REFRACTORY("cell_wax_refractory","WaxRefractory",10,0,true,false,false),
    CELL_WAX_MAGIC("cell_wax_magic","WaxMagic",0,2700,true,true,false),
    CELL_WAX_AMNESIC("cell_wax_amnesic","WaxAmnesic",0,2700,true,true,false),
    CELL_WAX_SOULFUL("cell_wax_soulful","WaxSoulful",10,0,true,true,false),
    CELL_PLASTIC("cell_plastic","Plastic",10,0,false,false,false),
    CELL_TIN("cell_tin","Tin",50,0,false,false,false),
    CELL_TIN_ALLOY("cell_tin_alloy","TinAlloy",50,0,false,false,false),
    CELL_INVAR("cell_invar","Invar",50,0,false,false,false),
    CELL_GOLD("cell_gold","Gold",50,0,true,false,false),
    CELL_ALUMINIUM("cell_aluminium","Aluminium",50,0,false,false,false),
    CELL_STAINLESS_STEEL("cell_stainless_steel","StainlessSteel",50,0,true,false,false),
    CELL_HSLA_TUNGSTEN_ALLOY("cell_hsla_tungsten_alloy","HSLATungstenAlloy",50,0,false,true,false),
    CELL_TITANIUM("cell_titanium","Titanium",50,0,false,false,false),
    CELL_NETHERITE("cell_netherite","Netherite",50,0,true,true,true),
    CELL_TUNGSTEN_STEEL("cell_tungsten_steel","TungstenSteel",50,0,false,true,false),
    CELL_TUNGSTEN_CARBIDE("cell_tungsten_carbide","TungstenCarbide",50,0,false,true,false),
    CELL_TUNGSTEN("cell_tungsten","Tungsten",50,0,true,true,false),
    CELL_PALLADIUM("cell_palladium","Palladium",50,0,false,true,false),
    CELL_TANTALUM_HAFNIUM_CARBIDE("cell_tantalum_hafnium_carbide","TantalumHafniumCarbide",50,0,false,true,false),
    CELL_DESH("cell_desh","Desh",50,0,false,true,false),
    CELL_WORKERS_ALLOY("cell_workers_alloy","WorkersAlloy",50,0,false,true,false),
    CELL_KREKNORITE("cell_kreknorite","Trinium",50,0,false,true,false),
    CELL_TRINITANIUM("cell_trinitanium","Trinitanium",50,0,false,true,true),
    CELL_ADAMANTIUM("cell_adamantium","Adamantium",50,0,true,true,true),
    CELL_SYRMORITE("cell_syrmorite","Syrmorite",50,0,false,true,false),
    CELL_EFRINE("cell_efrine","Efrine",50,0,false,true,true),
    CELL_THAUMIUM("cell_thaumium","Thaumium",50,0,true,true,false),
    CELL_VOID_METAL("cell_void_metal","VoidMetal",50,0,true,true,true),
    CELL_MANASTEEL("cell_manasteel","Manasteel",50,0,true,true,false),
    CELL_TERRASTEEL("cell_terrasteel","Terrasteel",50,0,true,true,false),
    CELL_ELVEN_ELEMENTIUM("cell_elven_elementium","ElvenElementium",50,0,true,true,false),
    CELL_GAIA_SPIRIT("cell_gaia_spirit","GaiaSpirit",50,0,true,true,true),
    CELL_DURANIUM("cell_duranium","Duranium",50,0,true,true,true),
    CELL_DRACONIUM("cell_draconium","Draconium",50,0,true,true,true),
    CELL_DRACONIUM_AWAKENED("cell_draconium_awakened","DraconiumAwakened",50,0,true,true,true),
    CELL_INFINITY("cell_infinity","Infinity",0,2147483647,true,true,true);

    private final String id;
    private final int capacity, temperatureMargin;
    private final Supplier<GTMaterial> material;
    private final boolean gasProof, magicProof, acidProof;
    private final boolean plasmaProof;
    private final int fixedTemperature;
    PortableFluidContainerSpec(String id,String material,int margin,int temperature,boolean acid,boolean magic,boolean plasma){
        this.id=id;this.capacity=1000;this.material=()->com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material);
        this.temperatureMargin=margin;this.fixedTemperature=temperature;this.gasProof=true;this.magicProof=magic;this.acidProof=acid;this.plasmaProof=plasma;
    }
    PortableFluidContainerSpec(String id, int capacity, Supplier<GTMaterial> material,
                               boolean gasProof, boolean magicProof, int temperatureMargin) {
        this(id,capacity,material,gasProof,magicProof,temperatureMargin,false);
    }
    PortableFluidContainerSpec(String id,int capacity,Supplier<GTMaterial> material,boolean gasProof,boolean magicProof,int temperatureMargin,boolean acidProof){
        this.plasmaProof=false;this.fixedTemperature=0;
        this.acidProof=acidProof;
        this.id = id;
        this.capacity = capacity;
        this.material = material;
        this.gasProof = gasProof;
        this.magicProof = magicProof;
        this.temperatureMargin = temperatureMargin;
    }
    public String id() { return id; }
    public String shapeId(){return id.startsWith("cell_")?"cell":id.startsWith("measuring_pot")?"measuring_pot":id.startsWith("barometer_gas_cylinder")?"barometer_gas_cylinder":id;}
    public int capacity() { return capacity; }
    public GTMaterial material() { return material.get(); }
    public int maxTemperature() { return fixedTemperature>0?fixedTemperature:material().getMeltingPoint() - temperatureMargin; }
    public boolean accepts(int temperature, boolean gas, boolean acid, boolean magic, boolean plasma) {
        return temperature <= maxTemperature() && (shapeId().equals("barometer_gas_cylinder") ? gas : (!gas || gasProof)) && (!acid || acidProof) && (!magic || magicProof) && (!plasma || plasmaProof);
    }
}
