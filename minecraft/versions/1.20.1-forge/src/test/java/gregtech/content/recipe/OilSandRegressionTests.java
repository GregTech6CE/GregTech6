package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreCrushingRules.Form;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** The OilSand event is independent of Crusher/Hammer permission and host quantity conversion. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class OilSandRegressionTests {
    private OilSandRegressionTests() {}
    private static final RecipeMap CENTRIFUGE = MachineRecipeMaps.Centrifuge;
    private static final RecipeMap CRUSHER = MachineRecipeMaps.Crusher, HAMMER = MachineRecipeMaps.Hammer;
    // Independent OP653-710 byproduct identities: each is direct dust4, never the crushing target.
    private record Host(OreHostStone host, String dust) {}
    private static final List<Host> HOSTS = List.of(
            new Host(OreHostStone.STONE, "Stone"), new Host(OreHostStone.GRANITE, "Granite"),
            new Host(OreHostStone.DIORITE, "Diorite"), new Host(OreHostStone.ANDESITE, "Andesite"),
            new Host(OreHostStone.DEEPSLATE, "Deepslate"), new Host(OreHostStone.NETHERRACK, "Netherrack"),
            new Host(OreHostStone.END_STONE, "Endstone"), new Host(OreHostStone.GRANITE_BLACK, "GraniteBlack"),
            new Host(OreHostStone.GRANITE_RED, "GraniteRed"), new Host(OreHostStone.BASALT, "Basalt"),
            new Host(OreHostStone.MARBLE, "Marble"), new Host(OreHostStone.LIMESTONE, "Limestone"),
            new Host(OreHostStone.GT_GRANITE, "Granite"), new Host(OreHostStone.GT_DIORITE, "Diorite"),
            new Host(OreHostStone.GT_ANDESITE, "Andesite"), new Host(OreHostStone.KOMATIITE, "Komatiite"),
            new Host(OreHostStone.GREENSCHIST, "Greenschist"), new Host(OreHostStone.BLUESCHIST, "Blueschist"),
            new Host(OreHostStone.KIMBERLITE, "Kimberlite"), new Host(OreHostStone.QUARTZITE, "Quartzite"),
            new Host(OreHostStone.PRISMARINE_LIGHT, "Prismarine"), new Host(OreHostStone.PRISMARINE_DARK, "PrismarineDark"),
            new Host(OreHostStone.SLATE, "Slate"), new Host(OreHostStone.SHALE, "Shale"),
            new Host(OreHostStone.GNEISS, "Gneiss"));

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void oilSandNativeHoldersAndIndependentMachines(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "registration and first server overlay completed");
        holders();
        golden();
        matching();
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void oilSandBoundariesAndOwnOnlyReload(GameTestHelper helper) {
        var raw = row(CENTRIFUGE, raw());
        int count = oilRows(CENTRIFUGE).size();
        controlledDefinitions();
        foreignOwnership();
        legacyFlags();
        try {
            SharedOilSandRows.clear();
            require(oilRows(CENTRIFUGE).isEmpty(), "server-stop hook removes only the captured OilSand event rows");
            require(find(CRUSHER, raw()) != null && find(HAMMER, blockRaw()) != null,
                    "clearing Centrifuge ownership leaves independent machine producers intact");
        } finally { SharedOilSandRows.rebuild(); }
        for (int reload = 0; reload < 2; reload++) {
            MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
            require(row(CENTRIFUGE, raw()) == raw, "unchanged raw Centrifuge preserves its accepted Recipe identity");
            require(oilRows(CENTRIFUGE).size() == count, "actual reload does not accumulate OilSand rows");
            golden();
        }
        helper.succeed();
    }

    private static void holders() {
        var oil = material("OilSand");
        require(oil.getId() == 9851 && GTMaterialRegistry.isCoreMaterial(oil)
                && GTMaterialRegistry.get("Oilsands").resolve() == oil, "canonical9851 OilSand, not an alias-shaped foreign material");
        var raw = raw();
        require(raw.getItem() instanceof MaterialFormItem form && form.getPrefix() == MaterialPrefix.oreRaw
                && form.getMaterial().resolve() == oil && form.getPrefix().getMaterialWeight() == 2 * GTValues.U,
                "actual oreRaw OilSand holder and2U metadata");
        for (var prefix : List.of(BlockMaterialPrefix.ore, BlockMaterialPrefix.blockRaw)) {
            var input = GTBlocks.getStack(prefix, oil); var holder = GTBlocks.getObject(prefix, oil);
            require(!input.isEmpty() && input.getCount() == 1 && input.getItem() instanceof BlockItem
                    && holder != null && holder.isPresent(), "bound physical " + prefix.getName());
            var block = ((BlockItem)input.getItem()).getBlock();
            require(block == holder.get() && block instanceof MaterialBlockLike form && form.prefix() == prefix
                    && form.material().resolve() == oil && prefix.getMaterialWeight() ==
                    (prefix == BlockMaterialPrefix.blockRaw ? 18 : 2) * GTValues.U,
                    "physical prefix/material/weight: " + prefix.getName());
            require(BuiltInRegistries.BLOCK.getKey(block).equals(BuiltInRegistries.ITEM.getKey(input.getItem())),
                    "bound block/item identity: " + prefix.getName());
        }
        require(oil.getTargetCrushingMaterial().resolve() == oil && oil.getTargetCrushingAmount() == GTValues.U
                && oil.getOreMultiplier() == 1 && oil.getToolQuality() == 0, "actual independent generic-handler inputs");
    }

    private static void golden() {
        var raw = row(CENTRIFUGE, raw());
        check(raw, 128, 500, dust("Sand", 4));
        require(!raw.hasSameItemInputPredicate(0) && SharedOilSandRows.baselineExcluded().contains(raw),
                "already-correct raw event retains the captured original, not a synthesized replacement");
        check(row(CENTRIFUGE, blockRaw()), 1152, 4500, dust("Sand", 36));
        Set<Recipe> hostRows = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var expected : HOSTS) {
            var input = host(expected.host()); var recipe = row(CENTRIFUGE, input);
            check(recipe, 128, 500, dust("Sand", 4), dust(expected.dust(), 4));
            require(recipe.hasSameItemInputPredicate(0) && recipe.viewerInputNote(0).isEmpty(),
                    "proven host uses a distinct unlabelled source domain: " + expected.host());
            require(recipe.viewerInputAlternatives(0).stream().allMatch(s -> recipe.matchesItemInput(0, s)),
                    "display representatives stay in their accepted source domain");
            hostRows.add(recipe);
        }
        require(HOSTS.size() == 25 && hostRows.size() == 25, "25 proven host domains are25 actual distinct accepted rows");
        // Loader_OreProcessing196 does not share Crusher's DUST_ORE exclusion.
        for (var sediment : List.of(OreHostStone.SAND, OreHostStone.RED_SAND, OreHostStone.GRAVEL))
            check(row(CENTRIFUGE, host(sediment)), 128, 500, dust("Sand", 4));
        check(row(CRUSHER, raw()), 128, 0, crushed(1), crushed(1));
        check(row(HAMMER, raw()), 32, 0, crushed(1));
        check(row(CRUSHER, host(OreHostStone.STONE)), 640, 0, crushed(1), crushed(1), dust("Stone", 4));
        check(row(HAMMER, host(OreHostStone.STONE)), 544, 0, crushed(1));
        var blockOutputs = new ItemStack[9]; Arrays.setAll(blockOutputs, ignored -> crushed(2));
        check(row(CRUSHER, blockRaw()), 1152, 0, blockOutputs);
        check(row(HAMMER, blockRaw()), 288, 0, crushed(9));
        long legacy = oilRows(CENTRIFUGE).stream().filter(r -> !r.viewerInputNote(0).isEmpty()).count();
        require(oilRows(CENTRIFUGE).size() == 31 && legacy == 1,
                "actual admission is30 source rows(raw+block+28hosts) plus1 UNKNOWN compatibility, not25/27 theoretical rows");
        require(SharedOilSandRows.deferred().isEmpty(), "default proven holders do not leave silent deferred OilSand groups");
        System.out.println("[oilsand-regression] canonical9851 holders=raw/ore/blockRaw, directHostTails=25/25"
                + ", supportedHostDomains=28, sourceNo=1, unknownHostDomains=10, sourceRows=30, legacyRows=" + legacy
                + ", acceptedCentrifuge=" + oilRows(CENTRIFUGE).size() + ", acceptedCrusher=" + oilRows(CRUSHER).size()
                + ", acceptedHammer=" + oilRows(HAMMER).size() + ", deferred=" + SharedOilSandRows.deferred());
    }

    private static void matching() {
        var stone = host(OreHostStone.STONE); var nether = host(OreHostStone.NETHERRACK);
        var source = row(CENTRIFUGE, stone); var legacy = row(CENTRIFUGE, host(OreHostStone.TUFF));
        check(legacy, 128, 500, dust("Sand", 4));
        require(source != legacy && !source.matchesItemInput(0, host(OreHostStone.TUFF))
                && !legacy.matchesItemInput(0, stone), "known and UNKNOWN domains are disjoint");
        require(!legacy.viewerInputNote(0).isEmpty(), "UNKNOWN legacy is visibly labelled, not claimed as source proof");
        int supported = 0, no = 0, unknown = 0;
        for (var host : OreHostStone.values()) {
            var found = find(CENTRIFUGE, host(host));
            if (host == OreHostStone.BEDROCK) { require(found == null, "bedrock is source NO"); no++; }
            else if (host.isSediment() || HOSTS.stream().anyMatch(h -> h.host() == host)) {
                require(found != null && found != legacy && found.viewerInputNote(0).isEmpty(), "proven source host " + host);
                supported++;
            } else { require(found == legacy, "unmapped host retains only compatibility " + host); unknown++; }
        }
        require(supported == 28 && no == 1 && unknown == 10, "actual39host partition is28YES/1NO/10UNKNOWN");
        for (var offered : List.of(stone, encoded("stone", null), encoded(null, "stone"), encoded("stone", "stone")))
            require(row(CENTRIFUGE, offered) == source, "default and legacy STONE encodings share one source row");
        require(row(CENTRIFUGE, encoded("netherrack", "stone")) == row(CENTRIFUGE, nether), "state host wins over entity host");
        for (var offered : List.of(encoded("future_host", null), encoded("future_host", "stone"),
                encoded("", "stone"), wrongType(), OreBlock.brokenStack(material("OilSand"), OreHostStone.DEEPSLATE)))
            require(row(CENTRIFUGE, offered) == legacy, "unproven/invalid host or broken mode remains UNKNOWN, never STONE");
        for (var expected : HOSTS) {
            var broken = OreBlock.brokenStack(material("OilSand"), expected.host());
            boolean unproved = expected.host() == OreHostStone.DEEPSLATE || expected.host() == OreHostStone.GNEISS;
            require(row(CENTRIFUGE, broken) == (unproved ? legacy : row(CENTRIFUGE, host(expected.host()))),
                    "only23 proven ordinary/broken prefix pairs share source domains: " + expected.host());
        }
        for (var sediment : List.of(OreHostStone.SAND, OreHostStone.RED_SAND, OreHostStone.GRAVEL))
            require(row(CENTRIFUGE, OreBlock.brokenStack(material("OilSand"), sediment)) == legacy,
                    "broken sediment mode is unproved rather than inferred from its ordinary prefix");
        for (var denied : List.of(host(OreHostStone.BEDROCK), GTBlocks.getStack(BlockMaterialPrefix.oreSmall, material("OilSand")))) {
            require(!denied.isEmpty() && find(CENTRIFUGE, denied) == null && !CENTRIFUGE.containsInput(denied),
                    "excluded source prefix has neither source nor UNKNOWN fallback: " + describe(denied));
        }
        var supplied = List.of(nether.copyWithCount(64), stone.copyWithCount(2));
        var consumed = RecipeInputs.consume(source, supplied, List.of(), 2);
        require(consumed != null && consumed.items().get(0).getCount() == 64 && consumed.items().get(1).isEmpty()
                && supplied.get(1).getCount() == 2, "parallel consumes only the correct host and leaves supplied stacks untouched");
        require(RecipeInputs.consume(source, List.of(nether.copyWithCount(64), stone), List.of(), 2) == null,
                "wrong-host counts cannot increase parallelism");
        var mutable = stone.copy();
        for (var offered : List.of(stone, nether, host(OreHostStone.TUFF), host(OreHostStone.BEDROCK), stone)) {
            mutable.setTag(offered.getTag() == null ? null : offered.getTag().copy());
            require(find(CENTRIFUGE, mutable) == find(CENTRIFUGE, offered), "warmed lookup observes in-place host changes");
        }
        mutable.getOrCreateTag().putString("fixture", "unrelated");
        require(row(CENTRIFUGE, mutable) == source, "unrelated stack metadata does not alter the host domain");
        for (var fixed : List.of(raw(), blockRaw())) {
            var decorated = fixed.copy(); decorated.addTagElement("BlockStateTag", hostTag("bedrock"));
            require(row(CENTRIFUGE, decorated) == row(CENTRIFUGE, fixed),
                    "arbitrary host metadata cannot turn a fixed raw kind into a bedrock ore");
        }
    }

    private static void controlledDefinitions() {
        var oil = material("OilSand"); var input = host(OreHostStone.STONE);
        var snapshot = new SharedOilSandRows.Snapshot();
        var candidate = SharedOilSandRows.candidate(oil, input, false, snapshot);
        var rawCandidate = SharedOilSandRows.candidate(oil, raw(), true, snapshot);
        require(candidate != null && rawCandidate != null, "real native host/raw candidates");
        require(SharedOilSandRows.candidate(new GTMaterial(9851, "OilSand", "Unregistered identity fixture", 0),
                input, false, snapshot) == null, "matching id/name without core provenance is not owned");
        require(SharedOilSandRows.candidate(oil, new ItemStack(Items.FLINT), false, snapshot) == null
                && SharedOilSandRows.candidate(material("Copper"), input, false, snapshot) == null,
                "foreign item and wrong canonical material never enter this event");
        var independent = SharedOilSandRows.definitions(candidate, new SharedOilSandRows.Snapshot() {
            @Override GTMaterial target(GTMaterial m) { throw new AssertionError("OilSand event must not read crushing target"); }
            @Override int quality(GTMaterial m) { throw new AssertionError("OilSand event must not read host/input quality"); }
            @Override long amount(GTMaterial m) { throw new AssertionError("OilSand event must not convert crushing amount"); }
            @Override int oreMultiplier(GTMaterial m) { throw new AssertionError("OilSand event must not read ore multiplier"); }
            @Override int processingMultiplier(GTMaterial m) { throw new AssertionError("OilSand event must not read processing multiplier"); }
            @Override boolean anti(GTMaterial m) { throw new AssertionError("OilSand event does not add an antimatter gate"); }
        });
        check(planned(independent, input), 128, 500, dust("Sand", 4), dust("Stone", 4));
        var unrelatedOverride = SharedOilSandRows.definitions(candidate, new SharedOilSandRows.Snapshot() {
            @Override boolean overridden(GTMaterial m, Collection<MaterialPrefix> prefixes) {
                return m == oil && prefixes.contains(MaterialPrefix.crushed) || super.overridden(m, prefixes);
            }
        });
        check(planned(unrelatedOverride, input), 128, 500, dust("Sand", 4), dust("Stone", 4));
        var absentTail = SharedOilSandRows.definitions(candidate, new SharedOilSandRows.Snapshot() {
            @Override NativeForm resolve(Form form, Identity id) {
                return id.name().equals("Stone") ? NativeForm.absent() : super.resolve(form, id);
            }
        });
        check(planned(absentTail, input), 128, 500, dust("Sand", 4));
        var smallerTail = SharedOilSandRows.definitions(candidate, new SharedOilSandRows.Snapshot() {
            @Override NativeForm resolve(Form form, Identity id) {
                return id.name().equals("Stone") && form == Form.DUST ? NativeForm.absent() : super.resolve(form, id);
            }
        });
        check(planned(smallerTail, input), 128, 500, dust("Sand", 4), form(MaterialPrefix.dustSmall, "Stone", 16));
        var fluidOnly = SharedOilSandRows.definitions(rawCandidate, new SharedOilSandRows.Snapshot() {
            @Override NativeForm resolve(Form form, Identity id) { return NativeForm.absent(); }
        });
        check(planned(fluidOnly, raw()), 128, 500);
        noRows(SharedOilSandRows.definitions(candidate, new SharedOilSandRows.Snapshot() {
            @Override NativeForm resolve(Form form, Identity id) {
                return id.name().equals("Stone") ? NativeForm.unknown() : super.resolve(form, id);
            }
        }), SharedOilSandRows.Action.LEGACY_UNSUPPORTED);
        noRows(SharedOilSandRows.definitions(rawCandidate, new SharedOilSandRows.Snapshot() {
            @Override boolean overridden(GTMaterial m, Collection<MaterialPrefix> prefixes) {
                return m == oil && prefixes.contains(MaterialPrefix.oreRaw) || super.overridden(m, prefixes);
            }
        }), SharedOilSandRows.Action.LEGACY_UNSUPPORTED);
        for (var invalidOil : List.of(
                new SharedOilSandRows.Snapshot() { @Override FluidStack oil(int amount) { return FluidStack.EMPTY; } },
                new SharedOilSandRows.Snapshot(FluidCatalog.all(), (field, amount) -> new FluidStack(Fluids.WATER, amount)),
                new SharedOilSandRows.Snapshot(FluidCatalog.all(), (field, amount) -> GTFluids.stack(field, amount - 1))))
            noRows(SharedOilSandRows.definitions(candidate, invalidOil), SharedOilSandRows.Action.DEFERRED_NO_ROW);
    }
    private static Recipe planned(SharedOilSandRows.Outcome outcome, ItemStack input) {
        require(outcome.action() == SharedOilSandRows.Action.SUPPORTED, "controlled source definition: " + outcome);
        var matching = outcome.rows().stream().filter(r -> r.matchesItemInput(0, input)).toList();
        require(matching.size() == 1, "controlled exact domain expected1, actual=" + matching.size()); return matching.get(0);
    }
    private static void noRows(SharedOilSandRows.Outcome outcome, SharedOilSandRows.Action action) {
        require(outcome.action() == action && outcome.rows().isEmpty() && !outcome.unknownCompatibility(),
                "controlled boundary expected=" + action + ", actual=" + outcome);
    }
    private static void foreignOwnership() {
        var input = host(OreHostStone.STONE); var foreignInput = input.copy();
        foreignInput.getOrCreateTag().putString("fixture", "independent-oilsand");
        var foreign = new Recipe(new ItemStack[]{foreignInput}, new ItemStack[]{new ItemStack(Items.DIAMOND)},
                null, null, null, null, 17, 7, 0);
        var original = SharedOilSandRows.baselineExcluded().stream().filter(r -> r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem() && !r.hasSameItemInputPredicate(0)).findFirst()
                .orElseThrow(() -> new AssertionError("Captured OilSand Centrifuge original missing: " + describe(input)));
        try {
            require(CENTRIFUGE.addRecipe(foreign) == foreign, "independent same-item tagged row admitted normally");
            for (int rebuild = 0; rebuild < 2; rebuild++) {
                SharedOilSandRows.rebuild();
                require(CENTRIFUGE.mRecipeList.contains(foreign) && !SharedOilSandRows.owned().contains(foreign)
                        && CENTRIFUGE.mRecipeList.contains(original),
                        "rebuild preserves foreign identity and restores the exact captured legacy object");
                require(SharedOilSandRows.deferred().stream().anyMatch(d -> d.reason().equals("independent-same-item-row")),
                        "same-item foreign collision is reported as deferral");
            }
        } finally { CENTRIFUGE.unlink(foreign); SharedOilSandRows.rebuild(); }
        var rawOriginal = row(CENTRIFUGE, raw());
        try {
            SharedOilSandRows.rebuild(new SharedOilSandRows.Snapshot() {
                @Override boolean overridden(GTMaterial m, Collection<MaterialPrefix> prefixes) {
                    return m == OilSandRegressionTests.material("OilSand") && prefixes.contains(MaterialPrefix.oreRaw)
                            || super.overridden(m, prefixes);
                }
            });
            require(row(CENTRIFUGE, input) != original && row(CENTRIFUGE, raw()) == rawOriginal,
                    "late raw-form override restores its exact old identity without suppressing independent physical ores");
            check(row(CENTRIFUGE, input), 128, 500, dust("Sand", 4), dust("Stone", 4));
            check(row(CENTRIFUGE, blockRaw()), 1152, 4500, dust("Sand", 36));
            require(SharedOilSandRows.deferred().size() == 1
                    && SharedOilSandRows.deferred().get(0).kind().equals("RAW_ITEM"),
                    "exactly the overridden raw input group is explicitly deferred");
        } finally { SharedOilSandRows.rebuild(); }
    }

    private static void legacyFlags() {
        for (var map : List.of(CENTRIFUGE, CRUSHER)) {
            // OilSand Crusher is newly wired with no historical row; Cinnabar has a captured OPL original.
            var input = map == CENTRIFUGE ? host(OreHostStone.STONE)
                    : GTBlocks.getStack(BlockMaterialPrefix.ore, material("Cinnabar"));
            require(!input.isEmpty(), "actual captured legacy holder for " + map);
            var originals = map == CENTRIFUGE ? SharedOilSandRows.baselineExcluded() : SharedOreCrushingRows.baselineExcluded();
            var original = originals.stream().filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == input.getItem()
                    && !r.hasSameItemInputPredicate(0) && r.mOutputs.length == (map == CENTRIFUGE ? 1 : 2)
                    && r.mFluidOutputs.length == (map == CENTRIFUGE ? 1 : 0)).findFirst()
                    .orElseThrow(() -> new AssertionError("Captured legacy flags fixture missing for " + map + ": " + describe(input)));
            Runnable rebuild = map == CENTRIFUGE ? SharedOilSandRows::rebuild : SharedOreCrushingRows::rebuild;
            boolean hidden = original.mHidden, fake = original.mFakeRecipe;
            try {
                for (int flags = 1; flags <= 3; flags++) {
                    original.mHidden = (flags & 1) != 0; original.mFakeRecipe = (flags & 2) != 0;
                    rebuild.run();
                    require(map.mRecipeList.contains(original) && original.mHidden == ((flags & 1) != 0)
                            && original.mFakeRecipe == ((flags & 2) != 0),
                            map + " must restore the exact overridden legacy object without resetting hidden/fake flags");
                }
            } finally {
                original.mHidden = hidden; original.mFakeRecipe = fake; rebuild.run();
            }
        }
    }

    private static List<Recipe> oilRows(RecipeMap map) {
        var items = List.of(raw().getItem(), host(OreHostStone.STONE).getItem(), blockRaw().getItem());
        return map.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 1
                && items.contains(r.mInputs[0].getItem())).toList();
    }
    private static ItemStack host(OreHostStone host) {
        var stack = GTBlocks.getStack(BlockMaterialPrefix.ore, material("OilSand"));
        require(!stack.isEmpty() && stack.getItem() instanceof BlockItem
                && ((BlockItem)stack.getItem()).getBlock() instanceof OreBlock, "actual OilSand OreBlock holder");
        return OreBlock.withStone(stack, ((OreBlock)((BlockItem)stack.getItem()).getBlock()).stateFor(host));
    }
    private static ItemStack encoded(String state, String entity) {
        var stack = host(OreHostStone.STONE);
        if (state != null) stack.addTagElement("BlockStateTag", hostTag(state));
        if (entity != null) stack.addTagElement("BlockEntityTag", hostTag(entity));
        return stack;
    }
    private static CompoundTag hostTag(String host) { var tag = new CompoundTag(); tag.putString("stone", host); return tag; }
    private static ItemStack wrongType() {
        var stack = encoded(null, "stone"); var tag = new CompoundTag(); tag.putInt("stone", 7);
        stack.addTagElement("BlockStateTag", tag); return stack;
    }
    private static ItemStack raw() { return form(MaterialPrefix.oreRaw, "OilSand", 1); }
    private static ItemStack blockRaw() { return GTBlocks.getStack(BlockMaterialPrefix.blockRaw, material("OilSand")); }
    private static ItemStack crushed(int count) { return form(MaterialPrefix.crushed, "OilSand", count); }
    private static ItemStack dust(String name, int count) { return form(MaterialPrefix.dust, name, count); }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var stack = GTItems.getStack(prefix, material(name), count);
        require(!stack.isEmpty() && stack.getCount() == count, "actual form " + prefix.getName() + "/" + name); return stack;
    }
    private static GTMaterial material(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        require(material.isValid() && material.getName().equals(name), "canonical material " + name); return material;
    }
    private static Recipe find(RecipeMap map, ItemStack input) { return map.findRecipe(List.of(input), List.of(), false, 1, map.mOutputItemsCount); }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var matching = map.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem() && new RecipeInputs.Matcher(List.of(input), List.of()).matches(r, 1)).toList();
        require(matching.size() == 1, map + " exact OilSand domain for " + describe(input) + ", rows=" + matching.size());
        require(find(map, input) == matching.get(0) && map.containsInput(input), "indexed lookup and input filtering: " + describe(input));
        return matching.get(0);
    }
    private static void check(Recipe recipe, long ticks, int oil, ItemStack... outputs) {
        var input = describe(recipe.mInputs[0]);
        require(recipe.mInputs[0].getCount() == 1 && recipe.mEUt == 16 && recipe.mDuration == ticks
                && recipe.mSpecialValue == 0, input + " expected16EU/" + ticks + "ticks, actual=" + recipe.mEUt + "/" + recipe.mDuration);
        require(recipe.mOutputs.length == outputs.length, input + " output slots expected=" + outputs.length + ", actual=" + recipe.mOutputs.length);
        for (int slot = 0; slot < outputs.length; slot++) require(ItemStack.matches(recipe.mOutputs[slot], outputs[slot]),
                input + " slot" + slot + " expected=" + describe(outputs[slot]) + ", actual=" + describe(recipe.mOutputs[slot]));
        var chances = new long[outputs.length]; Arrays.fill(chances, 10000);
        require(Arrays.equals(recipe.mChances, chances) && Arrays.equals(recipe.mMaxChances, chances), input + " guaranteed ordered outputs");
        require(recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == (oil == 0 ? 0 : 1), input + " fluid slots");
        if (oil != 0) require(recipe.mFluidOutputs[0].getFluid() == GTFluids.still("Oil_Normal").get()
                && recipe.mFluidOutputs[0].getAmount() == oil, input + " exact Oil_Normal amount" + oil);
    }
    private static String describe(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount() + " " + stack.getTag(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
