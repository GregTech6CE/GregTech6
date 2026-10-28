package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MaterialPrefixes;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.content.registry.GTWires;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** GT6 Loader_Recipes_Handlers:740–844: prefix matrix, forging targets and both mold families. */
public final class SourceExtruderRecipes {
    private SourceExtruderRecipes() {}
    public record Entry(GTMaterial inputMaterial, GTMaterial outputMaterial, String inputForm, String outputForm,
                        boolean lowHeatMold, boolean simple, long inputUnits, long outputUnits, Recipe recipe) {}
    private record Form(String name, long weight, ItemStack stack) {}
    private record Output(String form, String mold, boolean nonSmithable) {}
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static final List<Entry> PENDING=new ArrayList<>();
    private static final Set<GTMaterial> VISITED=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<GTMaterial, Map<String, Form>> FORMS=new IdentityHashMap<>();
    private static Map<GTMaterial, Map<String, Form>> physical;
    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    // OP.java's EXTRUDER_FODDER, INGOT_BASED and GEM_BASED prefixes; all current DUST_BASED
    // members are also EXTRUDER_FODDER in this pinned source. Lookup never invents absent forms.
    private static final List<String> INPUTS=List.of(
            "dust", "dustSmall", "dustTiny", "dustDiv72", "ingotQuintuple", "ingotQuadruple", "ingotTriple", "ingotDouble",
            "ingot", "billet", "chunkGt", "nugget", "gem", "gemChipped", "gemFlawed", "gemFlawless", "gemExquisite", "gemLegendary", "bouleGt",
            "lens", "scrapGt", "plateSteamcraft", "plateDense", "plateQuintuple", "plateQuadruple", "plateTriple", "plateDouble", "plate",
            "plateGem", "plateTiny", "plateGemTiny", "plateCurved", "compressed", "sheetGt", "foil", "stick", "stickLong", "bolt", "screw",
            "round", "ring", "chain", "spring", "springSmall", "wireFine", "minecartWheels", "gearGt", "gearGtSmall", "railGt", "casingSmall",
            "casingMachine", "casingMachineDouble", "casingMachineQuadruple", "casingMachineDense", "frameGt", "capcellcon",
            "pipeTiny", "pipeSmall", "pipeMedium", "pipeLarge", "pipeHuge", "pipeQuadruple", "pipeNonuple",
            "wireGt16", "wireGt15", "wireGt14", "wireGt13", "wireGt12", "wireGt11", "wireGt10", "wireGt09", "wireGt08", "wireGt07",
            "wireGt06", "wireGt05", "wireGt04", "wireGt03", "wireGt02", "wireGt01", "blockGem", "blockDust", "blockIngot", "blockPlate", "blockPlateGem", "blockSolid");
    private static final List<Output> OUTPUTS=List.of(
            out("ingot","ingot"), out("plate","plate"), out("plateCurved","curvedplate"), out("stick","rod"), out("stickLong","longrod"),
            out("bolt","bolt"), out("ring","ring"), out("wireGt01","wire"), out("casingSmall","casing"),
            out("toolHeadRawShovel","shovelhead"), out("pipeTiny","tinypipe"), out("pipeSmall","smallpipe"), out("pipeMedium","mediumpipe"),
            out("pipeLarge","largepipe"), out("pipeHuge","hugepipe"), out("toolHeadRawSword","swordblade"), out("toolHeadRawHoe","hoehead"),
            out("toolHeadRawSaw","sawblade"), out("toolHeadRawPickaxe","pickaxehead"), out("toolHeadRawAxe","axehead"),
            out("toolHeadFile","filehead"), out("toolHeadHammer","hammerhead"), out("gearGt","gear"), out("gearGtSmall","smallgear"),
            out("blockSolid","block"), out("capcellcon","capsulecellcontainer"), out("plateTiny","tinyplate"),
            new Output("foil","foil",true), new Output("wireFine","finewire",true));
    private static Output out(String form,String mold) { return new Output(form,mold,false); }

    public static int register(GTMaterial material) {
        if (!VISITED.add(material) || !material.isValid() || material.has(MaterialProperty.ANTIMATTER)) return 0;
        var profile=SourceExtrusionRules.profile(material);
        // Unknown native/addon materials retain only the former ordinary four-input matrix;
        // no original eligibility or low-heat permission is fabricated for them.
        if (profile==null) {
            if (!material.has(MaterialProperty.SMITHABLE)) return 0;
            profile=new SourceExtrusionRules.Profile(true,false,true,"",GTValues.U);
        }
        if (!profile.extrudable() || profile.forgingAmount()<GTValues.U) return 0;
        GTMaterial target=SourceExtrusionRules.target(material,profile);
        if (target==null || !target.isValid()) return 0;
        int before=PENDING.size();
        var inputs=forms(material); var outputs=forms(target);
        boolean nativeProfile=SourceExtrusionRules.profile(material)!=null;
        for (String name : INPUTS) {
            if (!nativeProfile && !Set.of("ingot","plate","gem","dust").contains(name)) continue;
            Form input=inputs.get(name);
            if (input==null) continue;
            for (Output row : OUTPUTS) {
                if (name.equals(row.form()) || row.nonSmithable() && profile.smithable()) continue;
                Form output=outputs.get(row.form());
                if (output==null) continue;
                long[] amounts=SourceExtrusionRules.amounts(input.weight(),output.weight());
                if (amounts==null) continue;
                long ticks=profile.simple()?SourceExtrusionRules.simpleTicks(output.weight()*amounts[2])
                        :SourceExtrusionRules.normalTicks(material,target,input.weight()*amounts[0]);
                add(material,target,input,output,row,amounts,ticks,profile.simple(),false);
                if (profile.simple()) add(material,target,input,output,row,amounts,ticks,true,true);
            }
        }
        return PENDING.size()-before;
    }

