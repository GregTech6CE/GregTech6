package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.engine.machine.PipeSpec;

/** Original GrapheneNanofabricationRecipes parameters; native stack/registry construction stays in the platforms. */
public final class GrapheneNanofabricationRecipeRows {
    private GrapheneNanofabricationRecipeRows() {}

    public record Entry(
            int ticks, int selector, MaterialPrefix input, int inputCount, MaterialPrefix output, int outputCount) {}

    /** Carbon fluid pipe rows; the source uses {@code MT.C} for both the dust input and the pipe output. */
    public record PipeEntry(
            int ticks,
            int selector,
            MaterialPrefix input,
            int inputCount,
            PipeSpec.PipeSize outputSize,
            int outputCount) {}

    private static final Entry[] DEFINITIONS = {
        new Entry(64, 0, MaterialPrefix.dustDiv72, 18, MaterialPrefix.foil, 1),
        new Entry(256, 0, MaterialPrefix.dustTiny, 9, MaterialPrefix.foil, 4),
        new Entry(64, 0, MaterialPrefix.dustSmall, 1, MaterialPrefix.foil, 1),
        new Entry(256, 0, MaterialPrefix.dust, 1, MaterialPrefix.foil, 4),
        new Entry(256, 1, MaterialPrefix.dustTiny, 9, MaterialPrefix.plate, 1),
        new Entry(256, 1, MaterialPrefix.dustSmall, 4, MaterialPrefix.plate, 1),
        new Entry(256, 1, MaterialPrefix.dust, 1, MaterialPrefix.plate, 1),
        new Entry(256, 2, MaterialPrefix.dustTiny, 9, MaterialPrefix.plateCurved, 1),
        new Entry(256, 2, MaterialPrefix.dustSmall, 4, MaterialPrefix.plateCurved, 1),
        new Entry(256, 2, MaterialPrefix.dust, 1, MaterialPrefix.plateCurved, 1),
        new Entry(128, 3, MaterialPrefix.dustDiv72, 36, MaterialPrefix.itemCasing, 1),
        new Entry(256, 3, MaterialPrefix.dustTiny, 9, MaterialPrefix.itemCasing, 2),
        new Entry(128, 3, MaterialPrefix.dustSmall, 2, MaterialPrefix.itemCasing, 1),
        new Entry(256, 3, MaterialPrefix.dust, 1, MaterialPrefix.itemCasing, 2),
        new Entry(32, 4, MaterialPrefix.dustDiv72, 9, MaterialPrefix.wireFine, 1),
        new Entry(256, 4, MaterialPrefix.dustTiny, 9, MaterialPrefix.wireFine, 8),
        new Entry(64, 4, MaterialPrefix.dustSmall, 1, MaterialPrefix.wireFine, 2),
        new Entry(256, 4, MaterialPrefix.dust, 1, MaterialPrefix.wireFine, 8),
        new Entry(128, 5, MaterialPrefix.dustDiv72, 36, null, 1),
        new Entry(256, 5, MaterialPrefix.dustTiny, 9, null, 2),
        new Entry(128, 5, MaterialPrefix.dustSmall, 2, null, 1),
        new Entry(256, 5, MaterialPrefix.dust, 1, null, 2),
        new Entry(32, 6, MaterialPrefix.dustDiv72, 9, MaterialPrefix.bolt, 1),
        new Entry(256, 6, MaterialPrefix.dustTiny, 9, MaterialPrefix.bolt, 8),
        new Entry(64, 6, MaterialPrefix.dustSmall, 1, MaterialPrefix.bolt, 2),
        new Entry(256, 6, MaterialPrefix.dust, 1, MaterialPrefix.bolt, 8),
        new Entry(128, 7, MaterialPrefix.dustDiv72, 36, MaterialPrefix.stick, 1),
        new Entry(256, 7, MaterialPrefix.dustTiny, 9, MaterialPrefix.stick, 2),
        new Entry(128, 7, MaterialPrefix.dustSmall, 2, MaterialPrefix.stick, 1),
        new Entry(256, 7, MaterialPrefix.dust, 1, MaterialPrefix.stick, 2),
        new Entry(256, 8, MaterialPrefix.dustTiny, 9, MaterialPrefix.stickLong, 1),
        new Entry(256, 8, MaterialPrefix.dustSmall, 4, MaterialPrefix.stickLong, 1),
        new Entry(256, 8, MaterialPrefix.dust, 1, MaterialPrefix.stickLong, 1),
        new Entry(64, 9, MaterialPrefix.dustDiv72, 18, MaterialPrefix.ring, 1),
        new Entry(256, 9, MaterialPrefix.dustTiny, 9, MaterialPrefix.ring, 4),
        new Entry(64, 9, MaterialPrefix.dustSmall, 1, MaterialPrefix.ring, 1),
        new Entry(256, 9, MaterialPrefix.dust, 1, MaterialPrefix.ring, 4),
        new Entry(256, 10, MaterialPrefix.dustTiny, 9, MaterialPrefix.gearGtSmall, 1),
        new Entry(256, 10, MaterialPrefix.dustSmall, 4, MaterialPrefix.gearGtSmall, 1),
        new Entry(256, 10, MaterialPrefix.dust, 1, MaterialPrefix.gearGtSmall, 1),
        new Entry(1024, 11, MaterialPrefix.dustTiny, 36, MaterialPrefix.gearGt, 1),
        new Entry(1024, 11, MaterialPrefix.dustSmall, 16, MaterialPrefix.gearGt, 1),
        new Entry(1024, 11, MaterialPrefix.dust, 4, MaterialPrefix.gearGt, 1),
        new Entry(1088, 12, MaterialPrefix.dustSmall, 17, MaterialPrefix.rotor, 1),
        new Entry(4352, 12, MaterialPrefix.dust, 17, MaterialPrefix.rotor, 4),
    };

