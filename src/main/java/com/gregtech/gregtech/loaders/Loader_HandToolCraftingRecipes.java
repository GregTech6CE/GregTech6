package com.gregtech.gregtech.loaders;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.recipe.CreosoteAxleRecipe;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTAxles;
import com.gregtech.gregtech.registry.GTGearboxes;
import com.gregtech.gregtech.registry.GTPumps;
import com.gregtech.gregtech.registry.GTItemPipes;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTStorageMetals;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.registry.GTToolItems;
import com.gregtech.gregtech.registry.GTWires;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The hand-crafting recipes GT6 ships as <em>pattern arguments</em> of its block registrations.
 *
 * <p>GT6 passes the crafting pattern to {@code aRegistry.add(name, category, id, tab, class, quality,
 * stack, block, nbt, "<pattern>", "<pattern>", 'K', <ingredient>, ’}, so a recipe never appears as a
 * {@code CR.shaped} call and is easy to miss when porting:</p>
 *
 * <ul>
 *   <li><b>Pipes</b> ({@code MultiTileEntityPipeFluid:92-98}, {@code MultiTileEntityPipeItem:77-82}),
 *       gated on their {@code aRecipe} flag:
 *       <pre>
 * tiny   "sP " / "wzh"            medium "PPP" / "wzh"
 * small  " P " / "wzh"            large  "PPP" / "wzh" / "PPP"
 * huge   "PPP" / "wzh" / "PPP" with a double plate
 * restrictive  " h " / "RPR" / " R "  (R = steel ring, P = the same size's plain pipe)
 *       </pre>
 *       Tools: {@code s} saw, {@code w} wrench, {@code z} bending cylinder, {@code h} hammer.</li>
 *   <li><b>Anvils</b> ({@code Loader_MultiTileEntities:2184-2218}): {@code "RRR"/"hR "/"RRR"} with
 *       vanilla stone for the stone anvil, the stone block for the stone variants and ingots for the
 *       metal ones.</li>
 *   <li><b>Storage</b> ({@code Loader_MultiTileEntities:132-141}): the metal chest
 *       ({@code "sPw"/"RSR"/"PPP"}), the mass storage ({@code "TCT"/"wMd"/"TCT"}), the compartment
 *       drawer ({@code "CTC"/"TdT"/"CTC"}) and the locker ({@code "SdS"/"LCL"/"TMT"}).</li>
 *   <li><b>1x wires</b>: requested by the user against the original; the pattern follows GT6's own
 *       {@code "xP"} rows ({@code Compat_Recipes_IndustrialCraft: plate + wire cutter}), applied to the
 *       port's wire blocks because GT6's Wiremill is the only documented source.</li>
 *   <li><b>Axles</b> ({@code Loader_MultiTileEntities:1663-1759}): 13 original material tiers and four
 *       diameters each. The huge treated-wood axle accepts a finite, drainable creosote container.</li>
 *   <li><b>Rotation engines and pumps</b> ({@code Loader_MultiTileEntities:1125-1128, 1667-1764}):
 *       their real medium axle, gears, casing, rotor, stainless pipe and bottled lubricant.</li>
 *   <li><b>Custom and transformer gearboxes</b> ({@code Loader_MultiTileEntities:1668-1766}):
 *       all 13 GT6 material grades of each, with the wooden patterns and early lubricant kept distinct.</li>
 * </ul>
 *
 * <p>Everything is registered through {@link RecipeManager#replaceRecipes} on server start, wrapped in a
 * {@link ToolShapedRecipe} so the tools wear exactly like in the original.</p>
 */
public final class Loader_HandToolCraftingRecipes {
    private static final List<String> REGISTERED = new ArrayList<>();

    private Loader_HandToolCraftingRecipes() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();

        int wires = wires(recipes);
        int pipes = pipes(recipes);
        int anvils = anvils(recipes);
        int storage = storage(recipes);
        int bars = bars(recipes);
        int circuits = circuits(recipes);
        int cells = cells(recipes);
        int axles = axles(recipes);
        int rotationEngines = rotationEngines(recipes);
        int rotationalPumps = rotationalPumps(recipes);
        int customGearboxes = customGearboxes(recipes);
        int transformerGearboxes = transformerGearboxes(recipes);

        if (!REGISTERED.isEmpty()) com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 hand recipes from registration patterns"
                        + " ({} wires, {} pipes, {} anvils, {} storage, {} bars, {} circuits, {} cells, {} axles, {} rotation engines, {} rotational pumps, {} custom gearboxes, {} transformer gearboxes)",
                REGISTERED.size(), wires, pipes, anvils, storage, bars, circuits, cells, axles,
                rotationEngines, rotationalPumps, customGearboxes, transformerGearboxes);
    }

    // ── GT6's 13 Custom Gearboxes and 13 Transformer Gearboxes ────────────

    private static int customGearboxes(List<Recipe<?>> recipes) {
        if (GTGearboxes.allGearboxes().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 custom gearboxes; got "
                    + GTGearboxes.allGearboxes().size());
        int added = 0;
        for (var holder : GTGearboxes.allGearboxes()) {
            if (!holder.isPresent()) throw new IllegalStateException("Unbound custom gearbox " + holder.getId());
            var spec = holder.get().spec();
            GTMaterial material = spec.material();
            String path = "gearbox/" + spec.id();
            ItemStack axle = requiredMediumAxle(material, path);
            boolean wooden = isTreatedWood(material);
            Ingredient lubricant = lubricant(earlyLubricant(material));
            Map<Character, Ingredient> key;
            String[] pattern;
            if (wooden) {
                // GT6 24809: "PsP" / "ALA" / "PAP", where s is a saw.
                ItemStack plate = requiredMaterial(MaterialPrefix.plate, material, path);
                pattern = new String[]{"PsP", "ALA", "PAP"};
                key = Map.of('P', Ingredient.of(plate), 'A', Ingredient.of(axle),
                        's', requiredTool(GTToolType.SAW), 'L', lubricant);
            } else {
                // GT6 24779-24899: "wAL" / "AMA", with a same-material machine casing.
                ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachine, material, path);
                pattern = new String[]{"wAL", "AMA"};
                key = Map.of('A', Ingredient.of(axle), 'M', Ingredient.of(casing),
                        'w', requiredTool(GTToolType.WRENCH), 'L', lubricant);
            }
            added += requiredAxle(recipes, path, pattern, key,
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 custom gearbox recipes; got " + added);
        return added;
    }

    private static int transformerGearboxes(List<Recipe<?>> recipes) {
        if (GTGearboxes.allTransformers().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 transformer gearboxes; got "
                    + GTGearboxes.allTransformers().size());
        int added = 0;
        for (var holder : GTGearboxes.allTransformers()) {
            if (!holder.isPresent()) throw new IllegalStateException("Unbound transformer gearbox " + holder.getId());
            var spec = holder.get().spec();
            GTMaterial material = spec.material();
            String path = "rotation_transformer/" + spec.id();
            ItemStack axle = requiredMediumAxle(material, path);
            ItemStack smallGear = requiredMaterial(MaterialPrefix.gearGtSmall, material, path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, material, path);
            boolean wooden = isTreatedWood(material);
            Ingredient lubricant = lubricant(earlyLubricant(material));
            ItemStack hull = wooden
                    ? requiredMaterial(MaterialPrefix.plate, material, path)
                    : requiredBlock(BlockMaterialPrefix.casingMachineDouble, material, path);
            // GT6 24808 uses a treated-wood plate at P; 24778-24898 use a double casing at M.
            String[] pattern = wooden
                    ? new String[]{"ASL", "SGS", "PSA"}
                    : new String[]{"ASL", "SGS", "MSA"};
            Map<Character, Ingredient> key = Map.of(
                    'A', Ingredient.of(axle), 'S', Ingredient.of(smallGear),
                    'G', Ingredient.of(gear), wooden ? 'P' : 'M', Ingredient.of(hull),
                    'L', lubricant);
            added += requiredAxle(recipes, path, pattern, key,
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 transformer gearbox recipes; got " + added);
        return added;
    }

    private static boolean isTreatedWood(GTMaterial material) {
        return material == com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated;
    }

    private static boolean earlyLubricant(GTMaterial material) {
        return isTreatedWood(material) || material == Materials.Bronze || material == Materials.Brass
                || material == Materials.ArsenicCopper || material == Materials.ArsenicBronze;
    }

    // ── GT6's 13 RU → KU engines and four RU rotational pumps ──────────────

    private static int rotationEngines(List<Recipe<?>> recipes) {
        if (MachineRegistry.rotationEngines().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 rotation engines; got "
                    + MachineRegistry.rotationEngines().size());
        int added = 0;
        for (var holder : MachineRegistry.rotationEngines()) {
            if (!holder.isPresent()) throw new IllegalStateException("Unbound rotation engine " + holder.getId());
            RotationEngineSpec spec = holder.get().engineSpec(RotationEngineSpec.class);
            GTMaterial material = spec.material();
            boolean wooden = isTreatedWood(material);
            boolean early = earlyLubricant(material);
            String path = "engine_rotation/" + spec.id();
            ItemStack smallGear = requiredMaterial(MaterialPrefix.gearGtSmall, material, path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, material, path);
            ItemStack axle = requiredMediumAxle(material, path);
            Ingredient lubricant = lubricant(early);
            Ingredient wrench = requiredTool(GTToolType.WRENCH);
            Map<Character, Ingredient> key;
            String[] pattern;
            if (wooden) {
                // GT6: "PSP" / "wAL" / "GAG" (treated wood plates, not a metal casing).
                ItemStack plate = requiredMaterial(MaterialPrefix.plate, material, path);
                pattern = new String[]{"PSP", "wAL", "GAG"};
                key = Map.of('P', Ingredient.of(plate), 'S', Ingredient.of(smallGear),
                        'G', Ingredient.of(gear), 'A', Ingredient.of(axle),
                        'w', wrench, 'L', lubricant);
            } else {
                // GT6: "SAS" / "wML" / "GAG", with the same material's machine casing.
                ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachine, material, path);
                pattern = new String[]{"SAS", "wML", "GAG"};
                key = Map.of('M', Ingredient.of(casing), 'S', Ingredient.of(smallGear),
                        'G', Ingredient.of(gear), 'A', Ingredient.of(axle),
                        'w', wrench, 'L', lubricant);
            }
            added += requiredAxle(recipes, path, pattern, key, new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 rotation engine recipes; got " + added);
        return added;
    }

    private static int rotationalPumps(List<Recipe<?>> recipes) {
        if (GTPumps.all().size() != 4)
            throw new IllegalStateException("Expected four GT6 rotational pumps; got " + GTPumps.all().size());
        int added = 0;
        for (var holder : GTPumps.all()) {
            if (!holder.isPresent()) throw new IllegalStateException("Unbound rotational pump " + holder.getId());
            var spec = holder.get().spec();
            String path = "rotational_pump/" + spec.id();
            PipeSpec.PipeSize pipeSize = switch (spec.tier()) {
                case 1 -> PipeSpec.PipeSize.SMALL;
                case 2 -> PipeSpec.PipeSize.MEDIUM;
                case 3 -> PipeSpec.PipeSize.LARGE;
                case 4 -> PipeSpec.PipeSize.HUGE;
                default -> throw new IllegalStateException("Unknown GT6 rotational pump tier " + spec.tier());
            };
            ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachineDouble, spec.material(), path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, spec.material(), path);
            ItemStack rotor = requiredMaterial(MaterialPrefix.rotor, Materials.StainlessSteel, path);
            ItemStack pipe = requiredStainlessPipe(pipeSize, path);
            added += requiredAxle(recipes, path, new String[]{"GwG", "PMP", "RPR"},
                    Map.of('G', Ingredient.of(gear), 'M', Ingredient.of(casing),
                            'R', Ingredient.of(rotor), 'P', Ingredient.of(pipe),
                            'w', requiredTool(GTToolType.WRENCH)),
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 4) throw new IllegalStateException("Expected four GT6 rotational pump recipes; got " + added);
        return added;
    }

    private static ItemStack requiredMaterial(MaterialPrefix form, GTMaterial material, String recipe) {
        ItemStack stack = GTItems.getStack(form, material, 1);
        if (stack.isEmpty()) throw new IllegalStateException(recipe + " lacks GT6 " + form.getName()
                + " of " + material.getName());
        return stack;
    }

    private static ItemStack requiredBlock(BlockMaterialPrefix form, GTMaterial material, String recipe) {
        ItemStack stack = GTBlocks.getStack(form, material);
        if (stack.isEmpty()) throw new IllegalStateException(recipe + " lacks GT6 " + form.getName()
                + " of " + material.getName());
        return stack;
    }

    private static ItemStack requiredMediumAxle(GTMaterial material, String recipe) {
        for (var holder : GTAxles.all()) {
            if (holder.isPresent() && holder.get().spec().size() == 2
                    && holder.get().spec().material() == material) return new ItemStack(holder.get().asItem());
        }
        throw new IllegalStateException(recipe + " lacks its GT6 medium axle of " + material.getName());
    }

    private static ItemStack requiredStainlessPipe(PipeSpec.PipeSize size, String recipe) {
        for (var holder : GTFluidPipes.all()) {
            if (holder.isPresent() && holder.get().spec().size() == size
                    && holder.get().spec().material() == Materials.StainlessSteel)
                return new ItemStack(holder.get().asItem());
        }
        throw new IllegalStateException(recipe + " lacks GT6 " + size + " StainlessSteel fluid pipe");
    }

    private static Ingredient lubricant(boolean early) {
        // GT6 LoaderOreDictReRegistrations:978-989: itemLubricant is filled lubricant,
        // itemLubricantEarly additionally accepts eight seed/vegetable/animal oil bottles.
        // BottleItem returns the GT6 empty bottle from getCraftingRemainingItem.
        String[] names = early
                ? new String[]{"lubricant_bottle", "olive_oil", "sunflower_oil", "nut_oil",
                    "seed_oil", "hemp_oil", "lin_oil", "fish_oil", "whale_oil"}
                : new String[]{"lubricant_bottle"};
        ItemStack[] bottles = new ItemStack[names.length];
        for (int i = 0; i < names.length; i++) {
            Item item = ForgeRegistries.ITEMS.getValue(GregTech.id(names[i]));
            if (item == null || item == Items.AIR)
                throw new IllegalStateException("Missing GT6 itemLubricant bottle " + names[i]);
            bottles[i] = new ItemStack(item);
        }
        return Ingredient.of(bottles);
    }

    // ── axles: GT6 kinetic registration recipes, 13 materials × 4 sizes ────

    private static int axles(List<Recipe<?>> recipes) {
        if (GTAxles.all().size() != 52)
            throw new IllegalStateException("Expected all 52 GT6 axle variants before adding their recipes; got "
                    + GTAxles.all().size());

        Ingredient file = requiredTool(GTToolType.FILE);
        Ingredient hardHammer = requiredTool(GTToolType.HARD_HAMMER);
        Ingredient softHammer = requiredTool(GTToolType.SOFT_HAMMER);
        int added = 0;
        for (var holder : GTAxles.all()) {
            if (!holder.isPresent()) throw new IllegalStateException("Unbound axle: " + holder.getId());
            AxleSpec spec = holder.get().spec();
            boolean wood = spec.material() == com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated;
            int size = spec.size();
            String path = "axle/" + spec.id();
            ItemStack output = new ItemStack(holder.get().asItem());
            if (wood && size == 4) {
                // GT6: "rS" / "Bf", S=OD.beamWood, B=OD.container1000creosote.
                added += requiredAxle(recipes, path, new String[]{"rS", "Bf"},
                        Map.of('r', softHammer, 'f', file,
                                'S', Ingredient.of(ItemTags.create(GregTech.id("wooden_beams"))),
                                'B', CreosoteAxleRecipe.creosote()), output, true);
                continue;
            }

            // GT6: small/medium wood are two treated rods; large wood uses four long rods.
            // Every metal size uses two units of its size-specific form.
            MaterialPrefix form = switch (size) {
                case 1 -> MaterialPrefix.stick;
                case 2 -> MaterialPrefix.stickLong;
                case 3 -> wood ? MaterialPrefix.stickLong : MaterialPrefix.ingotDouble;
                case 4 -> MaterialPrefix.ingotQuadruple;
                default -> throw new IllegalStateException("Unknown axle diameter " + size);
            };
            ItemStack material = GTItems.getStack(form, spec.material(), 1);
            if (material.isEmpty()) throw new IllegalStateException(
                    "GT6 axle " + spec.id() + " has no " + form.getName() + " form for "
                            + spec.material().getName());
            Ingredient piece = axleMaterial(form, spec.material(), material);
            String[] pattern = wood && size == 3
                    ? new String[]{"  S", "SrS", "S f"}
                    : new String[]{"  S", wood ? " r " : " h ", "S f"};
            added += requiredAxle(recipes, path, pattern,
                    Map.of('S', piece, 'r', softHammer, 'h', hardHammer, 'f', file), output, false);
        }
        if (added != 52) throw new IllegalStateException("Expected 52 GT6 axle recipes; got " + added);
        return added;
    }

    private static Ingredient requiredTool(GTToolType type) {
        ItemStack stack = tool(type);
        if (stack.isEmpty()) throw new IllegalStateException("Missing axle crafting tool " + type.id());
        return Ingredient.of(stack);
    }

    private static Ingredient axleMaterial(MaterialPrefix form, GTMaterial material, ItemStack fallback) {
        // Forge's matching exact-material rod tags preserve GT6's ANY.Steel equivalence and let
        // other mods contribute the same material form. Multi-ingots have no common Forge tag.
        if (form == MaterialPrefix.stick || form == MaterialPrefix.stickLong) {
            String kind = form == MaterialPrefix.stick ? "rods" : "long_rods";
            String name = MaterialEquivalence.materialName(material);
            return Ingredient.merge(List.of(Ingredient.of(fallback),
                    Ingredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", kind + "/" + name)))));
        }
        return Ingredient.of(fallback);
    }

    private static int requiredAxle(List<Recipe<?>> recipes, String path, String[] pattern,
                                    Map<Character, Ingredient> key, ItemStack output, boolean creosote) {
        int made = register(recipes, path, pattern, key, output, creosote);
        if (made != 1) throw new IllegalStateException("GT6 axle recipe could not be registered: " + path);
        return made;
    }

    // ── 1x wires: plate + wire cutter ───────────────────────────────────────

    private static int wires(List<Recipe<?>> recipes) {
        Map<String, GTMaterial> families = new HashMap<>();
        for (var holder : GTWires.allWires()) {
            if (!holder.isPresent()) continue;
            var spec = holder.get().spec();
            families.putIfAbsent(spec.id(), spec.material());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            ItemStack plate = plate(family.getValue());
            ItemStack cutter = tool(GTToolType.WIRE_CUTTER);
            if (plate.isEmpty() || cutter.isEmpty()) continue;
            added += register(recipes, "wire/" + family.getKey(), new String[]{"Px"},
                    Map.of('P', Ingredient.of(plate), 'x', Ingredient.of(cutter)),
                    wireItem(family.getKey(), 1));
        }
        return added;
    }

    // ── pipes: the aRecipe patterns of the pipe helpers ─────────────────────

    private static int pipes(List<Recipe<?>> recipes) {
        int added = 0;
        for (var holder : GTFluidPipes.all()) {
            if (!holder.isPresent()) continue;
            PipeSpec spec = holder.get().spec();
            GTMaterial material = spec.material();
            ItemStack output = new ItemStack(holder.get().asItem(),
                    spec.size() == PipeSpec.PipeSize.TINY ? 2 : 1);
            added += pipe(recipes, "fluid/" + spec.id(), spec.size(), material, output);
        }
        for (var holder : GTItemPipes.all()) {
            if (!holder.isPresent()) continue;
            ItemPipeSpec spec = holder.get().spec();
            ItemStack output = new ItemStack(holder.get().asItem());
            if (spec.size().restrictive()) {
                ItemStack plain = plainPipe(spec.material(), spec.size());
                if (plain.isEmpty()) continue;
                String[] pattern = switch (spec.size()) {
                    case RESTRICTIVE_MEDIUM -> new String[]{" h ", "RPR", " R "};
                    case RESTRICTIVE_LARGE -> new String[]{"hR ", "RPR", " R "};
                    default -> new String[]{" h ", "RPR", "RRR"};
                };
                added += register(recipes, "pipe/restrictive/" + spec.id(), pattern,
                        Map.of('P', Ingredient.of(plain), 'R', Ingredient.of(steelRing()),
                                'h', Ingredient.of(tool(GTToolType.HARD_HAMMER))),
                        output);
                continue;
            }
            PipeSpec.PipeSize size = switch (spec.size()) {
                case MEDIUM -> PipeSpec.PipeSize.MEDIUM;
                case LARGE -> PipeSpec.PipeSize.LARGE;
                default -> PipeSpec.PipeSize.HUGE;
            };
            added += pipe(recipes, "item/" + spec.id(), size, spec.material(), output);
        }
        return added;
    }

    /** GT6's five pipe patterns; huge pipes use a double plate instead of a curved one. */
    private static int pipe(List<Recipe<?>> recipes, String path, PipeSpec.PipeSize size,
                            GTMaterial material, ItemStack output) {
        boolean huge = size == PipeSpec.PipeSize.HUGE;
        ItemStack plate = GTItems.getStack(huge ? MaterialPrefix.plateDouble : MaterialPrefix.plateCurved,
                material, 1);
        if (plate.isEmpty() || output.isEmpty()) return 0;
        String[] pattern = switch (size) {
            case TINY -> new String[]{"sP ", "wzh"};
            case SMALL -> new String[]{" P ", "wzh"};
            case MEDIUM -> new String[]{"PPP", "wzh"};
            default -> new String[]{"PPP", "wzh", "PPP"};
        };
        Map<Character, Ingredient> key = Map.of(
                'P', Ingredient.of(plate),
                's', Ingredient.of(tool(GTToolType.SAW)),
                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                'z', Ingredient.of(tool(GTToolType.BENDING_CYLINDER)),
                'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)));
        return register(recipes, "pipe/" + path, pattern, key, output);
    }

    // ── anvils: "RRR" / "hR " / "RRR" ──────────────────────────────────────

    private static int anvils(List<Recipe<?>> recipes) {
        int added = 0;
        for (var holder : GTToolBlocks.ANVILS) {
            if (!holder.isPresent()) continue;
            var block = holder.get();
            GTMaterial material = block.material();
            ItemStack ring = anvilIngredient(material);
            if (ring.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            added += register(recipes, "anvil/" + (id == null ? material.getName() : id.getPath()),
                    new String[]{"RRR", "hR ", "RRR"},
                    Map.of('R', Ingredient.of(ring),
                            'h', Ingredient.of(tool(GTToolType.HARD_HAMMER))),
                    new ItemStack(block.asItem()));
        }
        return added;
    }

    /** Stone anvils use vanilla stone or their own stone block, metal anvils use ingots. */
    private static ItemStack anvilIngredient(GTMaterial material) {
        String name = material.getName();
        if (name.equals("Stone")) return new ItemStack(Items.STONE);
        // the port's stone blocks are "stone_<type>_stone" (Loader_Blocks.registerStoneBlocks)
        String type = MaterialEquivalence.materialName(material);
        for (String candidate : new String[]{type, type.replace("_", "")}) {
            Item item = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "stone_" + candidate + "_stone"));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return GTItems.getStack(MaterialPrefix.ingot, material, 1);
    }

    // ── BlockBaseBars: six rods and two reusable hand tools -> three segments ──
    private static int bars(List<Recipe<?>> recipes) {
        int added = 0;
        Object[][] entries = {
                {"iron", Materials.Iron, GTDecorBlocks.BARS_IRON.get()},
                {"steel", Materials.Steel, GTDecorBlocks.BARS_STEEL.get()},
                {"brass", Materials.Brass, GTDecorBlocks.BARS_BRASS.get()},
                {"bronze", Materials.Bronze, GTDecorBlocks.BARS_BRONZE.get()},
                {"wrought_iron", Materials.WroughtIron, GTDecorBlocks.BARS_WROUGHT_IRON.get()},
                {"stainless", Materials.StainlessSteel, GTDecorBlocks.BARS_STAINLESS.get()},
                {"tungsten_steel", Materials.Tungstensteel, GTDecorBlocks.BARS_TUNGSTEN_STEEL.get()},
        };
        for (Object[] entry : entries) {
            ItemStack rod = GTItems.getStack(MaterialPrefix.stick, (GTMaterial) entry[1], 1);
            if (rod.isEmpty()) continue;
            added += register(recipes, "bars/" + entry[0], new String[]{"BBB", "h w", "BBB"},
                    Map.of('B', Ingredient.of(rod),
                            'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)),
                            'w', Ingredient.of(tool(GTToolType.WRENCH))),
                    new ItemStack((Block) entry[2], 3), false, true);
        }
        return added;
    }

    // ── storage: chest, mass storage, drawer, locker ───────────────────────

    private static int storage(List<Recipe<?>> recipes) {
        int added = 0;
        // GT6 Loader_MultiTileEntities:184, treated-plank bottle crate 8762.
        var woodBolts=GTItems.creativeEntries(MaterialPrefix.bolt).stream()
                .filter(entry->entry.material().has(com.gregtech.gregtech.api.material.MaterialProperty.WOOD))
                .map(entry->new ItemStack(entry.item().get())).toArray(ItemStack[]::new);
        added += register(recipes,"storage/bottle_crate",new String[]{"sfr","PGP","BPB"},
                Map.of('s',Ingredient.of(tool(GTToolType.SAW)), 'f',Ingredient.of(tool(GTToolType.FILE)),
                        'r',Ingredient.of(tool(GTToolType.SOFT_HAMMER)),
                        'P',Ingredient.of(ForgeRegistries.ITEMS.getValue(GregTech.id("planks_treated"))),
                        'G',Ingredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge","glue"))),
                        'B',Ingredient.of(woodBolts)),new ItemStack(GTStorage.BOTTLE_CRATE.get()));
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            GTMaterial material = spec.material();
            ItemStack plate = tagOrPlate("plates", material, MaterialPrefix.plate);
            ItemStack ring = tagOrPlate("rings", material, MaterialPrefix.ring);
            ItemStack stick = tagOrPlate("rods", material, MaterialPrefix.stick);
            ItemStack screw = tagOrPlate("screws", material, MaterialPrefix.screw);
            ItemStack casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, material);
            ItemStack chest = metalChest(spec);
            ItemStack storage = massStorage(spec);
            if(!screw.isEmpty()){
                ItemStack cable=GTWires.allCables().stream().map(net.minecraftforge.registries.RegistryObject::get)
                        .filter(wire->wire.spec().material()==Materials.Gold&&wire.spec().size()==4).findFirst().map(block->new ItemStack(block)).orElseThrow();
                added += register(recipes,"storage/charging_crafting_table_"+spec.suffix(),new String[]{"WTW","dMx","WTW"},
                        Map.of('W',Ingredient.of(cable),'T',Ingredient.of(screw),'M',Ingredient.of(new ItemStack(com.gregtech.gregtech.registry.GTMiscBlocks.advancedCraftingTable(material))),
                                'd',Ingredient.of(tool(GTToolType.SCREWDRIVER)),'x',Ingredient.of(tool(GTToolType.WIRE_CUTTER))),
                        new ItemStack(com.gregtech.gregtech.registry.GTMiscBlocks.chargingCraftingTable(material)));
            }
            if(!plate.isEmpty()&&!screw.isEmpty()){
                added += register(recipes,"storage/advanced_crafting_table_"+spec.suffix(),new String[]{"PdP","TWT","PCP"},
                        Map.of('P',Ingredient.of(plate),'T',Ingredient.of(screw),'W',Ingredient.of(Items.CRAFTING_TABLE),
                                'C',Ingredient.of(net.minecraftforge.common.Tags.Items.CHESTS),'d',Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        new ItemStack(com.gregtech.gregtech.registry.GTMiscBlocks.advancedCraftingTable(material)));
            }
            if (!screw.isEmpty() && !chest.isEmpty()) {
                added += register(recipes, "storage/drawer_quad_" + spec.suffix(),
                        new String[]{"CTC", "TdT", "CTC"},
                        Map.of('C', Ingredient.of(chest), 'T', Ingredient.of(screw),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))), new ItemStack(GTStorage.drawer(material)));
            }
            ItemStack thickPlate = GTItems.getStack(MaterialPrefix.plateQuintuple, material, 1);
            ItemStack smallGear = GTItems.getStack(MaterialPrefix.gearGtSmall, material, 1);
            ItemStack gear = GTItems.getStack(MaterialPrefix.gearGt, material, 1);
            if (!thickPlate.isEmpty() && !smallGear.isEmpty() && !gear.isEmpty() && !stick.isEmpty()) {
                for (boolean keyed : new boolean[]{false, true}) {
                    added += register(recipes, "storage/" + (keyed ? "key_safe_" : "safe_") + spec.suffix(),
                            new String[]{"PGP", keyed ? "OGS" : "GOS", "PGP"},
                            Map.of('P', Ingredient.of(thickPlate), 'G', Ingredient.of(smallGear), 'O', Ingredient.of(gear), 'S', Ingredient.of(stick)),
                            new ItemStack(GTStorage.safe(material, keyed)));
                }
            }
            if (!ring.isEmpty() && !stick.isEmpty()) {
                // GT6 Loader_MultiTileEntities:133, reinforced wooden chest.
                added += register(recipes, "storage/reinforced_wood_chest_" + spec.suffix(),
                        new String[]{"sSw", "RCR", "SSS"},
                        Map.of('S', Ingredient.of(stick), 'R', Ingredient.of(ring),
                                'C', Ingredient.of(net.minecraftforge.common.Tags.Items.CHESTS_WOODEN),
                                's', Ingredient.of(tool(GTToolType.SAW)), 'w', Ingredient.of(tool(GTToolType.WRENCH))),
                        new ItemStack(GTStorage.reinforcedChest(material)));
            }
            if (!plate.isEmpty() && !ring.isEmpty() && !stick.isEmpty() && !chest.isEmpty()) {
                // Loader_MultiTileEntities:132 - "sPw" / "RSR" / "PPP"
                added += register(recipes, "storage/chest_" + spec.suffix(),
                        new String[]{"sPw", "RSR", "PPP"},
                        Map.of('P', Ingredient.of(plate), 'R', Ingredient.of(ring), 'S', Ingredient.of(stick),
                                's', Ingredient.of(tool(GTToolType.SAW)),
                                'w', Ingredient.of(tool(GTToolType.WRENCH))),
                        chest.copy());
            }
            if (!screw.isEmpty() && !chest.isEmpty() && !casing.isEmpty() && !storage.isEmpty()) {
                // Loader_MultiTileEntities:141 - "TCT" / "wMd" / "TCT"
                added += register(recipes, "storage/mass_storage_" + spec.suffix(),
                        new String[]{"TCT", "wMd", "TCT"},
                        Map.of('T', Ingredient.of(screw), 'C', Ingredient.of(chest), 'M', Ingredient.of(casing),
                                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        storage.copy());
            }
            ItemStack logisticsStorage = logisticsMassStorage(spec);
            ItemStack genericBus = tech("generic_logistics_storage_bus");
            if (!screw.isEmpty() && !storage.isEmpty() && !genericBus.isEmpty()
                    && !logisticsStorage.isEmpty()) {
                // Loader_MultiTileEntities:142 - GT6 6200+aID, including OD_CIRCUITS[4].
                // Tier-four-or-higher circuits are exposed through a tag so other mods may opt in.
                added += register(recipes, "storage/logistics_mass_storage_" + spec.suffix(),
                        new String[]{"TQT", "wCd", "TMT"},
                        Map.of('T', Ingredient.of(screw), 'Q', Ingredient.of(genericBus),
                                'C', Ingredient.of(ItemTags.create(GregTech.id("circuits_tier_4_plus"))),
                                'M', Ingredient.of(storage),
                                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        logisticsStorage);
            }
        }
        // Loader_MultiTileEntities:140/138 - the drawer and locker use a metal chest as their core
        ItemStack anyChest = GTStorage.METAL_CHESTS.isEmpty() ? ItemStack.EMPTY
                : metalChest(GTStorageMetals.ALL.get(Math.min(1, GTStorageMetals.ALL.size() - 1)));
        if (GTStorage.LOCKER != null && !anyChest.isEmpty()) {
            ItemStack screw = GTItems.getStack(MaterialPrefix.screw, Materials.Steel, 1);
            ItemStack stick = GTItems.getStack(MaterialPrefix.stick, Materials.Steel, 1);
            ItemStack casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, Materials.Steel);
            added += register(recipes, "storage/locker",
                    new String[]{"SdS", "LCL", "TMT"},
                    Map.of('C', Ingredient.of(anyChest), 'S', Ingredient.of(stick), 'T', Ingredient.of(screw),
                            'L', Ingredient.of(new ItemStack(Items.LEATHER)), 'M', Ingredient.of(casing),
                            'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                    new ItemStack(GTStorage.LOCKER.get().asItem()));
        }
        return added;
    }

    private static ItemStack goldIndexUnused() { return ItemStack.EMPTY; }

    /** The per-material storage/chest blocks are registered in {@link GTStorageMetals#ALL} order. */
    private static ItemStack metalChest(GTStorageMetals.Spec spec) {
        int index = GTStorageMetals.ALL.indexOf(spec);
        if (index < 0 || index >= GTStorage.METAL_CHESTS.size()) return ItemStack.EMPTY;
        var holder = GTStorage.METAL_CHESTS.get(index);
        return holder.isPresent() ? new ItemStack(holder.get().asItem()) : ItemStack.EMPTY;
    }

    private static ItemStack massStorage(GTStorageMetals.Spec spec) {
        int index = GTStorageMetals.ALL.indexOf(spec);
        if (index < 0 || index >= GTStorage.MASS_STORAGES.size()) return ItemStack.EMPTY;
        var holder = GTStorage.MASS_STORAGES.get(index);
        return holder.isPresent() ? new ItemStack(holder.get().asItem()) : ItemStack.EMPTY;
    }

    private static ItemStack logisticsMassStorage(GTStorageMetals.Spec spec) {
        int index = GTStorageMetals.ALL.indexOf(spec);
        if (index < 0 || index >= GTStorage.LOGISTICS_MASS_STORAGES.size()) return ItemStack.EMPTY;
        var holder = GTStorage.LOGISTICS_MASS_STORAGES.get(index);
        return holder.isPresent() ? new ItemStack(holder.get().asItem()) : ItemStack.EMPTY;
    }

    // ── crystal processor socket (MultiItemTechnological:765) ──────────────

    private static int circuits(List<Recipe<?>> recipes) {
        ItemStack circuit = tech("circuit_ultimate");
        ItemStack plate = tech("circuit_plate_platinum");
        ItemStack laser = tech("laser_emitter_heliumneon");
        if (circuit.isEmpty() || plate.isEmpty() || laser.isEmpty()) return 0;
        return register(recipes, "circuit/crystal_processor_socket",
                new String[]{"CLC", "LBL", "CLC"},
                Map.of('C', Ingredient.of(circuit), 'L', Ingredient.of(laser), 'B', Ingredient.of(plate)),
                tech("crystal_processor_socket"));
    }

    private static ItemStack tech(String id) {
        var item = com.gregtech.gregtech.registry.GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ── battery cells (MultiItemTechnological:464-484) ─────────────────────

    /**
     * The five empty battery cells; the filled ones come from the Canning Machine
     * ({@code :396-403} style rows, registered in {@code ElectronicsRecipes}). GT6's {@code 'x'} keys are
     * wire cutters and {@code 'h'} hard hammers; the alkaline/NiCd cells use an iron wire, which the port
     * registers as its fine wire form.
     */
    private static int cells(List<Recipe<?>> recipes) {
        int added = 0;
        ItemStack alloy = GTItems.getStack(MaterialPrefix.plateCurved, Materials.BatteryAlloy, 1);
        if (alloy.isEmpty()) return 0;
        added += register(recipes, "cell/lead_acid",
                new String[]{" Fh", "FPF", "xF "},
                Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Lead")),
                        'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)),
                        'x', Ingredient.of(tool(GTToolType.WIRE_CUTTER))),
                tech("lead_acid_cell_empty"));

        ItemStack stainless = GTItems.getStack(MaterialPrefix.plateCurved, Materials.StainlessSteel, 1);
        Ingredient plasticRing = tagIngredient("forge:rings/plastic",
                GTItems.getStack(MaterialPrefix.ring, Materials.Plastic, 1));
        ItemStack ironWire = GTItems.getStack(MaterialPrefix.wireFine, Materials.Iron, 1);
        if (!stainless.isEmpty() && !ironWire.isEmpty()) {
            added += register(recipes, "cell/alkaline",
                    new String[]{"KSM", "OPF", "CWZ"},
                    Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Aluminium")),
                            'S', Ingredient.of(stainless), 'O', plasticRing,
                            'W', Ingredient.of(ironWire), 'C', Ingredient.of(dust("Carbon")),
                            'K', Ingredient.of(dust("KOH")), 'Z', Ingredient.of(dust("Zinc")),
                            'M', Ingredient.of(dust("MnO2"))),
                    tech("alkaline_button_cell_empty"));
            added += register(recipes, "cell/nickel_cadmium",
                    new String[]{"KSM", "OPF", "CWZ"},
                    Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Aluminium")),
                            'S', Ingredient.of(stainless), 'O', plasticRing,
                            'W', Ingredient.of(ironWire), 'C', Ingredient.of(stick("Graphite")),
                            'K', Ingredient.of(dust("KOH")), 'Z', Ingredient.of(curved("Nickel")),
                            'M', Ingredient.of(curved("Cadmium"))),
                    tech("nickel_cadmium_cell_empty"));
        }
        ItemStack plasticFoil = plate("Plastic");
        ItemStack lithium = dust("LiClO4");
        ItemStack chromium = curved("Chromium");
        if (!plasticFoil.isEmpty() && !lithium.isEmpty() && !chromium.isEmpty()) {
            added += register(recipes, "cell/lithium_cobalt",
                    new String[]{"CLF", "XSG", "FLP"},
                    Map.of('P', Ingredient.of(alloy), 'X', Ingredient.of(stick("Cobalt")),
                            'G', Ingredient.of(stick("Graphite")), 'L', Ingredient.of(lithium),
                            'S', Ingredient.of(chromium), 'F', Ingredient.of(plate("Plastic")),
                            'C', Ingredient.of(tech("circuit_elite"))),
                    tech("lithium_cobalt_cell_empty"));
            added += register(recipes, "cell/lithium_manganese",
                    new String[]{"CLF", "XSG", "FLP"},
                    Map.of('P', Ingredient.of(alloy), 'X', Ingredient.of(stick("Manganese")),
                            'G', Ingredient.of(stick("Graphite")), 'L', Ingredient.of(lithium),
                            'S', Ingredient.of(chromium), 'F', Ingredient.of(plate("Plastic")),
                            'C', Ingredient.of(tech("circuit_ultimate"))),
                    tech("lithium_manganese_cell_empty"));
        }
        return added;
    }

    private static Ingredient tagIngredient(String tag, ItemStack fallback) {
        if (!fallback.isEmpty()) return Ingredient.of(fallback);
        return Ingredient.of(ItemTags.create(ResourceLocation.parse(tag)));
    }

    private static ItemStack plate(String material) {
        return materialItem(MaterialPrefix.plate, material);
    }

    private static ItemStack curved(String material) {
        return materialItem(MaterialPrefix.plateCurved, material);
    }

    private static ItemStack stick(String material) {
        return materialItem(MaterialPrefix.stick, material);
    }

    private static ItemStack dust(String material) {
        return materialItem(MaterialPrefix.dust, material);
    }

    private static ItemStack materialItem(MaterialPrefix prefix, String material) {
        var resolved = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material);
        return resolved == null ? ItemStack.EMPTY : GTItems.getStack(prefix, resolved, 1);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static ItemStack plainPipe(GTMaterial material, ItemPipeSpec.ItemPipeSize size) {
        PipeSpec.PipeSize wanted = switch (size) {
            case RESTRICTIVE_MEDIUM -> PipeSpec.PipeSize.MEDIUM;
            case RESTRICTIVE_LARGE -> PipeSpec.PipeSize.LARGE;
            default -> PipeSpec.PipeSize.HUGE;
        };
        for (var holder : GTFluidPipes.all()) {
            if (!holder.isPresent()) continue;
            var spec = holder.get().spec();
            if (spec.material() == material && spec.size() == wanted) {
                return new ItemStack(holder.get().asItem());
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack steelRing() {
        ItemStack ring = GTItems.getStack(MaterialPrefix.ring, Materials.Steel, 1);
        if (!ring.isEmpty()) return ring;
        Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("forge", "steel_ingot"));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack plate(GTMaterial material) {
        return GTItems.getStack(MaterialPrefix.plate, material, 1);
    }

    private static ItemStack tagOrPlate(String group, GTMaterial material, MaterialPrefix prefix) {
        ItemStack direct = GTItems.getStack(prefix, material, 1);
        return direct;
    }

    private static ItemStack wireItem(String family, int size) {
        for (var holder : GTWires.allWires()) {
            if (!holder.isPresent()) continue;
            var spec = holder.get().spec();
            if (spec.id().equals(family) && spec.size() == size) return new ItemStack(holder.get().asItem());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tool(GTToolType type) {
        var item = GTToolItems.get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Registers one shaped recipe, wrapped so GT tools wear like in the original. */
    private static int register(List<Recipe<?>> recipes, String path, String[] pattern,
                               Map<Character, Ingredient> key, ItemStack output) {
        return register(recipes, path, pattern, key, output, false);
    }

    private static int register(List<Recipe<?>> recipes, String path, String[] pattern,
                               Map<Character, Ingredient> key, ItemStack output, boolean creosote) {
        return register(recipes, path, pattern, key, output, creosote, false);
    }

    private static int register(List<Recipe<?>> recipes, String path, String[] pattern,
                               Map<Character, Ingredient> key, ItemStack output, boolean creosote,
                               boolean allowMirror) {
        if (output.isEmpty()) return 0;
        int height = pattern.length;
        int width = pattern[0].length();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                if (symbol == ' ') continue;
                Ingredient ingredient = key.get(symbol);
                if (ingredient == null) return 0;                  // incomplete pattern: skip the row
                ingredients.set(x + y * width, ingredient);
            }
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "hand/" + path);
        ShapedRecipe base = new ShapedRecipe(id, "gt.hand", CraftingBookCategory.MISC,
                width, height, ingredients, output.copy());
        // GT6's registration patterns are CR.DEF_REV style, i.e. without MIR (CR.java:161-163):
        // 1.20.1 mirrors shaped recipes by default, so the flag has to be stated.
        recipes.add(creosote ? new CreosoteAxleRecipe(base) : new ToolShapedRecipe(base, allowMirror));
        REGISTERED.add(id.toString());
        return 1;
    }
}
