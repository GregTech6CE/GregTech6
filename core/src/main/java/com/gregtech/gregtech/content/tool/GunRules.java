/**
 * Copyright (c) 2021 GregTech-6 Team
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

package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
/** GregTech-6 Team's Behavior_Gun magazine and material ballistics (LGPL-3.0-or-later). */
public final class GunRules {
 private GunRules(){}
 public record Definition(MaterialPrefix ammunition,int magazine,long power,int magic){}
 public static Definition of(String id){return switch(id){
  case "pistol"->new Definition(MaterialPrefix.bulletGtSmall,16,10000,1);
  case "carbine"->new Definition(MaterialPrefix.bulletGtMedium,8,17500,2);
  case "rifle"->new Definition(MaterialPrefix.bulletGtLarge,4,25000,3);
  default->throw new IllegalArgumentException("Unknown gun: "+id);};}
 public static long water(long power){return power>10000?0:power/2;}
 /** Bullet metal is one, two or three ninths of U; brass/gunpowder are separate byproducts. */
 public static float damage(Definition gun,GTMaterial weapon,GTMaterial bullet,long power,boolean player){
  float mass=(float)(bullet.getDensity()*111.111111*(gun.magic()/9.0)/50.0);
  float damage=Math.min(2F,power/5000F)*Math.max(0,weapon.getToolQuality()*.5F+mass);
  return player?damage/2:damage;
 }
}
