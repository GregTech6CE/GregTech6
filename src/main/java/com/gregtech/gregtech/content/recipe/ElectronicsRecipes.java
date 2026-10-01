package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** GT6 MultiItemTechnological circuit progression and Loader_Recipes_Other crystal growth.
 * Canonical copper/rubber materials are used; optional-mod substitutions remain separate work.
 */
public final class ElectronicsRecipes {
    public static final String[] TIERS = {"basic", "good", "advanced", "elite", "master", "ultimate"};
    private static final List<Recipe> REGISTERED = new ArrayList<>();
    private ElectronicsRecipes() {}
    public static List<Recipe> recipes() { return Collections.unmodifiableList(REGISTERED); }
    public static int register() {
        if (!REGISTERED.isEmpty()) throw new IllegalStateException("Electronics recipes registered twice");
        for (String quartz : new String[]{"NetherQuartz", "CertusQuartz", "MilkyQuartz"})
            press(64, tech("circuit_plate", 1), mat(MaterialPrefix.plate,"Plastic",1), mat(MaterialPrefix.dust,quartz,1));
        String[] conductors = {"Copper", "Gold", "Platinum"};
        String[] wiring = {"circuit_wiring_copper", "circuit_wire_gold", "circuit_wire_platinum"};
        for (int i=0;i<3;i++) {
            add(MachineRecipeMaps.LaserEngraver, new Recipe(new ItemStack[]{mat(MaterialPrefix.foil,conductors[i],4),mat(MaterialPrefix.lens,"Ruby",1)},
                    new ItemStack[]{tech(wiring[i],1)},null,null,null,null,64,16,0).withCatalystInputs(1));
            press(64,tech("circuit_plate_"+conductors[i].toLowerCase(Locale.ROOT),1),tech("circuit_plate",1),tech(wiring[i],1));
        }
        for (String semiconductor : new String[]{"Silicon","Germanium","RedstoneAlloy"}) {
            var crystal=mat(MaterialPrefix.plateGemTiny,semiconductor,1);
            press(16,tech("circuit_part_basic",1),mat(MaterialPrefix.wireFine,"Copper",1),mat(MaterialPrefix.dustTiny,"Redstone",1),crystal);
            press(16,tech("circuit_part_basic",9),mat(MaterialPrefix.wireFine,"Copper",9),mat(MaterialPrefix.dust,"Redstone",1),mat(MaterialPrefix.plateGemTiny,semiconductor,9));
            for (String redWire : new String[]{"Signalum","RedAlloy"})
                press(16,tech("circuit_part_good",1),mat(MaterialPrefix.wireFine,"Copper",1),mat(MaterialPrefix.wireFine,redWire,1),crystal.copy());
            boolean alloy=semiconductor.equals("RedstoneAlloy");
            press(16,tech("circuit_part_"+(alloy?"elite":"advanced"),1),mat(MaterialPrefix.wireFine,"Gold",1),mat(MaterialPrefix.wireFine,"Signalum",1),crystal.copy());
            press(16,tech("circuit_part_"+(alloy?"ultimate":"master"),1),mat(MaterialPrefix.wireFine,"Platinum",1),mat(MaterialPrefix.wireFine,"Signalum",1),crystal.copy());
        }
        for (int metal=0;metal<3;metal++) for (int tier=0;tier<6;tier++) {
            int capped=Math.min(tier,metal*2+1);
            press(64,tech("circuit_board_"+TIERS[capped],1),tech("circuit_plate_"+conductors[metal].toLowerCase(Locale.ROOT),1),tech("circuit_part_"+TIERS[tier],4));
        }
        String[] solders={"Lead","Tin","SolderingAlloy"};
        int[][] results={{0,0,0,1,2,3},{0,1,1,2,3,4},{0,1,2,3,4,5}};
        for(int solder=0;solder<3;solder++) for(int tier=0;tier<6;tier++)
            add(MachineRecipeMaps.Bath,new Recipe(new ItemStack[]{tech("circuit_board_"+TIERS[tier],1)},new ItemStack[]{tech("circuit_"+TIERS[results[solder][tier]],1)},
                    null,null,new FluidStack[]{fluid("GenMolten_",solders[solder],72)},null,64,0,0));
        for (String crystal : new String[]{"Silicon","Germanium","RedstoneAlloy","NikolineAlloy"})
            for (String gas : new String[]{"Helium","Neon","Argon","Krypton","Xenon","Radon"})
                for (int batch : new int[]{1,9})
                    add(MachineRecipeMaps.CrystallisationCrucible,new Recipe(new ItemStack[]{mat(batch==1?MaterialPrefix.dustTiny:MaterialPrefix.dust,crystal,1)},
                            new ItemStack[]{mat(MaterialPrefix.bouleGt,crystal,batch)},null,null,
                            new FluidStack[]{fluid("GenGas_",gas,1000*batch),fluid("GenMolten_",crystal,560*batch)},null,72000L*batch,16,0));
        // RecipeMapHandlerMaterial: 144 ticks per material unit, preserving each form.
        MaterialPrefix[] forms={MaterialPrefix.ingot,MaterialPrefix.stick,MaterialPrefix.stickLong,MaterialPrefix.bolt};
        int[] ticks={144,72,144,18};
        for(String metal:new String[]{"Iron","Steel","Neodymium"}) for(int i=0;i<forms.length;i++)
            add(MachineRecipeMaps.Polarizer,new Recipe(new ItemStack[]{mat(forms[i],metal,1)},new ItemStack[]{mat(forms[i],metal+"Magnetic",1)},
                    null,null,null,null,ticks[i],metal.equals("Neodymium")?128:16,0));

        // GT6 MultiItemTechnological:571-698 + :739-743 — the magical/enderium/signalum branch of the
        // circuit chain. The rows above cover GT6's copper/gold/platinum ladder; these were the items the
        // port registered with no way to make them (see docs/items-without-recipes.json).
        String[] magicWires={"Thaumium","Manasteel","Mithril","Netherite"};
        for(String wire:magicWires) engrave("circuit_wire_magic",wire);
        engrave("circuit_wire_enderium","Enderium");
        engrave("circuit_wire_signalum","Signalum");

        press(64,tech("circuit_plate_magic",1),tech("circuit_plate",1),tech("circuit_wire_magic",1));
        press(64,tech("circuit_plate_enderium",1),tech("circuit_plate",1),tech("circuit_wire_enderium",1));
        press(64,tech("circuit_plate_signalum",1),tech("circuit_plate",1),tech("circuit_wire_signalum",1));
        press(64,tech("circuit_plate_hsla",1),mat(MaterialPrefix.plate,"HSLASteel",1),tech("circuit_wire_gold",1));

        for(String semiconductor:new String[]{"Silicon","Germanium","RedstoneAlloy"}) {
            var crystal=mat(MaterialPrefix.plateGemTiny,semiconductor,1);
            for(String wire:magicWires)
                press(16,tech("circuit_part_magic",1),mat(MaterialPrefix.wireFine,wire,1),mat(MaterialPrefix.wireFine,"Signalum",1),crystal.copy());
            press(16,tech("circuit_part_enderium",1),mat(MaterialPrefix.wireFine,"Enderium",1),mat(MaterialPrefix.wireFine,"Signalum",1),crystal.copy());
            press(16,tech("circuit_part_signalum",1),mat(MaterialPrefix.wireFine,"Signalum",1),mat(MaterialPrefix.dustTiny,"Redstone",1),crystal.copy());
            press(16,tech("circuit_part_signalum",9),mat(MaterialPrefix.wireFine,"Signalum",9),mat(MaterialPrefix.dust,"Redstone",1),mat(MaterialPrefix.plateGemTiny,semiconductor,9));
        }

        press(64,tech("circuit_board_magic",1),tech("circuit_plate_magic",1),tech("circuit_part_magic",4));
        press(64,tech("circuit_board_enderium",1),tech("circuit_plate_enderium",1),tech("circuit_part_enderium",4));
        press(64,tech("circuit_board_signalum",1),tech("circuit_plate_signalum",1),tech("circuit_part_signalum",4));
        press(64,tech("circuit_board_hsla_circuit",1),tech("circuit_plate_hsla",1),tech("circuit_part_enderpearl",4));
        press(64,tech("circuit_board_power_module",1),tech("circuit_plate_hsla",1),tech("circuit_part_endereye",4));

        bath("circuit_board_magic","circuit_magic","SolderingAlloy");
        bath("circuit_board_enderium","circuit_enderium","SolderingAlloy");
        for(String solder:new String[]{"Lead","Tin","SolderingAlloy"}) bath("circuit_board_signalum","circuit_signalum",solder);

        // GT6 Loader_Recipes_Other:147-155 + MultiItemTechnological:765-770 — the crystal branch:
        // a gem plate is engraved into a crystal circuit, the socket is crafted from a platinum circuit
        // plate, two ultimate circuits and two helium-neon laser emitters, and the Press turns
        // socket + circuit into a processor.
        crystal("crystal_circuit_diamond", new String[]{"Diamond"}, com.gregtech.gregtech.data.MaterialGroups.Diamond);
        crystal("crystal_circuit_emerald", new String[]{"Emerald"}, com.gregtech.gregtech.data.MaterialGroups.Emerald);
        crystal("crystal_circuit_sapphire", new String[]{"Sapphire", "GreenSapphire"}, com.gregtech.gregtech.data.MaterialGroups.Sapphire);
        crystal("crystal_circuit_ruby", new String[]{"Ruby"}, null);   // GT6: plateGem(MT.Ruby) only
        for (String gem : new String[]{"Diamond", "Ruby", "Emerald", "Sapphire"}) {
            press(16, tech("crystal_processor_" + gem.toLowerCase(Locale.ROOT), 1),
                    tech("crystal_processor_socket", 1), tech("crystal_circuit_" + gem.toLowerCase(Locale.ROOT), 1));
        }
        return REGISTERED.size();
    }

