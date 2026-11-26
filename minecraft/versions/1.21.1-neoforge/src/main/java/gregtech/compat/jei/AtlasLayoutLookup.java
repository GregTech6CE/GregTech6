package gregtech.compat.jei;

import java.util.List;
import java.util.function.IntConsumer;

/** Locate a preview in JEI's display order without laying out every preceding recipe. */
public interface AtlasLayoutLookup {
    void locate(Object target, List<?> recipes, IntConsumer selectPage);
}
