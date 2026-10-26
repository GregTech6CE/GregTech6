package gregtech.content.recipe;
import java.util.List;
/** Seven pinned C input roles only. Missing/unreviewed holders defer this producer, never become dust9.
 * Facts are supplied by the native adapter; this pure policy does not scan tags or register forms. */
public final class SourceCentrifugeBlockDustInputs {
    private SourceCentrifugeBlockDustInputs() {}
    public record Binding(String sourceField,String materialName,int materialId) {}
    public enum Decision { PRESENT, UNRESOLVED, UNREVIEWED }
    public record Facts(boolean holderPresent,boolean exactBlockDust,boolean exactRegistry,
                        boolean plainAmountOne,boolean coreOwned,String materialName,int materialId) {}
    private static final List<Binding> BINDINGS=List.of(
        new Binding("Bedrock","Bedrock",8599),
        new Binding("Netherrack","Netherrack",8502),
        new Binding("Endstone","Endstone",8504),
        new Binding("EndSandWhite","WhiteEndSand",8105),
        new Binding("EndSandBlack","BlackEndSand",8106),
        new Binding("SpaceRock","SpaceStone",8512),
        new Binding("MoonTurf","MoonTurf",8514));
    public static List<Binding> bindings(){return BINDINGS;}
    public static Binding binding(String sourceField){
        for(var b:BINDINGS)if(b.sourceField().equals(sourceField))return b;
        return null;
    }
    public static Decision decide(Binding b,Facts f){
        if(b==null||!BINDINGS.contains(b)||f==null||!f.holderPresent())return Decision.UNRESOLVED;
        return f.exactBlockDust()&&f.exactRegistry()&&f.plainAmountOne()&&f.coreOwned()
                &&b.materialId()==f.materialId()&&b.materialName().equals(f.materialName())
                ?Decision.PRESENT:Decision.UNREVIEWED;
    }
}
