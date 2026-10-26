package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.Predicate;
import gregtech.content.recipe.SourceOreCrushingRules.Stack;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceOreCrushingRules.*;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** Native ownership and host decoding for the source ore handlers, not a global ore unifier. */
final class SourceOreCrushingAdapter {
    private SourceOreCrushingAdapter() {}
    enum NativeKind { HOST_ORE, FIXED_STONE, DENSE, RAW_ITEM, RAW_BLOCK }
    enum Domain { SOURCE_PROFILE, SOURCE_NO, UNKNOWN_HOST }
    enum Action { SUPPORTED, SOURCE_NO_ROW, DEFERRED_NO_ROW, LEGACY_UNSUPPORTED }
    record Candidate(GTMaterial material, ItemStack input, NativeKind kind) {
        Candidate { input = input.copy(); }
        @Override public ItemStack input() { return input.copy(); }
    }
    record HostSelection(Domain domain, String host, boolean broken) {}
    record Outcome(Action action, List<Recipe> rows, boolean unknownCompatibility, String reason) {
        Outcome { rows = List.copyOf(rows); }
    }
    private static Outcome none(Action action, String reason) { return new Outcome(action, List.of(), false, reason); }

    static Candidate candidate(GTMaterial material, ItemStack input, boolean raw, Snapshot s) {
        if (!s.context(material) || input == null || input.isEmpty() || input.getCount() != 1) return null;
        NativeKind kind = s.kind(material, input, raw);
        return kind == null ? null : new Candidate(material.resolve(), input, kind);
    }

    static Outcome definitions(Candidate candidate, Machine machine, Snapshot s) {
        GTMaterial m = candidate.material();
        if (!s.context(m) || s.kind(m, candidate.input(), candidate.kind() == NativeKind.RAW_ITEM) != candidate.kind())
            return none(Action.LEGACY_UNSUPPORTED, "native-context-changed");
        if (s.anti(m)) return none(Action.SOURCE_NO_ROW, "source-antimatter");
        GTMaterial target = s.target(m);
        if (target == null || !target.isValid()) return none(Action.SOURCE_NO_ROW, "invalid-crushing-target");
        if (!s.identity(target) || s.overridden(target, List.of(MaterialPrefix.crushed, MaterialPrefix.dust,
                MaterialPrefix.gem, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72)))
            return none(Action.LEGACY_UNSUPPORTED, "target-form-provenance");
        if (target.getName().equals("Gneiss") || target.getName().equals("PetrifiedWood"))
            return none(Action.LEGACY_UNSUPPORTED, "independent-source-special-target");
        var input = s.input(m, target);
        var rows = new ArrayList<Recipe>();
        if (candidate.kind() == NativeKind.HOST_ORE) {
            OreBlock block = (OreBlock)((BlockItem)candidate.input().getItem()).getBlock();
            for (OreHostStone host : OreHostStone.values()) {
                if (SourceOreCrushingFacts.hostDecision(host.id()) != Decision.YES) continue;
                var profile = SourceOreCrushingFacts.hostProfile(host.id());
                Gangue gangue = s.gangue(profile);
                if (gangue == null) return none(Action.LEGACY_UNSUPPORTED, "gangue-provenance:" + host.id());
                Result result = plan(new Request(machine, Kind.STANDARD, input, profile.hexoriumAdjusted(),
                        List.of(gangue), capacity(machine)), s::available);
                Outcome unsupported = unsupported(result);
                if (unsupported != null) return unsupported;
                if (result.status() == Status.NO_ROW) continue;
                ItemStack representative = OreBlock.withStone(candidate.input(), block.stateFor(host));
                Predicate<ItemStack> matches = stack -> {
                    HostSelection actual = classifyHost(stack);
                    return actual.domain() == Domain.SOURCE_PROFILE && actual.host().equals(host.id());
                };
                Recipe row = make(machine, representative, result, matches, s);
                if (row == null) return none(Action.DEFERRED_NO_ROW, "native-normalization");
                if (SourceOreCrushingFacts.hasBrokenSourcePair(host.id())) row.withViewerInputAlternatives(0,
                        List.of(representative, OreBlock.withStone(candidate.input(), block.brokenStateFor(host))));
                rows.add(row);
            }
            return new Outcome(Action.SUPPORTED, rows, true, "verified-host-domains");
        }
        Kind kind = switch (candidate.kind()) {
            case DENSE -> Kind.DENSE; case RAW_ITEM -> Kind.RAW_ITEM; case RAW_BLOCK -> Kind.RAW_BLOCK;
            default -> Kind.STANDARD;
        };
        var gangue = new ArrayList<Gangue>();
        if (candidate.kind() == NativeKind.FIXED_STONE) {
            Gangue stone = s.gangue(SourceOreCrushingFacts.hostProfile("stone"));
            if (stone == null) return none(Action.LEGACY_UNSUPPORTED, "stone-gangue-provenance");
            gangue.add(stone);
        }
        Result result = plan(new Request(machine, kind, input, false, gangue, capacity(machine)), s::available);
        Outcome unsupported = unsupported(result);
        if (unsupported != null) return unsupported;
        if (result.status() == Status.NO_ROW) return none(Action.DEFERRED_NO_ROW, "required-primary-absent");
        Recipe row = make(machine, candidate.input(), result, stack -> true, s);
        return row == null ? none(Action.DEFERRED_NO_ROW, "native-normalization")
                : new Outcome(Action.SUPPORTED, List.of(row), false, "verified-fixed-kind");
    }
    private static int capacity(Machine machine) {
        return machine == Machine.CRUSHER ? gregtech.content.data.MachineRecipeMaps.Crusher.mOutputItemsCount
                : gregtech.content.data.MachineRecipeMaps.Hammer.mOutputItemsCount;
    }
    private static Outcome unsupported(Result result) {
        if (result.status() != Status.UNSUPPORTED) return null;
        // A known zero Hammer yield must not revive a captured positive-count row, even for UNKNOWN hosts.
        return none(result.reason().startsWith("zero Hammer output") ? Action.DEFERRED_NO_ROW
                : Action.LEGACY_UNSUPPORTED, result.reason());
    }
    private static Recipe make(Machine machine, ItemStack input, Result result, Predicate<ItemStack> predicate, Snapshot s) {
        var outputs = new ItemStack[result.slots().size()]; var chances = new long[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            Slot slot = result.slots().get(i); chances[i] = slot.chance();
            outputs[i] = slot.stack() == null ? ItemStack.EMPTY : s.stack(slot.stack());
        }
        Recipe row = RecipeMap.make(result.optimize(), new ItemStack[]{input}, outputs, null, chances,
                new FluidStack[0], new FluidStack[0], result.ticks(), result.power(), 0);
        if (row == null) return null;
        // The source Hammer does not bindStack its one primary count. Native make otherwise clips
        // it to64; restore only that verified single-output normalization, not general recipe math.
        if (outputs.length == 1 && outputs[0].getCount() > 64) {
            if (machine != Machine.HAMMER || result.optimize() || row.mInputs.length != 1 || row.mInputs[0].getCount() != 1
                    || row.mOutputs.length != 1 || row.mOutputs[0].getCount() != 64
                    || row.mFluidInputs.length != 0 || row.mFluidOutputs.length != 0) return null;
            ItemStack expected = outputs[0].copy(); expected.setCount(64);
            if (!ItemStack.matches(expected, row.mOutputs[0])) return null;
            row = new Recipe(row.mInputs, copies(outputs), row.mSpecialItems, row.mChances,
                    row.mFluidInputs, row.mFluidOutputs, row.mDuration, row.mEUt, row.mSpecialValue);
        }
        return row.withSameItemInputPredicate(0, predicate);
    }

