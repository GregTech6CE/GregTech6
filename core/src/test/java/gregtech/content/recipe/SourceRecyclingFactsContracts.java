package gregtech.content.recipe;

import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.HashSet;
import java.util.List;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceRecyclingFacts.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Finite source/default provenance contracts; this does not assert native recipe registration counts. */
public final class SourceRecyclingFactsContracts {
    private SourceRecyclingFactsContracts() {}
    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        run();
        System.out.println("Source recycling facts contracts passed (definitions, not native registrations).");
    }
    public static void run() {
        identityAndClosedTargets();
        sourceFactoryAndDeclarationCases();
        copiedTargetsAndPortDefaults();
        phaseUnitsAndOptionalBoundary();
        currentDefaultFingerprint();
    }
    private static Facts fact(int id, String name) {
        Facts facts = find(id, name);
        check(facts != null, "reviewed identity " + id + "/" + name);
        return facts;
    }
    private static void identityAndClosedTargets() {
        equal(1004, all().size(), "1002 input identities and Water/Lava target-only facts");
        equal(1002L, all().stream().filter(f -> f.portDefaults() != null).count(), "bounded default input roster");
        equal(null, find(9516, "Superconductor"), "port-only identity remains unsupported");
        equal(null, find(280, "Ni"), "canonical source name, not an alias");
        equal(null, find(10280, "Nickel"), "same name with addon ID is not source provenance");
        equal(null, find(280, null), "missing name is unsupported");
        equal(null, defaults(9800, "Water"), "target-only material has no input default profile");
        equal(null, defaults(9810, "Lava"), "target-only Lava has no input default profile");
        check(contextDefaults(9800, "Water").matches(false, 9800, "Water", U), "Water target context defaults");
        check(contextDefaults(9810, "Lava").matches(false, 9810, "Lava", U), "Lava target context defaults");
        check(!contextDefaults(9810, "Lava").matches(false, 9107, "Calcite", U),
                "target-only Calcite conversion override is not a reviewed default");
        equal(null, contextDefaults(9800, "alias:Water"), "target-only profile still requires exact identity");
        var ids = new HashSet<Integer>();
        for (Facts f : all()) {
            check(ids.add(f.identity().id()), "unique identity");
            check(find(f.target().id(), f.target().name()) != null, "every conversion target is reviewed");
            check(f.sourceLine() > 0, "source declaration provenance");
            check(f.melting() != Decision.UNKNOWN && f.blacklistedSmelter() != Decision.UNKNOWN
                    && f.furnace() != Decision.UNKNOWN, "only closed facts enter the roster");
            equal(f.phase().kind() == PhaseKind.DEFAULT_BINDING, f.phase().binding() != null,
                    "binding presence is explicit evidence, not a guessed default");
        }
        equal(237L, all().stream().filter(f -> f.phase().kind() == PhaseKind.DEFAULT_BINDING).count(),
                "phase facts include explicit bindings, not just source MOLTEN flags");
        equal(List.of(8752), all().stream().filter(f -> f.phase().kind() == PhaseKind.OPTIONAL_EXTERNAL)
                .map(f -> f.identity().id()).toList(), "only Tritanium has an optional-only thermal phase in scope");
        try {
            all().clear(); throw new AssertionError("Facts roster was mutable");
        } catch (UnsupportedOperationException expected) { }
    }
    private static void sourceFactoryAndDeclarationCases() {
        equal(Decision.YES, fact(280, "Nickel").melting(), "element metal MELTING");
        equal(Decision.NO, fact(280, "Nickel").furnace(), "element factory tests initial1728K >1200");
        equal(Decision.YES, fact(300, "Zinc").furnace(), "element factory initial692K permits FURNACE");
        equal(Decision.NO, fact(340, "Selenium").melting(), "native element default is not a source fact");
        equal(Decision.YES, fact(8500, "Stone").furnace(), "stone family FURNACE missing from current positive-only helper");
        equal(Decision.YES, fact(9175, "Graphene").blacklistedSmelter(), "explicit source blacklist");
        equal(Decision.YES, fact(8347, "CertusQuartz").blacklistedSmelter(), "quartz factory blacklist closure");
        Facts netherrack = fact(8502, "Netherrack");
        equal(Decision.YES, netherrack.melting(), "MT1882 late positive setter implies MELTING");
        equal(new Identity(8503, "NetherBrick"), netherrack.target(), "late setter target, not declaration self");
        equal(U, netherrack.targetUnitsPerU(), "late setter quantity");
        equal(Decision.YES, netherrack.blacklistedSmelter(), "late setter does not remove blacklist");
        equal(PhaseKind.NO_DEFAULT_PHASE, fact(210, "Scandium").phase().kind(),
                "transmetal/MELTING does not create a source molten phase");
        check(fact(210, "Scandium").portDefaults().generatedMolten(),
                "known port artifact remains separate from source NO_PHASE");
        equal(PhaseKind.NO_DEFAULT_PHASE, fact(9374, "Magicwood").phase().kind(),
                "Loader MT.Magic optional binding is not WOODS.Magic");
    }
    private static void copiedTargetsAndPortDefaults() {
        copied(8713, "GlowstoneRefined", 8341, "Glowstone", U, true);
        copied(8714, "ObsidianRefined", 9810, "Lava", U, true);
        copied(8652, "TungstenSintered", 740, "Tungsten", U, true);
        copied(9243, "ClayBrick", 8225, "Ceramic", U, false);
        copied(8222, "WoodTreated", 8200, "Ashes", U / 4, false);
        copied(8267, "WoodPolished", 8200, "Ashes", U / 4, false);
        long missing = all().stream().filter(f -> f.portDefaults() != null && f.melting() == Decision.YES
                && !f.portDefaults().melting()).count();
        long extra = all().stream().filter(f -> f.portDefaults() != null && f.melting() == Decision.NO
                && f.portDefaults().melting()).count();
        equal(74L, missing, "current missing MELTING does not hide source eligibility");
        equal(17L, extra, "source-negative native MELTING additions, including built-in postInit fuel stats");
        for (Identity coke : List.of(new Identity(8365, "LigniteCoke"), new Identity(8390, "PetroleumCoke"))) {
            Facts f = fact(coke.id(), coke.name());
            equal(Decision.NO, f.melting(), "source coke does not acquire the port fuel helper's MELTING");
            check(f.portDefaults().melting(), "built-in postInit coke flag is a reviewed startup default");
            check(!f.portDefaults().matches(false, coke.id(), coke.name(), U),
                    "a recipe-phase eligibility override is still rejected");
        }
        equal(6L, all().stream().filter(f -> f.portDefaults() != null
                && (!f.target().equals(f.portDefaults().target())
                    || f.targetUnitsPerU() != f.portDefaults().targetUnitsPerU())).count(),
                "six independently identified import defects, not external overrides");
        Facts alumina = fact(8008, "Alumina");
        equal(Decision.YES, alumina.melting(), "Alumina explicit source MELTING");
        check(!alumina.portDefaults().melting(), "source-positive missing native flag remains recognizable");
    }
    private static void copied(int id, String name, int targetId, String targetName, long units, boolean nativeMelting) {
        Facts f = fact(id, name);
        equal(new Identity(targetId, targetName), f.target(), "literal copied source conversion " + name);
        equal(units, f.targetUnitsPerU(), "literal copied source amount " + name);
        equal(new Identity(id, name), f.portDefaults().target(), "reviewed current self-target defect " + name);
        equal(U, f.portDefaults().targetUnitsPerU(), "reviewed current self amount " + name);
        check(f.portDefaults().matches(nativeMelting, id, name, U), "known import defaults accepted " + name);
        check(!f.portDefaults().matches(!nativeMelting, id, name, U), "MELTING override not blessed " + name);
        check(!f.portDefaults().matches(nativeMelting, id, name, U / 2), "amount override not blessed " + name);
        check(!f.portDefaults().matches(nativeMelting, id, "alias:" + name, U), "target alias override not blessed " + name);
    }
    private static void phaseUnitsAndOptionalBoundary() {
        phase(300, "Zinc", "molten.zinc", Accessor.LIQUID, 144, U);
        phase(8008, "Alumina", "molten.alumina", Accessor.LIQUID, 504, U);
        phase(8218, "Plastic", "plastic", Accessor.LIQUID, 1000, U);
        equal(2 * U / 3, fact(8218, "Plastic").targetUnitsPerU(), "Plastic conversion precedes fluid ratio");
        phase(8211, "Blaze", "blaze", Accessor.LIQUID, 1296, U);
        phase(9883, "ConstructionFoam", "ic2constructionfoam", Accessor.LIQUID, 100, U);
        phase(8246, "Aerotheum", "aerotheum", Accessor.GAS, 250, U);
        phase(8213, "Cryotheum", "cryotheum", Accessor.LIQUID, 250, U);
        phase(9810, "Lava", "lava", Accessor.LIQUID, 1000, 9 * U);
        phase(9800, "Water", "water", Accessor.LIQUID, 1000, U);
        phase(8235, "Wax", "molten.wax", Accessor.LIQUID, 144, U);
        phase(8333, "Redstone", "molten.redstone", Accessor.LIQUID, 144, U);
        Facts tritanium = fact(8752, "Tritanium");
        equal(PhaseKind.OPTIONAL_EXTERNAL, tritanium.phase().kind(), "optional molten_tritanium is not default NO or YES");
        check(tritanium.phase().hasOptionalExternalBinding(), "optional provenance retained");
        equal(null, tritanium.phase().binding(), "no invented optional ratio/native binding");
        check(fact(280, "Nickel").phase().hasOptionalExternalBinding(),
                "default positive does not erase possible external compatibility override");
    }
    private static void phase(int id, String name, String path, Accessor accessor, long mb, long reference) {
        Phase phase = fact(id, name).phase();
        equal(PhaseKind.DEFAULT_BINDING, phase.kind(), "proven default phase " + name);
        equal(path, phase.binding().sourceRegistryPath(), "source path not native path alias " + name);
        equal(accessor, phase.binding().accessor(), "correct source liquid/gas accessor " + name);
        equal(mb, phase.binding().millibuckets(), "source ratio numerator " + name);
        equal(reference, phase.binding().referenceMaterialUnits(), "source ratio denominator " + name);
    }
    private static void currentDefaultFingerprint() {
        GTMaterialRegistry.init();
        // Match the real recipe-phase lifecycle, not declaration-only defaults: built-in fuel
        // setup adds MELTING to two cokes without changing their source thermal eligibility.
        GTMaterialRegistry.postInit();
        FluidDefinitions.prepare();
        for (Facts f : all()) {
            var material = GTMaterialRegistry.get(f.identity().id()).resolve();
            check(f.identity().matches(material.getId(), material.getName()), "current exact identity " + f.identity());
            PortDefaults defaults = contextDefaults(f.identity().id(), f.identity().name());
            var target = material.getTargetSmeltingMaterial().resolve();
            check(defaults.matches(material.has(MaterialProperty.MELTING), target.getId(), target.getName(),
                    material.getTargetSmeltingAmount()), "shipped metadata profile " + f.identity());
            String field = "GenMolten_" + material.getName();
            equal(defaults.generatedMolten(), FluidCatalog.isGeneratedMoltenDefinition(field, FluidCatalog.get(field)),
                    "known port artifact evidence is independent of source phase " + f.identity());
        }
    }
}
