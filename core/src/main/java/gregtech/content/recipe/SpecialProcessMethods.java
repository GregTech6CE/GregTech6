package gregtech.content.recipe;

import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.SpecialProcessMethod;
import gregtech.api.addon.material.SpecialProcessValue;
import gregtech.engine.material.MaterialInfoRegistry;
import java.util.List;

/** Built-in special process methods and their stable target/reagent process keys. */
public final class SpecialProcessMethods {
    public static final MaterialProcessKey<SpecialProcessValue> SULFURIC_ACID_LEACHING =
            process("sulfuric_acid_leaching");
    public static final MaterialProcessKey<SpecialProcessValue> AQUA_REGIA_LEACHING = process("aqua_regia_leaching");
    public static final MaterialProcessKey<SpecialProcessValue> HYDROFLUORIC_ACID_LEACHING =
            process("hydrofluoric_acid_leaching");
    public static final MaterialProcessKey<SpecialProcessValue> HYDROCHLORIC_ACID_MANGANESE_LEACHING =
            process("hydrochloric_acid_manganese_leaching");

    private static final List<SpecialProcessMethod> METHODS = List.of(
            method(SULFURIC_ACID_LEACHING),
            method(AQUA_REGIA_LEACHING),
            method(HYDROFLUORIC_ACID_LEACHING),
            method(HYDROCHLORIC_ACID_MANGANESE_LEACHING));

    private SpecialProcessMethods() {}

    public static List<SpecialProcessMethod> methods() {
        return METHODS;
    }

    public static void install() {
        for (var method : METHODS) MaterialInfoRegistry.installBuiltinSpecialProcess(method);
    }

    private static SpecialProcessMethod method(MaterialProcessKey<SpecialProcessValue> process) {
        return new SpecialProcessMethod(
                process, "gt.recipe.bath", "gt6ce." + process.id().path().replace('/', '.'));
    }

    private static MaterialProcessKey<SpecialProcessValue> process(String path) {
        return MaterialProcessKey.multiple(new DataKey("gt6ce", "special_process/" + path), SpecialProcessValue.class);
    }
}
