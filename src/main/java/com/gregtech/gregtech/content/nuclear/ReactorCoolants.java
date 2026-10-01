package com.gregtech.gregtech.content.nuclear;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluid;
/** Registry boundary over the shared original coolant table. */
public enum ReactorCoolants {
 DISTILLED(ReactorCoolantRules.DISTILLED),
 INDUSTRIAL(ReactorCoolantRules.INDUSTRIAL),
 TIN(ReactorCoolantRules.TIN),
 SODIUM(ReactorCoolantRules.SODIUM),
 SEMIHEAVY(ReactorCoolantRules.SEMIHEAVY),
 HEAVY(ReactorCoolantRules.HEAVY),
 TRITIATED(ReactorCoolantRules.TRITIATED),
 LITHIUM_CHLORIDE(ReactorCoolantRules.LITHIUM_CHLORIDE),
 CO2(ReactorCoolantRules.CO2),
 HELIUM(ReactorCoolantRules.HELIUM),
 THORIUM_SALT(ReactorCoolantRules.THORIUM_SALT);
 private final ReactorCoolantRules rules;
 public final String input,output;public final int heat,expansion,heatDivider;public final boolean moderates;
 ReactorCoolants(ReactorCoolantRules r){rules=r;input=r.input;output=r.output;heat=r.heat;expansion=r.expansion;heatDivider=r.heatDivider;moderates=r.moderates;}
 public Fluid inputFluid(){return resolve(input);}public Fluid outputFluid(){return resolve(output);}
 private static Fluid resolve(String field){var holder=GTFluids.still(field);return holder!=null&&holder.isPresent()?holder.get():null;}
 public static ReactorCoolants of(FluidStack stack){if(stack.isEmpty())return null;for(var c:values())if(c.inputFluid()==stack.getFluid())return c;return null;}
 public int self(ReactorRodCatalog.Rod r){return rules.self(r);}public int emission(ReactorRodCatalog.Rod r){return rules.emission(r);}public int divisor(ReactorRodCatalog.Rod r){return rules.divisor(r);}public int maximum(ReactorRodCatalog.Rod r){return rules.maximum(r);}
}
