package gregtech.content.recipe;
import gregtech.engine.tool.GTToolType;
import gregtech.content.tool.ManualToolRecipeCatalog;
import gregtech.content.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
/** Stack/item boundary for the shared, complete original shaped tool and head recipes. */
public final class GTToolRecipes {
    private GTToolRecipes(){}
    public record Gate(Map<Character,Item> items,@Nullable String toolMaterial,@Nullable String onlyMaterial,boolean onlyStone,boolean skipHeadGate){
        public static final Gate NONE=new Gate(Map.of(),null,null,false,false);
    }
    public record Pattern(String[] rows,Map<Character,MaterialPrefix> forms,Map<Character,GTToolType> tools,boolean mirror,boolean normalHandle,Gate gate){
        public Pattern(String[] rows,Map<Character,MaterialPrefix> forms,Map<Character,GTToolType> tools,boolean mirror,boolean normalHandle){this(rows,forms,tools,mirror,normalHandle,Gate.NONE);}
        public int width(){return rows[0].length();}
        public int height(){return rows.length;}
        public char at(int x,int y){return rows[y].charAt(x);}
        public List<Character> letters(){return rows[0].chars().mapToObj(c->(char)c).filter(c->c!=' ').distinct().toList();}
    }
    private static Pattern adapt(ManualToolRecipeCatalog.Pattern source){
        Map<Character,GTToolType> tools=new LinkedHashMap<>();source.tools().forEach((key,value)->tools.put(key,GTToolType.valueOf(value.name())));
        Map<Character,Item> items=new LinkedHashMap<>();source.gate().items().forEach((key,value)->items.put(key,item(value)));
        var gate=source.gate();return new Pattern(source.rows(),source.forms(),Map.copyOf(tools),source.mirror(),source.normalHandle(),new Gate(Map.copyOf(items),gate.toolMaterial(),gate.onlyMaterial(),gate.onlyStone(),gate.skipHeadGate()));
    }
//? if >=1.21.1 {
    /*private static Item item(String id){return id==null?null:BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
*///?} else {
    private static Item item(String id){return id==null?null:BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
//?}
    public static List<Pattern> shaped(GTToolType type){return ManualToolRecipeCatalog.shaped(type.definition()).stream().map(GTToolRecipes::adapt).toList();}
    public static List<Pattern> heads(GTToolType type){return ManualToolRecipeCatalog.heads(type.definition()).stream().map(GTToolRecipes::adapt).toList();}
    public static MaterialPrefix headPrefix(GTToolType type){return ManualToolRecipeCatalog.headPrefix(type.definition());}
    public static Map<GTToolType,List<Pattern>> allShaped(){return adapt(ManualToolRecipeCatalog.allShaped());}
    public static Map<GTToolType,List<Pattern>> allHeads(){return adapt(ManualToolRecipeCatalog.allHeads());}
    private static Map<GTToolType,List<Pattern>> adapt(Map<gregtech.engine.tool.ToolDefinition,List<ManualToolRecipeCatalog.Pattern>> source){Map<GTToolType,List<Pattern>> out=new LinkedHashMap<>();source.forEach((key,value)->out.put(GTToolType.valueOf(key.name()),value.stream().map(GTToolRecipes::adapt).toList()));return Map.copyOf(out);}
    public static boolean isHeadAssembly(GTToolType type){return ManualToolRecipeCatalog.isHeadAssembly(type.definition());}
    public static boolean isWoodenRollingPin(GTToolType type,Pattern pattern){
        return type==GTToolType.ROLLING_PIN&&pattern.forms().get('P')==MaterialPrefix.plate;
    }
    @Nullable public static Item vanilla(char letter){return item(ManualToolRecipeCatalog.vanilla(letter));}
}
