package gregtech.core;

import gregtech.api.kubejs.KubeJsLoadOrder;
import gregtech.api.recipe.data.FluidComponent;
import gregtech.api.recipe.data.JsonMaps;
import gregtech.api.recipe.data.ItemComponent;
import gregtech.api.recipe.data.ItemResult;
import gregtech.api.recipe.data.MachineRecipeDocuments;
import gregtech.api.recipe.data.MachineRecipeFields;
import gregtech.api.recipe.data.MachineRecipeMatch;
import gregtech.api.recipe.data.MachineRecipeRemovalSpec;
import gregtech.api.recipe.data.MachineRecipeScript;

import java.util.List;

/** Shared machine-recipe datapack documents and removal matching, without Minecraft. */
public final class MachineRecipeOverlayContracts {
    private static int assertions;

    private MachineRecipeOverlayContracts() {}

    public static void main(String[] args) {
        fields();
        additionRoundTrip();
        removalRoundTrip();
        rejectedDocuments();
        matching();
        scriptJson();
        System.out.println("machine recipe overlay passed " + assertions);
    }

    private static void fields() {
        check(MachineRecipeFields.TYPE_ADDITION.equals("gt6ce:machine"), "addition type");
        check(MachineRecipeFields.TYPE_REMOVAL.equals("gt6ce:machine_removal"), "removal type");
        check(MachineRecipeFields.CHANCE_CERTAIN == 10_000, "chance scale");
        check(MachineRecipeFields.EXPANSION_LIMIT == 4096, "tag expansion limit");
    }

    private static void additionRoundTrip() {
        var spec = MachineRecipeDocuments.addition("""
                {"map":"gt.recipe.crusher","item_inputs":[{"tag":"minecraft:coals","count":2}],
                "fluid_inputs":[{"fluid":"minecraft:water","amount":1000}],
                "item_outputs":[{"item":"minecraft:emerald","chance":5000}],
                "fluid_outputs":[{"fluid":"minecraft:lava","amount":1}],
                "eu_per_tick":16,"duration":64,"special":1200}""");
        check(spec.map().equals("gt.recipe.crusher"), "map");
        check(spec.itemInputs().equals(List.of(new ItemComponent(null, "minecraft:coals", 2))), "tag input");
        check(spec.fluidInputs().equals(List.of(new FluidComponent("minecraft:water", 1000))), "fluid input");
        check(spec.itemOutputs().equals(List.of(new ItemResult("minecraft:emerald", 1, 5000))), "default output count");
        check(spec.fluidOutputs().equals(List.of(new FluidComponent("minecraft:lava", 1))), "fluid output");
        check(spec.euPerTick() == 16 && spec.duration() == 64 && spec.special() == 1200, "work");
        var again = MachineRecipeDocuments.addition(MachineRecipeDocuments.canonical(spec));
        check(again.equals(spec), "addition canonical round trip");
        var defaults = MachineRecipeDocuments.addition("""
                {"map":"gt.recipe.crusher","item_inputs":[{"item":"minecraft:apple"}],
                "item_outputs":[{"item":"minecraft:emerald"}],"eu_per_tick":0,"duration":1}""");
        check(defaults.itemInputs().get(0).count() == 1, "default input count");
        check(defaults.itemOutputs().get(0).chance() == 10_000, "default chance");
        check(defaults.special() == 0, "default special");
        var escaped = MachineRecipeDocuments.addition(MachineRecipeDocuments.canonical(defaults));
        check(escaped.equals(defaults), "compact addition round trip");
        var annotated = MachineRecipeDocuments.addition("""
                {"_comment":"note","_kubejs_changed_marker":true,"map":"gt.recipe.crusher",
                "item_inputs":[{"item":"minecraft:apple"}],"item_outputs":[{"item":"minecraft:emerald"}],
                "eu_per_tick":0,"duration":1}""");
        check(annotated.equals(defaults), "root annotations ignored on addition");
        var catalyst = MachineRecipeDocuments.addition("""
                {"map":"gt.recipe.crusher","item_inputs":[{"item":"minecraft:apple"}],
                "fluid_inputs":[{"fluid":"minecraft:water","amount":0}],
                "item_outputs":[{"item":"minecraft:emerald"}],"eu_per_tick":0,"duration":1}""");
        check(catalyst.fluidInputs().equals(List.of(new FluidComponent("minecraft:water", 0))), "0 mB catalyst input");
        check(MachineRecipeDocuments.addition(MachineRecipeDocuments.canonical(catalyst)).equals(catalyst),
                "0 mB catalyst round trip");
        check(MachineRecipeDocuments.removal("{\"_kubejs_changed_marker\":true,\"map\":\"gt.recipe.crusher\",\"clear\":true}").clear(),
                "root annotations ignored on removal");
    }

