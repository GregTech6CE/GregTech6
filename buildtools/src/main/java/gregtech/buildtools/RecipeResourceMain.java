package gregtech.buildtools;

import java.nio.file.Path;

/**
 * CLI: {@code <source data> <output data> <exclusions> [<version node data>] [<report>]}.
 * Used by the NeoForge build; the JUnit tests call the same converter directly.
 */
public final class RecipeResourceMain {
    private RecipeResourceMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 5)
            throw new IllegalArgumentException(
                    "usage: <source data> <output data> <exclusions> [<version node data>] [<report>]");
        var exclusions = RecipeResourceWriter.readExclusions(Path.of(args[2]));
        var summary = RecipeResourceWriter.write(
                Path.of(args[0]),
                Path.of(args[1]),
                exclusions,
                args.length > 3 ? Path.of(args[3]) : null,
                args.length > 4 ? Path.of(args[4]) : null);
        System.out.println("Recipe resources: " + summary.recipes() + " recipes, " + summary.tags()
                + " tags, " + summary.excluded() + " replaced by generators.");
    }
}
