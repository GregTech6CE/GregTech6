import com.gregtech.gregtech.content.cover.ComponentCoverRules;
import com.gregtech.gregtech.content.cover.ComponentCoverRules.Kind;
import com.gregtech.gregtech.content.cover.DrainCoverRules;

/** Pure-rule comparison with the ten original registered tiers; no native transfer execution. */
public final class ComponentCoverRulesAudit {
    private static int checks;

    public static void main(String[] args) {
        for (int tier = 0; tier < 10; tier++) {
            int pump = 250 << (2 * tier), interval = Math.max(1, 512 >> tier);
            check(ComponentCoverRules.pumpThroughput(tier) == pump);
            check(ComponentCoverRules.itemInterval(tier) == interval);
            for (long clock = 0; clock < 1024; clock++) {
                for (Kind kind : Kind.values()) {
                    boolean due = kind == Kind.PUMP ? clock % 20 == 5 : clock % interval == 0;
                    check(ComponentCoverRules.due(kind, tier, clock, false) == due);
                    check(!ComponentCoverRules.due(kind, tier, clock, true));
                }
            }
            System.out.println("TIER|" + tier + "|PUMP=" + pump + "|ITEM_INTERVAL=" + interval);
        }
        for (int visual = 0; visual < 2; visual++) {
            check(ComponentCoverRules.toggle(visual, true) == 1);
            check(ComponentCoverRules.toggle(visual, false) == 1 - visual);
            check(ComponentCoverRules.allowsInsert(visual) == (visual != 0));
            check(ComponentCoverRules.allowsExtract(visual) == (visual == 0));
        }
        check(ComponentCoverRules.sourceSlot(-1) == 0);
        check(ComponentCoverRules.targetSlot(-1) == -1);
        check(ComponentCoverRules.sourceSlot(4) == -1);
        check(ComponentCoverRules.targetSlot(4) == 4);
        check(ComponentCoverRules.slotStep(-1, false, true) == -1);
        check(ComponentCoverRules.slotStep(Short.MIN_VALUE, true, true) == Short.MIN_VALUE);
        check(ComponentCoverRules.slotStep(Short.MAX_VALUE, false, false) == Short.MAX_VALUE);
        check("Water".equals(DrainCoverRules.bulkWaterField("riverwater")));
        check("Ocean".equals(DrainCoverRules.bulkWaterField("seawater")));
        check("Dirty_Water".equals(DrainCoverRules.bulkWaterField("swampwater")));
        check(DrainCoverRules.bulkWaterField("water") == null);
        check(DrainCoverRules.rainAmount(0.8F, 0.2F, false) == 8000);
        check(DrainCoverRules.rainAmount(0.8F, 0.2F, true) == 16000);
        check(DrainCoverRules.rainAmount(0.8F, 0.19F, false) == 0);
        check(DrainCoverRules.rainAmount(0, 1, true) == 0);
        check(DrainCoverRules.rainAmount(0.00001F, 1, false) == 1);
        System.out.println("RULE_CHECKS|" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Rule observation " + checks);
        checks++;
    }
}
