package com.gregtech.gregtech.content.bumble;
/** Original20 species combinations, mutation and comb probabilities. */
public final class BumbleSpeciesRules {
 private BumbleSpeciesRules(){}
    private static final int[][] COMBINATIONS = {
            // case 3: Cultivated Bumblebee
            {30, 130, 10100},    // Normal + Water = Sticky
            {30, 530, 10000},    // Normal + Rocky = Clay
            {30, 930, 10200},    // Normal + Sandy = Royal
            // case 13: Subnautic Bumblebee
            {130, 30, 10100},    // Normal + Water = Sticky (reverse)
            {130, 530, 10400},   // Rock + Water = Amnesic / Lubricant
            // case 33: Demonic Bumblebee
            {330, 430, 10300},   // Nether + End = Satanic
            {330, 10530, 20000}, // Nether + Military = Pyro
            // case 43: Nihilistic Bumblebee
            {430, 330, 10300},   // Nether + End = Satanic (reverse)
            {430, 10530, 20200}, // End + Military = Aero
            // case 53: Bumbelvis
            {530, 30, 10000},    // Normal + Rocky = Clay (reverse)
            {530, 130, 10400},   // Rock + Water = Amnesic / Lubricant (reverse)
            {530, 10530, 20300}, // Rocky + Military = Tera
            // case 63: Bumblezan
            {630, 930, 10500},   // Jungle + Sandy = Soldier
            // case 73: Bumble Claus
            {730, 10530, 20100}, // Frosty + Military = Cryo
            // case 93: Bumbobee
            {930, 30, 10200},    // Normal + Sandy = Royal (reverse)
            {930, 630, 10500},   // Jungle + Sandy = Soldier (reverse)
            // case 1053: General Bumblemond, the military tier's level 3
            {10530, 330, 20000}, // Nether + Military = Pyro (reverse)
            {10530, 430, 20200}, // End + Military = Aero (reverse)
            {10530, 530, 20300}, // Rocky + Military = Tera (reverse)
            {10530, 730, 20100}  // Frosty + Military = Cryo (reverse)
    };

 public static int level(int id){return (id/10)%10;}
 public static int mutationChanceForLevel(int level){return switch(level){case 0,1->500;case 2->250;case 3->25;default->0;};}
 public static int productChance(int species){return switch(level(species)){case 0->2500;case 1->5000;case 2->7500;default->10000;};}
 public static int combine(int first,int second){for(int[] entry:COMBINATIONS)if(entry[0]==first&&entry[1]==second&&GTBumbleSpecies.byId(entry[2])!=null)return entry[2];return first;}
 public static int mutatedSpecies(int id,java.util.function.BooleanSupplier random){return switch(level(id)){case 0->id+10;case 1,2->id+(random.getAsBoolean()?10:-10);case 3->id-10;default->id;};}
}
