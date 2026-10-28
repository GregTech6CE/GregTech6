package gregtech.content.data;

import java.util.List;

/**
 * GT6's OreDictionary unification targets as 1.20.1 data: other mods' items that stand for a GT
 * material form ({@code LoaderUnificationTargets} BOTA/TF rows).
 *
 * <p>An item binding makes GT treat the other mod's item as that form everywhere (recipe inputs,
 * melting, mass storage). A tag binding also puts GT's item into the other mod's own tag, so its
 * recipes accept GT's item even though it never uses {@code forge:} tags.</p>
 */
public final class ModMaterialUnification {
    private ModMaterialUnification() {}

    /** Other-mod item → GT material form. Registered by registry id; absent mods resolve to air. */
    public record ItemForm(String itemId, String prefix, String material) {}

    /**
     * @param itemPrefix  GT item prefix whose stack joins the tag, or null for a block-only entry
     * @param blockPrefix GT block prefix whose block item also joins the matching block tag
     */
    public record Binding(String namespace, String path, String itemPrefix, String blockPrefix, String material) {}

    /**
     * Untagged Botania items that GT6 unified: its quartz has no {@code forge:} tags, and the
     * ingots/nuggets/blocks are covered by {@link #BINDINGS} instead.
     */
    public static final List<ItemForm> ITEM_FORMS = List.of(
            new ItemForm("botania:mana_diamond", "gem", "ManaDiamond"),
            new ItemForm("botania:dragonstone", "gem", "ElvenDragonstone"),
            new ItemForm("botania:gaia_ingot", "ingot", "GaiaSpirit"),
            new ItemForm("botania:livingwood_twig", "stick", "Livingwood"),
            new ItemForm("botania:dreamwood_twig", "stick", "Dreamwood"),
            new ItemForm("botania:livingwood_planks", "plate", "Livingwood"),
            new ItemForm("botania:dreamwood_planks", "plate", "Dreamwood"),
            new ItemForm("botania:shimmerwood_planks", "plate", "Shimmerwood"),
            new ItemForm("botania:dark_quartz", "gem", "SmokeyQuartz"),
            new ItemForm("botania:mana_quartz", "gem", "ManaQuartz"),
            new ItemForm("botania:blaze_quartz", "gem", "BlazeQuartz"),
            new ItemForm("botania:lavender_quartz", "gem", "LavenderQuartz"),
            new ItemForm("botania:red_quartz", "gem", "RedQuartz"),
            new ItemForm("botania:elf_quartz", "gem", "ElvenQuartz"),
            new ItemForm("botania:sunny_quartz", "gem", "SunnyQuartz"));

    /**
     * Botania's own 1.20.1 material tags ({@code data/botania/tags/items}). Twilight Forest
     * publishes its metals under {@code forge:} tags already, so it needs no private tag.
     */
    public static final List<Binding> BINDINGS = List.of(
            new Binding("botania", "manasteel_ingots", "ingot", null, "Manasteel"),
            new Binding("botania", "manasteel_nuggets", "nugget", null, "Manasteel"),
            new Binding("botania", "manasteel_blocks", null, "blockIngot", "Manasteel"),
            new Binding("botania", "terrasteel_ingots", "ingot", null, "Terrasteel"),
            new Binding("botania", "terrasteel_nuggets", "nugget", null, "Terrasteel"),
            new Binding("botania", "terrasteel_blocks", null, "blockIngot", "Terrasteel"),
            new Binding("botania", "elementium_ingots", "ingot", null, "ElvenElementium"),
            new Binding("botania", "elementium_nuggets", "nugget", null, "ElvenElementium"),
            new Binding("botania", "elementium_blocks", null, "blockIngot", "ElvenElementium"),
            new Binding("botania", "mana_diamond_gems", "gem", null, "ManaDiamond"),
            new Binding("botania", "dragonstone_gems", "gem", null, "ElvenDragonstone"));

    /** Tag files are only published into a mod's namespace when that mod is present. */
    public static boolean isModLoaded(String namespace) {
        return switch (namespace) {
            case "botania" -> ModReferences.BOTA.isLoaded();
            default -> true;
        };
    }
}
