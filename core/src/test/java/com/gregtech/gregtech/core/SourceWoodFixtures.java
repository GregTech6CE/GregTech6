package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.generated.GT6Materials;

/** Literal MT.java woodnormal rows; independent of the port's generated holder and factories. */
public final class SourceWoodFixtures {
    private SourceWoodFixtures() {}
    private static final String ROWS = """
9300,Oak,B4905A,3.0,32,false
9301,Birch,D7CC8E,2.5,24,false
9302,Spruce,664F2F,3.0,24,false
9303,Junglewood,B1805C,2.0,16,false
9304,Acacia,BA683B,2.5,24,false
9305,DarkOak,462D15,3.5,32,false
9306,Crimsonwood,B45A6A,2.5,24,false
9307,Warpedwood,2A8D85,3.5,32,false
9408,Foxfirewood,333365,4.0,24,false
9308,WoodCompressed,5E3C19,1.5,8,false
9309,WoodDead,746C3F,1.5,8,false
9310,WoodRotten,162C0F,1.0,8,false
9311,WoodMossy,1D7F00,1.5,8,false
9312,WoodFrozen,547D7D,1.0,8,false
9404,WoodScorched,2C2C2C,1.0,8,false
9405,WoodVarnished,493010,3.0,32,false
9407,WoodBleached,FFFFFF,2.0,16,false
9406,WoodTainted,5A17E7,1.0,64,true
9313,Maple,971A1A,3.0,24,false
9314,Willow,259600,2.0,16,false
9315,BlueMahoe,0F67FE,3.0,24,false
9316,Hazel,E4AFAF,2.5,16,false
9317,Cinnamonwood,41C0C0,1.5,16,false
9318,Coconutwood,FFAA00,3.0,16,false
9319,Rainbowood,C840F5,4.0,64,true
9409,BlueSpruce,D5D5D9,3.0,24,false
9320,Towerwood,A6653A,4.0,64,false
9321,Witchwood,76708E,3.5,48,true
9322,Ogrewood,B45A6A,4.0,48,false
9323,Wyvernwood,4D9F9E,4.0,48,false
9324,Aspen,444132,2.0,24,false
9325,DouglasFir,F9C59A,2.5,24,false
9326,Sycamore,D69B45,3.0,16,false
9327,WhiteCedar,DBDBCD,2.5,24,false
9328,WhiteElm,A2A767,2.0,32,false
9329,Thorntree,B4905A,3.0,32,false
9330,SilverPine,200746,3.0,32,false
9331,Alder,B15F57,2.5,32,false
9332,Hawthorn,BCB6B2,3.0,24,false
9333,Rowan,CDAC57,3.5,24,false
9356,Mahogany,6F3D37,4.0,48,false
9358,Palm,C97C45,3.0,16,false
9334,Autumnwood,BF4023,2.5,24,false
9336,Cypress,B9BBB5,2.5,16,false
9337,Fir,6E6A3F,2.0,32,false
9338,JapaneseMaple,984C56,3.0,24,false
9339,RainbowEucalyptus,748DC6,3.0,32,false
9340,Redwood,A37346,3.5,24,false
9341,Sakura,FAA17A,3.0,32,false
9342,Balsa,A59E97,2.0,16,false
9343,Baobab,88915F,2.0,16,false
9344,Cherrywood,AD7C32,2.5,24,false
9345,Chestnutwood,B3A255,3.0,32,false
9346,Citruswood,98A31C,2.5,24,false
9347,Cocobolowood,791202,3.0,16,false
9348,Ebony,3A342E,4.0,48,false
9349,Giganteumwood,662F27,2.0,16,false
9350,Greenheart,4C7658,2.5,16,false
9351,Ipe,653A27,2.0,24,false
9352,Kapok,746C34,2.0,24,false
9353,Larch,D79785,2.5,16,false
9354,Limewood,CE9A68,2.5,24,false
9355,Mahoe,7993A6,3.0,24,false
9357,Padauk,B3633B,2.0,24,false
9359,Papayawood,DAC86D,3.0,16,false
9360,Plumwood,AB637B,2.5,16,false
9361,Poplar,CCCC7B,2.5,24,false
9362,Sequoia,8E5754,2.0,24,false
9363,Teak,7B735F,3.0,16,false
9364,Walnutwood,624E40,3.0,32,false
9365,Wenge,585146,2.5,16,false
9366,Zebrawood,AC8B56,2.0,24,false
9335,Pine,BB974D,3.0,32,false
9367,Darkwood,332D36,2.5,32,false
9368,Etherealwood,4C9673,3.0,24,false
9369,Goldwood,D2BB97,2.5,24,false
9370,Hellbark,C89664,4.0,16,false
9371,Jacaranda,C9ABA2,2.5,16,false
9372,Mangrove,ECE4D9,2.0,24,false
9373,SacredOak,9F844D,4.0,48,false
9374,Magicwood,5A69B4,3.5,32,true
9375,Applewood,613124,2.0,24,false
9376,Ashwood,F4BE5A,3.5,16,false
9377,Beech,E29044,2.0,32,false
9378,Boxwood,FDEDC0,2.0,24,false
9379,Brazilwood,703754,3.0,16,false
9380,Butternutwood,EDA370,2.5,16,false
9381,Cedar,D95825,2.0,24,false
9382,Elderwood,BD8D73,2.5,16,false
9383,Elm,F3A35A,3.0,24,false
9384,Eucalyptus,F5A482,2.5,24,false
9385,Figwood,CA7E1B,2.0,16,false
9386,Gingko,F3E2AD,2.0,24,false
9387,Hemlock,C4AE60,3.0,16,false
9388,Hickory,DAAE86,2.5,16,false
9389,Holly,F8F2E2,2.0,24,false
9390,Hornbeam,C39357,2.0,16,false
9391,Iroko,752F00,3.0,24,false
9392,Locust,C38C57,2.0,24,false
9393,Logwood,A62C22,2.5,24,false
9394,Maclura,F2A81D,2.0,32,false
9395,Olivewood,AEA981,3.0,16,false
9396,Pearwood,B47F61,2.5,24,false
9397,PinkIvory,EA7D94,2.5,24,false
9398,Purpleheart,5B162D,2.0,16,false
9399,Rosewood,800C00,3.0,16,false
9400,Sweetgum,D78C4A,2.5,16,false
9401,Syzgium,DDB8B7,2.5,24,false
9402,Whitebeam,C0B7AE,3.0,16,false
9403,Yew,E2A072,2.5,32,false
""";
    public static int validate() {
        int checks=0;
        var carbon=GTMaterialRegistry.get("Carbon"); var water=GTMaterialRegistry.get("Water"); var ash=GTMaterialRegistry.get("Ashes");
        for(String line:ROWS.strip().split("\n")) {
            String[] row=line.split(","); var m=GTMaterialRegistry.get(row[1]);
            if(m.getId()!=Integer.parseInt(row[0])||m.getColor()!=Integer.parseInt(row[2],16)) fail("identity/RGB",m); checks++;
            if(m.getToolTypes()!=1||m.getToolQuality()!=0||Float.compare(m.getToolSpeed(),Float.parseFloat(row[3]))!=0
                    ||m.getToolDurability()!=Long.parseLong(row[4])) fail("qual(1,speed,durability,0)",m); checks++;
            if(m.getMeltingPoint()!=400||m.getBoilingPoint()!=500) fail("heat(400,500)",m); checks++;
            if(m.has(MaterialProperty.MAGICAL)!=Boolean.parseBoolean(row[5])||!m.has(MaterialProperty.WOOD)) fail("wood/magical flags",m); checks++;
            var components=m.getCompositionComponents();
            if(components.size()!=2||components.get(0).material()!=carbon||components.get(0).amount()!=6*GregTechConstants.U
                    ||components.get(1).material()!=water||components.get(1).amount()!=15*GregTechConstants.U
                    ||m.getCompositionDivider()!=21) fail("C6(H2O)15",m); checks++;
            if(m.getFurnaceBurnTime()!=100||m.getTargetBurningMaterial()!=ash||m.getTargetBurningAmount()!=GregTechConstants.U9
                    ||m.getTargetSmeltingMaterial()!=ash||m.getTargetSmeltingAmount()!=GregTechConstants.U4) fail("fuel/ash",m); checks++;
            for(var family:java.util.List.of(MaterialGroups.Wood,MaterialGroups.WoodPlastic,MaterialGroups.WoodNormal,MaterialGroups.WoodDefault,MaterialGroups.WoodUntreated)) {
                if(!family.getReRegistrations().contains(m)) fail("missing "+family.getName(),m); checks++;
            }
        }
        var magic=GTMaterialRegistry.get("Magicwood");
        if(GT6Materials.Woods.Magic!=magic||magic.getId()!=9374||magic==GTMaterialRegistry.get("Magic")) fail("WOODS.Magic",magic); checks++;
        if(!MaterialGroups.WoodMagical.getReRegistrations().contains(magic)
                ||MaterialGroups.WoodMagical.getReRegistrations().contains(GTMaterialRegistry.get("Magic"))) fail("magical wood family",magic); checks++;
        System.out.println("Source wood fixtures passed: 110 MT.woodnormal rows; "+checks+" assertions");
        return checks;
    }
    private static void fail(String property,GTMaterial material) {throw new AssertionError(property+": "+material);}
}