    private static final PipeEntry[] PIPE_DEFINITIONS = {
        new PipeEntry(128, 13, MaterialPrefix.dustDiv72, 36, PipeSpec.PipeSize.TINY, 1),
        new PipeEntry(256, 13, MaterialPrefix.dustTiny, 9, PipeSpec.PipeSize.TINY, 2),
        new PipeEntry(128, 13, MaterialPrefix.dustSmall, 2, PipeSpec.PipeSize.TINY, 1),
        new PipeEntry(256, 13, MaterialPrefix.dust, 1, PipeSpec.PipeSize.TINY, 2),
        new PipeEntry(256, 14, MaterialPrefix.dustTiny, 9, PipeSpec.PipeSize.SMALL, 1),
        new PipeEntry(256, 14, MaterialPrefix.dustSmall, 4, PipeSpec.PipeSize.SMALL, 1),
        new PipeEntry(256, 14, MaterialPrefix.dust, 1, PipeSpec.PipeSize.SMALL, 1),
        new PipeEntry(768, 15, MaterialPrefix.dustTiny, 27, PipeSpec.PipeSize.MEDIUM, 1),
        new PipeEntry(768, 15, MaterialPrefix.dustSmall, 12, PipeSpec.PipeSize.MEDIUM, 1),
        new PipeEntry(768, 15, MaterialPrefix.dust, 3, PipeSpec.PipeSize.MEDIUM, 1),
        new PipeEntry(1536, 16, MaterialPrefix.dustTiny, 54, PipeSpec.PipeSize.LARGE, 1),
        new PipeEntry(1536, 16, MaterialPrefix.dustSmall, 24, PipeSpec.PipeSize.LARGE, 1),
        new PipeEntry(1536, 16, MaterialPrefix.dust, 6, PipeSpec.PipeSize.LARGE, 1),
        new PipeEntry(3072, 17, MaterialPrefix.dustSmall, 48, PipeSpec.PipeSize.HUGE, 1),
        new PipeEntry(3072, 17, MaterialPrefix.dust, 12, PipeSpec.PipeSize.HUGE, 1),
    };

    public static Entry[] definitions() {
        return DEFINITIONS.clone();
    }

    public static PipeEntry[] pipeDefinitions() {
        return PIPE_DEFINITIONS.clone();
    }
}
