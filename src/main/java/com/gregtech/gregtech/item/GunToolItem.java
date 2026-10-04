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

package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.tool.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Port of GregTech-6 Team Behavior_Gun: finite magazine, material bullets and server hitscan. */
public final class GunToolItem extends GTToolItem {
 public GunToolItem(Properties properties,GTToolType type){super(properties,type);}
 public GunRules.Definition definition(){return GunRules.of(toolType().id());}
 public ItemStack ammunition(ItemStack gun,Level level){return gun.hasTag()?ItemStack.of(gun.getTag().getCompound("gt.ammo")):ItemStack.EMPTY;}
 private void magazine(ItemStack gun,ItemStack ammo,Level level){var tag=gun.getOrCreateTag();if(ammo.isEmpty())tag.remove("gt.ammo");else tag.put("gt.ammo",ammo.save(new CompoundTag()));}
 public boolean accepts(ItemStack stack){return stack.getItem() instanceof MaterialFormItem form&&form.getPrefix()==definition().ammunition()&&!form.getMaterial().getName().equals("Empty");}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){var player=context.getPlayer();if(player==null)return InteractionResult.PASS;activate(context.getLevel(),player,context.getHand());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
 @Override public InteractionResult useOn(UseOnContext context){var player=context.getPlayer();if(player==null)return InteractionResult.PASS;activate(context.getLevel(),player,context.getHand());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){activate(level,player,hand);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
 public void activate(Level level,Player player,InteractionHand hand){
  if(level.isClientSide||player.isSpectator()||player.getCooldowns().isOnCooldown(this))return;
  var gun=player.getItemInHand(hand);if(!GTToolHelper.isUsable(gun))return;
  var ammo=ammunition(gun,level);
  if(player.isShiftKeyDown()){
   if(ammo.isEmpty())reload(gun,player);else {give(player,ammo);magazine(gun,ItemStack.EMPTY,level);}
   level.playSound(null,player.blockPosition(),SoundEvents.UI_BUTTON_CLICK.value(),SoundSource.PLAYERS,.5F,1);player.getInventory().setChanged();return;
  }
  if(ammo.isEmpty()){level.playSound(null,player.blockPosition(),SoundEvents.UI_BUTTON_CLICK.value(),SoundSource.PLAYERS,.5F,1);return;}
  shoot(gun,ammo.copyWithCount(1),player);
  level.playSound(null,player.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST_FAR,SoundSource.PLAYERS,4F,1);
  if(!player.getAbilities().instabuild&&level.random.nextInt(1+enchantment(gun,"infinity",level))==0){ammo.shrink(1);magazine(gun,ammo,level);give(player,GTItems.getStack(MaterialPrefix.scrapGt,com.gregtech.gregtech.content.material.Materials.Brass,definition().magic()*8));}
  GTToolHelper.damageForUse(gun,100,player);player.getInventory().setChanged();
 }
 private static void give(Player player,ItemStack stack){if(!stack.isEmpty()&&!player.addItem(stack))player.drop(stack,false);}
 public boolean reload(ItemStack gun,Player player){
  if(!ammunition(gun,player.level()).isEmpty())return false;
  var inventory=player.getInventory();int selected=inventory.selected;var order=new ArrayList<Integer>();
  // Original prefers ammunition in the same inventory column, then searches backwards.
  if(inventory.getItem(selected)==gun)for(int offset:new int[]{9,18,27})if(selected+offset<36)order.add(selected+offset);
  for(int slot=35;slot>=0;slot--)if(!order.contains(slot))order.add(slot);
  for(int slot:order){var stock=inventory.getItem(slot);if(!accepts(stock))continue;int count=Math.min(definition().magazine(),stock.getCount());magazine(gun,enchantedAmmo(stock.copyWithCount(count),player.level()),player.level());stock.shrink(count);inventory.setChanged();return true;}
  return false;
 }
 private static int enchantment(ItemStack stack,String name,Level level){var enchant=net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.parse(name.contains(":")?name:"minecraft:"+name));return enchant==null?0:net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchant,stack);}
 private static int ammoMultiplier(ItemStack ammo){var prefix=((MaterialFormItem)ammo.getItem()).getPrefix();return prefix==MaterialPrefix.bulletGtLarge?3:prefix==MaterialPrefix.bulletGtMedium?2:1;}
 private static ItemStack enchantedAmmo(ItemStack ammo,Level level){
  var values=new java.util.LinkedHashMap<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(ammo));
  for(var entry:MaterialToolEnchantments.ammunition(((MaterialFormItem)ammo.getItem()).getMaterial()).entrySet()){
   var enchant=net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(MaterialToolEnchantments.id(entry.getKey())));
   if(enchant!=null)values.merge(enchant,entry.getValue()*(entry.getKey().equals("looting")?ammoMultiplier(ammo):1),Math::max);
  }
  net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(values,ammo);return ammo;
 }
 public void shoot(ItemStack gun,ItemStack ammo,Player player){
  if(!(player.level() instanceof ServerLevel level)||!accepts(ammo))return;
  ammo=enchantedAmmo(ammo,level);
  var form=(MaterialFormItem)ammo.getItem();var material=form.getMaterial();var primary=GTToolHelper.getHead(gun);
  Vec3 start=player.getEyePosition(),direction=player.getLookAngle(),end=start.add(direction.scale(200));
  var targets=new ArrayList<Entity>(level.getEntities(player,new AABB(start,end).inflate(2),e->e.isAlive()&&!e.isSpectator()&&e.isPickable()&&e.getBoundingBox().clip(start,end).isPresent()));
  targets.sort(Comparator.comparingDouble(e->e.getBoundingBox().clip(start,end).orElse(end).distanceToSqr(start)));
  long power=definition().power()+2000L*enchantment(gun,"power",level);
  int fire=enchantment(gun,"flame",level)+Math.max(enchantment(ammo,"fire_aspect",level),MaterialToolEnchantments.ammunition(material).getOrDefault("fire_aspect",0));
  boolean water=!level.getFluidState(BlockPos.containing(start)).isEmpty();BlockPos previous=BlockPos.containing(start);
  // Traverse distinct voxels in ray order. Sampling <= 1/16 block resolves thin panes and entities.
  for(int step=1;step<=3200&&power>0;step++){
   Vec3 point=start.add(direction.scale(step/16.0));BlockPos pos=BlockPos.containing(point);
   if(!level.hasChunkAt(pos))break;
   while(!targets.isEmpty()&&targets.get(0).getBoundingBox().clip(start,end).orElse(end).distanceToSqr(start)<=point.distanceToSqr(start)){
    var target=targets.remove(0);if(hit(gun,ammo,player,target,power,direction,fire))power-=10000;if(power<=0)return;
   }
   if(pos.equals(previous))continue;previous=pos;var state=level.getBlockState(pos);var block=state.getBlock();
   if(!state.getFluidState().isEmpty()){if(!water)power=GunRules.water(power);water=true;continue;}water=false;
   boolean glass=block instanceof HalfTransparentBlock||block instanceof IceBlock||block instanceof RedstoneLampBlock;
   int fragile=glass?2000:block instanceof PumpkinBlock||block==Blocks.MELON||block instanceof CactusBlock?3000:block==Blocks.COCOA?2000:0;
   if(fragile>0){if(player.mayBuild()&&level.mayInteract(player,pos)&&!state.hasBlockEntity()){
     if(glass){level.destroyBlock(pos,false,player);giveScrap(level,pos,state);}
     else level.destroyBlock(pos,true,player);
    }power-=fragile;continue;}
   if(state.is(BlockTags.WOOL)||state.is(BlockTags.WOOL_CARPETS)){power-=4000;if(fire>1&&player.mayBuild()&&level.mayInteract(player,pos))level.destroyBlock(pos,false,player);continue;}
   if(state.isAir()||state.is(BlockTags.LEAVES)||state.is(BlockTags.FENCES)||state.is(BlockTags.FENCE_GATES)||state.is(BlockTags.RAILS)||block instanceof BushBlock||block instanceof VineBlock||block instanceof IronBarsBlock||block instanceof TorchBlock||block instanceof FireBlock||block instanceof com.gregtech.gregtech.block.misc.BarsBlock||block==Blocks.SPAWNER||block==Blocks.COBWEB){power-=200;continue;}
   if(state.getCollisionShape(level,pos).clip(start,end,pos)!=null)return;
   power-=200;
  }
 }
 private static void giveScrap(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
  var data=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(new ItemStack(state.getBlock()));
  if(data.isPresent())for(var component:data.get().components()){long amount=component.amount()/com.gregtech.gregtech.api.material.GTValues.U72;while(amount>0){int count=(int)Math.min(64,amount);Block.popResource(level,pos,GTItems.getStack(MaterialPrefix.scrapGt,component.material(),count));amount-=count;}}
 }
 private boolean hit(ItemStack gun,ItemStack ammo,Player shooter,Entity target,long power,Vec3 direction,int fire){
  var level=shooter.level();if(target.isInvulnerable()||target instanceof Player p&&(p.getAbilities().invulnerable||!shooter.canHarmPlayer(p)))return false;
  if(target instanceof EnderMan enderman&&!enderman.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)&&enchantment(ammo,"gregtech:disjunction",level)<=0){ // Native projectile immunity also performs its teleport.
   return target.hurt(level.damageSources().thrown(new net.minecraft.world.entity.projectile.Snowball(level,shooter),shooter),0);
  }
  if(!(target instanceof LivingEntity||target instanceof net.minecraft.world.entity.boss.EnderDragonPart||target instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal))return false;
  var bullet=((MaterialFormItem)ammo.getItem()).getMaterial();float damage=GunRules.damage(definition(),GTToolHelper.getHead(gun),bullet,power,target instanceof Player);
  Entity attacker=shooter;
  int looting=Math.max(enchantment(ammo,"looting",level),MaterialToolEnchantments.ammunition(bullet).getOrDefault("looting",0));
  if(looting>0&&level instanceof ServerLevel server&&!(target instanceof Player)){var fake=net.minecraftforge.common.util.FakePlayerFactory.get(server,new com.mojang.authlib.GameProfile(new java.util.UUID(0,0),shooter.getGameProfile().getName()));fake.setItemInHand(InteractionHand.MAIN_HAND,ammo);fake.moveTo(shooter.getX(),shooter.getY(),shooter.getZ());attacker=fake;}
  var key=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("gregtech",power>25000?"bullet_piercing":"bullet"));
  var source=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),shooter,attacker);
  if(fire>0)target.setSecondsOnFire(4*fire);
  float magic=target instanceof LivingEntity living?net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(ammo,living.getMobType()):enchantment(ammo,"gregtech:disjunction",level);
  damage+=magic*(target instanceof Player?.5F:definition().magic());
  boolean struck=target.hurt(source,damage);
  if(struck){int knock=enchantment(gun,"punch",level)+Math.max(enchantment(ammo,"knockback",level),MaterialToolEnchantments.ammunition(bullet).getOrDefault("knockback",0));if(knock>0)target.push(direction.x*knock*power/50000,.05,direction.z*knock*power/50000);if(target instanceof Creeper creeper&&fire>0)creeper.ignite();}
  return struck||target.invulnerableTime<=0;
 }
 @Override public void appendHoverText(ItemStack stack,Level context,List<net.minecraft.network.chat.Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);
  tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.gun.reload"));
  tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.gun.caliber",net.minecraft.network.chat.Component.translatable("item.gregtech."+definition().ammunition().getRegistryName()),definition().magazine()));
  var ammo=ammunition(stack,context);tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.gun.ammo",ammo.getCount(),definition().magazine()));if(!ammo.isEmpty())tooltip.add(ammo.getHoverName());
 }
}
