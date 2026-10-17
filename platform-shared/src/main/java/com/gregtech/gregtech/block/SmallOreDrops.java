package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** GT6 Drops_SmallOre weighted gems/crushed drops, seeded by location. */
public final class SmallOreDrops {
    private SmallOreDrops() {}
    public static List<ItemStack> drops(GTMaterial source,BlockPos pos,int fortune,boolean silk,GTMaterial host) {
        var material=source.getTargetCrushingMaterial().resolve();
        var random=new Random(pos.getX()^pos.getY()^pos.getZ());
        for(int i=0;i<16;i++)random.nextInt(10000);
        fortune=Math.max(0,Math.min(255,fortune));
        int multiplier=Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier());
        var result=new ArrayList<ItemStack>();
        if(material.getName().equals("Gneiss")||material.getName().equals("PetrifiedWood")) {
            var rock=GTItems.getStack(MaterialPrefix.rockGt,material);
            int count=Math.max(1,(multiplier+(fortune>0?random.nextInt((1+fortune)*multiplier):0))/2+random.nextInt(2));
            if(!rock.isEmpty())for(int i=0;i<count;i++)result.add(rock.copy());
        } else {
            var legendary=GTItems.getStack(MaterialPrefix.gemLegendary,material);
            if(!legendary.isEmpty()&&random.nextInt(silk?5000:10000)<=fortune)result.add(legendary);
            else {
                var pool=new ArrayList<ItemStack>();
                add(pool,fallback(MaterialPrefix.gemExquisite,material,1,MaterialPrefix.gem,4),silk?3:1);
                add(pool,fallback(MaterialPrefix.gemFlawless,material,1,MaterialPrefix.gem,2),silk?6:2);
                add(pool,GTItems.getStack(MaterialPrefix.gem,material),silk?6:12);
                var flawed=GTItems.getStack(MaterialPrefix.gemFlawed,material,2);
                add(pool,flawed,silk?10:5);
                add(pool,GTItems.getStack(MaterialPrefix.crushed,material),flawed.isEmpty()?15:silk?5:10);
                var chipped=GTItems.getStack(MaterialPrefix.gemChipped,material,4);
                add(pool,chipped,silk?10:5);
                add(pool,chipped.isEmpty()?GTItems.getStack(MaterialPrefix.crushed,material):fallback(MaterialPrefix.crushed,material,1,MaterialPrefix.dust,1),chipped.isEmpty()?15:silk?5:10);
                int count=Math.max(1,(multiplier+(fortune>0?random.nextInt((1+fortune)*multiplier):0))/2);
                if(!pool.isEmpty())for(int i=0;i<count;i++)result.add(pool.get(random.nextInt(pool.size())).copy());
            }
        }
        if(host!=null&&host.isValid()&&random.nextInt(3+fortune)>1) {
            var dust=GTItems.getStack(MaterialPrefix.dust,host.getTargetCrushingMaterial());
            if(!dust.isEmpty())result.add(dust);
        }
        return result;
    }
    private static ItemStack fallback(MaterialPrefix p,GTMaterial m,int n,MaterialPrefix f,int amount) {
        var stack=GTItems.getStack(p,m,n);return stack.isEmpty()?GTItems.getStack(f,m,amount):stack;
    }
    private static void add(List<ItemStack> pool,ItemStack stack,int weight) {
        if(!stack.isEmpty())for(int i=0;i<weight;i++)pool.add(stack);
    }
}