    private static void removalRoundTrip() {
        var any = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher","item_inputs":[{"item":"minecraft:iron_ore"}]}""");
        check(any.itemInputs().equals(List.of(new ItemComponent("minecraft:iron_ore", null, 0))), "omitted count matches any");
        check(MachineRecipeDocuments.removal(MachineRecipeDocuments.canonical(any)).equals(any), "removal round trip");
        var clear = MachineRecipeDocuments.removal("{\"map\":\"gt.recipe.crusher\",\"clear\":true}");
        check(clear.clear() && clear.itemInputs().isEmpty(), "clear");
        check(MachineRecipeDocuments.removal(MachineRecipeDocuments.canonical(clear)).equals(clear), "clear round trip");
        check(JsonMaps.object("{\"item\":\"a\\\"b\\\\c\"}").get("item").equals("a\"b\\c"), "json escapes");
        reject("{\"map\":\"gt.recipe.crusher\",\"item_outputs\":[{\"item\":\"minecraft:a\\\"b\"}]}", false);
    }

    private static void rejectedDocuments() {
        reject("{\"map\":\"gt.recipe.crusher\",\"item_inputs\":[{\"item\":\"minecraft:apple\",\"tag\":\"minecraft:coals\"}],\"item_outputs\":[{\"item\":\"minecraft:emerald\"}],\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\",\"item_inputs\":[{}],\"item_outputs\":[{\"item\":\"minecraft:emerald\"}],\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\",\"item_inputs\":[{\"item\":\"minecraft:apple\"}],\"item_outputs\":[{\"item\":\"minecraft:emerald\"}],\"eu_per_tick\":1,\"duration\":1,\"nope\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\",\"item_inputs\":[{\"item\":\"minecraft:apple\",\"_count\":2}],\"item_outputs\":[{\"item\":\"minecraft:emerald\"}],\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\",\"item_inputs\":[{\"item\":\"minecraft:apple\"}],\"item_outputs\":[{\"item\":\"minecraft:emerald\",\"chance\":0}],\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\",\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"NotAnId\",\"item_inputs\":[{\"item\":\"minecraft:apple\"}],\"item_outputs\":[{\"item\":\"minecraft:emerald\"}],\"eu_per_tick\":1,\"duration\":1}", true);
        reject("{\"map\":\"gt.recipe.crusher\"}", false);
        reject("{\"map\":\"gt.recipe.crusher\",\"clear\":true,\"item_inputs\":[{\"item\":\"minecraft:apple\"}]}", false);
        reject("{\"map\":\"gt.recipe.crusher\",\"clear\":\"yes\"}", false);
        reject("{\"map\":\"gt.recipe.crusher\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1.5}]}", false);
    }

    private static void reject(String json, boolean addition) {
        boolean failed = false;
        try {
            if (addition) MachineRecipeDocuments.addition(json);
            else MachineRecipeDocuments.removal(json);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        check(failed, "rejected " + json);
    }

    private static void matching() {
        var iron = view(new String[]{"minecraft:iron_ingot", "minecraft:iron_ingot"}, new int[]{1, 2},
                new String[]{"minecraft:water"}, new long[]{1000},
                new String[]{"minecraft:emerald"}, new int[]{1}, new int[]{10_000},
                new String[]{"minecraft:lava"}, new long[]{50});
        var tags = (MachineRecipeMatch.TagQuery) (item, tag) -> tag.equals("minecraft:ingots") && item.equals("minecraft:iron_ingot");
        check(MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"item\":\"minecraft:iron_ingot\"}]}"), iron, tags), "item id matches");
        check(!MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"item\":\"minecraft:gold_ingot\"}]}"), iron, tags), "other item does not match");
        check(MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"item\":\"minecraft:iron_ingot\",\"count\":2}]}"), iron, tags), "exact count uses the second slot");
        check(!MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"item\":\"minecraft:iron_ingot\",\"count\":3}]}"), iron, tags), "wrong count matches nothing");
        check(!MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"item\":\"minecraft:iron_ingot\",\"count\":1},{\"item\":\"minecraft:iron_ingot\",\"count\":1}]}"), iron, tags), "one slot cannot satisfy two counts");
        check(MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"tag\":\"minecraft:ingots\"}]}"), iron, tags), "tag matches a member");
        check(!MachineRecipeMatch.matches(removal("{\"item_inputs\":[{\"tag\":\"minecraft:gems\"}]}"), iron, tags), "tag misses a non-member");
        check(MachineRecipeMatch.matches(removal("{\"fluid_inputs\":[{\"fluid\":\"minecraft:water\"}]}"), iron, tags), "omitted fluid amount matches");
        check(!MachineRecipeMatch.matches(removal("{\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":999}]}"), iron, tags), "different fluid amount misses");
        check(!MachineRecipeMatch.matches(removal("{\"item_outputs\":[{\"item\":\"minecraft:emerald\",\"chance\":5000}]}"), iron, tags), "different chance misses");
        check(MachineRecipeMatch.matches(removal("{\"item_outputs\":[{\"item\":\"minecraft:emerald\"}]}"), iron, tags), "omitted chance matches");
        check(MachineRecipeMatch.matches(removal("{\"fluid_outputs\":[{\"fluid\":\"minecraft:lava\",\"amount\":50}]}"), iron, tags), "fluid output matches");
        check(MachineRecipeMatch.matches(MachineRecipeDocuments.removal("{\"map\":\"gt.recipe.crusher\",\"clear\":true}"), iron, tags), "clear matches every static row");
    }

    private static MachineRecipeRemovalSpec removal(String body) {
        return MachineRecipeDocuments.removal("{\"map\":\"gt.recipe.crusher\"," + body.substring(1));
    }

    private static MachineRecipeMatch.RecipeView view(String[] items, int[] itemCounts, String[] fluids, long[] fluidAmounts,
            String[] outputs, int[] outputCounts, int[] chances, String[] outFluids, long[] outAmounts) {
        return new MachineRecipeMatch.RecipeView() {
            public int inputItems() { return items.length; }
            public String inputItem(int index) { return items[index]; }
            public int inputItemCount(int index) { return itemCounts[index]; }
            public int inputFluids() { return fluids.length; }
            public String inputFluid(int index) { return fluids[index]; }
            public long inputFluidAmount(int index) { return fluidAmounts[index]; }
            public int outputItems() { return outputs.length; }
            public String outputItem(int index) { return outputs[index]; }
            public int outputItemCount(int index) { return outputCounts[index]; }
            public int outputItemChance(int index) { return chances[index]; }
            public int outputFluids() { return outFluids.length; }
            public String outputFluid(int index) { return outFluids[index]; }
            public long outputFluidAmount(int index) { return outAmounts[index]; }
        };
    }

    private static void scriptJson() {
        var handwritten = MachineRecipeDocuments.addition("""
                {"type":"gt6ce:machine","map":"gt.recipe.crusher",
                "item_inputs":[{"tag":"minecraft:coals","count":2}],
                "fluid_inputs":[{"fluid":"minecraft:water","amount":1000}],
                "item_outputs":[{"item":"minecraft:emerald","count":1,"chance":5000}],
                "fluid_outputs":[],"eu_per_tick":16,"duration":64,"special":0}""");
        var generated = MachineRecipeScript.typed(MachineRecipeFields.TYPE_ADDITION, MachineRecipeScript.addition(
                "gt.recipe.crusher",
                List.of(MachineRecipeScript.item("#minecraft:coals", 2)),
                List.of(MachineRecipeScript.fluid("minecraft:water", 1000)),
                List.of(MachineRecipeScript.result("minecraft:emerald", 1, 5000)),
                List.of(), 16, 64, 0));
        check(MachineRecipeDocuments.addition(generated).equals(handwritten), "script addition matches a handwritten document");
        var removal = MachineRecipeDocuments.removal(MachineRecipeScript.typed(MachineRecipeFields.TYPE_REMOVAL,
                MachineRecipeScript.removal("gt.recipe.crusher",
                        List.of(MachineRecipeScript.item("minecraft:iron_ore", 0)),
                        List.of(), List.of(), List.of())));
        check(removal.itemInputs().equals(List.of(new ItemComponent("minecraft:iron_ore", null, 0))), "script removal");
        check(MachineRecipeDocuments.removal(MachineRecipeScript.typed(MachineRecipeFields.TYPE_REMOVAL,
                MachineRecipeScript.clear("gt.recipe.crusher"))).clear(), "script clear");
        try {
            MachineRecipeScript.requireKnownMap("no.such.map");
            throw new AssertionError("unknown map");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Unknown machine recipe map"), "unknown map message");
        }
        KubeJsLoadOrder.requireScriptsLoaded(true);
        try {
            KubeJsLoadOrder.requireScriptsLoaded(false);
            throw new AssertionError("load order");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("KubeJS startup scripts have not loaded"), "load order message");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