    /** Only this complementary domain uses the old producer's normalized fields. */
    static Recipe compatibility(Candidate candidate, Recipe legacy) {
        if (candidate.kind() != NativeKind.HOST_ORE || legacy == null || legacy.mInputs.length != 1
                || legacy.mFluidInputs.length != 0) return null;
        OreBlock block = (OreBlock)((BlockItem)candidate.input().getItem()).getBlock();
        ItemStack representative = OreBlock.withStone(candidate.input(), block.stateFor(OreHostStone.TUFF));
        if (classifyHost(representative).domain() != Domain.UNKNOWN_HOST) return null;
        var row = new Recipe(new ItemStack[]{representative}, copies(legacy.mOutputs), legacy.mSpecialItems,
                legacy.mChances.clone(), Arrays.stream(legacy.mFluidInputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                Arrays.stream(legacy.mFluidOutputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                legacy.mDuration, legacy.mEUt, legacy.mSpecialValue);
        row.mEnabled = legacy.mEnabled; row.mCanBeBuffered = legacy.mCanBeBuffered;
        row.mNeedsEmptyOutput = legacy.mNeedsEmptyOutput;
        System.arraycopy(legacy.mMaxChances, 0, row.mMaxChances, 0, legacy.mMaxChances.length);
        return row.withSameItemInputPredicate(0, stack -> classifyHost(stack).domain() == Domain.UNKNOWN_HOST)
                .withViewerInputNote(0, "gt6ce.jei.ore_host_legacy");
    }
    private static ItemStack[] copies(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(s -> s == null ? ItemStack.EMPTY : s.copy()).toArray(ItemStack[]::new);
    }

    /** Strict evidence reader, intentionally not OreHostStone.byId's unknown-to-STONE convenience. */
    static HostSelection classifyHost(ItemStack stack) {
        HostData data = hostData(stack);
        Decision host = SourceOreCrushingFacts.hostDecision(data.host());
        if (host == Decision.NO) return new HostSelection(Domain.SOURCE_NO, data.host(), data.broken());
        if (!data.knownMode() || host == Decision.UNKNOWN
                || data.broken() && !SourceOreCrushingFacts.hasBrokenSourcePair(data.host()))
            return new HostSelection(Domain.UNKNOWN_HOST, data.host(), data.broken());
        return new HostSelection(Domain.SOURCE_PROFILE, data.host(), data.broken());
    }
    private record HostData(String host, boolean broken, boolean knownMode) {}
    private static HostData hostData(ItemStack stack) {
        String host = null, mode = null; boolean knownMode = true;
//? if >=1.21.1 {
        /*var properties = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).properties();
        if (properties.containsKey("stone")) host = properties.get("stone");
        if (properties.containsKey("broken")) mode = properties.get("broken");
        var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom == null ? null : custom.copyTag();
        if (host == null && tag != null) host = legacyHost(tag);
*///?} else {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            host = legacyHost(tag);
            if (tag.contains("BlockStateTag") && !tag.contains("BlockStateTag", Tag.TAG_COMPOUND)) knownMode = false;
            CompoundTag state = tag.getCompound("BlockStateTag");
            if (state.contains("broken")) mode = state.contains("broken", Tag.TAG_STRING) ? state.getString("broken") : "?";
        }
//?}
        if (mode != null && !mode.equals("true") && !mode.equals("false")) knownMode = false;
        return new HostData(host == null ? "stone" : host, "true".equals(mode), knownMode);
    }
    private static String legacyHost(CompoundTag tag) {
        for (String key : List.of("BlockStateTag", "BlockEntityTag")) {
            if (!tag.contains(key)) continue;
            if (!tag.contains(key, Tag.TAG_COMPOUND)) return "";
            CompoundTag part = tag.getCompound(key);
            if (part.contains("stone")) return part.contains("stone", Tag.TAG_STRING) ? part.getString("stone") : "";
        }
        return null;
    }

    /** One rebuild's holder/override snapshot; runtime conversion quantities remain source inputs. */
    static class Snapshot extends SourceOreMacerationAdapter.Snapshot {
        private record FormKey(Form form, GTMaterial material) {}
        private final Map<FormKey, NativeForm> forms = new HashMap<>();
        @Override boolean identity(GTMaterial material) {
            return material != null && material.isValid() && material.resolve() == material
                    && GTMaterialRegistry.isCoreMaterial(material) && !AddonMaterials.owns(material)
                    && SourceOreCrushingFacts.pulverizingCinnabar(material.getId(), material.getName()) != Decision.UNKNOWN;
        }
        boolean context(GTMaterial material) {
            return identity(material) && !legacyContinuation(material) && !material.getName().equals("Oilsands")
                    && !overridden(material, List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed, MaterialPrefix.crushedTiny,
                    MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny,
                    MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny));
        }
        NativeKind kind(GTMaterial material, ItemStack input, boolean raw) {
            if (raw) return SourceFurnaceNativeAdapter.nativeForm(MaterialPrefix.oreRaw, material, input)
                    && direct(MaterialPrefix.oreRaw, material).availability() == Availability.PRESENT
                    && direct(MaterialPrefix.oreRaw, material).stack().getItem() == input.getItem() ? NativeKind.RAW_ITEM : null;
            if (!(input.getItem() instanceof BlockItem item)
                    || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")) return null;
            var block = item.getBlock();
            if (block.getClass() == OreBlock.class) {
                OreBlock ore = (OreBlock)block;
                return ore.prefix() == BlockMaterialPrefix.ore && ore.material().resolve() == material
                        && BlockMaterialPrefix.ore.getMaterialWeight() == 2 * U
                        && GTBlocks.getStack(BlockMaterialPrefix.ore, material).getItem() == item
                        && composition(input, MaterialPrefix.oreVanillastone, material, 2 * U, true) ? NativeKind.HOST_ORE : null;
            }
            if (block instanceof MaterialBlockLike physical && physical.prefix() == BlockMaterialPrefix.blockRaw)
                return physical.material().resolve() == material && physical.prefix().getMaterialWeight() == 18 * U
                        && GTBlocks.getStack(BlockMaterialPrefix.blockRaw, material).getItem() == item
                        && composition(input, null, material, 18 * U, true) ? NativeKind.RAW_BLOCK : null;
            if (!special(block)) return null;
            if (block instanceof DenseOreBlock dense && dense.material().resolve() == material)
                return composition(input, MaterialPrefix.oreDense, material, 4 * U, false) ? NativeKind.DENSE : null;
            if (block instanceof VanillaOreBlock ordinary && ordinary.material().resolve() == material)
                return composition(input, MaterialPrefix.oreVanillastone, material, 2 * U, false) ? NativeKind.FIXED_STONE : null;
            return null;
        }
        private static boolean special(net.minecraft.world.level.block.Block block) {
//? if >=1.21.1 {
            /*return gregtech.content.registry.GTSpecialOreBlocks.all().stream()
                    .anyMatch(holder -> holder.isBound() && holder.get() == block);
*///?} else {
            return gregtech.content.registry.GTIconSetBlocks.all().stream()
                    .anyMatch(holder -> holder.isPresent() && holder.get() == block);
//?}
        }
        private static boolean composition(ItemStack input, MaterialPrefix prefix, GTMaterial material, long amount, boolean optional) {
            var data = ItemMaterialRegistry.explicit(input.getItem()).orElse(null);
            return data == null ? optional : data.recoverable() && data.prefix() == prefix && data.components().size() == 1
                    && data.material().resolve() == material && data.amount() == amount;
        }
        @Override GTMaterial target(GTMaterial material) {
            var target = material.getTargetCrushingMaterial(); return target == null ? null : target.resolve();
        }
        long amount(GTMaterial material) { return material.getTargetCrushingAmount(); }
        int oreMultiplier(GTMaterial material) { return material.getOreMultiplier(); }
        int processingMultiplier(GTMaterial material) { return material.getOreProcessingMultiplier(); }
        Input input(GTMaterial material, GTMaterial target) {
            return new Input(id(material), id(target), amount(material), oreMultiplier(material), processingMultiplier(material),
                    quality(material), new Bonus(SourceOreCrushingFacts.pulverizingCinnabar(material.getId(), material.getName()),
                    byproduct(material, 800, "Mercury"), byproduct(material, 9114, "Cinnabar"), byproduct(material, 8333, "Redstone")),
                    SourceOreCrushingFacts.hexorium(material.getId(), material.getName()));
        }
        private boolean byproduct(GTMaterial material, int id, String name) {
            return material.getByProducts().stream().map(GTMaterial::resolve)
                    .anyMatch(m -> identity(m) && m.getId() == id && m.getName().equals(name));
        }
        Gangue gangue(SourceOreCrushingFacts.HostProfile profile) {
            GTMaterial material = material(profile.gangueMaterial());
            GTMaterial target = material == null ? null : target(material);
            return !identity(material) || !identity(target) ? null
                    : new Gangue(id(material), profile.gangueAmount(), quality(material), id(target), amount(material));
        }
        GTMaterial material(Identity identity) {
            var material = GTMaterialRegistry.get(identity.name());
            return material != null && material.getId() == identity.id() && material.getName().equals(identity.name())
                    && identity(material) ? material : null;
        }
        Availability available(Form form, Identity material, int count) { return resolve(form, material).availability(); }
        ItemStack stack(Stack stack) {
            NativeForm resolved = resolve(stack.form(), stack.material());
            if (resolved.availability() != Availability.PRESENT) return ItemStack.EMPTY;
            ItemStack result = resolved.stack().copy(); result.setCount(stack.count()); return result;
        }
        NativeForm resolve(Form form, Identity identity) {
            GTMaterial material = material(identity); if (material == null) return NativeForm.unknown();
            return forms.computeIfAbsent(new FormKey(form, material), key -> {
                if (form == Form.BLOCK_DUST) return blockDust(material);
                MaterialPrefix prefix = switch (form) {
                    case CRUSHED -> MaterialPrefix.crushed; case CRUSHED_TINY -> MaterialPrefix.crushedTiny;
                    case DUST -> MaterialPrefix.dust; case DUST_SMALL -> MaterialPrefix.dustSmall;
                    case DUST_TINY -> MaterialPrefix.dustTiny; case DUST_DIV72 -> MaterialPrefix.dustDiv72;
                    case GEM -> MaterialPrefix.gem; default -> throw new IllegalArgumentException("Dust block is physical");
                };
                return overridden(material, List.of(prefix)) ? NativeForm.unknown() : direct(prefix, material);
            });
        }
        NativeForm blockDust(GTMaterial material) {
            ItemStack selected = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
            if (selected.isEmpty()) return NativeForm.absent();
            if (!(selected.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof MaterialBlockLike physical)
                    || physical.prefix() != BlockMaterialPrefix.blockDust || physical.prefix().getMaterialWeight() != 9 * U
                    || physical.material().resolve() != material || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce"))
                return NativeForm.unknown();
            return NativeForm.present(selected);
        }
    }
    private static Identity id(GTMaterial material) { return new Identity(material.getId(), material.getName()); }
}
