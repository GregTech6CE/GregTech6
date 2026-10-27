package gregtech.content.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Fixed port placement policy with a remaining-slot exception for the modern bone-meal dye route. */
public enum FormConversionPlacement implements StringRepresentable {
    FIXED("fixed"), REMAINING("remaining");

    public static final Codec<FormConversionPlacement> CODEC = StringRepresentable.fromEnum(FormConversionPlacement::values);
    private final String name;
    FormConversionPlacement(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
    public static FormConversionPlacement fromName(String name) {
        for (var placement : values()) if (placement.name.equals(name)) return placement;
        throw new IllegalArgumentException("Unknown form conversion placement: " + name);
    }

    public boolean matches(FormConversionSelector selector, int gridSize, int first, int occupied) {
        if (this == FIXED || selector.variants() <= 1) return selector.matches(gridSize, first, occupied);
        return occupied == 1 && first >= 0 && first < gridSize && gridSize >= selector.minimumGridSize()
                && first >= selector.offset();
    }

    /** Requested port order: first slot gives four small dusts, second gives nine tiny dusts. */
    public static FormConversionSelector orderedSelector(OriginalFormConversions.Conversion conversion) {
        var source = OriginalFormConversions.selector(conversion);
        if (conversion.single() && conversion.input().equals("dust")) {
            if (conversion.output().equals("dustSmall")) return new FormConversionSelector(source.variants(), 0);
            if (conversion.output().equals("dustTiny")) return new FormConversionSelector(source.variants(), 1);
        }
        return source;
    }
}
