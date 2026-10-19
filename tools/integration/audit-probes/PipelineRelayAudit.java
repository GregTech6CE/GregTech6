import com.gregtech.gregtech.content.logistics.LongDistanceCatalog;
import com.gregtech.gregtech.content.logistics.RelayVisitSet;

/** Core catalog and recursion-guard observations, not a native item/fluid capability test. */
class PipelineRelayAudit {
    public static void main(String[] args) throws InterruptedException {
        for (var spec : LongDistanceCatalog.LINES) {
            if (spec.kind().endsWith("PIPE")) {
                System.out.println("PIPE|" + spec.id() + "|" + spec.sourceMeta() + "|" + spec.maximumTemperature());
            }
        }
        var visits = new RelayVisitSet<String>();
        var current = visits.get();
        System.out.println("VISIT|first=" + current.add("A") + "|recursive=" + current.add("A"));
        System.out.println("VISIT|nested=" + current.add("B"));
        current.remove("B");
        System.out.println("VISIT|outerStillPresent=" + current.contains("A"));
        var isolated = new boolean[1];
        var thread = new Thread(() -> {
            isolated[0] = visits.get().isEmpty();
            visits.remove();
        });
        thread.start();
        thread.join();
        System.out.println("THREAD_ISOLATED|" + isolated[0]);
        current.remove("A");
        visits.remove();
        System.out.println("CLEANUP|fresh=" + (visits.get() != current) + "|empty=" + visits.get().isEmpty());
        visits.remove();
    }
}