    /**
     * GT6 {@code Loader_Recipes_Other:147-155}: the Laser Engraver burns a gem plate into a crystal
     * circuit with a lens as the (unconsumed) catalyst. GT6 loops over every material that re-registers
     * to the gem group ({@code ANY.Diamond.mToThis} and friends); the port names those materials
     * directly and adds the group's re-registrations on top.
     */
    private static void crystal(String circuit, String[] names,
                                com.gregtech.gregtech.api.material.GTMaterial group) {
        java.util.Set<com.gregtech.gregtech.api.material.GTMaterial> gems = new java.util.LinkedHashSet<>();
        for (String name : names) {
            var material = GTMaterialRegistry.get(name);
            if (material != null && material.isValid()) gems.add(material.resolve());
        }
        if (group != null) {
            gems.add(group.resolve());
            gems.addAll(group.getReRegistrations());
        }
        for (var gem : gems) {
            if (gem == null || !gem.isValid()) continue;
            ItemStack plate = mat(MaterialPrefix.plateGem, gem.getName(), 1);
            if (plate.isEmpty()) continue;
            add(MachineRecipeMaps.LaserEngraver, new Recipe(
                    new ItemStack[]{plate, mat(MaterialPrefix.lens, "Ruby", 1)},
                    new ItemStack[]{tech(circuit, 1)}, null, null, null, null, 64, 256, 0)
                    .withCatalystInputs(1));
        }
    }

