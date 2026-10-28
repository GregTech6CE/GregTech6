package gregtech.content.recipe;

import java.util.List;

/** Original GT6 MultiItemRandomTools:126,128-132 and Loader_MultiTileEntities firing rows.
 * SPDX-License-Identifier: LGPL-3.0-or-later; Gregorius Techneticies and contributors. */
public final class ClayUtilityCatalog {
    private ClayUtilityCatalog() {}

    public record Row(
            String raw, String fired, int clayBalls, String pattern, int formingSourceLine, int firingSourceLine) {}

    public static final List<Row> ROWS = List.of(
            new Row("clay_basin", "mold_basin_ceramic", 5, "CkC|CRC| C ", 126, 430),
            new Row("clay_faucet", "crucible_faucet_ceramic", 3, "C C|kCR", 128, 305),
            new Row("clay_crossing", "crucible_crossing_ceramic", 5, "kCR|CCC| C ", 129, 477),
            new Row("clay_juicer", "juicer", 4, "kCR|CCC", 131, 2184),
            new Row("clay_bowl", "mixing_bowl", 5, "k R|C C|CCC", 132, 2177));
}
