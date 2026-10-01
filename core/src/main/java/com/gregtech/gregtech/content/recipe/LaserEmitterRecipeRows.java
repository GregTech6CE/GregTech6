package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original LaserEmitterRecipes parameters; native stack/registry construction stays in the platforms. */
public final class LaserEmitterRecipeRows {
    private LaserEmitterRecipeRows(){}
    private static final String[][] GASES = {
            {"Helium", "laser_emitter_helium"},
            {"Neon", "laser_emitter_neon"},
            {"Argon", "laser_emitter_argon"},
            {"Krypton", "laser_emitter_krypton"},
            {"Xenon", "laser_emitter_xenon"},
            {"HeliumNeon", "laser_emitter_heliumneon"},
            {"CarbonMonoxide", "laser_emitter_carbonmonoxide"},
            {"CarbonDioxide", "laser_emitter_carbondioxide"}};

    public static String[][] gases(){String[][] copy=new String[GASES.length][];for(int i=0;i<copy.length;i++)copy[i]=GASES[i].clone();return copy;}
}