    private static void add(GTMaterial material,GTMaterial target,Form input,Form output,Output row,
                            long[] amounts,long ticks,boolean simple,boolean lowHeat) {
        var shape=GTTechnological.get((lowHeat?"low_heat_extruder_shape_":"extruder_shape_")+row.mold());
        if (shape==null) throw new IllegalStateException("Missing registered extruder mold "+row.mold());
        ItemStack in=input.stack().copyWithCount((int)amounts[0]);
        int count=(int)amounts[1];
        ItemStack[] out=count>64?new ItemStack[]{output.stack().copyWithCount(64),output.stack().copyWithCount(count-64)}
                :new ItemStack[]{output.stack().copyWithCount(count)};
        Recipe recipe=new Recipe(new ItemStack[]{in,new ItemStack(shape)},out,null,null,null,null,ticks,simple?16:96,0)
                .withCatalystInputs(1).withItemInputGuard(0,stack -> !ItemMaterialRegistry.hasStoredContents(stack));
        PENDING.add(new Entry(material,target,input.name(),output.name(),lowHeat,simple,
                    input.weight()*amounts[0],output.weight()*amounts[1],recipe));
    }

    /** Flush before the next bootstrap loader; earlier extruder recipes retain precedence. */
    public static int finishRegistration() {
        var accepted=Collections.newSetFromMap(new IdentityHashMap<Recipe,Boolean>());
        accepted.addAll(MachineRecipeMaps.Extruder.addRecipes(PENDING.stream().map(Entry::recipe).toList()));
        for (Entry entry:PENDING) if (accepted.contains(entry.recipe())) ENTRIES.add(entry);
        PENDING.clear();
        return ENTRIES.size();
    }

    private static Map<String,Form> forms(GTMaterial material) {
        return FORMS.computeIfAbsent(material,m -> {
            if (physical==null) physical=physicalForms();
            Map<String,Form> result=new LinkedHashMap<>(physical.getOrDefault(m,Map.of()));
            Set<String> names=new LinkedHashSet<>(INPUTS);
            OUTPUTS.forEach(o -> names.add(o.form()));
            var blockPrefixes=MaterialPrefixes.all();
            for (String name:names) {
                MaterialPrefix prefix=PrefixRegistry.byName(name);
                if (prefix!=null) {
                    ItemStack stack=GTItems.getStack(prefix,m,1);
                    if (!stack.isEmpty()) result.putIfAbsent(name,new Form(name,prefix.getMaterialWeight(),stack));
                }
                var entry=blockPrefixes.get(name);
                if (entry!=null && entry.isBlock()) {
                    var block=entry.getBlockDelegate(); ItemStack stack=GTBlocks.getStack(block,m);
                    if (!stack.isEmpty()) result.putIfAbsent(name,new Form(name,block.getMaterialWeight(),stack));
                }
            }
            return result;
        });
    }
    private static Map<GTMaterial,Map<String,Form>> physicalForms() {
        Map<GTMaterial,Map<String,Form>> result=new IdentityHashMap<>();
        for (var holder:GTWires.allWires()) {
            var block=holder.get(); int size=block.spec().size();
            String name=String.format(Locale.ROOT,"wireGt%02d",size);
            result.computeIfAbsent(block.spec().material().resolve(),k -> new LinkedHashMap<>())
                    .put(name,new Form(name,GTValues.U2*size,new ItemStack(block)));
        }
        for (var holder:gregtech.content.registry.GTSignalWires.all()) {
            var block=holder.get();
            if (!block.insulated()) result.computeIfAbsent(block.material().resolve(),k -> new LinkedHashMap<>())
                    .putIfAbsent("wireGt01",new Form("wireGt01",GTValues.U2,new ItemStack(block)));
        }
        for (var spec:PortableFluidContainerSpec.values()) if (spec.shapeId().equals("cell")) {
            ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gt6ce:fluid_"+spec.id())));
            if (!stack.isEmpty()) result.computeIfAbsent(spec.material().resolve(),k -> new LinkedHashMap<>())
                    .put("capcellcon",new Form("capcellcon",GTValues.U9,stack));
        }
        return result;
    }
}
