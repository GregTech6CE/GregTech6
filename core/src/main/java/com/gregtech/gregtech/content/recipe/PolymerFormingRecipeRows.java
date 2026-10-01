package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original PolymerFormingRecipes parameters; native stack/registry construction stays in the platforms. */
public final class PolymerFormingRecipeRows {
    private PolymerFormingRecipeRows(){}
    public record Form(String shape,MaterialPrefix prefix,int count,int units) {
        private Form(String shape,MaterialPrefix prefix,int count){this(shape,prefix,count,1);}
    }

    private static final Form[] FORMS={new Form("ingot",MaterialPrefix.ingot,1),new Form("plate",MaterialPrefix.plate,1),
        new Form("rod",MaterialPrefix.stick,2),new Form("longrod",MaterialPrefix.stickLong,1),new Form("bolt",MaterialPrefix.bolt,8),
        new Form("ring",MaterialPrefix.ring,4),new Form("foil",MaterialPrefix.foil,4),new Form("curvedplate",MaterialPrefix.plateCurved,1),
        new Form("tinyplate",MaterialPrefix.plateTiny,9),new Form("finewire",MaterialPrefix.wireFine,8),new Form("smallgear",MaterialPrefix.gearGtSmall,1),new Form("gear",MaterialPrefix.gearGt,1,4)};

    public static Form[] forms(){return FORMS.clone();}
}
