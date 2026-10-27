package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMapDefinitions;
import java.util.List;
import java.util.Objects;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreCrushingRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** Independent source literals and evidence boundaries; native registration/matching is tested separately. */
public final class SourceOreCrushingContracts {
    private static final Identity STONE = new Identity(8500,"Stone"), OTHER = new Identity(50,"Boron");
    private static final Bonus BONUS = new Bonus(Decision.YES,false,false,true);
    private static final Bonus NONE = new Bonus(Decision.NO,false,false,false);
    private static final Resolver ALL = (f,m,n) -> Availability.PRESENT;
    private static Input input(long amount,int ore,int quality,Bonus bonus) {
        return new Input(CINNABAR,CINNABAR,amount,ore,1,quality,bonus,Hexorium.NONE);
    }
    private static Gangue stone(long amount,int quality,long targetAmount) {
        return new Gangue(STONE,amount,quality,STONE,targetAmount);
    }
    private static Result row(Machine machine,Kind kind,Input in,List<Gangue> host,int cap,Resolver forms) {
        return plan(new Request(machine,kind,in,false,host,cap),forms);
    }
    private static Result row(Machine machine,Kind kind,Input in,List<Gangue> host) {
        return row(machine,kind,in,host,machine==Machine.CRUSHER?12:1,ALL);
    }
    public static void main(String[] args) {
        run();
        System.out.println("Source ore crushing contracts passed (no native execution).");
    }
    public static void run() {
        eq(12,MachineRecipeMapDefinitions.Crusher.mOutputItemsCount,"native Crusher capacity");
        eq(1,MachineRecipeMapDefinitions.Hammer.mOutputItemsCount,"native Hammer capacity");
        var input=input(U,1,0,BONUS);
        var crusher=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(4*U,1,U)));
        eq(Status.ROW,crusher.status(),"default stone row");eq(640L,crusher.ticks(),"Cinnabar stone Crusher golden");
        eq(new Slot(new Stack(Form.CRUSHED,CINNABAR,1),10000),crusher.slots().get(0),"primary one");
        eq(crusher.slots().get(0),crusher.slots().get(1),"primary two independent slot");
        eq(new Slot(new Stack(Form.GEM,CINNABAR,1),3000),crusher.slots().get(2),"literal30%bonus");
        eq(new Slot(new Stack(Form.DUST,STONE,4),10000),crusher.slots().get(3),"4Uhost, not target byproducts");
        eq(12,crusher.slots().size(),"retain raw source capacity/chance positions");
        var hammer=row(Machine.HAMMER,Kind.STANDARD,input,List.of(stone(4*U,1,U)),1,(f,m,n)->{
            if(m.equals(STONE))throw new AssertionError("full Hammer must not resolve host output");return Availability.PRESENT;
        });
        eq(544L,hammer.ticks(),"Hammer pays host even though capacity1");eq(1,hammer.slots().size(),"no fake host output");
        var raw=row(Machine.CRUSHER,Kind.RAW_ITEM,input,List.of());eq(128L,raw.ticks(),"raw Cinnabar unchanged128ticks");
        eq(3000L,raw.slots().get(2).chance(),"raw30%unchanged");
        var dense=row(Machine.CRUSHER,Kind.DENSE,input,List.of());eq(256L,dense.ticks(),"dense primary time doubled");
        for(int i=0;i<4;i++)eq(1,dense.slots().get(i).stack().count(),"dense FOUR base-count slots");
        eq(2,dense.slots().get(4).stack().count(),"dense bonuscount2");
        var block=row(Machine.CRUSHER,Kind.RAW_BLOCK,input,List.of());eq(1152L,block.ticks(),"rawblock count2 then9/2time");
        for(int i=0;i<9;i++)eq(2,block.slots().get(i).stack().count(),"rawblock NINE primaryslots");
        eq(9,block.slots().get(9).stack().count(),"rawblock bonuscount9");
        var capped=row(Machine.CRUSHER,Kind.RAW_BLOCK,input(U,40,0,NONE),List.of());
        eq(64,capped.slots().get(0).stack().count(),"cap BEFORE duplication");eq(36864L,capped.ticks(),"cap affects actualstack time");
        var uncapped=row(Machine.HAMMER,Kind.RAW_BLOCK,input(U,40,0,NONE),List.of());
        eq(360,uncapped.slots().get(0).stack().count(),"Hammer not64capped");eq(11520L,uncapped.ticks(),"Hammer uses actual count");
        eq(3,row(Machine.HAMMER,Kind.DENSE,input(U/2,3,0,NONE),List.of()).slots().get(0).stack().count(),"multiplydense BEFOREfloor");
        eq(1,row(Machine.CRUSHER,Kind.DENSE,input(U/2,3,0,NONE),List.of()).slots().get(0).stack().count(),"Crusherfloorbase THENfour slots");
        eq(3,row(Machine.CRUSHER,Kind.RAW_BLOCK,input(U/2,3,0,NONE),List.of()).slots().get(0).stack().count(),"rawblockmultiply2 BEFOREfloor");
        eq(1,row(Machine.CRUSHER,Kind.RAW_ITEM,input(0,1,0,NONE),List.of()).slots().get(0).stack().count(),"Crusher zero clamps1");
        eq(Status.UNSUPPORTED,row(Machine.HAMMER,Kind.RAW_ITEM,input(0,1,0,NONE),List.of()).status(),"zero Hammer stack is unrepresentable, not output1");
        var nullBonus=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(U,0,U)),12,(f,m,n)->f==Form.GEM?Availability.ABSENT:Availability.PRESENT);
        eq(new Slot(null,3000),nullBonus.slots().get(2),"missingbonus preservesempty slot/chance");
        eq(STONE,nullBonus.slots().get(3).stack().material(),"gangue cannot move into missingbonus slot");
        var missingHost=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(U,0,U),new Gangue(OTHER,U,0,OTHER,U)),12,
                (f,m,n)->m.equals(STONE)?Availability.ABSENT:Availability.PRESENT);
        eq(256L,missingHost.ticks(),"both hostcostscharged");eq(OTHER,missingHost.slots().get(3).stack().material(),"missinghostdoesnotconsume slot");
        var full=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(4*U,1,U)),3,(f,m,n)->{
            if(m.equals(STONE))throw new AssertionError("full map must not resolve host output");return Availability.PRESENT;
        });eq(640L,full.ticks(),"fullCrusher stillpays512hostticks");
        var halfHost=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(4*U,1,U/2)));
        eq(640L,halfHost.ticks(),"hostquality/amount drivescost, not targetamount");eq(2,halfHost.slots().get(3).stack().count(),"hosttargetamount scalesonlyoutput");
        var smallFallback=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(4*U,0,U)),12,
                (f,m,n)->m.equals(STONE)&&f==Form.DUST?Availability.ABSENT:Availability.PRESENT);
        eq(new Stack(Form.DUST_SMALL,STONE,16),smallFallback.slots().get(3).stack(),"OM dustavailable fallback, not onepileoracle");
        var blockDust=row(Machine.CRUSHER,Kind.STANDARD,input,List.of(stone(72*U,0,U)));
        eq(new Stack(Form.BLOCK_DUST,STONE,8),blockDust.slots().get(3).stack(),"physical blockDust first at72U");
        var onlyGem=(Resolver)(f,m,n)->f==Form.GEM?Availability.PRESENT:Availability.ABSENT;
        eq(Status.ROW,row(Machine.CRUSHER,Kind.RAW_ITEM,input,List.of(),12,onlyGem).status(),"Crushergemfallback");
        eq(Status.NO_ROW,row(Machine.HAMMER,Kind.RAW_ITEM,input,List.of(),1,onlyGem).status(),"HammerNOgemfallback");
        eq(Status.UNSUPPORTED,row(Machine.CRUSHER,Kind.RAW_ITEM,input,List.of(),12,(f,m,n)->f==Form.CRUSHED?Availability.UNKNOWN:Availability.PRESENT).status(),"unknownpreferredformcannotfallthrough");
        var converted=new Input(OTHER,CINNABAR,U,1,1,2,NONE,Hexorium.NONE);
        var convertedRow=row(Machine.CRUSHER,Kind.RAW_ITEM,converted,List.of());
        eq(384L,convertedRow.ticks(),"inputquality2 drivescost");eq(CINNABAR,convertedRow.slots().get(0).stack().material(),"crushingtargetonlycontrolsoutputidentity");
        eq(null,convertedRow.slots().get(2).stack(),"targetCinnabar doesnotcreateinputbonus");
        var hex=new Input(new Identity(9224,"HexoriumBlack"),OTHER,U,3,1,0,NONE,Hexorium.BLACK_WHITE);
        var hx=plan(new Request(Machine.CRUSHER,Kind.STANDARD,hex,true,List.of(),12),ALL);
        eq(4,hx.slots().get(0).stack().count(),"specialhostBlackWhite sourceadjustment");
        eq(3,plan(new Request(Machine.HAMMER,Kind.STANDARD,hex,true,List.of(),1),ALL).slots().get(0).stack().count(),"HammerdoesnotapplyHexoriumadjustment");
        eq(4500L,bonusChance(new Bonus(Decision.YES,true,true,true)),"fourindependentcontainsconditions");
        eq(48L,row(Machine.CRUSHER,Kind.POOR,input,List.of(stone(4*U,1,U))).ticks(),"poorcrush3tiny16tickseach,nohost");
        eq(32L,row(Machine.HAMMER,Kind.POOR,input,List.of(stone(4*U,1,U))).ticks(),"poorhammer2tiny,nohost");
        eq(Status.NO_ROW,row(Machine.CRUSHER,Kind.SMALL_RICH_NORMAL,input,List.of()).status(),"literalTODO no row");
        eq(false,eligible(Machine.CRUSHER,true,false,true,false,false,false,true),"PFAAcrusherexcluded");
        eq(true,eligible(Machine.HAMMER,true,false,true,false,false,false,true),"PFAAhammerhasnoexplicitexclusion");
        eq(false,eligible(Machine.HAMMER,true,false,true,true,false,false,false),"bedrocksourceNO");
        eq(false,eligible(Machine.CRUSHER,true,true,true,false,false,false,false),"antiNO");
        eq(false,crusher.optimize(),"sourcefalseoptimize");eq(16,crusher.power(),"source16power");
        sourceFactBoundaries();
        unsupportedArithmetic();
    }
    private static void sourceFactBoundaries() {
        long positives = 0, negatives = 0;
        for (var fact : SourceRecyclingFacts.all()) {
            var id = fact.identity();
            Decision decision = SourceOreCrushingFacts.pulverizingCinnabar(id.id(), id.name());
            if (fact.portDefaults() == null) eq(Decision.UNKNOWN, decision, "target-only identity remains outside input roster");
            else {
                if (decision == Decision.YES) positives++;
                else if (decision == Decision.NO) negatives++;
                else throw new AssertionError("Missing reviewed input " + id);
                eq(Hexorium.NONE, SourceOreCrushingFacts.hexorium(id.id(), id.name()), "ordinary reviewed identity");
            }
            eq(Decision.UNKNOWN, SourceOreCrushingFacts.pulverizingCinnabar(id.id(), "not:" + id.name()), "canonical identity required");
        }
        eq(3L, positives, "three audited positive declarations, not native row count");
        eq(999L, negatives, "closed source input negatives, not omitted positive guesses");
        eq(Decision.YES, SourceOreCrushingFacts.pulverizingCinnabar(800, "Mercury"), "MT470");
        eq(Decision.YES, SourceOreCrushingFacts.pulverizingCinnabar(8333, "Redstone"), "MT1540");
        eq(Decision.YES, SourceOreCrushingFacts.pulverizingCinnabar(9114, "Cinnabar"), "MT3709");
        eq(Decision.UNKNOWN, SourceOreCrushingFacts.pulverizingCinnabar(9516, "Superconductor"), "port-only material unsupported");
        eq(Decision.UNKNOWN, SourceOreCrushingFacts.pulverizingCinnabar(10800, "Mercury"), "addon ID not source proof");
        eq(Decision.UNKNOWN, SourceOreCrushingFacts.pulverizingCinnabar(800, "Hg"), "symbol not canonical name");
        eq(null, SourceOreCrushingFacts.hexorium(9224, "HexoriumRed"), "mismatched Hexorium identity");
        for (var hex : List.of(new Identity(9224, "HexoriumBlack"), new Identity(9239, "HexoriumWhite"))) {
            eq(Hexorium.BLACK_WHITE, SourceOreCrushingFacts.hexorium(hex.id(), hex.name()), "five explicit special identities");
            eq(Decision.NO, SourceOreCrushingFacts.pulverizingCinnabar(hex.id(), hex.name()), "Hexorium factory has no Cinnabar tag");
        }
        for (var hex : List.of(new Identity(9225, "HexoriumRed"), new Identity(9226, "HexoriumGreen"), new Identity(9228, "HexoriumBlue")))
            eq(Hexorium.RED_GREEN_BLUE, SourceOreCrushingFacts.hexorium(hex.id(), hex.name()), "colored adjustment");
        eq(Decision.UNKNOWN, SourceOreCrushingFacts.pulverizingCinnabar(9227, "HexoriumBrown"), "unreviewed extra Hexorium not inferred");
        for (String host : List.of("stone", "granite", "stone_granite", "diorite", "stone_diorite", "andesite",
                "stone_andesite", "deepslate", "netherrack", "end_stone", "stone_granite_black", "stone_granite_red",
                "stone_komatiite", "stone_basalt", "stone_marble", "stone_limestone", "stone_shale", "stone_slate",
                "stone_greenschist", "stone_blueschist", "stone_gneiss", "stone_prismarine_light", "stone_prismarine_dark",
                "stone_kimberlite", "stone_quartzite")) {
            var profile = SourceOreCrushingFacts.hostProfile(host);
            eq(Decision.YES, SourceOreCrushingFacts.hostDecision(host), "finite host source profile");
            eq(4 * U, profile.gangueAmount(), "each reviewed standard prefix has one 4U gangue");
            var identity = profile.gangueMaterial();
            if (SourceRecyclingFacts.find(identity.id(), identity.name()) == null) throw new AssertionError("Unclosed gangue " + identity);
            eq(!host.equals("deepslate") && !host.equals("stone_gneiss"),
                    SourceOreCrushingFacts.hasBrokenSourcePair(host), "separate finite paired-source proof");
        }
        eq(new Identity(9219, "Prismarine"), SourceOreCrushingFacts.hostProfile("stone_prismarine_light").gangueMaterial(),
                "source PrismarineLight symbol resolves canonical Prismarine, not guessed name");
        eq(SourceOreCrushingFacts.hostProfile("granite"), SourceOreCrushingFacts.hostProfile("stone_granite"), "explicit host aliases");
        eq(true, SourceOreCrushingFacts.hostProfile("deepslate").hexoriumAdjusted(), "exact adjusted prefix");
        eq(false, SourceOreCrushingFacts.hostProfile("stone").hexoriumAdjusted(), "ordinary stone not adjusted");
        for (String host : List.of("bedrock", "sand", "red_sand", "gravel")) {
            eq(Decision.NO, SourceOreCrushingFacts.hostDecision(host), "known source exclusion");
            eq(null, SourceOreCrushingFacts.hostProfile(host), "not ordinary gangue profile");
        }
        for (String host : List.of("tuff", "stone_chalk", "stone_dolomite", "stone_gabbro", "stone_gypsum", "stone_oilshale",
                "stone_rhyolite", "stone_salt", "stone_sylvite", "stone_talc", "unknown")) {
            eq(Decision.UNKNOWN, SourceOreCrushingFacts.hostDecision(host), "unproved host is not stone/empty gangue");
            eq(false, SourceOreCrushingFacts.hasBrokenSourcePair(host), "no inferred mode support");
        }
        eq(Decision.UNKNOWN, SourceOreCrushingFacts.hostDecision(null), "missing host context");
    }
    private static void unsupportedArithmetic() {
        eq(Status.UNSUPPORTED, row(Machine.HAMMER, Kind.RAW_ITEM, input(0,1,0,NONE), List.of(), 1,
                (f,m,n) -> { throw new AssertionError("Zero Hammer must defer before native zero-stack lookup"); }).status(), "unrepresentable zero");
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_ITEM, input(U,1,Integer.MAX_VALUE,NONE), List.of()).status(), "extreme quality");
        eq(Status.UNSUPPORTED, row(Machine.HAMMER, Kind.RAW_ITEM, input(Long.MAX_VALUE,127,0,NONE), List.of()).status(), "Hammer int count overflow");
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_ITEM, input(Long.MAX_VALUE,127,0,NONE), List.of()).status(), "original UT intermediate overflow");
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_BLOCK, input(U,1,1000000,NONE), List.of()).status(), "raw-block int overflow before divide");
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_ITEM, input(-1,1,0,NONE), List.of()).status(), "negative quantity");
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_ITEM, input(U,1,0,new Bonus(Decision.UNKNOWN,false,false,false)), List.of()).status(), "unknown tag");
        var missingContext = new Input(OTHER, CINNABAR, U, 1, 1, 0, NONE, null);
        eq(Status.UNSUPPORTED, row(Machine.CRUSHER, Kind.RAW_ITEM, missingContext, List.of()).status(), "unknown Hexorium identity");
        eq(Status.UNSUPPORTED, plan(null, ALL).status(), "missing request");
    }
    private static void eq(Object expected,Object actual,String message) {
        if(!Objects.equals(expected,actual))throw new AssertionError(message+" expected="+expected+" actual="+actual);
    }
}
