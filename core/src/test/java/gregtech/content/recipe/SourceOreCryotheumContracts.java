package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
import static gregtech.content.recipe.SourceOreCryotheumRules.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Source roster/shape contracts. Native registry presence is deliberately not inferred from forms. */
public final class SourceOreCryotheumContracts {
    private SourceOreCryotheumContracts() {}
    public static void run() {
        equal(List.of(8469, 8422, 8341, 8368, 8369, 8370, 8371, 8372, 8456, 8470, 8340, 8333, 9114),
                bindings().stream().map(Binding::materialId).toList(), "all13 exact source participants");
        equal(0, POWER, "no Bath power");
        equal(256, TICKS, "literal Cryotheum ticks");
        check(OPTIMIZE, "source optimize=true");
        equal(List.of(new Shape(Input.BLOCK_DUST, 1, MaterialPrefix.gemExquisite, 1, 25),
                new Shape(Input.CRUSHED, 1, MaterialPrefix.gemFlawless, 1, 13),
                new Shape(Input.CRUSHED_TINY, 9, MaterialPrefix.gemFlawless, 1, 13)),
                shapes(), "physical blockDust1 and 13mB rounded half, not dust9 or50mB");
        gregtech.content.data.MaterialPrefixes.bootstrap();
        for (Binding row : bindings()) {
            var material = GTMaterialRegistry.get(row.materialId());
            equal(row.materialName(), material.getName(), "exact Cryotheum material");
            equal(row, binding(material.getId(), material.getName()), "canonical identity lookup");
            equal(null, binding(material.getId(), "alias:" + material.getName()), "no alias participants");
            equal(null, binding(material.getId() + 10000, material.getName()), "no addon ID substitution");
            for (Shape shape : shapes()) {
                if (shape.input() == Input.BLOCK_DUST) {
                    check(gregtech.engine.prefix.BlockMaterialPrefix.blockDust.isValidFor(material),
                            "core defines physical blockDust " + row);
                } else {
                    MaterialPrefix prefix = shape.input() == Input.CRUSHED
                            ? MaterialPrefix.crushed : MaterialPrefix.crushedTiny;
                    check(prefix.isValidFor(material), "core defines expected crushed input " + row + " " + shape);
                }
                check(shape.output().isValidFor(material), "core defines expected output " + row + " " + shape);
            }
        }
        var ordinaryAmber = GTMaterialRegistry.get("Amber");
        check(ordinaryAmber != null, "ordinary Amber exists but is not ANY.Amber member");
        equal(null, binding(ordinaryAmber.getId(), ordinaryAmber.getName()), "ordinary Amber excluded");
    }
}
