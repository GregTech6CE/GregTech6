package gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("gt6ce_forge_attachment_followup") @PrefixGameTestTemplate(false)
public final class ForgeAttachmentFollowupTests {
    @GameTest(template="test_blueprint_large",timeoutTicks=1200)
    public static void attachmentPhaseAndFlowRules(GameTestHelper h) {
        FluidToolVariantTests.attachmentPhaseAndFlowRules(h);
    }
}
