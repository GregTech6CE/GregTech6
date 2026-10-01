package com.gregtech.gregtech.api.tool;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
/** Minecraft boundary for the single shared original GT6 tool definition table. */
public enum GTToolType {
    SWORD,
    PICKAXE,
    SHOVEL,
    AXE,
    HOE,
    SAW,
    HARD_HAMMER,
    SOFT_HAMMER,
    WRENCH,
    FILE,
    CROWBAR,
    SCREWDRIVER,
    CLUB,
    WIRE_CUTTER,
    SCOOP,
    BRANCH_CUTTER,
    UNIVERSAL_SPADE,
    KNIFE,
    BUTCHERY_KNIFE,
    SENSE,
    PLOW,
    PLUNGER,
    ROLLING_PIN,
    CHISEL,
    FLINT_AND_TINDER,
    MONKEY_WRENCH,
    BENDING_CYLINDER,
    BENDING_CYLINDER_SMALL,
    DOUBLE_AXE,
    CONSTRUCTION_PICK,
    MAGNIFYING_GLASS,
    SCISSORS,
    PINCERS,
    SPADE,
    GEM_PICK,
    HAND_DRILL,
    BUILDER_WAND;
    private final ToolDefinition definition = ToolDefinition.valueOf(name());
    public ToolDefinition definition(){return definition;}
    public int gt6Id(){return definition.gt6Id();}
    public String id(){return definition.id();}
    @Nullable public MaterialPrefix headPrefix(){return definition.headPrefix();}
    private static ResourceLocation location(String id){return id==null?null:new ResourceLocation(id);}
    @Nullable public ResourceLocation headlessIcon(){return location(definition.headlessIcon());}
    @Nullable public ResourceLocation handleIcon(){return location(definition.handleIcon());}
    public float durabilityMultiplier(){return definition.durabilityMultiplier();}
    public float baseDamage(){return definition.baseDamage();}
    public int baseQuality(){return definition.baseQuality();}
    public float speedMultiplier(){return definition.speedMultiplier();}
    public int damagePerCraft(){return definition.damagePerCraft();}
    public int minToolTypes(){return definition.minToolTypes();}
    public boolean requiresHeadAssembly(){return definition.requiresHeadAssembly();}
    public boolean isHeadless(){return definition.isHeadless();}
    public boolean isMiningTool(){return definition.isMiningTool();}
    public boolean canPenetrate(){return definition.canPenetrate();}
    public boolean canCollect(){return definition.canCollect();}
    @Nullable public String tooltipKey(){return definition.tooltipKey();}
    @Nullable public TagKey<Block> harvestTag(){return definition.harvestTag()==null?null:TagKey.create(Registries.BLOCK,location(definition.harvestTag()));}
    public float attackSpeed(){return definition.attackSpeed();}
    public int damagePerBlockBreak(){return definition.damagePerBlockBreak();}
    public int damagePerEntityAttack(){return definition.damagePerEntityAttack();}
    public boolean canUseHead(GTMaterial head){return definition.canUseHead(head);}
    public boolean canHarvest(BlockState state){var tag=harvestTag();return tag!=null&&state.is(tag);}
    public static GTToolType byId(String id){return valueOf(ToolDefinition.byId(id).name());}
    public String translationKey(){return definition.translationKey();}
    @Override public String toString(){return definition.toString();}
}
