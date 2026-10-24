package gregtech.bootstrap;

/** One ordered initialization action; implementations must not reorder dependent registrations. */
@FunctionalInterface
public interface InitializationStep extends Runnable {
    @Override
    void run();
}