    /** GT6 {@code Loader_Recipes_Other:158-166}: four foils and a lens engrave a circuit wire. */
    private static void engrave(String wire,String foilMaterial) {
        add(MachineRecipeMaps.LaserEngraver,new Recipe(
                new ItemStack[]{mat(MaterialPrefix.foil,foilMaterial,4),mat(MaterialPrefix.lens,"Ruby",1)},
                new ItemStack[]{tech(wire,1)},null,null,null,null,64,16,0).withCatalystInputs(1));
    }

    /** GT6 {@code MultiItemTechnological:720-743}: soldering a circuit board into a finished circuit. */
    private static void bath(String board,String circuit,String solder) {
        add(MachineRecipeMaps.Bath,new Recipe(new ItemStack[]{tech(board,1)},
                new ItemStack[]{tech(circuit,1)},null,null,
                new FluidStack[]{fluid("GenMolten_",solder,72)},null,64,0,0));
    }
    private static void press(long ticks,ItemStack output,ItemStack... inputs) {
        add(MachineRecipeMaps.Press,new Recipe(inputs,new ItemStack[]{output},null,null,null,null,ticks,16,0));
    }
    private static void add(RecipeMap map,Recipe recipe) {
        if(map.addRecipe(recipe)==null) throw new IllegalStateException("Rejected electronics recipe in "+map);
        REGISTERED.add(recipe);
    }
    private static ItemStack tech(String id,int count) {
        var item=GTTechnological.get(id);
        if(item==null)throw new IllegalStateException("Missing electronics item: "+id);
        return new ItemStack(item,count);
    }
    private static ItemStack mat(MaterialPrefix prefix,String material,int count) {
        var stack=GTItems.getStack(prefix,GTMaterialRegistry.get(material),count);
        if(stack.isEmpty())throw new IllegalStateException("Missing electronics material: "+prefix.getName()+"/"+material);
        return stack;
    }
    private static FluidStack fluid(String prefix,String material,int amount) {
        var fluid=GTFluids.still(prefix+GTMaterialRegistry.get(material).getName());
        if(fluid==null||!fluid.isPresent())throw new IllegalStateException("Missing electronics fluid: "+prefix+material);
        return new FluidStack(fluid.get(),amount);
    }
}
