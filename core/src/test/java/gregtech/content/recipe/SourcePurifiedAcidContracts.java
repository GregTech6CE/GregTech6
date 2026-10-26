package gregtech.content.recipe;
import gregtech.content.data.MaterialPrefix;
import java.util.*;
import static gregtech.content.recipe.SourcePurifiedAcidData.*;
import static gregtech.content.recipe.SourcePurifiedAcidRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

/** Independent literal representatives from pinned GT6 Ores 108-159, not planner-derived
 * expected values. Planned candidate counts are not native holder/admission counts. */
public final class SourcePurifiedAcidContracts {
    interface ItemLookup { Availability find(MaterialPrefix p,Identity m,int count); }
    interface FluidLookup { Availability find(Phase p,long mb); }
    static Resolver resolver(ItemLookup items,FluidLookup fluids) {
        return new Resolver() {
            public Availability item(MaterialPrefix p,Identity m,int n) { return items.find(p,m,n); }
            public Availability fluid(Phase p,long n) { return fluids.find(p,n); }
        };
    }
    static final Resolver ALL=resolver((p,m,n)->Availability.PRESENT,(p,n)->Availability.PRESENT);
    static Identity id(int n,String s) { return new Identity(n,s); }
    static Item item(MaterialPrefix p,int id,String name,int count) { return new Item(p,id(id,name),count); }
    static Slot slot(MaterialPrefix p,int id,String name,int count,long chance) { return new Slot(item(p,id,name,count),chance); }
    static String fluid(Fluid f) { return f.phase().nativeField()+":"+f.millibuckets(); }
    static void golden(int line,Identity material,List<String> inputFluids,List<String> outputFluids,List<Slot> outputItems) {
        for(var shape:Shape.values()) {
            var result=line==156?manganesePlan(material,shape,Decision.YES,ALL):fixedPlan(family(line),material,shape,ALL);
            eq(Status.ROW,result.status(),"golden row "+line+"/"+shape);
            eq(shape==Shape.WHOLE?MaterialPrefix.crushedPurified:MaterialPrefix.crushedPurifiedTiny,result.input().prefix(),"input prefix");
            eq(shape==Shape.WHOLE?1:9,result.input().count(),"input amount1/9");
            eq(material,result.input().material(),"input identity");
            eq(inputFluids,result.fluidInputs().stream().map(SourcePurifiedAcidContracts::fluid).toList(),"input phase+amount");
            eq(outputFluids,result.fluidOutputs().stream().map(SourcePurifiedAcidContracts::fluid).toList(),"ordered output phase+amount");
            eq(outputItems,result.outputs(),"independent solid slots+chances");
            eq(256,result.ticks(),"source ticks");eq(0,result.power(),"source power");eq(true,result.optimize(),"source optimize");
        }
    }
    static void goldens() {
        golden(108,id(120,"Magnesium"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_PinkVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 120, "Magnesium", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 120, "Magnesium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 120, "Magnesium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 120, "Magnesium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 120, "Magnesium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 120, "Magnesium", 2, 5000)));
        golden(112,id(280,"Nickel"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_CyanVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 280, "Nickel", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 280, "Nickel", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 280, "Nickel", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 280, "Nickel", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 280, "Nickel", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 280, "Nickel", 2, 5000)));
        golden(116,id(250,"Manganese"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_GrayVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 250, "Manganese", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 250, "Manganese", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 250, "Manganese", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 250, "Manganese", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 250, "Manganese", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 250, "Manganese", 2, 5000)));
        golden(120,id(8750,"Desh"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_MartianVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 8750, "Desh", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 8750, "Desh", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8750, "Desh", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8750, "Desh", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8750, "Desh", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8750, "Desh", 2, 5000)));
        golden(124,id(9104,"Hematite"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_GreenVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 9104, "Hematite", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 9104, "Hematite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9104, "Hematite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9104, "Hematite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9104, "Hematite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9104, "Hematite", 2, 5000)));
        golden(128,id(290,"Copper"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_BlueVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 290, "Copper", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 290, "Copper", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 290, "Copper", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 290, "Copper", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 290, "Copper", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 290, "Copper", 2, 5000)));
        golden(132,id(270,"Cobalt"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_RedVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 270, "Cobalt", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 270, "Cobalt", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 270, "Cobalt", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 270, "Cobalt", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 270, "Cobalt", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 270, "Cobalt", 2, 5000)));
        golden(136,id(300,"Zinc"),List.of("GenLiquid_SulfuricAcid:3500"),List.of("GenLiquid_WhiteVitriol:3000", "Hydrogen:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 300, "Zinc", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 300, "Zinc", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 300, "Zinc", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 300, "Zinc", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 300, "Zinc", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 300, "Zinc", 2, 5000)));
        golden(140,id(9101,"Almandine"),List.of("GenLiquid_SulfuricAcid:10500"),List.of("GenLiquid_VitriolOfClay:8500", "Hydrogen:3000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 9101, "Almandine", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 9101, "Almandine", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9101, "Almandine", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9101, "Almandine", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9101, "Almandine", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9101, "Almandine", 2, 5000)));
        golden(144,id(440,"Ruthenium"),List.of("GenLiquid_AquaRegia:9750"),List.of("GenLiquid_ChloroplatinicAcid:4500", "GenGas_NitrogenMonoxide:1500", "Water:4125"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 440, "Ruthenium", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 8103, "PlatinumGroupSludge", 8, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8103, "PlatinumGroupSludge", 8, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8103, "PlatinumGroupSludge", 8, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8103, "PlatinumGroupSludge", 8, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 8103, "PlatinumGroupSludge", 8, 5000)));
        golden(148,id(230,"Vanadium"),List.of("GenGas_HydrogenFluoride:500"),List.of(),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 230, "Vanadium", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 230, "Vanadium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 230, "Vanadium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 230, "Vanadium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 230, "Vanadium", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 230, "Vanadium", 2, 5000)));
        golden(152,id(8334,"Coal"),List.of("GenGas_HydrogenFluoride:500"),List.of(),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 8334, "Coal", 1, 10000), slot(MaterialPrefix.crushedCentrifugedTiny, 9174, "Graphite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9174, "Graphite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9174, "Graphite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9174, "Graphite", 2, 5000), slot(MaterialPrefix.crushedCentrifugedTiny, 9174, "Graphite", 2, 5000)));
        golden(156,id(9164,"Wollastonite"),List.of("GenGas_HydrochloricAcid:4000"),List.of("Water:3000", "GenGas_Chlorine:1000"),
                List.of(slot(MaterialPrefix.crushedCentrifuged, 9164, "Wollastonite", 1, 10000), slot(MaterialPrefix.dustSmall, 8031, "ManganeseChloride", 6, 10000)));
    }
    static void countsAndOrder() {
        eq(6,gregtech.content.data.MachineRecipeMapDefinitions.Bath.mOutputItemsCount,"Bath solid capacity");
        eq(3,gregtech.content.data.MachineRecipeMapDefinitions.Bath.mOutputFluidCount,"Bath fluid capacity");
        eq(12,families().size(),"12 fixed families");
        eq(78,families().stream().mapToInt(f->f.members().size()).sum(),"78 memberships");
        eq(74L,families().stream().flatMap(f->f.members().stream()).distinct().count(),"74 fixed identities");
        eq(156,fixedCandidates().size(),"156 source calls, not native accepted count");
        eq(12,manganeseDefaults().size(),"12 positive default fingerprints");
        eq(76L,java.util.stream.Stream.concat(families().stream().flatMap(f->f.members().stream()),manganeseDefaults().stream().map(ManganeseDefault::material)).distinct().count(),"76 union input identities");
        var fixed=fixedCandidates();
        for(int i=0;i<fixed.size();i+=2) {
            var whole=fixed.get(i);var tiny=fixed.get(i+1);
            eq(Shape.WHOLE,whole.shape(),"whole before tiny");eq(Shape.TINY,tiny.shape(),"tiny second");
            eq(whole.family(),tiny.family(),"same family");eq(whole.material(),tiny.material(),"same material");
            eq(Status.ROW,fixedPlan(whole.family(),whole.material(),whole.shape(),ALL).status(),"all fixed source calls plan");
            eq(Status.ROW,fixedPlan(tiny.family(),tiny.material(),tiny.shape(),ALL).status(),"all tiny source calls plan");
            if(i>0 && fixed.get(i-2).family()!=whole.family())eq(true,fixed.get(i-2).family().sourceLine()<whole.family().sourceLine(),"source family order");
        }
        for(int i=0;i<manganeseDefaults().size();i++) {
            var d=manganeseDefaults().get(i);
            if(i>0)eq(true,manganeseDefaults().get(i-1).material().id()<d.material().id(),"MATERIAL_ARRAY ID ordering");
            eq(true,d.byproducts().contains(MNO2),"positive direct selector");
            eq(true,matchesManganeseDefault(d.material().id(),d.material().name(),d.byproducts()),"exact default fingerprint");
            for(var shape:Shape.values())eq(Status.ROW,manganesePlan(d.material(),shape,Decision.YES,ALL).status(),"24 dynamic default calls");
        }
        for(int materialId:new int[]{280,780,8649,9116}) {
            long count=families().stream().filter(f->f.members().stream().anyMatch(m->m.id()==materialId)).count();
            // Source repeats Ni/Pt/MeteoricIron/Cooperite in different acid families.
            eq(2L,count,"overlapping fixed member "+materialId);
        }
        eq(18,Phase.values().length,"18 phase bindings");
        for(var phase:Phase.values()) { eq(1000L,phase.millibucketsPerUnit(),"not molten144");eq(gregtech.api.material.GTValues.U,phase.materialUnit(),"one material unit"); }
        eq(State.GAS,Phase.HYDROGENFLUORIDE.state(),"HF gas");eq(State.GAS,Phase.HYDROCHLORICACID.state(),"HCl gas");
        eq(State.LIQUID,Phase.SULFURICACID.state(),"ambient H2SO4 liquid");eq("minecraft:water",Phase.WATER.registryId(),"vanilla water binding");
    }
    static void boundaries() {
        Identity mg=id(120,"Magnesium"),coal=id(8334,"Coal"),wollastonite=id(9164,"Wollastonite");
        var onlyTiny=resolver((p,m,n)->p==MaterialPrefix.crushedPurified?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(Status.NO_ROW,fixedPlan(family(108),mg,Shape.WHOLE,onlyTiny).status(),"absent whole input only skips whole");
        eq(Status.ROW,fixedPlan(family(108),mg,Shape.TINY,onlyTiny).status(),"tiny independent of missing whole");
        var onlyWhole=resolver((p,m,n)->p==MaterialPrefix.crushedPurifiedTiny?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(Status.ROW,fixedPlan(family(108),mg,Shape.WHOLE,onlyWhole).status(),"whole independent of missing tiny");
        eq(Status.NO_ROW,fixedPlan(family(108),mg,Shape.TINY,onlyWhole).status(),"absent tiny only skips tiny");
        var missingMain=resolver((p,m,n)->p==MaterialPrefix.crushedCentrifuged?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        var nullMain=fixedPlan(family(108),mg,Shape.WHOLE,missingMain);
        eq(Status.ROW,nullMain.status(),"missing main is not invented gate");eq(null,nullMain.outputs().get(0).item(),"main empty slot remains");eq(5000L,nullMain.outputs().get(1).chance(),"first tail does not acquire main chance");
        var noSolids=resolver((p,m,n)->p==MaterialPrefix.crushedPurified||p==MaterialPrefix.crushedPurifiedTiny?Availability.PRESENT:Availability.ABSENT,(p,n)->Availability.PRESENT);
        eq(Status.ROW,fixedPlan(family(108),mg,Shape.WHOLE,noSolids).status(),"vitriol fluid-only result valid");
        eq(Status.NO_ROW,fixedPlan(family(152),coal,Shape.WHOLE,noSolids).status(),"HF row without any output rejected");
        var unknownMain=resolver((p,m,n)->p==MaterialPrefix.crushedCentrifuged?Availability.UNKNOWN:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(Status.UNSUPPORTED,fixedPlan(family(108),mg,Shape.WHOLE,unknownMain).status(),"unknown main does not fall back to dust");
        var unknownAcid=resolver((p,m,n)->Availability.PRESENT,(p,n)->p==Phase.SULFURICACID?Availability.UNKNOWN:Availability.PRESENT);
        eq(Status.UNSUPPORTED,fixedPlan(family(108),mg,Shape.WHOLE,unknownAcid).status(),"unproven input phase defers");
        var noAcid=resolver((p,m,n)->Availability.PRESENT,(p,n)->p==Phase.SULFURICACID?Availability.ABSENT:Availability.PRESENT);
        eq(Status.NO_ROW,fixedPlan(family(108),mg,Shape.WHOLE,noAcid).status(),"known missing acid rejects row");
        var unknownOutputFluid=resolver((p,m,n)->Availability.PRESENT,(p,n)->p==Phase.HYDROGEN?Availability.UNKNOWN:Availability.PRESENT);
        eq(Status.UNSUPPORTED,fixedPlan(family(108),mg,Shape.WHOLE,unknownOutputFluid).status(),"unproven output phase defers");
        var noHydrogen=resolver((p,m,n)->Availability.PRESENT,(p,n)->p==Phase.HYDROGEN?Availability.ABSENT:Availability.PRESENT);
        eq(1,fixedPlan(family(108),mg,Shape.WHOLE,noHydrogen).fluidOutputs().size(),"known absent output fluid removed");
        eq(Decision.NO,fixedMembership(family(108),coal),"reviewed but not member is NO for fixed family");
        eq(Decision.UNKNOWN,fixedMembership(family(108),id(120,"Mg")),"alias cannot prove membership");
        eq(Decision.UNKNOWN,fixedMembership(family(108),id(9516,"Superconductor")),"unreviewed input remains unknown");
        eq(Decision.YES,directManganeseMembership(List.of(mg,MNO2,coal)),"MnO2 may occur after first byproduct");
        eq(Decision.YES,directManganeseMembership(Arrays.asList(null,MNO2)),"proven positive survives an unrelated unresolved byproduct");
        eq(Decision.NO,directManganeseMembership(List.of(id(9210,"Bromargyrite"),id(9104,"Hematite"),id(9113,"Chromite"))),"Pyrolusite actual direct list excludes itself");
        eq(Decision.NO,directManganeseMembership(List.of(coal)),"no composition/nested expansion");
        eq(Decision.YES,manganeseSelector(true,false,directManganeseMembership(List.of(MNO2))),"proven core live membership is not restricted to default12 fingerprints");
        eq(Status.ROW,manganesePlan(id(50,"Boron"),Shape.WHOLE,Decision.YES,ALL).status(),"caller-proven new core positive outside default12 is not blocked");
        eq(Decision.UNKNOWN,directManganeseMembership(null),"unknown direct list not NO");
        eq(Decision.UNKNOWN,directManganeseMembership(Arrays.asList((Identity)null)),"unresolved byproduct not NO");
        eq(Decision.NO,manganeseSelector(true,true,Decision.YES),"dynamic antimatter excluded");
        eq(Decision.NO,manganeseSelector(false,false,Decision.YES),"dynamic null material excluded");
        eq(Decision.UNKNOWN,manganeseSelector(true,false,Decision.UNKNOWN),"unknown byproduct context deferred");
        eq(Decision.NO,manganeseSelector(true,false,Decision.NO),"explicit no direct MnO2 is NO");
        eq(false,matchesManganeseDefault(9126,"Pyrolusite",List.of(MNO2)),"self MnO2 not a positive default");
        eq(false,matchesManganeseDefault(9164,"Wollastonite",List.of(MNO2)),"changed list differs from default diagnostic only");
        var d=manganeseDefault(9164,"Wollastonite");var reversed=new ArrayList<>(d.byproducts());Collections.reverse(reversed);
        eq(false,matchesManganeseDefault(9164,"Wollastonite",reversed),"default diagnostic observes ordering; selector still uses contains");
        var noSmall=resolver((p,m,n)->p==MaterialPrefix.dustSmall?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        var tiny=manganesePlan(wollastonite,Shape.TINY,Decision.YES,noSmall);
        eq(new Item(MaterialPrefix.dustTiny,MNCL2,13),tiny.outputs().get(1).item(),"OM1.5U small absent -> tiny13 floor");
        var only72=resolver((p,m,n)->p==MaterialPrefix.dustSmall||p==MaterialPrefix.dustTiny?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(new Item(MaterialPrefix.dustDiv72,MNCL2,64),manganesePlan(wollastonite,Shape.WHOLE,Decision.YES,only72).outputs().get(1).item(),"OM final108div72 clamps64");
        var unknownSmall=resolver((p,m,n)->p==MaterialPrefix.dustSmall?Availability.UNKNOWN:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(Status.UNSUPPORTED,manganesePlan(wollastonite,Shape.WHOLE,Decision.YES,unknownSmall).status(),"unknown preferred dust cannot fallthrough");
        var noMnDust=resolver((p,m,n)->m.equals(MNCL2)?Availability.ABSENT:Availability.PRESENT,(p,n)->Availability.PRESENT);
        eq(null,manganesePlan(wollastonite,Shape.WHOLE,Decision.YES,noMnDust).outputs().get(1).item(),"absent MnCl2 preserves empty second slot");
        eq(Status.NO_ROW,manganesePlan(wollastonite,Shape.WHOLE,Decision.NO,ALL).status(),"dynamic proven no selector");
        eq(Status.UNSUPPORTED,manganesePlan(wollastonite,Shape.WHOLE,Decision.UNKNOWN,ALL).status(),"dynamic unknown selector");
    }
    static void eq(Object e,Object a,String text) { if(!Objects.equals(e,a))throw new AssertionError(text+": expected="+e+" actual="+a); }
    public static void run() { goldens(); countsAndOrder(); boundaries(); }
    public static void main(String[] args) {
        run();
        System.out.println("Purified acid core contracts passed (no native admission coverage).");
    }
}
