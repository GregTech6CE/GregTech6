package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Reads/writes GT6-style {@code GT.ToolStats} NBT on tool stacks (type is fixed per item). */
public final class GTToolHelper {
    public static final String ROOT = "GT.ToolStats";
    private static final String KEY_HEAD = "head";
    private static final String KEY_HANDLE = "handle";

    private GTToolHelper() {}

    public static ItemStack write(ItemStack stack, GTMaterial head, GTMaterial handle) {
        CompoundTag stats = new CompoundTag();
        stats.putString(KEY_HEAD, head.getName());stats.putString(KEY_HANDLE, handle.getName());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,tag->tag.put(ROOT,stats));
        stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,computeMaxDurability(stack.getItem() instanceof GTToolItem tool?tool.toolType():getType(stack),head));
        stack.set(net.minecraft.core.component.DataComponents.DAMAGE,0);
        if(stack.getItem() instanceof GTToolItem tool)stack.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,tool.getDefaultAttributeModifiers(stack).withTooltip(false));
        GTToolEnchantments.applyCurrent(stack);
        return stack;
    }

    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof GTToolItem && hasStats(stack);
    }

    /** Identity for GUI/cover dispatch, including an empty electric tool (not a bare hand). */
    public static boolean isInteractionTool(ItemStack stack) {
        return isTool(stack) || stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric
                && electric.interactionType(stack) != null;
    }

    public static boolean isUsable(ItemStack stack) {
        return isTool(stack) && stack.getDamageValue() < getMaxDurability(stack);
    }

    public static GTToolType getType(ItemStack stack) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric
                && electric.interactionType(stack) != null) return electric.interactionType(stack);
        if (stack.getItem() instanceof GTToolItem toolItem) {
            return toolItem instanceof com.gregtech.gregtech.item.PocketToolItem pocket?pocket.activeType():toolItem.toolType();
        }
        return GTToolType.PICKAXE;
    }

    /** GT6 {@code ToolsGT.contains} subset: GT meta-tools plus vanilla shovel. */
    public static boolean matchesTool(ItemStack stack, GTToolType type) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric) {
            return electric.interactionType(stack) == type && electric.canInteract(stack);
        }
        if (stack.getItem() instanceof GTToolItem toolItem) {
            if (getType(stack) != type) {
                return false;
            }
            if (!type.requiresHeadAssembly()) {
                return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage();
            }
            return isUsable(stack);
        }
        return type == GTToolType.SHOVEL && stack.getItem() instanceof ShovelItem;
    }

    /** GT6 crucible / smeltery shovel scrape (solid content → scrap). */
    public static boolean isCrucibleScrapeShovel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (matchesTool(stack, GTToolType.SHOVEL)
                || matchesTool(stack, GTToolType.SPADE)
                || matchesTool(stack, GTToolType.UNIVERSAL_SPADE)) {
            return true;
        }
        return stack.getItem() instanceof ShovelItem && !(stack.getItem() instanceof GTToolItem)
                && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
    }

    /** Vanilla {@link ShovelItem} only — always 1 durability per crucible scrape click. */
    public static boolean isVanillaShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem && !(stack.getItem() instanceof GTToolItem);
    }

    /** GT6 {@code MultiTileEntitySmeltery#onToolClick2} — {@code 1000 * stackSize} tool cost for GT tools. */
    public static void damageCrucibleScrape(ItemStack stack, int scrapStackSize, @Nullable LivingEntity user) {
        damageCrucibleScrape(stack, scrapStackSize, user, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public static void damageCrucibleScrape(ItemStack stack, int scrapStackSize, @Nullable LivingEntity user,
                                            net.minecraft.world.InteractionHand hand) {
        if (scrapStackSize <= 0 || stack.isEmpty()) {
            return;
        }
        if (user != null && isCreative(user)) {
            return;
        }
        EquipmentSlot slot = hand == net.minecraft.world.InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        long gt6Return = 1000L * scrapStackSize;
        if (stack.getItem() instanceof GTToolItem toolItem) {
            if (isTool(stack)) {
                damageForToolClickReturn(stack, gt6Return, user, slot);
            } else if (toolItem.toolType() == GTToolType.SHOVEL
                    || toolItem.toolType() == GTToolType.SPADE
                    || toolItem.toolType() == GTToolType.UNIVERSAL_SPADE) {
                int amount = (int) Math.max(1L, Math.ceil(gt6Return * 100.0D / 10000.0D));
                hurt(stack, amount, user, slot);
            }
        } else if (isVanillaShovel(stack)) {
            hurt(stack, 1, user, slot);
        } else if (stack.isDamageableItem()) {
            int amount = (int) Math.max(1L, (gt6Return * 100L + 9999L) / 10000L);
            hurt(stack, amount, user, slot);
        }
    }

    /** GT6 machine tool-click durability cost ({@code onToolClick2} return value). */
    public static void damageForUse(ItemStack stack, int amount, @Nullable LivingEntity user) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric) {
            electric.consumeInteractionEnergy(stack, Math.max(0L, amount) * 100L, user);
        } else if (stack.getItem() instanceof GTToolItem) {
            hurt(stack, amount, user);
        } else if (user != null && !isCreative(user) && stack.isDamageableItem()) {
            stack.hurtAndBreak(amount, user, EquipmentSlot.MAINHAND);
        }
    }

    /** Gem tip / tool head  - sole material used for combat and mining stats. */
    public static void damageForUse(ItemStack stack,int amount,LivingEntity user,EquipmentSlot slot){if(stack.getItem() instanceof PoweredToolItem electric)electric.consumeInteractionEnergy(stack,Math.max(0L,amount)*100L,user);else hurt(stack,amount,user,slot);}
    public static GTMaterial getHead(ItemStack stack) {
        CompoundTag stats = stats(stack);
        if (stats == null) {
            return GTMaterialRegistry.get("NULL");
        }
        return GTMaterialRegistry.get(stats.getString(KEY_HEAD));
    }

    /** Stick / handle material from assembly (wood rod for gem pick). */
    public static GTMaterial getHandle(ItemStack stack) {
        CompoundTag stats = stats(stack);
        if (stats == null) {
            return GTMaterialRegistry.get("NULL");
        }
        return GTMaterialRegistry.get(stats.getString(KEY_HANDLE));
    }

    public static GTMaterial getStatMaterial(ItemStack stack) {
        return getHead(stack);
    }

    /** Fixed iron pickaxe body for gem picks (cosmetic only). */
    public static GTMaterial gemPickBodyMaterial() {
        return com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron;
    }

    public static boolean isGemPick(ItemStack stack) {
        return getType(stack) == GTToolType.GEM_PICK;
    }

    public static int getMaxDurability(ItemStack stack) {
        if (!isTool(stack)) {
            return 1;
        }
        return computeMaxDurability(((GTToolItem)stack.getItem()).toolType(), getStatMaterial(stack));
    }

    /**
     * GT6 formula: {@code material.mToolDurability * 100 * tool.getMaxDurabilityMultiplier()}.
     * Gem pick uses {@code 0.25} multiplier (= quarter durability of the same gem as a normal pick).
     */
    public static int computeMaxDurability(GTToolType type, GTMaterial statMaterial) {
        return com.gregtech.gregtech.api.machine.ManualToolRules.maximumDurability(statMaterial, type.durabilityMultiplier());
    }

    public static int getHarvestLevel(ItemStack stack) {
        GTToolType type = getType(stack);
        GTMaterial stat = getStatMaterial(stack);
        return type.baseQuality() + stat.getToolQuality();
    }

    public static float getCombatDamage(ItemStack stack) {
        GTToolType type = stack.getItem() instanceof com.gregtech.gregtech.item.PocketToolItem pocket?pocket.toolType():getType(stack);
        GTMaterial stat = getStatMaterial(stack);
        return type.baseDamage() + stat.getToolQuality();
    }

    public static float getMiningSpeed(ItemStack stack, BlockState state) {
        if (!canEffectiveHarvest(stack, state)) {
            return 0.0F;
        }
        GTToolType type = getType(stack);
        GTMaterial stat = getStatMaterial(stack);
        return Math.max(Float.MIN_NORMAL, type.speedMultiplier() * stat.getToolSpeed());
    }

    public static boolean canEffectiveHarvest(ItemStack stack, BlockState state) {
        if (!isUsable(stack)) {
            return false;
        }
        if (isSpecialHarvestTool(stack, state)) return true;
        GTToolType type = getType(stack);
        if (!type.isMiningTool() || !type.canHarvest(state)) {
            return false;
        }
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        return getHarvestLevel(stack) >= requiredHarvestLevel(state);
    }

    public static boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (isSpecialHarvestTool(stack, state)) return true;
        if (!isUsable(stack)) {
            return false;
        }
        GTToolType type = getType(stack);
        if (type.harvestTag() == null || !state.is(type.harvestTag())) {
            return false;
        }
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        return getHarvestLevel(stack) >= requiredHarvestLevel(state);
    }

    public static boolean isMachineWrench(ItemStack stack) {
        return matchesTool(stack, GTToolType.WRENCH) || matchesTool(stack, GTToolType.MONKEY_WRENCH);
    }

    public static boolean isScrewdriver(ItemStack stack) {
        return matchesTool(stack, GTToolType.SCREWDRIVER);
    }

    public static boolean isMonkeyWrench(ItemStack stack) {
        return matchesTool(stack, GTToolType.MONKEY_WRENCH);
    }

    public static boolean isSoftHammer(ItemStack stack) {
        return matchesTool(stack, GTToolType.SOFT_HAMMER);
    }

    public static boolean isMagnifyingGlass(ItemStack stack) {
        return matchesTool(stack, GTToolType.MAGNIFYING_GLASS);
    }

    public static boolean isWireCutter(ItemStack stack) {
        return matchesTool(stack, GTToolType.WIRE_CUTTER);
    }

    public static boolean isMachineHarvestTool(ItemStack stack, BlockState state) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric)
            return state.getBlock() instanceof com.gregtech.gregtech.api.machine.GTMachineBlock
                    && electric.canHarvest(stack,state);
        return state.getBlock() instanceof com.gregtech.gregtech.api.machine.GTMachineBlock && isMachineWrench(stack);
    }

    /** GT6 {@code canCollectDropsDirectly} for wrench-dismantleable machines. */
    public static boolean canCollectMachineDrop(ItemStack stack, BlockState state) {
        if (!isMachineHarvestTool(stack, state)) {
            return false;
        }
        GTToolType type = getType(stack);
        return type.canCollect() && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
    }

    /** GT6 wrench dismantle speed (left-click hold). */
    public static float getMachineHarvestSpeed(ItemStack stack) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric)
            return electric.harvestSpeed(stack);
        return 12.0F;
    }

    /**
     * GT6 {@code Behavior_Tool}: {@code units(return, 10000, 100, true)} for wrench/MTE tool clicks.
     */
    public static void damageForToolClickReturn(ItemStack stack, long gt6Return, @Nullable LivingEntity user) {
        damageForToolClickReturn(stack, gt6Return, user, EquipmentSlot.MAINHAND);
    }

    public static void damageForToolClickReturn(ItemStack stack, long gt6Return, @Nullable LivingEntity user,
                                                EquipmentSlot slot) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric) {
            if (gt6Return > 0) electric.consumeInteractionEnergy(stack,
                    gt6Return / 100 + (gt6Return % 100 == 0 ? 0 : 1), user);
            return;
        }
        if (gt6Return <= 0 || !isTool(stack) || user != null && isCreative(user)) {
            return;
        }
        int amount = com.gregtech.gregtech.api.machine.ManualToolRules.clickDamage(gt6Return);
        hurt(stack, amount, user, slot);
    }

    public static boolean isSpecialHarvestTool(ItemStack stack, BlockState state) {
        if (stack.getItem() instanceof com.gregtech.gregtech.api.tool.PoweredToolItem electric)
            return electric.canHarvest(stack,state);
        if (!isUsable(stack)) return false;
        var tool=com.gregtech.gregtech.data.BlockHarvestPolicy.tool(state.getBlock());
        var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(id==null || !id.getNamespace().equals("gregtech")) return false;
        return switch(tool) {
            case SWORD -> matchesTool(stack, GTToolType.SWORD);
            case WRENCH -> isMachineWrench(stack) && (!state.requiresCorrectToolForDrops()
                    || getHarvestLevel(stack) >= requiredHarvestLevel(state));
            case CROWBAR -> matchesTool(stack, GTToolType.CROWBAR);
            case CUTTER -> isWireCutter(stack);
            case SHEARS -> matchesTool(stack,GTToolType.SCISSORS) || matchesTool(stack,GTToolType.BRANCH_CUTTER);
            default -> false;
        };
    }

    public static int requiredHarvestLevel(BlockState state) {
        int materialLevel=com.gregtech.gregtech.data.BlockHarvestPolicy.level(state.getBlock());
        if(materialLevel>3) return materialLevel;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return 1;
        }
        return 0;
    }

    private static boolean isCreative(LivingEntity user) {
        return user instanceof Player player && player.getAbilities().instabuild;
    }

    public static void damageForBlockBreak(ItemStack stack, BlockState state, @Nullable LivingEntity user) {
        if (!isTool(stack) || user != null && isCreative(user)) {
            return;
        }
        float hardness = state.getDestroySpeed(null, null);
        if (hardness <= 0.0F) {
            return;
        }
        GTToolType type = getType(stack);
        int perBreak = (stack.getItem() instanceof com.gregtech.gregtech.item.PocketToolItem pocket?pocket.toolType():type).damagePerBlockBreak();
        if (state.getBlock() instanceof com.gregtech.gregtech.api.machine.GTMachineBlock && isMachineWrench(stack)) {
            perBreak = GTToolType.WRENCH.damagePerBlockBreak();
        }
        int amount = (int) Math.ceil(perBreak * hardness);
        hurt(stack, amount, user);
    }

    public static void damageForEntityAttack(ItemStack stack, @Nullable LivingEntity user) {
        if (!isTool(stack) || user != null && isCreative(user)) {
            return;
        }
        hurt(stack, (stack.getItem() instanceof com.gregtech.gregtech.item.PocketToolItem pocket?pocket.toolType():getType(stack)).damagePerEntityAttack(), user);
    }

    private static void hurt(ItemStack stack, int amount, @Nullable LivingEntity user) {
        hurt(stack, amount, user, EquipmentSlot.MAINHAND);
    }

    private static void hurt(ItemStack stack, int amount, @Nullable LivingEntity user, EquipmentSlot slot) {
        if (amount <= 0 || stack.isEmpty()) {
            return;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        if (user != null && isCreative(user)) {
            return;
        }
        if (stack.getItem() instanceof GTToolItem) {
            applyToolDamage(stack, amount, user, slot, maxDamage);
            return;
        }
        if (user instanceof Player player) {
            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(amount, player, slot);
            }
            player.getInventory().setChanged();
        } else if (user != null) {
            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(amount, user, slot);
            }
        } else {
            stack.setDamageValue(Math.min(maxDamage, stack.getDamageValue() + amount));
        }
    }

    /** GT tools may not pass {@link ItemStack#isDamageableItem()} — apply damage directly. */
    private static void applyToolDamage(ItemStack stack, int amount, @Nullable LivingEntity user,
                                        EquipmentSlot slot, int maxDamage) {
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= maxDamage) {
            stack.shrink(1);
            if (user != null) {
                user.onEquippedItemBroken(stack.getItem(),slot);
            }
        } else {
            stack.setDamageValue(newDamage);
        }
        if (user instanceof Player player) {
            player.getInventory().setChanged();
        }
    }

    public static GTMaterial defaultHandle(GTMaterial head) {
        if (head.has(MaterialProperty.WOOD)) {
            return head;
        }
        return com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood;
    }

    public static boolean isValidStick(GTMaterial material) {
        return MaterialPrefix.stick.isValidFor(material);
    }

    /** The first material whose {@code prefix} form exists and that may head the given tool. */
    @Nullable
    public static GTMaterial firstToolMaterial(@Nullable MaterialPrefix prefix, GTToolType type) {
        if (prefix == null) return null;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !type.canUseHead(material) || !prefix.isValidFor(material)) continue;
            if (!com.gregtech.gregtech.registry.GTItems.getStack(prefix, material, 1).isEmpty()) return material;
        }
        return null;
    }

    /** The first material that yields a valid handle stick, for recipe display. */
    @Nullable
    public static GTMaterial firstHandleMaterial() {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && isValidStick(material)) return material;
        }
        return null;
    }

    /**
     * A usable tool stack for recipe display and for tests: assembled from a real material, because a
     * bare {@code ItemStack} of the tool item has no {@code GT.ToolStats} and is therefore not usable.
     */
    public static ItemStack displayTool(GTToolType type) {
        GTMaterial material = type.headPrefix() != null
                ? firstToolMaterial(type.headPrefix(), type) : Materials.Steel;
        if (material == null) material = Materials.Steel;
        GTMaterial handle = isValidStick(material) ? material : firstHandleMaterial();
        return com.gregtech.gregtech.item.GTToolItem.create(type, material, handle == null ? material : handle);
    }

    private static boolean hasStats(ItemStack stack) {
        CompoundTag tag = stats(stack);
        return tag != null && tag.contains(KEY_HEAD) && tag.contains(KEY_HANDLE);
    }

    @Nullable
    private static CompoundTag stats(ItemStack stack) {
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data==null)return null;CompoundTag root=data.copyTag();
        return root.contains(ROOT,net.minecraft.nbt.Tag.TAG_COMPOUND)?root.getCompound(ROOT):null;
    }
}
