package com.gregtech.gregtech.data.generated;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialComponent;
import com.gregtech.gregtech.api.material.MaterialChemistry;

/** Auto-generated from GT6 MT.java composition chains. */
public final class MaterialCompositionData {
    private MaterialCompositionData() {}

    public static void apply() {
        bind("H2O", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("HDO", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("D"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("D2O", 0,
                MaterialComponent.of(GTMaterialRegistry.get("D"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("T2O", 0,
                MaterialComponent.of(GTMaterialRegistry.get("T"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Steam", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Snow", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Ice", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("FreshWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("HolyWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("SeaWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("DirtyWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("DistWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("H2O2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("HCl", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("HF", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 1));
        bind("HeNe", 0,
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ne"), GTValues.U * 1));
        bind("Air", 0,
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 40),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 11),
                MaterialComponent.of(GTMaterialRegistry.get("Ar"), GTValues.U * 1));
        bind("NO", 0,
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("NO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("NH3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 3));
        bind("HNO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("CO", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("CO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("CO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("CH4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 4));
        bind("Sugar", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 22),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 11));
        bind("Vanilla", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Glycerol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Glyceryl", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 9));
        bind("SO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("SO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("H2S", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("H2SO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("H2S2O7", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("AgI", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("I"), GTValues.U * 1));
        bind("H2SiF6", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("SiC", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("SiO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Glass", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("Flint", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("H3BO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("B"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Datolite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("B"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 10));
        bind("V2O5", 0,
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 5));
        bind("Nb2O5", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Nb"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 5));
        bind("Ta2O5", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ta"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 5));
        bind("PO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("P"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("WO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("H2WO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Al2O3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("AlF3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 3));
        bind("AlO3H3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 3));
        bind("TiO2", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("TiCl4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 4));
        bind("MnO2", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("MnCl2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 2));
        bind("Fe2O3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("FeCl2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 2));
        bind("FeCl3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 3));
        bind("FeO3H3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 3));
        bind("MgCl2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 2));
        bind("MgCO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 4));
        bind("CaCl2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 2));
        bind("CaSO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Gypsum", 6,
                MaterialComponent.of(GTMaterialRegistry.get("CaSO4"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6));
        bind("Quicklime", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("CaCO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 4));
        bind("LiCl", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("LiClO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("LiClO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Li2O", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Li2Fe2O4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("LiOH", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1));
        bind("NaCl", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("NaNO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("NaOH", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1));
        bind("NaHCO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("NaHSO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("NaSO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Na2S", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Na2SO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Na2SO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Na2S2O7", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("Na2CO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 4));
        bind("NaAlO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("NaF", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 1));
        bind("Na3AlF6", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("SaltWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("NaCl"), GTValues.U * 1));
        bind("KIO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("I"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("KCl", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("KNO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("KOH", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1));
        bind("KHSO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("KSO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("K2S", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("K2SO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("K2SO4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("K2S2O7", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("K2CO3", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 4));
        bind("KAlO2", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("KF", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 1));
        bind("K2TaF7", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ta"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 7));
        bind("SaltedWater", 0,
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("KCl"), GTValues.U * 1));
        bind("ChloroauricAcid", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1));
        bind("ChloroplatinicAcid", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("StannicChloride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 4));
        bind("BlackVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("BlueVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("GreenVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("RedVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("PinkVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("CyanVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("WhiteVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("GrayVitriol", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("MartianVitriol", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 12));
        bind("VitriolOfClay", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 9));
        bind("UF4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_238"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 4));
        bind("UF6", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_238"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("U238F4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_238"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 4));
        bind("U238F6", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_238"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("U235F4", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_235"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 4));
        bind("U235F6", 0,
                MaterialComponent.of(GTMaterialRegistry.get("U_235"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("AquaRegia", 0,
                MaterialComponent.of(GTMaterialRegistry.get("HNO3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("HCl"), GTValues.U * 8));
        bind("CobaltHexahydrate", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6));
        bind("MethaneIce", 2,
                MaterialComponent.of(GTMaterialRegistry.get("CH4"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ice"), GTValues.U * 2));
        bind("NitroCarbon", 0,
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("NitroFuel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Glyceryl"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fuel"), GTValues.U * 4));
        bind("VolcanicAsh", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Fe2O3"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1));
        bind("Chalk", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 1));
        bind("Dolomite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MgCO3"), GTValues.U * 1));
        bind("Asbestos", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Talc", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Pyrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2));
        bind("PotassiumFeldspar", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Biotite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18));
        bind("Bark", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Wood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("WoodTreated", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("WoodPolished", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("WoodRubber", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Bamboo", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Skyroot", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Weedwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Livingwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Dreamwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Shimmerwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Greatwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Silverwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("Peanutwood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15));
        bind("LiveRoot", 3,
                MaterialComponent.of(GTMaterialRegistry.get("Wood"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("PetrifiedWood", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Wood"), GTValues.U * 1));
        bind("Ceramic", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12));
        bind("Brick", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Ceramic"), GTValues.U * 1));
        bind("Clay", 2,
                MaterialComponent.of(GTMaterialRegistry.get("Ceramic"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 1));
        bind("Porcelain", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ceramic"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("PotassiumFeldspar"), GTValues.U * 1));
        bind("Graphite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("Niter", 0,
                MaterialComponent.of(GTMaterialRegistry.get("KNO3"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("NaNO3"), GTValues.U * 1));
        bind("Phosphorus", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 2));
        bind("PhosphorusBlue", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 2));
        bind("PhosphorusRed", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 2));
        bind("PhosphorusWhite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 2));
        bind("Apatite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("Phosphorite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 1));
        bind("Rubber", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 8));
        bind("Plastic", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("Teflon", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("PVC", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("Bakelite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("Polycarbonate", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("Bone", 8,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1));
        bind("SlimyBone", 8,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1));
        bind("Gunpowder", 4,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("NaNO3"), GTValues.U * 1));
        bind("Dynamite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Glyceryl"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Wood"), GTValues.U * 1));
        bind("Chocolate", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cocoa"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Sugar"), GTValues.U * 1));
        bind("Butter", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Milk"), GTValues.U * 1));
        bind("ButterSalted", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Milk"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("NaCl"), GTValues.U * 1));
        bind("Diamond", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondBlue", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondGreen", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondPurple", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondRed", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondYellow", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondPink", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("DiamondIndustrial", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("ManaDiamond", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("ElvenDragonstone", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 2));
        bind("Gravitite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Gt"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Emerald", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Aquamarine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Morganite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Heliodor", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Goshenite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Bixbite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Maxixe", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Sapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5));
        bind("Ruby", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1));
        bind("BlueSapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("GreenSapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1));
        bind("PurpleSapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 1));
        bind("YellowSapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("TiO2"), GTValues.U * 1));
        bind("OrangeSapphire", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1));
        bind("Spinel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("BalasRuby", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Almandine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Grossular", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Pyrope", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Spessartine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Andradite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 6));
        bind("Uvarovite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 6));
        bind("Topaz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3));
        bind("BlueTopaz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3));
        bind("Tanzanite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Zanite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Amazonite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Alexandrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Opal", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("OnyxRed", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("OnyxBlack", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("Sugilite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("MnO2"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 36),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Peridot", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 2));
        bind("Amethyst", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("Dioptase", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3));
        bind("Vinteum", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("VinteumPurified", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("EnderAmethyst", 5,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("EnderPearl", 10,
                MaterialComponent.of(GTMaterialRegistry.get("Be"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 6));
        bind("EnderEye", 9,
                MaterialComponent.of(GTMaterialRegistry.get("EnderPearl"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Blaze"), GTValues.U * 1));
        bind("Zircon", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Zr"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Azurite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3));
        bind("Eudialyte", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Zircon"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("MnO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 75),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 12));
        bind("Lazurite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 8));
        bind("Sodalite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 1));
        bind("Lapis", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Lazurite"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("Sodalite"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Pyrite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 1));
        bind("Charcoal", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("Coal", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("CoalCoke", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("Anthracite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 2));
        bind("Prismane", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 4));
        bind("Lonsdaleite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 8));
        bind("Lignite", 7,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 1));
        bind("LigniteCoke", 7,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 1));
        bind("PetCoke", 1,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("HydratedCoal", 8,
                MaterialComponent.of(GTMaterialRegistry.get("Coal"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 1));
        bind("Graphene", 0,
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("Redstone", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pyrite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Hg"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ruby"), GTValues.U * 1));
        bind("Nikolite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sodalite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ar"), GTValues.U * 1));
        bind("Glowstone", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("GlowstoneCeres", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("GlowstoneIo", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("GlowstoneEnceladus", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("GlowstoneProteus", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("GlowstonePluto", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("Gloomstone", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Phosphorite"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("He"), GTValues.U * 1));
        bind("MilkyQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("NetherQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("VoidQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("SunnyQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("LavenderQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("RedQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("BlazeQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("SmokeyQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("ManaQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("ElvenQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("BlackQuartz", 1,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("CertusQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("ChargedCertusQuartz", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 1));
        bind("Fluix", 2,
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1));
        bind("EnergiumRed", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sapphire"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 5));
        bind("EnergiumCyan", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sapphire"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Nikolite"), GTValues.U * 5));
        bind("Monazite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("RareEarth"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("PO4"), GTValues.U * 1));
        bind("Concrete", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Stone"), GTValues.U * 1));
        bind("Obsidian", 64,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Oilshale", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("MilkyQuartz"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Clay"), GTValues.U * 1));
        bind("Petrotheum", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Clay"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Basalz"), GTValues.U * 1));
        bind("Aerotheum", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Sand"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("KNO3"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Blitz"), GTValues.U * 1));
        bind("Pyrotheum", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Coal"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Blaze"), GTValues.U * 1));
        bind("Cryotheum", 18,
                MaterialComponent.of(GTMaterialRegistry.get("Snow"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("KNO3"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Blizz"), GTValues.U * 1));
        bind("WroughtIron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("AnnealedCopper", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1));
        bind("Adamantine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ad"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Prometheum", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pm"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Vulcanite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Te"), GTValues.U * 1));
        bind("Orichalcum", 4,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 2));
        bind("AstralSilver", 2,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Midasium", 2,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Mithril", 2,
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Celenegil", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Orichalcum"), GTValues.U * 1));
        bind("ShadowSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("ShadowIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Lemurite"), GTValues.U * 1));
        bind("Inolashite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Alduorite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ceruclase"), GTValues.U * 1));
        bind("Haderoth", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mithril"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Rubracium"), GTValues.U * 1));
        bind("Desichalkos", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Eximite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Meutoite"), GTValues.U * 1));
        bind("Tartarite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Adamantine"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Atl"), GTValues.U * 1));
        bind("Amordrine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Prometheum"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Kalendrite"), GTValues.U * 1));
        bind("Electrum", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1));
        bind("SterlingSilver", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 4));
        bind("RoseGold", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 4));
        bind("Angmallen", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1));
        bind("InductiveAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1));
        bind("Cd_In_Ag_Alloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cd"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("In"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1));
        bind("GildedIron", 9,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1));
        bind("Brass", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1));
        bind("CobaltBrass", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Brass"), GTValues.U * 7),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1));
        bind("AluminiumAlloy", 45,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 45),
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1));
        bind("Bronze", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1));
        bind("BlackBronze", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Electrum"), GTValues.U * 2));
        bind("BismuthBronze", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Bi"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Brass"), GTValues.U * 4));
        bind("Hepatizon", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Bronze"), GTValues.U * 1));
        bind("ArsenicCopper", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 1));
        bind("ArsenicBronze", 0,
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Bronze"), GTValues.U * 4));
        bind("Steel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1));
        bind("BlackSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("BlackBronze"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 3));
        bind("BlueSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SterlingSilver"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("BismuthBronze"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("BlackSteel"), GTValues.U * 4));
        bind("RedSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("RoseGold"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Brass"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("BlackSteel"), GTValues.U * 4));
        bind("DamascusSteel", 50,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 50),
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1));
        bind("VanadiumSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 1));
        bind("TungstenSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1));
        bind("TungstenCarbide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("HSLA", 2,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1));
        bind("SpringSteel", 45,
                MaterialComponent.of(GTMaterialRegistry.get("HSLA"), GTValues.U * 45),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 2));
        bind("TungstenAlloy", 180,
                MaterialComponent.of(GTMaterialRegistry.get("SpringSteel"), GTValues.U * 180),
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1));
        bind("PigIron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1));
        bind("IronCompressed", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("IronCast", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("IronMagnetic", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("SteelMagnetic", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1));
        bind("NeodymiumMagnetic", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Nd"), GTValues.U * 1));
        bind("DarkIron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("SteelGalvanized", 9,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1));
        bind("TungstenSintered", 0,
                MaterialComponent.of(GTMaterialRegistry.get("W"), GTValues.U * 1));
        bind("TitaniumGold", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1));
        bind("Ta4HfC5", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ta"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Hf"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 5));
        bind("MeteoricIron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("MeteoricSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricIron"), GTValues.U * 1));
        bind("MeteoricBlackSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("BlackBronze"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricSteel"), GTValues.U * 3));
        bind("MeteoricBlueSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("SterlingSilver"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("BismuthBronze"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricSteel"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricBlackSteel"), GTValues.U * 4));
        bind("MeteoricRedSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("RoseGold"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Brass"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricSteel"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricBlackSteel"), GTValues.U * 4));
        bind("RedAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 4));
        bind("BlueAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Nikolite"), GTValues.U * 4));
        bind("PurpleAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("RedAlloy"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("BlueAlloy"), GTValues.U * 1));
        bind("Mingrade", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1));
        bind("RedstoneAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1));
        bind("NikolineAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Nikolite"), GTValues.U * 1));
        bind("ElectrotineAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Nikolite"), GTValues.U * 8));
        bind("ElectrumFlux", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Electrum"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 2));
        bind("ConductiveIron", 1,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1));
        bind("EnergeticSilver", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Redstone"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Glowstone"), GTValues.U * 1));
        bind("Invar", 0,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1));
        bind("Constantan", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1));
        bind("Nichrome", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1));
        bind("Kanthal", 0,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1));
        bind("Magnalium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 2));
        bind("StainlessSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Invar"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1));
        bind("Ultimet", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Mo"), GTValues.U * 1));
        bind("TinAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1));
        bind("BatteryAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Sb"), GTValues.U * 1));
        bind("SolderingAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Sb"), GTValues.U * 1));
        bind("IronWood", 18,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("LiveRoot"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Angmallen"), GTValues.U * 2));
        bind("Steeleaf", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Knightmetal", 2,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("FierySteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Fireleaf", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 2));
        bind("MeteoflameSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("MeteoflameBlackSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricBlackSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("MeteoflameBlueSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricBlueSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("MeteoflameRedSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricRedSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("FlamascusSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("DamascusSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Thaumium", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Osmiridium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Os"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ir"), GTValues.U * 1));
        bind("ChromiumDioxide", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("VanadiumGallium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Ga"), GTValues.U * 1));
        bind("YttriumBariumCuprate", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Y"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ba"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("NiobiumNitride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Nb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("N"), GTValues.U * 1));
        bind("NiobiumTitanium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Nb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1));
        bind("AluminiumBrass", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1));
        bind("Alumite", 9,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 18));
        bind("Manyullyn", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ardite"), GTValues.U * 1));
        bind("VibraniumSteel", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Vb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 3));
        bind("VibraniumSilver", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Vb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 3));
        bind("Vibramantium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Vb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ad"), GTValues.U * 3));
        bind("Signalum", 8,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("RedAlloy"), GTValues.U * 5));
        bind("Lumium", 4,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Glowstone"), GTValues.U * 4));
        bind("EnderiumBase", 4,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 1));
        bind("Enderium", 1,
                MaterialComponent.of(GTMaterialRegistry.get("EnderiumBase"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("EnderPearl"), GTValues.U * 1));
        bind("RefinedGlowstone", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Glowstone"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ge"), GTValues.U * 1));
        bind("RefinedObsidian", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Diamond"), GTValues.U * 1));
        bind("Yellorite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Yellorium"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Bedrock_HSLA_Alloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Bedrock"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("HSLA"), GTValues.U * 1));
        bind("ObsidianSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 9));
        bind("PulsatingIron", 1,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("EnderPearl"), GTValues.U * 1));
        bind("EnergeticAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("InductiveAlloy"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Glowstone"), GTValues.U * 1));
        bind("VibrantAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("EnergeticAlloy"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("EnderPearl"), GTValues.U * 1));
        bind("ElectricalSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Steel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1));
        bind("Soularium", 1,
                MaterialComponent.of(GTMaterialRegistry.get("SoulSand"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 1));
        bind("CrudeSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Stone"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ceramic"), GTValues.U * 1));
        bind("EndSteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Endstone"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("ObsidianSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 9));
        bind("MelodicAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("EndSteel"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("EnderEye"), GTValues.U * 1));
        bind("StellarAlloy", 2,
                MaterialComponent.of(GTMaterialRegistry.get("MelodicAlloy"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("NetherStar"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Clay"), GTValues.U * 4));
        bind("VividAlloy", 1,
                MaterialComponent.of(GTMaterialRegistry.get("EnergeticSilver"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("EnderPearl"), GTValues.U * 1));
        bind("SpectreIron", 1,
                MaterialComponent.of(GTMaterialRegistry.get("WroughtIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ectoplasm"), GTValues.U * 1));
        bind("Manasteel", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        bind("Elvorium", 1,
                MaterialComponent.of(GTMaterialRegistry.get("ElvenElementium"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("ElvenDragonstone"), GTValues.U * 1));
        bind("NiflheimPower", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Elvorium"), GTValues.U * 1));
        bind("MuspelheimPower", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Elvorium"), GTValues.U * 1));
        bind("Netherite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Au"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("AncientDebris"), GTValues.U * 4));
        bind("NetherizedDiamond", 4,
                MaterialComponent.of(GTMaterialRegistry.get("Netherite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Diamond"), GTValues.U * 4));
        bind("Desh", 0,
                MaterialComponent.of(GTMaterialRegistry.get("B"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("La"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Nd"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Nb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ce"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 1));
        bind("DeshAlloy", 4,
                MaterialComponent.of(GTMaterialRegistry.get("Desh"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Hg"), GTValues.U * 1));
        bind("DuraniumAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 7),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1));
        bind("TritaniumAlloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1));
        bind("Duralumin", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1));
        bind("Meteorite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("FrozenIron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("HSSG", 0,
                MaterialComponent.of(GTMaterialRegistry.get("TungstenSteel"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mo"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("V"), GTValues.U * 1));
        bind("HSSE", 0,
                MaterialComponent.of(GTMaterialRegistry.get("HSSG"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Si"), GTValues.U * 1));
        bind("HSSS", 0,
                MaterialComponent.of(GTMaterialRegistry.get("HSSG"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Osmiridium"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ir"), GTValues.U * 1));
        bind("DraconiumAwakened", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Draconium"), GTValues.U * 1));
        bind("CrystalMatrix", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Diamond"), GTValues.U * 20),
                MaterialComponent.of(GTMaterialRegistry.get("NetherStar"), GTValues.U * 2));
        bind("Trinaquadalloy", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ke"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("Nq"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1));
        bind("Trinitanium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ke"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1));
        bind("Iritanium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ir"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1));
        bind("TitaniumAluminide", 3,
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Al"), GTValues.U * 7));
        bind("Cassiterite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Garnierite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Uraninite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("U_238"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Magnetite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("BasalticMineralSand", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("GraniticMineralSand", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Realgar", 0,
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Cinnabar", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Hg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Molybdenite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mo"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2));
        bind("Sphalerite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Stibnite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sb"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 3));
        bind("Pentlandite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 9),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 8));
        bind("Chalcopyrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2));
        bind("Arsenopyrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Cobaltite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Co"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Galena", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 2));
        bind("Cooperite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Ni"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Pd"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1));
        bind("Tetrahedrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Sb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 3));
        bind("Kesterite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 4));
        bind("Stannite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Sn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 4));
        bind("Barite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ba"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Celestine", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Sr"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("S"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Scheelite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Wolframite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Ferberite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Huebnerite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Tungstate", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Stolzite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Russellite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Bi"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Pinalite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("WO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Wollastonite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Zeolite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Pollucite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Cs"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("BrownLimonite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("YellowLimonite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Ferrovanadium", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Magnetite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("V2O5"), GTValues.U * 1));
        bind("Tantalite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ta2O5"), GTValues.U * 7),
                MaterialComponent.of(GTMaterialRegistry.get("MnO2"), GTValues.U * 1));
        bind("Columbite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Nb2O5"), GTValues.U * 7),
                MaterialComponent.of(GTMaterialRegistry.get("MnO2"), GTValues.U * 1));
        bind("Coltan", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tantalite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Columbite"), GTValues.U * 1));
        bind("Ilmenite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ti"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Bauxite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("TiO2"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Ilmenite"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 2));
        bind("Chromite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cr"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Powellite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ca"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mo"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Wulfenite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mo"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("Bastnasite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ce"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Pitchblende", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Uraninite"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Th"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Pb"), GTValues.U * 1));
        bind("Malachite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Cu"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("CO3"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Bromargyrite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ag"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Br"), GTValues.U * 1));
        bind("Smithsonite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Zn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("C"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("Sperrylite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Pt"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("As"), GTValues.U * 2));
        bind("Perlite", 1,
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 1));
        bind("Trona", 6,
                MaterialComponent.of(GTMaterialRegistry.get("Na2CO3"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6));
        bind("Mirabilite", 7,
                MaterialComponent.of(GTMaterialRegistry.get("Na2SO4"), GTValues.U * 7),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 30));
        bind("Bischofite", 3,
                MaterialComponent.of(GTMaterialRegistry.get("MgCl2"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 6));
        bind("Borax", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("B"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 30),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("Diatomite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("Fe2O3"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Sapphire"), GTValues.U * 1));
        bind("Spodumene", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 1));
        bind("Lepidolite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 10),
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Li"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 6));
        bind("Glauconite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 10),
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 7));
        bind("Vermiculite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 10),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 12),
                MaterialComponent.of(GTMaterialRegistry.get("H"), GTValues.U * 2));
        bind("Mica", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("K"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 18),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 4));
        bind("Kyanite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 5),
                MaterialComponent.of(GTMaterialRegistry.get("SiO2"), GTValues.U * 3));
        bind("Alunite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Al2O3"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("KOH"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("SO3"), GTValues.U * 16),
                MaterialComponent.of(GTMaterialRegistry.get("H2O"), GTValues.U * 15),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 9));
        bind("GarnetSand", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Almandine"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Andradite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Grossular"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Pyrope"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Spessartine"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Uvarovite"), GTValues.U * 1));
        bind("QuartzSand", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CertusQuartz"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MilkyQuartz"), GTValues.U * 1));
        bind("DiduraniumTrioxide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 3));
        bind("DuraniumHexafluoride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("DuraniumHexachloride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 6));
        bind("DuraniumHexabromide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Br"), GTValues.U * 6));
        bind("DuraniumHexaiodide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("I"), GTValues.U * 6));
        bind("DuraniumHexaastatide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Dn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("At"), GTValues.U * 6));
        bind("TritaniumDioxide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("TritaniumHexafluoride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("F"), GTValues.U * 6));
        bind("TritaniumHexachloride", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Cl"), GTValues.U * 6));
        bind("TritaniumHexabromide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Br"), GTValues.U * 6));
        bind("TritaniumHexaiodide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("I"), GTValues.U * 6));
        bind("TritaniumHexaastatide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Tn"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("At"), GTValues.U * 6));
        bind("SkyStone", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Peridot"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("RareEarth"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MeteoricIron"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Obsidian"), GTValues.U * 5));
        bind("Shale", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("MilkyQuartz"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Clay"), GTValues.U * 1));
        bind("Redrock", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("ClayRed"), GTValues.U * 1));
        bind("Komatiite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Peridot"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("MgCO3"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 6),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 3));
        bind("Pumice", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Peridot"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("MgCO3"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 4),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 2));
        bind("Gabbro", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Peridot"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 4));
        bind("Basalt", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Peridot"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 3),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 8),
                MaterialComponent.of(GTMaterialRegistry.get("DarkAsh"), GTValues.U * 4));
        bind("Marble", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Mg"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 7));
        bind("Limestone", 0,
                MaterialComponent.of(GTMaterialRegistry.get("CaCO3"), GTValues.U * 1));
        bind("GraniteRed", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Biotite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("PotassiumFeldspar"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 1));
        bind("GraniteBlack", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Biotite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("PotassiumFeldspar"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 1));
        bind("Granite", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Biotite"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("PotassiumFeldspar"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Flint"), GTValues.U * 1));
        bind("OsmiumTetroxide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Os"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 4));
        bind("SodiumPeroxide", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Na"), GTValues.U * 2),
                MaterialComponent.of(GTMaterialRegistry.get("O"), GTValues.U * 2));
        bind("Iridiron", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ir"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("IridironReinforced", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ir"), GTValues.U * 1),
                MaterialComponent.of(GTMaterialRegistry.get("Fe"), GTValues.U * 1));
        bind("Vis", 0,
                MaterialComponent.of(GTMaterialRegistry.get("Ma"), GTValues.U * 1));
        MaterialChemistry.rebuildAll();
    }

    private static void bind(String materialName, long divider, MaterialComponent... components) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (!material.isValid()) {
            return;
        }
        java.util.ArrayList<MaterialComponent> valid = new java.util.ArrayList<>();
        for (MaterialComponent component : components) {
            if (component != null && component.material().isValid()) {
                valid.add(component);
            }
        }
        if (valid.isEmpty()) {
            return;
        }
        material.setComposition(divider, valid.toArray(MaterialComponent[]::new));
    }
}
