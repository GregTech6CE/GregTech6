package com.gregtech.gregtech.content.logistics;
import net.minecraft.world.level.Level;
/** Thin world/capability boundary over the shared original routing sequence. */
public final class LogisticsRoutingPass {
    public record Result(int itemsMoved, int fluidMoved, int operations, long energyCost,
                         int usedLogic, int usedConversion) {
        /** Keep the original four-field result usable by existing callers. */
        public Result(int itemsMoved, int fluidMoved, int operations, long energyCost) {
            this(itemsMoved, fluidMoved, operations, energyCost, operations, 0);
        }
    }

    private LogisticsRoutingPass() {}
    public static Result route(Level level, LogisticsNetwork.Snapshot network, int logic, int conversion) {
        if(logic<=0||conversion<=0||network.size()==0)return new Result(0,0,0,0);
        var fluids=LogisticsFluidRouter.collect(level,network);
        var items=LogisticsItemRouter.collect(level,network);
        var result=LogisticsRoutingRules.route(logic,conversion,new LogisticsRoutingRules.FluidChannel() {
            public int tryPair(LogisticsCoverDefinition.Role source,int sourceRank,LogisticsCoverDefinition.Role destination,int destinationRank,int conversion) {
                return fluids.tryPair(LogisticsCoverType.Role.valueOf(source.name()),sourceRank,LogisticsCoverType.Role.valueOf(destination.name()),destinationRank,conversion);
            }
        },new LogisticsRoutingRules.ItemChannel() {
            public int tryPair(LogisticsCoverDefinition.Role source,int sourceRank,LogisticsCoverDefinition.Role destination,int destinationRank,int conversion) {
                return items.tryPair(LogisticsCoverType.Role.valueOf(source.name()),sourceRank,LogisticsCoverType.Role.valueOf(destination.name()),destinationRank,conversion);
            }
            public int tryDump(int conversion) { return items.tryDump(conversion); }
            public int lastConversionUsed() { return items.lastConversionUsed(); }
        });
        return new Result(result.itemsMoved(),result.fluidMoved(),result.operations(),result.energyCost(),result.usedLogic(),result.usedConversion());
    }
}
