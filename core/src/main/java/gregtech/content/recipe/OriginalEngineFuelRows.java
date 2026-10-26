package gregtech.content.recipe;

import gregtech.content.data.FluidCatalog.FluidFlags;
import java.util.List;

/**
 * GT6 Loader_Fuels Engine rows (original:77-147; W:82-152), LGPL-3.0-or-later.
 * Each charge is 1 mB fuel -> 1 mB CO2, with energy = eut * duration.
 * Names are original registry identities/FL.list aliases, NOT Java field keys.
 */
public final class OriginalEngineFuelRows {
    private OriginalEngineFuelRows() {}
    public record Row(long eut, long duration, long category, List<String> registryNames) {}
    private static Row names(long eut,long ticks,String... names) {return new Row(eut,ticks,0,List.of(names));}
    public static final List<Row> ROWS=List.of(
        names(128,12,"rc jet fuel"),
        names(64,7,"kerosine","kerosene"),
        names(64,7,"petrol","gasoline"),
        names(64,7,"diesel"),
        names(64,8,"fuel"),
        names(64,12,"nitrofuel"),
        new Row(16,9,FluidFlags.RUM,List.of()),
        new Row(16,9,FluidFlags.WHISKEY,List.of()),
        new Row(16,9,FluidFlags.VINEGAR,List.of()),
        names(16,9,"binnie.vodka","potion.vodka","vodka"),
        names(16,18,"potion.dragonblood","potion.goldencider","potion.notchesbrew"),
        names(16,12,"bioethanol","ethanol"),
        names(16,12,"rc ethanol"),
        names(64,8,"biodiesel"),
        names(64,12,"biofuel"),
        names(16,6,"hootch"),
        names(32,10,"fire_water"),
        names(64,5,"rocket_fuel")
    );
}
