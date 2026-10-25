package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.nuclear.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("gt6ce")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class NuclearSurvivalTests {
    @GameTest(template="test_empty") public static void placedRodsKeepFuelAndBreedingState(GameTestHelper h) {
        var p=new BlockPos(1,2,1);var player=h.makeMockPlayer();
        for(var definition:ReactorRodCatalog.ALL){
            var stack=GTFuelRods.stack(definition.originalId());
            h.assertTrue(stack.getMaxStackSize()==16,"original rod stack limit: "+definition.id());
            stack.getOrCreateTag().putLong("gt.reactor_life",9876543210L);
            stack.getOrCreateTag().putBoolean("gt.test.marker",true);
            var block=((BlockItem)stack.getItem()).getBlock();h.setBlock(p,block);
            var absolute=h.absolutePos(p);block.setPlacedBy(h.getLevel(),absolute,block.defaultBlockState(),player,stack);
            var be=(com.gregtech.gregtech.blockentity.energy.PlacedReactorRodBlockEntity)h.getLevel().getBlockEntity(absolute);
            be.load(be.saveWithoutMetadata());
            var drops=Block.getDrops(be.getBlockState(),h.getLevel(),absolute,be);
            h.assertTrue(drops.size()==1&&ItemStack.matches(stack,drops.get(0)),"placing/saving/breaking preserves exact rod: "+definition.id());
            var bounds=be.getBlockState().getShape(h.getLevel(),absolute).bounds();
            h.assertTrue(bounds.minX==0.375&&bounds.maxX==0.625&&bounds.maxY==1,"original 4x16x4 rod collision");
            var copy=be.rod();copy.getOrCreateTag().putLong("gt.reactor_life",1);
            h.assertTrue(be.rod().getTag().getLong("gt.reactor_life")==9876543210L,"inspection cannot mutate stored fuel");
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void playerRadiationAccumulatesAndPersists(GameTestHelper h){
        // Creative players are deliberately immune to reactor exposure.
        var player=h.makeMockSurvivalPlayer();
        ReactorRadiation.apply(player,3,8);PlayerRadiation.applySymptoms(player);
        h.assertTrue(PlayerRadiation.dose(player)==24&&!player.hasEffect(MobEffects.POISON),"below 25 has no radiation symptom");
        ReactorRadiation.apply(player,1,1);PlayerRadiation.applySymptoms(player);
        h.assertTrue(player.hasEffect(MobEffects.POISON)&&!player.hasEffect(MobEffects.WEAKNESS),"25 has poison only");
        PlayerRadiation.change(player,25);PlayerRadiation.applySymptoms(player);
        h.assertTrue(player.getEffect(MobEffects.WEAKNESS).getAmplifier()==0,"50 begins systemic symptoms");
        PlayerRadiation.change(player,25);PlayerRadiation.applySymptoms(player);
        h.assertTrue(player.getEffect(MobEffects.WEAKNESS).getAmplifier()==1,"75 increases systemic symptoms");
        PlayerRadiation.change(player,Long.MAX_VALUE);PlayerRadiation.applySymptoms(player);
        h.assertTrue(PlayerRadiation.dose(player)==127&&player.hasEffect(MobEffects.WITHER)&&player.getEffect(MobEffects.WEAKNESS).getAmplifier()==2,"100+ causes severe radiation, no dose overflow");
        var saved=new net.minecraft.nbt.CompoundTag();player.saveWithoutId(saved);
        var restored=h.makeMockSurvivalPlayer();restored.load(saved);h.assertTrue(PlayerRadiation.dose(restored)==127,"dose survives player NBT save/load");
        var clone=h.makeMockSurvivalPlayer();PlayerRadiation.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,player,false));
        h.assertTrue(PlayerRadiation.dose(clone)==127,"non-death player replacement preserves dose");
        PlayerRadiation.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,player,true));
        h.assertTrue(PlayerRadiation.dose(clone)==0,"death clears accumulated dose");
        PlayerRadiation.change(player,Long.MIN_VALUE);h.assertTrue(PlayerRadiation.dose(player)==0,"negative dose clamps without overflow");h.succeed();
    }
    @GameTest(template="test_empty") public static void radiationSuitAndMedicine(GameTestHelper h){
        // Keep real consumption ticks separate from the independent symptom tick handler.
        if(h.getLevel().getGameTime()%50==0){h.runAfterDelay(1,()->radiationSuitAndMedicine(h));return;}
        var player=h.makeMockSurvivalPlayer();
        for(var entry:GTRadiationProtection.SUIT.entrySet()){
            var stack=new ItemStack(entry.getValue().get());player.setItemSlot(entry.getKey().getSlot(),stack);
            h.assertTrue(stack.getMaxDamage()==128,"each original suit piece has 128 durability");
        }
        h.assertTrue(ReactorRadiation.protectedByArmor(player)&&!ReactorRadiation.apply(player,100,100),"full tagged suit prevents exposure");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,ItemStack.EMPTY);
        h.assertTrue(ReactorRadiation.apply(player,1,100)&&PlayerRadiation.dose(player)==100,"missing one piece removes protection");
        var medicine=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","radaway"));
        var stack=new ItemStack(medicine,2);player.getFoodData().setFoodLevel(20);
        h.assertTrue(medicine.getFoodProperties().canAlwaysEat()&&medicine.getFoodProperties().getNutrition()==0,"medicine can be taken at full hunger without adding food");
        stack=consumeMedicine(h,player,stack);
        h.assertTrue(PlayerRadiation.dose(player)==50&&stack.getCount()==1,"one pill removes exactly 50 dose and is consumed");
        stack=consumeMedicine(h,player,stack);
        h.assertTrue(PlayerRadiation.dose(player)==0&&stack.isEmpty(),"second pill completes treatment and is consumed");
        for(var entry:GTRadiationProtection.SUIT.values()){
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","nuclear/"+entry.getId().getPath());
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isPresent(),"suit crafting route loaded: "+id);
        }
        h.succeed();
    }
    private static ItemStack consumeMedicine(GameTestHelper h,net.minecraft.world.entity.player.Player player,ItemStack stack){
        var pos=h.absolutePos(new BlockPos(1,2,1));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        player.setNoGravity(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        stack.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(player.isUsingItem(),"medicine starts native consumption at full hunger");
        // The helper does not insert its player into the world. Real player ticks complete use
        // and let Forge emit Finish, where the shared food table applies the dose once.
        for(int ticks=0;ticks<100&&player.isUsingItem();ticks++)player.tick();
        h.assertTrue(!player.isUsingItem(),"medicine completes the native item-use lifecycle");
        return player.getMainHandItem();
    }
    @GameTest(template="test_empty") public static void radiationMedicineManufacturingAndGeiger(GameTestHelper h){
        var items=net.minecraftforge.registries.ForgeRegistries.ITEMS;
        var pill=items.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","empty_wax_pill"));
        var medicine=items.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","radaway"));
        var counter=items.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","geiger_counter"));
        var routes=com.gregtech.gregtech.data.MachineRecipeMaps.Extruder.mRecipeList.stream()
                .filter(r->java.util.Arrays.stream(r.mOutputs).anyMatch(s->s.is(pill))).toList();
        h.assertTrue(routes.size()==18,"three food-grade waxes, three dust sizes, two bottle molds");
        for(var recipe:routes){
            h.assertTrue(recipe.mInputs[1].getItem() instanceof com.gregtech.gregtech.item.TechItem mold&&mold.isCatalyst(),"pill mold is not consumed");
            h.assertTrue(recipe.mDuration==64&&recipe.mEUt==16,"original wax extrusion work");
        }
        h.assertTrue(com.gregtech.gregtech.data.MachineRecipeMaps.Boxinator.mRecipeList.stream().anyMatch(r->java.util.Arrays.stream(r.mOutputs).anyMatch(s->s.is(medicine))),"iodine packaging route exists");
        long filled=com.gregtech.gregtech.data.MachineRecipeMaps.Canner.mRecipeList.stream().filter(r->java.util.Arrays.stream(r.mOutputs).anyMatch(s->s.is(counter))).count();
        h.assertTrue(filled==3,"helium/neon/argon filling routes");
        var pos=new BlockPos(1,2,1);h.setBlock(pos,GTEnergyNodes.REACTOR_CORE_2X2.get());
        var core=(com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        core.neutrons[0]=11;core.neutrons[1]=22;core.neutrons[2]=33;core.neutrons[3]=44;
        var reading=com.gregtech.gregtech.item.GeigerCounterItem.reading(core).getString();
        h.assertTrue(reading.contains("11 n; 22 n; 33 n; 44 n"),"counter reports individual rod levels");
        h.assertTrue(counter instanceof com.gregtech.gregtech.item.GeigerCounterItem,"counter has actual interaction behavior");h.succeed();
    }
}
