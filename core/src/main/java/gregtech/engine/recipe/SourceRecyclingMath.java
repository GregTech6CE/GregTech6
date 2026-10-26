/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

// Bounded arithmetic adaptation; original UT.Code.units and RecyclingProcessing source provenance in thermal-source-draft.json.
package gregtech.engine.recipe;
/** Source RecyclingProcessing arithmetic for a single reviewed material portion and non-lava liquid.
 * Generic multi-component collection, lava energy and source eligibility live outside this bounded rule.
 * Source: Loader_OreProcessing240-291; OM324; OreDictMaterial1391; UT.Code.units1677.
 */
public final class SourceRecyclingMath {
 private SourceRecyclingMath(){}
 public record Result(long targetUnits,int millibuckets,long ticks){}
 public static Result singleLiquid(long inputUnits,long sourceUnit,long targetPerUnit,long liquidMbPerUnit,
    double inputDensity,long targetMelting,long liquidTemperature,long environmentTemperature){
  if(inputUnits<0||sourceUnit<=0||targetPerUnit<0||liquidMbPerUnit<0||!Double.isFinite(inputDensity)||inputDensity<0)
   throw new IllegalArgumentException("Unreviewed thermal arithmetic inputs");
  long target=unitsDown(inputUnits,sourceUnit,targetPerUnit);
  long mb=unitsDown(target,sourceUnit,liquidMbPerUnit);
  // Do not use the INPUT melting point, fluid quantity as mass, or integerized density.
  double weight=(inputDensity*111.111111*inputUnits)/sourceUnit;
  long ticks=(long)Math.max(16,(weight*(Math.max(targetMelting,liquidTemperature)-environmentTemperature))/1600);
  return new Result(target,Math.toIntExact(mb),ticks);
 }
 private static long unitsDown(long amount,long original,long target){
  if(target==0)return 0;
  if(original==target||original==0)return amount;
  if(original%target==0){original/=target;target=1;}
  else if(target%original==0){target/=original;original=1;}
  // Reject unreviewed overflow rather than silently invent a saturated quantity.
  return Math.max(0,Math.multiplyExact(amount,target)/original);
 }
}
