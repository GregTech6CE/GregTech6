package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem;import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry-driven built-in datapack: avoids thousands of tiny processResources files. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class MaterialTagPack extends AbstractPackResources {
    private Map<ResourceLocation,byte[]> resources;
    public MaterialTagPack(PackLocationInfo info) { super(info); }
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(output->{
            var location=new PackLocationInfo("gregtech:material_tags",Component.literal("GregTech material tags"),PackSource.BUILT_IN,Optional.empty());
            var pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(PackLocationInfo info){return new MaterialTagPack(info);}
                public PackResources openFull(PackLocationInfo info,Pack.Metadata metadata){return new MaterialTagPack(info);}
            },PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if(pack!=null) output.accept(pack);
        });
    }
    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var tags=new TreeMap<String,Set<String>>();
        var copperMaterials=MaterialGroups.Cu.getReRegistrations().stream().map(m->m.resolve()).collect(java.util.stream.Collectors.toSet());
        // Per-material tag: every form of one material (gear, rod, ingot, dust, block, fluid, unit...)
        // shares "gregtech:material/<material>", which is what recipe filtering and pack scripts want.
        // Per-form tags drop the "item" directory from their name: the tag is
        // "gregtech:<prefix registry name>/<material>" (e.g. gregtech:ingot/iron).
        // There is deliberately NO umbrella "gregtech:material" tag that lists all of them: resolving it
        // enumerates every material item in the mod, which costs JEI several seconds on each open.
        add(tags,"gregtech","item","ammunition/sticks_wood","minecraft:stick");
        var byMaterial=new TreeMap<String,Set<String>>();
        var blocksByMaterial=new TreeMap<String,Set<String>>();
        for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item); if(id==null||item==Items.AIR)continue;
            var form=MaterialEquivalence.form(new ItemStack(item));
            if(form!=null) {
                if(form.prefix()==MaterialPrefix.stick || form.prefix()==MaterialPrefix.plateTiny) {
                    var material=form.material().resolve();
                    boolean wood=com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Wood");
                    boolean plastic=com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Plastic");
                    if(form.prefix()==MaterialPrefix.stick && wood)
                        add(tags,"gregtech","item","ammunition/sticks_wood",id.toString());
                    if(plastic)
                        add(tags,"gregtech","item",form.prefix()==MaterialPrefix.stick
                                ? "ammunition/sticks_plastic" : "ammunition/tiny_plastic_plates",id.toString());
                }

                if(com.gregtech.gregtech.content.recipe.MaterialArrowRules.isArrow(form.prefix(),form.material()))
                    add(tags,"minecraft","item","arrows",id.toString());
                String common=MaterialEquivalence.tagPath(form);
                if(common!=null) add(tags,"forge","item",common,id.toString());
                addMaterial(byMaterial,form.material(),id.toString());
                // The canonical WoodTreated plate is a placeable BlockItem. Material
                // forms backed by blocks still need the same GT prefix tag as an item.
                add(tags,"gregtech","item",form.prefix().getRegistryName()+"/"
                        +MaterialEquivalence.materialName(form.material()),id.toString());
            } else {
                // Vanilla-unified items (minecraft:iron_block, ...) carry a material without a prefix.
                var data=com.gregtech.gregtech.api.material.ItemMaterialRegistry.base(item).orElse(null);
                if(data!=null&&data.components().size()==1) {
                    var material=data.components().get(0).material().resolve();
                    if(material.isValid()) {
                        addMaterial(byMaterial,material,id.toString());
                        String name=MaterialEquivalence.materialName(material);
                        if(data.prefix()==null&&data.components().get(0).amount()==GTValues.U*9)
                            add(tags,"forge","item","storage_blocks/"+name,id.toString());
                        else if(data.prefix()==null&&data.components().get(0).amount()==GTValues.U*2)
                            add(tags,"forge","item","ores/"+name,id.toString());
                    }
                }
            }
            // GT tools: standard Forge convention, e.g. forge:tools/wrench.
            String toolKind=com.gregtech.gregtech.platform.neoforge.NeoToolBindings.craftKind(new ItemStack(item));
            if(!toolKind.isEmpty()) add(tags,"forge","item","tools/"+toolKind,id.toString());
            // GT6 fluid containers: one item per fluid, so a material's fluid forms belong to that
            // material's tag (RegisteredFluids knows which material a fluid was created from).
            if(item instanceof FluidDisplayItem fluidItem) {
                // GT6's fluids know their material: either as the declaration's materialKey (the molten
                // and generated entries) or through the side binding created when a generated gas/liquid
                // reuses a dedicated fluid (RegisteredFluids.BOUND_MATERIALS).
                String bound=fluidItem.fluidEntry().materialKey();
                if(bound==null) bound=RegisteredFluids.boundMaterial(fluidItem.fluidEntry().registryName());
                if(bound!=null) {
                    var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(bound);
                    if(material!=null&&material.isValid()) {
                        String name=MaterialEquivalence.materialName(material);
                        add(tags,"gregtech","item","fluid/"+name,id.toString());
                        addMaterial(byMaterial,material,id.toString());
                    }
                }
                continue;
            }
            if(item instanceof BlockItem b && b.getBlock() instanceof com.gregtech.gregtech.block.BlackSandBlock sand) {
                String name=MaterialEquivalence.materialName(sand.material());
                for(String kind:List.of("item","block")) {
                    add(tags,"gregtech",kind,"block_dust/"+name,id.toString());
                    add(tags,"forge",kind,"storage_blocks/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,sand.material(),id.toString());
            }
            if(item instanceof BlockItem b && b.getBlock() instanceof MaterialBlockLike m) {
                String name=MaterialEquivalence.materialName(m.material());
                String group=switch(m.prefix().getName()) {
                    case "blockIngot","blockGem","blockDust" -> "storage_blocks";
                    case "blockRaw" -> "storage_blocks/raw";
                    case "ore" -> "ores";
                    default -> null;
                };
                for(String kind:List.of("item","block")) {
                    add(tags,"gregtech",kind,m.prefix().getRegistryName()+"/"+name,id.toString());
                    if(group!=null) add(tags,"forge",kind,group.equals("storage_blocks/raw")?"storage_blocks/raw_"+name:group+"/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,m.material(),id.toString());
            }

            if(item instanceof BlockItem b) {
                var block=b.getBlock();
                if(block instanceof com.gregtech.gregtech.api.energy.WireMaterialLike wire
                        && copperMaterials.contains(wire.spec().material().resolve())) {
                    String wireForm=(wire.spec().insulated()?"cable_":"wire_")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                    add(tags,"gregtech","item",wireForm+"/any_copper",id.toString());
                }
                if(block instanceof com.gregtech.gregtech.block.DenseOreBlock denseOre
                        && denseOre.material().isValid()) {
                    String name=MaterialEquivalence.materialName(denseOre.material());
                    // The existing block item is the GT6 oreDense form, not a new material item.
                    for(String kind:List.of("item","block")) {
                        add(tags,"gregtech",kind,"ore_dense/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,denseOre.material(),id.toString());
                }
                if(block instanceof com.gregtech.gregtech.block.VanillaOreBlock ore
                        && ore.material().isValid()) {
                    String name=MaterialEquivalence.materialName(ore.material());
                    for(String kind:List.of("item","block")) {
                        add(tags,"gregtech",kind,"ore_vanillastone/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,ore.material(),id.toString());
                }
            }
        }
        // GT6 ANY.Iron includes ten iron/steel materials, not just vanilla iron.
        var ironScrews = tags.computeIfAbsent("gregtech:tags/item/screws/any_iron_or_steel.json", k -> new TreeSet<>());
        for (var material : MaterialGroups.Iron.getReRegistrations()) {
            String path = "screws/" + MaterialEquivalence.materialName(material.resolve());
            if (tags.containsKey("forge:tags/item/" + path + ".json")) ironScrews.add("#forge:" + path);
        }
        var ironDoublePlates=tags.computeIfAbsent("gregtech:tags/item/plate_double/any_iron_or_steel.json",k->new TreeSet<>());
        for(var material:MaterialGroups.Iron.getReRegistrations()) {
            String path="plate_double/"+MaterialEquivalence.materialName(material.resolve());
            if(tags.containsKey("gregtech:tags/item/"+path+".json"))ironDoublePlates.add("#gregtech:"+path);
        }
        byMaterial.forEach((name,values)->{
            var tag=tags.computeIfAbsent("gregtech:tags/item/material/"+name+".json",k->new TreeSet<>());
            tag.addAll(values);
            tag.addAll(blocksByMaterial.getOrDefault(name,Set.of()));
        });
        // Wood form tags for the wooden-pipe recipes (GT6's OD.plankAnyWood / OD.beamWood): vanilla
        // planks and slabs join through nested tag references, the port's own species are listed.
        tags.computeIfAbsent("gregtech:tags/item/wooden_planks.json", k -> new TreeSet<>())
                .add("#minecraft:planks");
        tags.computeIfAbsent("gregtech:tags/item/wooden_slabs.json", k -> new TreeSet<>())
                .add("#minecraft:wooden_slabs");
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !id.getNamespace().equals("gregtech")) continue;
            if(item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof com.gregtech.gregtech.block.wood.WoodPlanksBlock)
                tags.computeIfAbsent("gregtech:tags/item/wooden_planks.json",key->new TreeSet<>()).add(id.toString());
            if(item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof com.gregtech.gregtech.block.wood.WoodBeamBlock)
                tags.computeIfAbsent("gregtech:tags/item/wooden_beams.json",key->new TreeSet<>()).add(id.toString());
        }
        // Common spelling aliases used by Forge 1.20 packs. References keep datapack additions shared.
        var aliases=Map.of("aluminium","aluminum","quartz","nether_quartz","sulfur","sulphur","tungsten_steel","tungstensteel");
        for(var path:new ArrayList<>(tags.keySet())) for(var alias:aliases.entrySet()) {
            String suffix="/"+alias.getKey()+".json";
            if(path.startsWith("forge:")&&path.endsWith(suffix)) {
                String alt=path.substring(0,path.length()-suffix.length())+"/"+alias.getValue()+".json";
                // Union aliases without recursive tag references.
                var values=tags.get(path);tags.computeIfAbsent(alt,k->new TreeSet<>()).addAll(values);
            }
        }
        for (var path:new ArrayList<>(tags.keySet())) {
            if(path.startsWith("forge:")) tags.computeIfAbsent("c:"+path.substring(6),k->new TreeSet<>()).addAll(tags.get(path));
        }
        var gson=new com.google.gson.Gson(); var result=new HashMap<ResourceLocation,byte[]>();
        tags.forEach((path,values)->result.put(ResourceLocation.parse(path),gson.toJson(Map.of("replace",false,"values",compatibleValues(path,values,aliases))).getBytes(StandardCharsets.UTF_8)));
        result.put(ResourceLocation.parse("gregtech:tags/item/ammunition/feathers.json"),gson.toJson(Map.of(
                "replace",false,"values",List.of("minecraft:feather",
                        Map.of("id","#forge:feathers","required",false),
                        Map.of("id","#c:feathers","required",false)))).getBytes(StandardCharsets.UTF_8));
        resources=Collections.unmodifiableMap(result); return resources;
    }

    /** Legacy recipe tags accept native common additions; c tags retain their independent values. */
    private static List<Object> compatibleValues(String path, Set<String> values, Map<String,String> aliases) {
        var result = new ArrayList<Object>(values);
        String prefix = path.startsWith("forge:tags/item/") ? "forge:tags/item/"
                : path.startsWith("forge:tags/block/") ? "forge:tags/block/" : null;
        if (prefix == null) return result;
        String common = path.substring(prefix.length(), path.length() - ".json".length());
        result.add(Map.of("id", "#c:" + common, "required", false));
        for (var alias : aliases.entrySet()) {
            String from = "/" + alias.getKey(), to = "/" + alias.getValue();
            if (common.endsWith(from)) {
                result.add(Map.of("id", "#c:" + common.substring(0, common.length() - from.length()) + to, "required", false));
            } else if (common.endsWith(to)) {
                result.add(Map.of("id", "#c:" + common.substring(0, common.length() - to.length()) + from, "required", false));
            }
        }
        return result;
    }

    /** Aggregate tag value list for one material (forms + its blocks). */
    private static void addMaterial(Map<String,Set<String>> target,com.gregtech.gregtech.api.material.GTMaterial material,String value) {
        if(material==null||!material.isValid())return;
        target.computeIfAbsent(MaterialEquivalence.materialName(material),k->new TreeSet<>()).add(value);
    }
    private static void add(Map<String,Set<String>> tags,String ns,String kind,String path,String value) {
        tags.computeIfAbsent(ns+":tags/"+kind+"/"+path+".json",k->new TreeSet<>()).add(value);
        int slash=path.indexOf('/');
        if(slash>0) tags.computeIfAbsent(ns+":tags/"+kind+"/"+path.substring(0,slash)+".json",k->new TreeSet<>()).add("#"+ns+":"+path);
    }
    public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1||!path[0].equals("pack.mcmeta"))return null;
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":48,\"description\":\"GregTech material tags\"}}".getBytes(StandardCharsets.UTF_8));
    }
    public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id) {
        if(type!=PackType.SERVER_DATA)return null; var bytes=data().get(id);
        return bytes==null?null:()->new ByteArrayInputStream(bytes);
    }
    public void listResources(PackType type,String ns,String path,ResourceOutput out) {
        if(type==PackType.SERVER_DATA)data().forEach((id,bytes)->{if(id.getNamespace().equals(ns)&&id.getPath().startsWith(path+"/"))out.accept(id,()->new ByteArrayInputStream(bytes));});
    }
    public Set<String> getNamespaces(PackType type) { return type==PackType.SERVER_DATA?Set.of("forge","c","gregtech","minecraft"):Set.of(); }
    public void close() { resources=null; }
}
