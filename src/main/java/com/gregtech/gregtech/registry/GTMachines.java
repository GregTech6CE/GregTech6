package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.BurningBoxFuelType;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.machine.MachineTextures;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.BurningBoxBlock;
import com.gregtech.gregtech.block.machine.HopperBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.data.generated.GT6Materials;
import net.minecraftforge.registries.RegistryObject;

/** GregTech machine blocks (generators, crucibles, ...). */
public final class GTMachines {
    private GTMachines() {}

    /** Bronze solid burning box — GT6 meta 1102: efficiency 7500, output 24 HU/t, resistance 7.0. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_BRONZE = solidBurningBox(
            "burning_box_solid_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, 7500, 24, 7.0F, 7.0F);

    /** Lead solid burning box — GT6 meta 1100: efficiency 5000, output 16 HU/t, resistance 4.0. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_LEAD = solidBurningBox(
            "burning_box_solid_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, 5000, 16, 4.0F, 4.0F);

    /** Bismuth — GT6 meta 1101. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_BISMUTH = solidBurningBox(
            "burning_box_solid_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, 4500, 20, 4.0F, 4.0F);

    /** Arsenic copper — GT6 meta 1111. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_ARSENIC_COPPER = solidBurningBox(
            "burning_box_solid_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, 8000, 24, 7.0F, 7.0F);

    /** Arsenic bronze — GT6 meta 1112. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_ARSENIC_BRONZE = solidBurningBox(
            "burning_box_solid_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, 9000, 28, 7.0F, 7.0F);

    /** Invar — GT6 meta 1103. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_INVAR = solidBurningBox(
            "burning_box_solid_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, 10000, 16, 4.0F, 4.0F);

    /** Steel — GT6 meta 1104. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_STEEL = solidBurningBox(
            "burning_box_solid_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, 7000, 32, 6.0F, 6.0F);

    /** Chromium — GT6 meta 1105. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_CHROMIUM = solidBurningBox(
            "burning_box_solid_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, 8500, 112, 4.0F, 4.0F);

    /** Titanium — GT6 meta 1106. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_TITANIUM = solidBurningBox(
            "burning_box_solid_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, 8500, 96, 9.0F, 9.0F);

    /** Netherite — GT6 meta 1110. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_NETHERITE = solidBurningBox(
            "burning_box_solid_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, 9000, 96, 9.0F, 9.0F);

    /** Tungsten — GT6 meta 1107. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_TUNGSTEN = solidBurningBox(
            "burning_box_solid_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, 10000, 128, 10.0F, 10.0F);

    /** Tungstensteel — GT6 meta 1108. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_TUNGSTEN_STEEL = solidBurningBox(
            "burning_box_solid_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, 9000, 128, 12.5F, 12.5F);

    /** Tantalum hafnium carbide — GT6 meta 1109. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_TA4HFC5 = solidBurningBox(
            "burning_box_solid_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, 10000, 256, 12.5F, 12.5F);

    /** Ultimet (Hastelloy) — GT6 meta 1113. */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_ULTIMET = solidBurningBox(
            "burning_box_solid_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, 9000, 256, 12.5F, 12.5F);

    /** Brick — GT6 meta 1199 ({@code MultiTileEntityGeneratorBrick}). */
    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_BRICK = solidBurningBox(
            "burning_box_solid_brick", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ClayBrick, 2500, 16, 6.0F, 6.0F);

    // --- Dense solid burning boxes (GT6 meta 1150–1162) ---

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_LEAD = solidBurningBox(
            "burning_box_solid_dense_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, 5000, 64, 4.0F, 4.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_BISMUTH = solidBurningBox(
            "burning_box_solid_dense_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, 4500, 80, 4.0F, 4.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_BRONZE = solidBurningBox(
            "burning_box_solid_dense_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, 7500, 96, 7.0F, 7.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_ARSENIC_COPPER = solidBurningBox(
            "burning_box_solid_dense_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, 8000, 96, 7.0F, 7.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_ARSENIC_BRONZE = solidBurningBox(
            "burning_box_solid_dense_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, 9000, 112, 7.0F, 7.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_INVAR = solidBurningBox(
            "burning_box_solid_dense_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, 10000, 64, 4.0F, 4.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_STEEL = solidBurningBox(
            "burning_box_solid_dense_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, 7000, 128, 6.0F, 6.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_CHROMIUM = solidBurningBox(
            "burning_box_solid_dense_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, 8500, 448, 4.0F, 4.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_TITANIUM = solidBurningBox(
            "burning_box_solid_dense_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, 8500, 384, 9.0F, 9.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_NETHERITE = solidBurningBox(
            "burning_box_solid_dense_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, 9000, 384, 9.0F, 9.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_TUNGSTEN = solidBurningBox(
            "burning_box_solid_dense_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, 10000, 512, 10.0F, 10.0F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_TUNGSTEN_STEEL = solidBurningBox(
            "burning_box_solid_dense_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, 9000, 512, 12.5F, 12.5F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_TA4HFC5 = solidBurningBox(
            "burning_box_solid_dense_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, 10000, 1024, 12.5F, 12.5F);

    public static final RegistryObject<SolidBurningBoxBlock> BURNING_BOX_SOLID_DENSE_ULTIMET = solidBurningBox(
            "burning_box_solid_dense_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, 9000, 1024, 12.5F, 12.5F);

    // --- Liquid burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_LEAD = burningBox(
            "burning_box_liquid_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.LIQUID, 5000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_BISMUTH = burningBox(
            "burning_box_liquid_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.LIQUID, 4500, 20, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_BRONZE = burningBox(
            "burning_box_liquid_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.LIQUID, 7500, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_ARSENIC_COPPER = burningBox(
            "burning_box_liquid_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.LIQUID, 8000, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_ARSENIC_BRONZE = burningBox(
            "burning_box_liquid_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.LIQUID, 9000, 28, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_INVAR = burningBox(
            "burning_box_liquid_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.LIQUID, 10000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_STEEL = burningBox(
            "burning_box_liquid_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.LIQUID, 7000, 32, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_CHROMIUM = burningBox(
            "burning_box_liquid_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.LIQUID, 8500, 112, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_TITANIUM = burningBox(
            "burning_box_liquid_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.LIQUID, 8500, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_NETHERITE = burningBox(
            "burning_box_liquid_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.LIQUID, 9000, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_TUNGSTEN = burningBox(
            "burning_box_liquid_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.LIQUID, 10000, 128, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_TUNGSTEN_STEEL = burningBox(
            "burning_box_liquid_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.LIQUID, 9000, 128, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_TA4HFC5 = burningBox(
            "burning_box_liquid_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.LIQUID, 10000, 256, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_ULTIMET = burningBox(
            "burning_box_liquid_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.LIQUID, 9000, 256, 12.5F, 12.5F);

    // --- Gas burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_LEAD = burningBox(
            "burning_box_gas_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.GAS, 5000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_BISMUTH = burningBox(
            "burning_box_gas_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.GAS, 4500, 20, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_BRONZE = burningBox(
            "burning_box_gas_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.GAS, 7500, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_ARSENIC_COPPER = burningBox(
            "burning_box_gas_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.GAS, 8000, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_ARSENIC_BRONZE = burningBox(
            "burning_box_gas_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.GAS, 9000, 28, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_INVAR = burningBox(
            "burning_box_gas_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.GAS, 10000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_STEEL = burningBox(
            "burning_box_gas_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.GAS, 7000, 32, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_CHROMIUM = burningBox(
            "burning_box_gas_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.GAS, 8500, 112, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_TITANIUM = burningBox(
            "burning_box_gas_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.GAS, 8500, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_NETHERITE = burningBox(
            "burning_box_gas_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.GAS, 9000, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_TUNGSTEN = burningBox(
            "burning_box_gas_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.GAS, 10000, 128, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_TUNGSTEN_STEEL = burningBox(
            "burning_box_gas_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.GAS, 9000, 128, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_TA4HFC5 = burningBox(
            "burning_box_gas_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.GAS, 10000, 256, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_ULTIMET = burningBox(
            "burning_box_gas_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.GAS, 9000, 256, 12.5F, 12.5F);

    // --- Fluidized Bed burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_LEAD = burningBox(
            "burning_box_fluidbed_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.FLUIDIZED_BED, 5000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_BISMUTH = burningBox(
            "burning_box_fluidbed_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.FLUIDIZED_BED, 4500, 20, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_BRONZE = burningBox(
            "burning_box_fluidbed_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.FLUIDIZED_BED, 7500, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_ARSENIC_COPPER = burningBox(
            "burning_box_fluidbed_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.FLUIDIZED_BED, 8000, 24, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_ARSENIC_BRONZE = burningBox(
            "burning_box_fluidbed_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.FLUIDIZED_BED, 9000, 28, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_INVAR = burningBox(
            "burning_box_fluidbed_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.FLUIDIZED_BED, 10000, 16, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_STEEL = burningBox(
            "burning_box_fluidbed_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.FLUIDIZED_BED, 7000, 32, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_CHROMIUM = burningBox(
            "burning_box_fluidbed_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.FLUIDIZED_BED, 8500, 112, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_TITANIUM = burningBox(
            "burning_box_fluidbed_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.FLUIDIZED_BED, 8500, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_NETHERITE = burningBox(
            "burning_box_fluidbed_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.FLUIDIZED_BED, 9000, 96, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_TUNGSTEN = burningBox(
            "burning_box_fluidbed_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.FLUIDIZED_BED, 10000, 128, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_TUNGSTEN_STEEL = burningBox(
            "burning_box_fluidbed_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.FLUIDIZED_BED, 9000, 128, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_TA4HFC5 = burningBox(
            "burning_box_fluidbed_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.FLUIDIZED_BED, 10000, 256, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_ULTIMET = burningBox(
            "burning_box_fluidbed_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.FLUIDIZED_BED, 9000, 256, 12.5F, 12.5F);

    // --- Dense liquid burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_LEAD = burningBox(
            "burning_box_liquid_dense_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.LIQUID, 5000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_BISMUTH = burningBox(
            "burning_box_liquid_dense_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.LIQUID, 4500, 80, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_BRONZE = burningBox(
            "burning_box_liquid_dense_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.LIQUID, 7500, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_ARSENIC_COPPER = burningBox(
            "burning_box_liquid_dense_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.LIQUID, 8000, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_ARSENIC_BRONZE = burningBox(
            "burning_box_liquid_dense_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.LIQUID, 9000, 112, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_INVAR = burningBox(
            "burning_box_liquid_dense_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.LIQUID, 10000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_STEEL = burningBox(
            "burning_box_liquid_dense_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.LIQUID, 7000, 128, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_CHROMIUM = burningBox(
            "burning_box_liquid_dense_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.LIQUID, 8500, 448, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_TITANIUM = burningBox(
            "burning_box_liquid_dense_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.LIQUID, 8500, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_NETHERITE = burningBox(
            "burning_box_liquid_dense_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.LIQUID, 9000, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_TUNGSTEN = burningBox(
            "burning_box_liquid_dense_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.LIQUID, 10000, 512, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_TUNGSTEN_STEEL = burningBox(
            "burning_box_liquid_dense_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.LIQUID, 9000, 512, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_TA4HFC5 = burningBox(
            "burning_box_liquid_dense_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.LIQUID, 10000, 1024, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_LIQUID_DENSE_ULTIMET = burningBox(
            "burning_box_liquid_dense_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.LIQUID, 9000, 1024, 12.5F, 12.5F);

    // --- Dense gas burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_LEAD = burningBox(
            "burning_box_gas_dense_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.GAS, 5000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_BISMUTH = burningBox(
            "burning_box_gas_dense_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.GAS, 4500, 80, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_BRONZE = burningBox(
            "burning_box_gas_dense_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.GAS, 7500, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_ARSENIC_COPPER = burningBox(
            "burning_box_gas_dense_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.GAS, 8000, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_ARSENIC_BRONZE = burningBox(
            "burning_box_gas_dense_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.GAS, 9000, 112, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_INVAR = burningBox(
            "burning_box_gas_dense_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.GAS, 10000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_STEEL = burningBox(
            "burning_box_gas_dense_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.GAS, 7000, 128, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_CHROMIUM = burningBox(
            "burning_box_gas_dense_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.GAS, 8500, 448, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_TITANIUM = burningBox(
            "burning_box_gas_dense_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.GAS, 8500, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_NETHERITE = burningBox(
            "burning_box_gas_dense_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.GAS, 9000, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_TUNGSTEN = burningBox(
            "burning_box_gas_dense_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.GAS, 10000, 512, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_TUNGSTEN_STEEL = burningBox(
            "burning_box_gas_dense_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.GAS, 9000, 512, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_TA4HFC5 = burningBox(
            "burning_box_gas_dense_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.GAS, 10000, 1024, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_GAS_DENSE_ULTIMET = burningBox(
            "burning_box_gas_dense_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.GAS, 9000, 1024, 12.5F, 12.5F);

    // --- Dense fluidized bed burning boxes ---

    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_LEAD = burningBox(
            "burning_box_fluidbed_dense_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, BurningBoxFuelType.FLUIDIZED_BED, 5000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_BISMUTH = burningBox(
            "burning_box_fluidbed_dense_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, BurningBoxFuelType.FLUIDIZED_BED, 4500, 80, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_BRONZE = burningBox(
            "burning_box_fluidbed_dense_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, BurningBoxFuelType.FLUIDIZED_BED, 7500, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_ARSENIC_COPPER = burningBox(
            "burning_box_fluidbed_dense_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, BurningBoxFuelType.FLUIDIZED_BED, 8000, 96, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_ARSENIC_BRONZE = burningBox(
            "burning_box_fluidbed_dense_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, BurningBoxFuelType.FLUIDIZED_BED, 9000, 112, 7.0F, 7.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_INVAR = burningBox(
            "burning_box_fluidbed_dense_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, BurningBoxFuelType.FLUIDIZED_BED, 10000, 64, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_STEEL = burningBox(
            "burning_box_fluidbed_dense_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, BurningBoxFuelType.FLUIDIZED_BED, 7000, 128, 6.0F, 6.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_CHROMIUM = burningBox(
            "burning_box_fluidbed_dense_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, BurningBoxFuelType.FLUIDIZED_BED, 8500, 448, 4.0F, 4.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_TITANIUM = burningBox(
            "burning_box_fluidbed_dense_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, BurningBoxFuelType.FLUIDIZED_BED, 8500, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_NETHERITE = burningBox(
            "burning_box_fluidbed_dense_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, BurningBoxFuelType.FLUIDIZED_BED, 9000, 384, 9.0F, 9.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_TUNGSTEN = burningBox(
            "burning_box_fluidbed_dense_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, BurningBoxFuelType.FLUIDIZED_BED, 10000, 512, 10.0F, 10.0F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_TUNGSTEN_STEEL = burningBox(
            "burning_box_fluidbed_dense_tungsten_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, BurningBoxFuelType.FLUIDIZED_BED, 9000, 512, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_TA4HFC5 = burningBox(
            "burning_box_fluidbed_dense_ta4hfc5", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, BurningBoxFuelType.FLUIDIZED_BED, 10000, 1024, 12.5F, 12.5F);
    public static final RegistryObject<BurningBoxBlock> BURNING_BOX_FLUIDBED_DENSE_ULTIMET = burningBox(
            "burning_box_fluidbed_dense_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, BurningBoxFuelType.FLUIDIZED_BED, 9000, 1024, 12.5F, 12.5F);

    // --- Smelting crucibles (GT6 meta 1000–1039, texture 1022) ---

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_STONE =
            crucible("smelting_crucible_stone", com.gregtech.gregtech.content.material.generated.StoneMaterials.Stone, 1000, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_BASALT =
            crucible("smelting_crucible_basalt", com.gregtech.gregtech.content.material.generated.StoneMaterials.Basalt, 1001, 15.0F, 15.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_GRANITE_BLACK =
            crucible("smelting_crucible_granite_black", Materials.GraniteBlack, 1002, 15.0F, 15.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_GRANITE_RED =
            crucible("smelting_crucible_granite_red", Materials.GraniteRed, 1003, 15.0F, 15.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_NETHER_BRICK =
            crucible("smelting_crucible_nether_brick", com.gregtech.gregtech.content.material.generated.StoneMaterials.NetherBrick, 1004, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_CERAMIC =
            crucible("smelting_crucible_ceramic", Materials.Ceramic, 1005, 5.0F, 5.0F, false, 2000, 4000, 0.8181818181818182D);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_UMBER =
            crucible("smelting_crucible_umber", com.gregtech.gregtech.content.material.generated.StoneMaterials.Umber, 1006, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_LIVINGROCK =
            crucible("smelting_crucible_livingrock", com.gregtech.gregtech.content.material.generated.StoneMaterials.Livingrock, 1007, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_HOLYSTONE =
            crucible("smelting_crucible_holystone", com.gregtech.gregtech.content.material.generated.StoneMaterials.Holystone, 1008, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_BETWEENSTONE =
            crucible("smelting_crucible_betweenstone", com.gregtech.gregtech.content.material.generated.StoneMaterials.Betweenstone, 1009, 5.0F, 5.0F, false, 1100, 3220, 2.65F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_QUARTZ =
            crucible("smelting_crucible_quartz", Materials.MilkyQuartz, 1018, 5.0F, 5.0F, false, 1986, 3220, 2.2F,
                    CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_CARBON =
            crucible("smelting_crucible_carbon", Materials.Carbon, 1019, 10.0F, 10.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_BRONZE =
            crucible("smelting_crucible_bronze", Materials.Bronze, 1020, 7.0F, 7.0F, false, 1357, 2835, 8.96F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_INVAR =
            crucible("smelting_crucible_invar", Materials.Invar, 1021, 4.0F, 4.0F, false, 1700, 3000, 8.0F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_STEEL =
            crucible("smelting_crucible_steel", Materials.Steel, 1022, 6.0F, 6.0F, false, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_HSLA =
            crucible("smelting_crucible_hsla", Materials.HSLASteel, 1041, 6.0F, 6.0F, false, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_TITANIUM =
            crucible("smelting_crucible_titanium", Materials.Titanium, 1023, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_TUNGSTEN =
            crucible("smelting_crucible_tungsten", Materials.Tungsten, 1024, 10.0F, 10.0F, true);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_STAINLESS_STEEL =
            crucible("smelting_crucible_stainless_steel", Materials.StainlessSteel, 1025, 6.0F, 6.0F, true, 2046, 3134, 8.0F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_DARK_IRON =
            crucible("smelting_crucible_dark_iron", Materials.DarkIron, 1026, 6.0F, 6.0F, false, 1811, 3134, 7.87F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_KNIGHTMETAL =
            crucible("smelting_crucible_knightmetal", Materials.Knightmetal, 1027, 6.0F, 6.0F, false, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_FIERY_STEEL =
            crucible("smelting_crucible_fiery_steel", Materials.FierySteel, 1028, 6.0F, 6.0F, false, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_OCTINE =
            crucible("smelting_crucible_octine", Materials.Octine, 1042, 6.0F, 6.0F, false, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_THAUMIUM =
            crucible("smelting_crucible_thaumium", Materials.Thaumium, 1029, 6.0F, 6.0F, true, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_VOID_METAL =
            crucible("smelting_crucible_void_metal", Materials.VoidMetal, 1030, 10.0F, 10.0F, true, 2046, 3134, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_METEORIC_IRON =
            crucible("smelting_crucible_meteoric_iron", Materials.MeteoricIron, 1031, 6.0F, 6.0F, false, 2011, 3334, 7.87F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_METEORIC_STEEL =
            crucible("smelting_crucible_meteoric_steel", Materials.MeteoricSteel, 1032, 6.0F, 6.0F, false, 2246, 3334, 7.85F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_CHROMIUM =
            crucible("smelting_crucible_chromium", Materials.Chromium, 1033, 9.0F, 9.0F, true);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_MOLYBDENUM =
            crucible("smelting_crucible_molybdenum", Materials.Molybdenum, 1034, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_NIOBIUM =
            crucible("smelting_crucible_niobium", Materials.Niobium, 1035, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_TANTALUM =
            crucible("smelting_crucible_tantalum", Materials.Tantalum, 1036, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_OSMIUM =
            crucible("smelting_crucible_osmium", Materials.OsmiumElemental, 1037, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_VANADIUM =
            crucible("smelting_crucible_vanadium", Materials.Vanadium, 1038, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_IRIDIUM =
            crucible("smelting_crucible_iridium", Materials.Iridium, 1039, 9.0F, 9.0F, true);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_NIOBIUM_TITANIUM =
            crucible("smelting_crucible_niobium_titanium", Materials.NiobiumTitanium, 1040, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_TA4HFC5 =
            crucible("smelting_crucible_ta4hfc5", Materials.TantalumHafniumCarbide, 1043, 9.0F, 9.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_NETHERITE =
            crucible("smelting_crucible_netherite", Materials.Netherite, 1044, 6.0F, 6.0F, true, 2046, 3134, 8.0F);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_BEDROCK_HSLA_ALLOY =
            crucible("smelting_crucible_bedrock_hsla_alloy", Materials.BedrockHSLAAlloy, 1048, 100.0F, 100.0F, false);

    public static final RegistryObject<SmeltingCrucibleBlock> SMELTING_CRUCIBLE_AD =
            crucible("smelting_crucible_ad", Materials.Adamantium, 1049, 100.0F, 100.0F, true);

    // --- Hoppers (GT6 meta 8000+aID, ordered by original GT6 aID) ---

    public static final RegistryObject<HopperBlock> HOPPER_LEAD =
            hopper("hopper_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, 1, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_BISMUTH =
            hopper("hopper_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, 2, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ANTIMONY =
            hopper("hopper_antimony", com.gregtech.gregtech.content.material.generated.ElementMaterials.Antimony, 2, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_NICKEL =
            hopper("hopper_nickel", com.gregtech.gregtech.content.material.generated.ElementMaterials.Nickel, 3, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_CONSTANTAN =
            hopper("hopper_constantan", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Constantan, 3, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_BRONZE =
            hopper("hopper_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, 3, 7.0F, 7.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ARSENIC_COPPER =
            hopper("hopper_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, 4, 7.5F, 7.5F);
    public static final RegistryObject<HopperBlock> HOPPER_ALUMINIUM =
            hopper("hopper_aluminium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Aluminium, 4, 2.0F, 2.0F);
    public static final RegistryObject<HopperBlock> HOPPER_BRASS =
            hopper("hopper_brass", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Brass, 4, 2.5F, 2.5F);
    public static final RegistryObject<HopperBlock> HOPPER_TIN_ALLOY =
            hopper("hopper_tin_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TinAlloy, 4, 3.0F, 3.0F);
    public static final RegistryObject<HopperBlock> HOPPER_COBALT =
            hopper("hopper_cobalt", com.gregtech.gregtech.content.material.generated.ElementMaterials.Cobalt, 4, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ARDITE =
            hopper("hopper_ardite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ardite, 4, 2.0F, 2.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ARSENIC_BRONZE =
            hopper("hopper_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, 5, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_BISMUTH_BRONZE =
            hopper("hopper_bismuth_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BismuthBronze, 5, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_GERMANIUM =
            hopper("hopper_germanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Germanium, 5, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_INVAR =
            hopper("hopper_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, 5, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_STEEL =
            hopper("hopper_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, 5, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_HSLA =
            hopper("hopper_hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLASteel, 6, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_GOLD =
            hopper("hopper_gold", com.gregtech.gregtech.content.material.generated.ElementMaterials.Gold, 6, 3.0F, 3.0F);
    public static final RegistryObject<HopperBlock> HOPPER_SILVER =
            hopper("hopper_silver", com.gregtech.gregtech.content.material.generated.ElementMaterials.Silver, 6, 3.0F, 3.0F);
    public static final RegistryObject<HopperBlock> HOPPER_MANGANESE =
            hopper("hopper_manganese", com.gregtech.gregtech.content.material.generated.ElementMaterials.Manganese, 6, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_MANYULLYN =
            hopper("hopper_manyullyn", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manyullyn, 6, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_LUMIUM =
            hopper("hopper_lumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Lumium, 6, 2.0F, 2.0F);
    public static final RegistryObject<HopperBlock> HOPPER_KNIGHTMETAL =
            hopper("hopper_knightmetal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Knightmetal, 7, 7.0F, 7.0F);
    public static final RegistryObject<HopperBlock> HOPPER_STEEL_GALVANIZED =
            hopper("hopper_steel_galvanized", com.gregtech.gregtech.content.material.generated.CompoundMaterials.SteelGalvanized, 7, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_METEORITE =
            hopper("hopper_meteorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Meteorite, 7, 7.0F, 7.0F);
    public static final RegistryObject<HopperBlock> HOPPER_METEORIC_STEEL =
            hopper("hopper_meteoric_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.MeteoricSteel, 8, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_GILDED_IRON =
            hopper("hopper_gilded_iron", com.gregtech.gregtech.content.material.generated.CompoundMaterials.GildedIron, 8, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_MOLYBDENUM =
            hopper("hopper_molybdenum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Molybdenum, 8, 6.0F, 6.0F);
    public static final RegistryObject<HopperBlock> HOPPER_SYRMORITE =
            hopper("hopper_syrmorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Syrmorite, 9, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ELECTRUM =
            hopper("hopper_electrum", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Electrum, 9, 3.0F, 3.0F);
    public static final RegistryObject<HopperBlock> HOPPER_STAINLESS_STEEL =
            hopper("hopper_stainless_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.StainlessSteel, 9, 5.0F, 5.0F);
    public static final RegistryObject<HopperBlock> HOPPER_THAUMIUM =
            hopper("hopper_thaumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Thaumium, 9, 9.0F, 9.0F);
    public static final RegistryObject<HopperBlock> HOPPER_MANASTEEL =
            hopper("hopper_manasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manasteel, 9, 9.0F, 9.0F);
    public static final RegistryObject<HopperBlock> HOPPER_EFRINE =
            hopper("hopper_efrine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Efrine, 9, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TUNGSTEN_ALLOY =
            hopper("hopper_tungsten_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy, 12, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TITANIUM =
            hopper("hopper_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, 12, 9.0F, 9.0F);
    public static final RegistryObject<HopperBlock> HOPPER_NETHERITE =
            hopper("hopper_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, 12, 10.0F, 10.0F);
    public static final RegistryObject<HopperBlock> HOPPER_CHROMIUM =
            hopper("hopper_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, 14, 4.0F, 4.0F);
    public static final RegistryObject<HopperBlock> HOPPER_PLATINUM =
            hopper("hopper_platinum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Platinum, 18, 2.0F, 2.0F);
    public static final RegistryObject<HopperBlock> HOPPER_OCTINE =
            hopper("hopper_octine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Octine, 18, 8.0F, 8.0F);
    public static final RegistryObject<HopperBlock> HOPPER_DESH =
            hopper("hopper_desh", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Desh, 18, 15.0F, 15.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TERRASTEEL =
            hopper("hopper_terrasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Terrasteel, 18, 15.0F, 15.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TUNGSTENSTEEL =
            hopper("hopper_tungstensteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, 27, 12.5F, 12.5F);
    public static final RegistryObject<HopperBlock> HOPPER_TUNGSTEN_CARBIDE =
            hopper("hopper_tungsten_carbide", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TungstenCarbide, 27, 12.5F, 12.5F);
    public static final RegistryObject<HopperBlock> HOPPER_DURANIUM =
            hopper("hopper_duranium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Duranium, 27, 20.0F, 20.0F);
    public static final RegistryObject<HopperBlock> HOPPER_DRACONIUM =
            hopper("hopper_draconium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Draconium, 27, 50.0F, 50.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ULTIMET =
            hopper("hopper_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, 27, 12.5F, 12.5F);
    public static final RegistryObject<HopperBlock> HOPPER_DESH_ALLOY =
            hopper("hopper_desh_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.WorkersAlloy, 27, 15.0F, 15.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TUNGSTEN =
            hopper("hopper_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, 36, 10.0F, 10.0F);
    public static final RegistryObject<HopperBlock> HOPPER_PALLADIUM =
            hopper("hopper_palladium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Palladium, 36, 15.0F, 15.0F);
    public static final RegistryObject<HopperBlock> HOPPER_IRIDIUM =
            hopper("hopper_iridium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Iridium, 36, 15.0F, 15.0F);
    public static final RegistryObject<HopperBlock> HOPPER_OSMIUM =
            hopper("hopper_osmium", com.gregtech.gregtech.content.material.generated.ElementMaterials.OsmiumElemental, 36, 9.0F, 9.0F);
    public static final RegistryObject<HopperBlock> HOPPER_VOID_METAL =
            hopper("hopper_void_metal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.VoidMetal, 36, 30.0F, 30.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ELVEN_ELEMENTIUM =
            hopper("hopper_elven_elementium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ElvenElementium, 36, 30.0F, 30.0F);
    public static final RegistryObject<HopperBlock> HOPPER_TRITANIUM =
            hopper("hopper_tritanium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tritanium, 36, 30.0F, 30.0F);
    public static final RegistryObject<HopperBlock> HOPPER_ADAMANTIUM =
            hopper("hopper_adamantium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium, 36, 100.0F, 100.0F);
    public static final RegistryObject<HopperBlock> HOPPER_BEDROCK_HSLA =
            hopper("hopper_bedrock_hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy, 36, 100.0F, 100.0F);
    public static final RegistryObject<HopperBlock> HOPPER_DRACONIUM_AWAKENED =
            hopper("hopper_draconium_awakened", com.gregtech.gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 36, 100.0F, 100.0F);
    public static final RegistryObject<HopperBlock> HOPPER_INFINITY =
            hopper("hopper_infinity", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Infinity, 36, 100.0F, 100.0F);

    // --- Queue Hoppers (GT6 meta 8200+aID, ordered by original GT6 aID) ---

    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_LEAD =
            queueHopper("queue_hopper_lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, 2, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_BISMUTH =
            queueHopper("queue_hopper_bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, 2, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ANTIMONY =
            queueHopper("queue_hopper_antimony", com.gregtech.gregtech.content.material.generated.ElementMaterials.Antimony, 2, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_NICKEL =
            queueHopper("queue_hopper_nickel", com.gregtech.gregtech.content.material.generated.ElementMaterials.Nickel, 3, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_CONSTANTAN =
            queueHopper("queue_hopper_constantan", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Constantan, 3, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_BRONZE =
            queueHopper("queue_hopper_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, 3, 7.0F, 7.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ARSENIC_COPPER =
            queueHopper("queue_hopper_arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, 4, 7.5F, 7.5F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ALUMINIUM =
            queueHopper("queue_hopper_aluminium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Aluminium, 4, 2.0F, 2.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_BRASS =
            queueHopper("queue_hopper_brass", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Brass, 4, 2.5F, 2.5F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TIN_ALLOY =
            queueHopper("queue_hopper_tin_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TinAlloy, 4, 3.0F, 3.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_COBALT =
            queueHopper("queue_hopper_cobalt", com.gregtech.gregtech.content.material.generated.ElementMaterials.Cobalt, 4, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ARDITE =
            queueHopper("queue_hopper_ardite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ardite, 4, 2.0F, 2.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ARSENIC_BRONZE =
            queueHopper("queue_hopper_arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, 5, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_BISMUTH_BRONZE =
            queueHopper("queue_hopper_bismuth_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BismuthBronze, 5, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_GERMANIUM =
            queueHopper("queue_hopper_germanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Germanium, 5, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_INVAR =
            queueHopper("queue_hopper_invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, 5, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_STEEL =
            queueHopper("queue_hopper_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, 5, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_HSLA =
            queueHopper("queue_hopper_hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLASteel, 6, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_GOLD =
            queueHopper("queue_hopper_gold", com.gregtech.gregtech.content.material.generated.ElementMaterials.Gold, 6, 3.0F, 3.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_SILVER =
            queueHopper("queue_hopper_silver", com.gregtech.gregtech.content.material.generated.ElementMaterials.Silver, 6, 3.0F, 3.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_MANGANESE =
            queueHopper("queue_hopper_manganese", com.gregtech.gregtech.content.material.generated.ElementMaterials.Manganese, 6, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_MANYULLYN =
            queueHopper("queue_hopper_manyullyn", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manyullyn, 6, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_LUMIUM =
            queueHopper("queue_hopper_lumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Lumium, 6, 2.0F, 2.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_KNIGHTMETAL =
            queueHopper("queue_hopper_knightmetal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Knightmetal, 7, 7.0F, 7.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_STEEL_GALVANIZED =
            queueHopper("queue_hopper_steel_galvanized", com.gregtech.gregtech.content.material.generated.CompoundMaterials.SteelGalvanized, 7, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_METEORITE =
            queueHopper("queue_hopper_meteorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Meteorite, 7, 7.0F, 7.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_METEORIC_STEEL =
            queueHopper("queue_hopper_meteoric_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.MeteoricSteel, 8, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_GILDED_IRON =
            queueHopper("queue_hopper_gilded_iron", com.gregtech.gregtech.content.material.generated.CompoundMaterials.GildedIron, 8, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_MOLYBDENUM =
            queueHopper("queue_hopper_molybdenum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Molybdenum, 8, 6.0F, 6.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_SYRMORITE =
            queueHopper("queue_hopper_syrmorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Syrmorite, 9, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ELECTRUM =
            queueHopper("queue_hopper_electrum", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Electrum, 9, 3.0F, 3.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_STAINLESS_STEEL =
            queueHopper("queue_hopper_stainless_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.StainlessSteel, 9, 5.0F, 5.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_THAUMIUM =
            queueHopper("queue_hopper_thaumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Thaumium, 9, 9.0F, 9.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_MANASTEEL =
            queueHopper("queue_hopper_manasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manasteel, 9, 9.0F, 9.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_EFRINE =
            queueHopper("queue_hopper_efrine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Efrine, 9, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TUNGSTEN_ALLOY =
            queueHopper("queue_hopper_tungsten_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy, 12, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TITANIUM =
            queueHopper("queue_hopper_titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, 12, 9.0F, 9.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_NETHERITE =
            queueHopper("queue_hopper_netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, 12, 10.0F, 10.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_CHROMIUM =
            queueHopper("queue_hopper_chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, 14, 4.0F, 4.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_PLATINUM =
            queueHopper("queue_hopper_platinum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Platinum, 18, 2.0F, 2.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_OCTINE =
            queueHopper("queue_hopper_octine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Octine, 18, 8.0F, 8.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_DESH =
            queueHopper("queue_hopper_desh", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Desh, 18, 15.0F, 15.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TERRASTEEL =
            queueHopper("queue_hopper_terrasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Terrasteel, 18, 15.0F, 15.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TUNGSTENSTEEL =
            queueHopper("queue_hopper_tungstensteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, 27, 12.5F, 12.5F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TUNGSTEN_CARBIDE =
            queueHopper("queue_hopper_tungsten_carbide", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TungstenCarbide, 27, 12.5F, 12.5F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_DURANIUM =
            queueHopper("queue_hopper_duranium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Duranium, 27, 20.0F, 20.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_DRACONIUM =
            queueHopper("queue_hopper_draconium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Draconium, 27, 50.0F, 50.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ULTIMET =
            queueHopper("queue_hopper_ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, 27, 12.5F, 12.5F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_DESH_ALLOY =
            queueHopper("queue_hopper_desh_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.WorkersAlloy, 27, 15.0F, 15.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TUNGSTEN =
            queueHopper("queue_hopper_tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, 36, 10.0F, 10.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_PALLADIUM =
            queueHopper("queue_hopper_palladium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Palladium, 36, 15.0F, 15.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_IRIDIUM =
            queueHopper("queue_hopper_iridium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Iridium, 36, 15.0F, 15.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_OSMIUM =
            queueHopper("queue_hopper_osmium", com.gregtech.gregtech.content.material.generated.ElementMaterials.OsmiumElemental, 36, 9.0F, 9.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_VOID_METAL =
            queueHopper("queue_hopper_void_metal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.VoidMetal, 36, 30.0F, 30.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ELVEN_ELEMENTIUM =
            queueHopper("queue_hopper_elven_elementium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ElvenElementium, 36, 30.0F, 30.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_TRITANIUM =
            queueHopper("queue_hopper_tritanium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tritanium, 36, 30.0F, 30.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_ADAMANTIUM =
            queueHopper("queue_hopper_adamantium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium, 36, 100.0F, 100.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_BEDROCK_HSLA =
            queueHopper("queue_hopper_bedrock_hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy, 36, 100.0F, 100.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_DRACONIUM_AWAKENED =
            queueHopper("queue_hopper_draconium_awakened", com.gregtech.gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 36, 100.0F, 100.0F);
    public static final RegistryObject<QueueHopperBlock> QUEUE_HOPPER_INFINITY =
            queueHopper("queue_hopper_infinity", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Infinity, 36, 100.0F, 100.0F);

    public static void bootstrap() {
        if (MachineRegistry.solidBurningBoxes().isEmpty()) {
            throw new IllegalStateException("GTMachines failed to initialize solid burning boxes");
        }
        if (MachineRegistry.burningBoxes().isEmpty()) {
            throw new IllegalStateException("GTMachines failed to initialize burning boxes");
        }
        if (SMELTING_CRUCIBLE_STEEL == null || SMELTING_CRUCIBLE_CERAMIC == null) {
            throw new IllegalStateException("GTMachines failed to initialize smelting crucibles");
        }
    }

    private static RegistryObject<SolidBurningBoxBlock> solidBurningBox(
            String id, GTMaterial material, int efficiency, long outputHu, float hardness, float blastResistance) {
        return MachineRegistry.registerSolidBurningBox(
                id, material.getLocalName(), material.getColor(), efficiency, outputHu, hardness, blastResistance);
    }

    private static RegistryObject<BurningBoxBlock> burningBox(
            String id, GTMaterial material, BurningBoxFuelType fuelType,
            int efficiency, long outputHu, float hardness, float blastResistance) {
        String textureSet = switch (fuelType) {
            case LIQUID -> MachineTextures.BURNING_LIQUID;
            case GAS -> MachineTextures.BURNING_GAS;
            case FLUIDIZED_BED -> MachineTextures.BURNING_FLUIDBED;
            case SOLID -> MachineTextures.BURNING_SOLID;
        };
        return MachineRegistry.registerBurningBox(
                fuelType, id, material.getLocalName(), material.getColor(),
                efficiency, outputHu, hardness, blastResistance, textureSet);
    }

    private static RegistryObject<SmeltingCrucibleBlock> crucible(String id, GTMaterial material, int gt6MetaId,
                                                                 float hardness, float blastResistance, boolean acidProof) {
        return MachineRegistry.registerSmeltingCrucible(
                CrucibleSpec.of(id, material, gt6MetaId, hardness, blastResistance, acidProof));
    }

    private static RegistryObject<SmeltingCrucibleBlock> crucible(String id, GTMaterial material, int gt6MetaId,
                                                                 float hardness, float blastResistance, boolean acidProof,
                                                                 int meltingPointK, int boilingPointK, double hullDensity) {
        return MachineRegistry.registerSmeltingCrucible(CrucibleSpec.of(
                id, material, gt6MetaId, hardness, blastResistance, acidProof, meltingPointK, boilingPointK, hullDensity));
    }

    private static RegistryObject<SmeltingCrucibleBlock> crucible(String id, GTMaterial material, int gt6MetaId,
                                                                 float hardness, float blastResistance, boolean acidProof,
                                                                 int meltingPointK, int boilingPointK, double hullDensity,
                                                                 long hullMaterialUnits) {
        return MachineRegistry.registerSmeltingCrucible(CrucibleSpec.of(
                id, material, gt6MetaId, hardness, blastResistance, acidProof,
                meltingPointK, boilingPointK, hullDensity, hullMaterialUnits));
    }

    private static RegistryObject<HopperBlock> hopper(String id, GTMaterial material, int slotCount,
                                                       float hardness, float blastResistance) {
        return MachineRegistry.registerHopper(HopperSpec.of(id, material, slotCount, hardness, blastResistance));
    }

    private static RegistryObject<QueueHopperBlock> queueHopper(String id, GTMaterial material, int slotCount,
                                                                 float hardness, float blastResistance) {
        return MachineRegistry.registerQueueHopper(HopperSpec.of(id, material, slotCount, hardness, blastResistance));
    }
}
