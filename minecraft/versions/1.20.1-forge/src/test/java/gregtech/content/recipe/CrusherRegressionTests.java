package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputSemanticsChecks;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreCrushingAdapter.*;
import static gregtech.content.recipe.SourceOreCrushingRules.Machine;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual native host rows and the shared consumption kernel; no material/holder registry mutations. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CrusherRegressionTests {
    private CrusherRegressionTests() {}
    private static final RecipeMap CRUSHER = MachineRecipeMaps.Crusher, HAMMER = MachineRecipeMaps.Hammer;

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceRowsAndCompatibilityDomains(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and first host overlay completed");
        golden();
        controlledDefinitions();
        admissionAndForeignOwnership();
        int ownedCount = SharedOreCrushingRows.owned().size();
        for (var map : List.of(CRUSHER, HAMMER)) {
            var stone = host(OreHostStone.STONE); var tuff = host(OreHostStone.TUFF);
            var source = row(map, stone); var compatibility = row(map, tuff);
            require(source != compatibility && source.hasSameItemInputPredicate(0)
                    && compatibility.hasSameItemInputPredicate(0), "source and compatibility own disjoint semantic domains");
            require(!source.matchesItemInput(0, tuff) && !compatibility.matchesItemInput(0, stone), "neither domain leaks into the other");
            require(compatibility.viewerInputNote(0).equals("gt6ce.jei.ore_host_legacy"), "TUFF representative must be labelled as compatibility, not source proof");
            require(map.addRecipe(source) == null && map.addRecipe(compatibility) == null,
                    "ordinary admission rejects duplicate source and compatibility representatives");
            for (var no : List.of(host(OreHostStone.BEDROCK), host(OreHostStone.SAND),
                    GTBlocks.getStack(BlockMaterialPrefix.oreSmall, material("Cinnabar")))) {
                require(!no.isEmpty() && !compatibility.matchesItemInput(0, no), "source NO never falls through to compatibility");
                require(find(map, no) == null && !map.containsInput(no), "source NO is refused by lookup and input filtering: " + describe(no));
            }
        }
        // The real owner is rebuilt by the existing overlay, not a second private test registry.
        for (int reload = 0; reload < 2; reload++) {
            MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
            golden();
            require(SharedOreCrushingRows.owned().size() == ownedCount, "source/compatibility rebuild must not accumulate rows");
        }
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void hostMatchingConsumesOnlyEligibleSlots(GameTestHelper helper) {
        var stone = host(OreHostStone.STONE); var nether = host(OreHostStone.NETHERRACK); var tuff = host(OreHostStone.TUFF);
        var stoneRow = row(CRUSHER, stone); var netherRow = row(CRUSHER, nether); var compatibility = row(CRUSHER, tuff);
        require(stoneRow != netherRow && netherRow != compatibility, "actual known hosts are not handled by a broad compatibility row");
        RecipeInputSemanticsChecks.mixedSlots(stoneRow, netherRow, stone, nether, tuff);
        RecipeInputSemanticsChecks.legacyChecks(stoneRow, stone, encoded("stone", null));
        // Same semantic host in old/new Forge representations; high-priority invalid data never defaults to stone.
        for (var offered : List.of(stone, encoded("stone", null), encoded(null, "stone"), encoded("stone", "stone")))
            require(row(CRUSHER, offered) == stoneRow, "all supported representations resolve to the same STONE row");
        require(row(CRUSHER, encoded("netherrack", "stone")) == netherRow, "BlockStateTag takes precedence over legacy entity data");
        for (var offered : List.of(encoded("future_host", null), encoded("future_host", "stone"), encoded("", "stone"), wrongType()))
            require(row(CRUSHER, offered) == compatibility, "unknown/empty/wrong-type high-priority host stays UNKNOWN");
        var broken = OreBlock.brokenStack(material("Cinnabar"), OreHostStone.STONE);
        check(row(CRUSHER, broken), 640, standardOutputs(), new long[]{10000, 10000, 3000, 10000});
        check(row(HAMMER, broken), 544, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1)});
        var mutable = stone.copyWithCount(2);
        for (var representation : List.of(stone, tuff, host(OreHostStone.BEDROCK), stone)) {
            mutable.setTag(representation.getTag() == null ? null : representation.getTag().copy());
            var expected = representation == tuff ? compatibility : representation == stone ? stoneRow : null;
            require(find(CRUSHER, mutable) == expected, "warmed candidate cache must observe in-place host metadata changes");
            if (expected != null) require(RecipeInputs.consume(expected, List.of(mutable), List.of(), 2) != null,
                    "lookup and parallel consumption use the same host predicate");
        }
        mutable.setCount(1);
        require(find(CRUSHER, mutable) == stoneRow && RecipeInputs.consume(stoneRow, List.of(mutable), List.of(), 2) == null,
                "candidate cache never caches counts or parallel acceptance");
        mutable.getOrCreateTag().putString("fixture", "unrelated");
        require(find(CRUSHER, mutable) == stoneRow, "unrelated data is not a host distinction");
        var rock = form(MaterialPrefix.rockGt, "Copper", 1); var oldRock = row(CRUSHER, rock);
        rock.getOrCreateTag().putString("fixture", "unrelated");
        require(!oldRock.hasSameItemInputPredicate(0) && find(CRUSHER, rock) == oldRock && CRUSHER.containsInput(rock),
                "unmarked independent rock row keeps its original wildcard and containsInput behavior");
        helper.succeed();
    }

    private static void controlledDefinitions() {
        var material = material("Cinnabar"); var input = host(OreHostStone.STONE);
        var candidate = candidate(material, input, false, new Snapshot());
        require(candidate != null && candidate.kind() == NativeKind.HOST_ORE, "actual host candidate ownership");
        require(candidate(material, new ItemStack(Items.FLINT), false, new Snapshot()) == null, "foreign selected item is not owned");
        var noPrimary = definitions(candidate, Machine.CRUSHER, new Snapshot() {
            @Override NativeForm direct(MaterialPrefix prefix, GTMaterial m) {
                return m == material && (prefix == MaterialPrefix.crushed || prefix == MaterialPrefix.dust || prefix == MaterialPrefix.gem)
                        ? NativeForm.absent() : super.direct(prefix, m);
            }
        });
        require(noPrimary.action() == Action.SUPPORTED && noPrimary.rows().isEmpty()
                && classifyHost(input).domain() == Domain.SOURCE_PROFILE,
                "known absent primary forms do not reclassify a supported host as UNKNOWN");
        require(!row(CRUSHER, host(OreHostStone.TUFF)).matchesItemInput(0, input), "compatibility cannot revive that missing source output");
        var noBonus = definitions(candidate, Machine.CRUSHER, new Snapshot() {
            @Override NativeForm direct(MaterialPrefix prefix, GTMaterial m) {
                return m == material && prefix == MaterialPrefix.gem ? NativeForm.absent() : super.direct(prefix, m);
            }
        });
        check(planned(noBonus, input), 640, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1),
                form(MaterialPrefix.crushed, "Cinnabar", 1), ItemStack.EMPTY, form(MaterialPrefix.dust, "Stone", 4)},
                new long[]{10000, 10000, 3000, 10000});
        var hammer = definitions(candidate, Machine.HAMMER, new Snapshot() {
            @Override NativeForm resolve(SourceOreCrushingRules.Form form, SourceRecyclingFacts.Identity id) {
                if (id.name().equals("Stone")) throw new AssertionError("full Hammer must not look up a host output");
                return super.resolve(form, id);
            }
        });
        check(planned(hammer, input), 544, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1)});
        var anti = definitions(candidate, Machine.CRUSHER, new Snapshot() {
            @Override boolean anti(GTMaterial m) { return true; }
        });
        require(anti.action() == Action.SOURCE_NO_ROW && anti.rows().isEmpty() && !anti.unknownCompatibility(),
                "source NO never restores an UNKNOWN compatibility route");
        var block = GTBlocks.getStack(BlockMaterialPrefix.blockRaw, material);
        var rawBlock = candidate(material, block, false, new Snapshot());
        require(rawBlock != null && rawBlock.kind() == NativeKind.RAW_BLOCK, "actual physical raw block candidate");
        var large = planned(definitions(rawBlock, Machine.HAMMER, new Snapshot() {
            @Override int oreMultiplier(GTMaterial m) { return 40; }
        }), block);
        var existing = row(HAMMER, block);
        require(HAMMER.unlink(existing), "isolate controlled large-output admission on the actual holder");
        try {
            require(HAMMER.addRecipe(large) == large, "source Hammer output360 uses ordinary native admission");
            check(row(HAMMER, block), 11520, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 360)});
        } finally { HAMMER.unlink(large); HAMMER.add(existing); }
        var zero = definitions(rawBlock, Machine.HAMMER, new Snapshot() {
            @Override long amount(GTMaterial m) { return 0; }
        });
        require(zero.action() == Action.DEFERRED_NO_ROW && zero.rows().isEmpty() && !zero.unknownCompatibility(),
                "unrepresentable source zero Hammer output defers without inventing one or reviving legacy");
    }
    private static Recipe planned(Outcome outcome, ItemStack input) {
        var matches = outcome.rows().stream().filter(r -> r.matchesItemInput(0, input)).toList();
        require(matches.size() == 1, "controlled source definition expected one row: " + outcome.reason()); return matches.get(0);
    }
    private static void admissionAndForeignOwnership() {
        var input = host(OreHostStone.STONE); var source = row(CRUSHER, input); var fallback = row(CRUSHER, host(OreHostStone.TUFF));
        var original = SharedOreCrushingRows.baselineExcluded().stream().filter(r -> r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem() && !r.hasSameItemInputPredicate(0)
                && r.mDuration == 128 && r.mOutputs.length == 2).findFirst().orElseThrow();
        var group = CRUSHER.mRecipeList.stream().filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == input.getItem()).toList();
        require(group.stream().allMatch(SharedOreCrushingRows.owned()::contains), "isolate only this producer's accepted group");
        group.forEach(CRUSHER::unlink);
        try {
            for (var order : List.of(List.of(source, fallback), List.of(fallback, source))) {
                var admitted = SharedOreCrushingRows.admitGroup(CRUSHER, order, original);
                require(admitted.committed() && admitted.accepted().size() == 2 && row(CRUSHER, input) == source
                        && row(CRUSHER, host(OreHostStone.TUFF)) == fallback, "normal admission supports both disjoint-domain orders");
                admitted.accepted().forEach(CRUSHER::unlink);
            }
            var rollback = SharedOreCrushingRows.admitGroup(CRUSHER, List.of(source, fallback, fallback), original);
            require(!rollback.committed() && rollback.accepted().equals(List.of(original)) && find(CRUSHER, input) == original
                    && !CRUSHER.mRecipeList.contains(source) && !CRUSHER.mRecipeList.contains(fallback),
                    "ordinary duplicate rejection rolls back the whole new group and restores exact legacy identity");
        } finally {
            CRUSHER.unlink(source); CRUSHER.unlink(fallback); CRUSHER.unlink(original);
            group.forEach(CRUSHER::add);
        }
        var foreignInput = input.copy(); foreignInput.getOrCreateTag().putString("fixture", "independent");
        var foreign = new Recipe(new ItemStack[]{foreignInput}, new ItemStack[]{new ItemStack(Items.DIAMOND)}, null,
                new long[]{10000}, null, null, 17, 7, 0);
        try {
            require(CRUSHER.addRecipe(foreign) == foreign, "independent tagged row admitted without collision bypass");
            for (int rebuild = 0; rebuild < 2; rebuild++) {
                SharedOreCrushingRows.rebuild();
                require(CRUSHER.mRecipeList.contains(foreign) && CRUSHER.mRecipeList.contains(original)
                        && !SharedOreCrushingRows.owned().contains(foreign), "foreign same-item preflight retains foreign and exact old producer identities");
                require(SharedOreCrushingRows.deferred().stream().anyMatch(d -> d.material().equals("Cinnabar")
                        && d.map().equals(CRUSHER.mNameInternal) && d.reason().equals("independent-same-item-row")), "whole-item deferral is explicit");
            }
        } finally { CRUSHER.unlink(foreign); SharedOreCrushingRows.rebuild(); }
    }

    private static void golden() {
        check(row(CRUSHER, host(OreHostStone.STONE)), 640, standardOutputs(), new long[]{10000, 10000, 3000, 10000});
        check(row(HAMMER, host(OreHostStone.STONE)), 544, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1)});
        for (var input : List.of(host(OreHostStone.TUFF), encoded("future_host", null))) {
            check(row(CRUSHER, input), 128, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.crushed, "Cinnabar", 1)});
            check(row(HAMMER, input), 32, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1)});
        }
        var raw = form(MaterialPrefix.oreRaw, "Cinnabar", 1);
        check(row(CRUSHER, raw), 128, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.crushed, "Cinnabar", 1),
                form(MaterialPrefix.gem, "Cinnabar", 1)}, new long[]{10000, 10000, 3000});
        check(row(HAMMER, raw), 32, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1)});
        var dense = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("gt6ce", "crystal_ore_cinnabar")));
        require(!dense.isEmpty(), "real dense Cinnabar icon holder");
        check(row(CRUSHER, dense), 256, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.crushed, "Cinnabar", 1),
                form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.gem, "Cinnabar", 2)},
                new long[]{10000, 10000, 10000, 10000, 3000});
        check(row(HAMMER, dense), 64, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 2)});
        var block = GTBlocks.getStack(BlockMaterialPrefix.blockRaw, material("Cinnabar"));
        require(!block.isEmpty() && block.getCount() == 1 && block.getItem() instanceof BlockItem, "real physical blockRaw1, not nine raw items");
        var physical = ((BlockItem) block.getItem()).getBlock();
        require(physical instanceof MaterialBlockLike form && form.prefix() == BlockMaterialPrefix.blockRaw
                && form.prefix().getMaterialWeight() == 18 * GTValues.U && form.material().resolve() == material("Cinnabar"),
                "physical raw block identity and18U metadata");
        var outputs = new ItemStack[10]; var chances = new long[10]; Arrays.fill(chances, 10000);
        for (int slot = 0; slot < 9; slot++) outputs[slot] = form(MaterialPrefix.crushed, "Cinnabar", 2);
        outputs[9] = form(MaterialPrefix.gem, "Cinnabar", 9); chances[9] = 3000;
        // GT blockRaw is newly registered in this slice, not a previously captured OPL input.
        check(row(CRUSHER, block), 1152, outputs, chances);
        check(row(HAMMER, block), 288, new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 9)});
        for (var fixed : List.of(raw, dense, block)) {
            var decorated = fixed.copy(); decorated.addTagElement("BlockStateTag", hostTag("bedrock"));
            require(row(CRUSHER, decorated) == row(CRUSHER, fixed), "arbitrary stone data cannot change a fixed raw/dense/block source kind");
        }
    }
    private static ItemStack[] standardOutputs() {
        return new ItemStack[]{form(MaterialPrefix.crushed, "Cinnabar", 1), form(MaterialPrefix.crushed, "Cinnabar", 1),
                form(MaterialPrefix.gem, "Cinnabar", 1), form(MaterialPrefix.dust, "Stone", 4)};
    }
    private static ItemStack host(OreHostStone host) {
        var stack = GTBlocks.getStack(BlockMaterialPrefix.ore, material("Cinnabar"));
        require(!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof OreBlock,
                "actual Cinnabar OreBlock holder");
        var ore = (OreBlock) ((BlockItem) stack.getItem()).getBlock();
        return OreBlock.withStone(stack, ore.stateFor(host));
    }
    private static ItemStack encoded(String state, String entity) {
        var stack = host(OreHostStone.STONE);
        if (state != null) stack.addTagElement("BlockStateTag", hostTag(state));
        if (entity != null) stack.addTagElement("BlockEntityTag", hostTag(entity));
        return stack;
    }
    private static ItemStack wrongType() {
        var stack = encoded(null, "stone"); var tag = new CompoundTag(); tag.putInt("stone", 7);
        stack.addTagElement("BlockStateTag", tag); return stack;
    }
    private static CompoundTag hostTag(String host) { var tag = new CompoundTag(); tag.putString("stone", host); return tag; }
    private static Recipe find(RecipeMap map, ItemStack input) { return map.findRecipe(List.of(input), List.of(), false, 1, map.mOutputItemsCount); }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var matching = map.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem() && new RecipeInputs.Matcher(List.of(input), List.of()).matches(r, 1)).toList();
        require(matching.size() == 1, map + " exact accepted domain for " + describe(input) + ", rows=" + matching.size());
        require(find(map, input) == matching.get(0) && map.containsInput(input), "indexed native lookup/filter: " + describe(input));
        return matching.get(0);
    }
    private static void check(Recipe recipe, long ticks, ItemStack[] outputs) {
        var chances = new long[outputs.length]; Arrays.fill(chances, 10000); check(recipe, ticks, outputs, chances);
    }
    private static void check(Recipe recipe, long ticks, ItemStack[] outputs, long[] chances) {
        var input = describe(recipe.mInputs[0]);
        require(recipe.mEUt == 16 && recipe.mDuration == ticks && recipe.mSpecialValue == 0,
                input + " expected16EU/" + ticks + "ticks, actual=" + recipe.mEUt + "/" + recipe.mDuration);
        require(recipe.mOutputs.length == outputs.length, input + " ordered output count expected=" + outputs.length + ", actual=" + recipe.mOutputs.length);
        for (int slot = 0; slot < outputs.length; slot++) require(ItemStack.matches(recipe.mOutputs[slot], outputs[slot]),
                input + " slot" + slot + " expected=" + describe(outputs[slot]) + ", actual=" + describe(recipe.mOutputs[slot]));
        require(Arrays.equals(recipe.mChances, chances), input + " chances expected=" + Arrays.toString(chances) + ", actual=" + Arrays.toString(recipe.mChances));
        require(recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 0, input + " no fluid slots");
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var stack = GTItems.getStack(prefix, material(name), count);
        require(!stack.isEmpty() && stack.getCount() == count, "actual registered " + prefix.getName() + "/" + name); return stack;
    }
    private static GTMaterial material(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        require(material.isValid() && material.getName().equals(name), "canonical material " + name); return material;
    }
    private static String describe(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount() + " " + stack.getTag(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
