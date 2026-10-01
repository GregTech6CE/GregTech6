package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.data.MaterialPrefix;import static com.gregtech.gregtech.data.MaterialPrefix.*;import java.util.List;
public final class AnvilForgingCatalog {private AnvilForgingCatalog(){}
    public record Rule(MaterialPrefix first,MaterialPrefix second,MaterialPrefix output,int outputCount,MaterialPrefix scrap,int scrapCount) {}
    private static Rule join(MaterialPrefix first,MaterialPrefix second,MaterialPrefix output) { return new Rule(first,second,output,1,null,0); }
    private static Rule flatten(MaterialPrefix first,MaterialPrefix output,int count,MaterialPrefix scrap,int scrapCount) { return new Rule(first,null,output,count,scrap,scrapCount); }
    public static final List<Rule> RULES=List.of(
            join(ingot,ingot,ingotDouble), join(ingot,ingotDouble,ingotTriple), join(ingot,ingotTriple,ingotQuadruple), join(ingot,ingotQuadruple,ingotQuintuple),
            join(ingotDouble,ingotDouble,ingotQuadruple), join(ingotDouble,ingotTriple,ingotQuintuple),
            join(plate,plate,plateDouble), join(plate,plateDouble,plateTriple), join(plate,plateTriple,plateQuadruple), join(plate,plateQuadruple,plateQuintuple),
            join(plateDouble,plateDouble,plateQuadruple), join(plateDouble,plateTriple,plateQuintuple), join(stick,stick,stickLong),
            flatten(ingotDouble,plate,1,scrapGt,9), flatten(ingotTriple,plateDouble,1,scrapGt,9),
            flatten(ingotQuadruple,plateTriple,1,scrapGt,9), flatten(ingotQuintuple,plateQuadruple,1,scrapGt,9),
            flatten(chunkGt,plateTiny,1,scrapGt,1),
            flatten(plate,itemCasing,1,scrapGt,4), flatten(plateCurved,plate,1,null,0), flatten(itemCasing,railGt,2,null,0));
}
