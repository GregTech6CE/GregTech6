package com.gregtech.gregtech.content.nuclear;
import com.gregtech.gregtech.item.FuelRodItem;
import com.gregtech.gregtech.registry.GTFuelRods;
import net.minecraft.world.item.ItemStack;
/** GT6 rod rules separated from world scheduling, item registration and container storage. */
public final class RodPhysics {
    private RodPhysics(){}
    public static ReactorRodCatalog.Rod definition(ItemStack stack){return stack.getItem() instanceof FuelRodItem rod?rod.definition():null;}
    public static long life(ItemStack stack){
        var d=definition(stack);if(d==null)return 0;var tag=stack.getTag();
        if(tag==null)return d.life();
        if(tag.contains("gt.reactor_life"))return Math.max(0,tag.getLong("gt.reactor_life"));
        if(tag.getBoolean(FuelRodItem.TAG_DEPLETED))return 0;
        if(tag.contains(FuelRodItem.TAG_HEALTH)&&tag.getInt(FuelRodItem.TAG_MAX_HEALTH)>0)
            return (long)(d.life()*Math.min(1D,Math.max(0D,(double)tag.getInt(FuelRodItem.TAG_HEALTH)/tag.getInt(FuelRodItem.TAG_MAX_HEALTH))));
        return d.life();
    }
    public static boolean moderated(ItemStack stack){var d=definition(stack);return d!=null&&(d.kind()==ReactorRodCatalog.Kind.MODERATOR||stack.hasTag()&&stack.getTag().getBoolean("gt.moderated"));}
    public static void moderate(ItemStack stack){if(definition(stack)!=null&&definition(stack).kind()==ReactorRodCatalog.Kind.NUCLEAR)stack.getOrCreateTag().putBoolean("gt.moderated_next",true);}
    public static int bound(long n){return RodPhysicsMath.bound(n);}
    public static long ceil(long n,long d){return RodPhysicsMath.ceil(n,d);}
    public static int self(ItemStack stack,ReactorCoolants coolant){var d=definition(stack);return d==null||d.kind()!=ReactorRodCatalog.Kind.NUCLEAR||life(stack)<=0?0:coolant==null?d.self():coolant.self(d);}
    public static int emission(ItemStack stack,int previous,ReactorCoolants coolant){
        var d=definition(stack);if(d==null||d.kind()!=ReactorRodCatalog.Kind.NUCLEAR||life(stack)<=0)return 0;
        return bound((coolant==null?d.emission():coolant.emission(d))+ceil(Math.max(0L,(long)previous-self(stack,coolant)),coolant==null?d.divisor():coolant.divisor(d)));
    }
    public record Reception(int absorbed,int reflected,boolean moderates){}
    public static Reception receive(ItemStack stack,int incoming,boolean moderated){
        var d=definition(stack);if(d==null)return new Reception(0,0,false);
        return switch(d.kind()){
            case NUCLEAR->{if(moderated)moderate(stack);yield new Reception(incoming,0,false);}
            case ABSORBER,PRODUCT->new Reception(incoming,0,false);
            case BREEDER->new Reception(moderated?0:Math.max(0,incoming-d.loss()),0,false);
            case REFLECTOR->new Reception(0,incoming,false);
            case MODERATOR->{var tag=stack.getOrCreateTag();if(incoming>0)tag.putInt("gt.contacts_next",tag.getInt("gt.contacts_next")+1);yield new Reception(0,bound((long)incoming*tag.getInt("gt.contacts")),true);}
            default->new Reception(0,0,false);
        };
    }
    public static void finishCycle(ItemStack stack){var d=definition(stack);if(d!=null&&d.kind()==ReactorRodCatalog.Kind.NUCLEAR){var t=stack.getOrCreateTag();t.putBoolean("gt.moderated",t.getBoolean("gt.moderated_next"));t.remove("gt.moderated_next");}if(d!=null&&d.kind()==ReactorRodCatalog.Kind.MODERATOR){var t=stack.getOrCreateTag();t.putInt("gt.contacts",t.getInt("gt.contacts_next"));t.remove("gt.contacts_next");}}
    public record Reaction(ItemStack stack,long heat){}
    public static Reaction react(ItemStack stack,int flux,ReactorCoolants coolant){
        var d=definition(stack);if(d==null)return new Reaction(stack,0);
        long heat=RodPhysicsMath.heat(d.kind(),flux);
        if(d.kind()==ReactorRodCatalog.Kind.NUCLEAR||d.kind()==ReactorRodCatalog.Kind.BREEDER){
            if(coolant!=null&&coolant.moderates){moderate(stack);stack.getOrCreateTag().putBoolean("gt.moderated",true);}
            long maximum=coolant==null?d.maximum():coolant.maximum(d);
            long loss=RodPhysicsMath.loss(d.kind(),flux,maximum,moderated(stack));
            long remaining=Math.max(0,life(stack)-loss);stack.getOrCreateTag().putLong("gt.reactor_life",remaining);
            if(remaining==0&&d.product()!=0)return new Reaction(GTFuelRods.stack(d.product()),heat);
        }
        return new Reaction(stack,heat);
    }
}
