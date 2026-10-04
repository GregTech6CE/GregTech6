package com.gregtech.gregtech.api.mod;

/** Serializes setup callbacks, retaining the first failure instead of hiding it on a later call. */
public final class SetupOnce {
    private boolean running;
    private boolean complete;
    private Throwable failure;

    public synchronized void run(Runnable action) {
        if (complete) return;
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
        if (running) throw new IllegalStateException("Recursive GregTech setup initialization");
        running = true;
        try {
            action.run();
            complete = true;
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            running = false;
        }
    }
}
