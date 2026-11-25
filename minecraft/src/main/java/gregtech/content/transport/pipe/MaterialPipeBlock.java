package gregtech.content.transport.pipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.GTBlock;
import gregtech.content.item.ElectricToolItem;
import gregtech.content.tool.ElectricWrenchHarvest;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Material-dependent mining speed and harvest requirements of item and fluid pipes. */
public abstract class MaterialPipeBlock extends GTBlock {
    private final GTMaterial material;

    protected MaterialPipeBlock(GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public GTToolType harvestTool() {
        MaterialTextureSet texture = material.getTextureSet();
        if (texture == MaterialTextureSet.WOOD) return GTToolType.AXE;
        if (texture == MaterialTextureSet.RUBBER) return GTToolType.SCISSORS;
        return GTToolType.WRENCH;
    }

    public int harvestLevel() {
        return Math.max(0, material.getToolQuality());
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof ElectricToolItem) return super.getDestroyProgress(state, player, level, pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0;

        GTToolType required = harvestTool();
        if (required == GTToolType.WRENCH) {
            if (GTToolHelper.isMachineWrench(tool)) {
                return GTToolHelper.wrenchReachesQuality(tool, harvestLevel())
                        ? GTToolHelper.getMachineHarvestSpeed(tool) / hardness / 30.0F
                        : 0.0F;
            }
        } else {
            boolean vanilla = vanillaTool(tool, required);
            boolean matches = GTToolHelper.matchesTool(tool, required);
            if (vanilla || matches) {
                float speed;
                if (matches && GTToolHelper.isUsable(tool)) speed = GTToolHelper.getMiningSpeed(tool, state);
                else speed = vanilla ? tool.getDestroySpeed(state) : 1.0F;
                return speed / hardness / 30.0F;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof ElectricToolItem electric) {
            return ElectricWrenchHarvest.canHarvest(electric, tool, state);
        }
        GTToolType required = harvestTool();
        if (required == GTToolType.WRENCH) {
            if (GTToolHelper.isMachineWrench(tool)) return GTToolHelper.wrenchReachesQuality(tool, harvestLevel());
        } else if (vanillaTool(tool, required) || GTToolHelper.matchesTool(tool, required)) {
            return true;
        }
        return super.canHarvestBlock(state, level, pos, player);
    }

    private static boolean vanillaTool(ItemStack tool, GTToolType required) {
        return required == GTToolType.AXE && tool.getItem() instanceof AxeItem
                || required == GTToolType.SCISSORS && tool.getItem() instanceof ShearsItem;
    }
}
