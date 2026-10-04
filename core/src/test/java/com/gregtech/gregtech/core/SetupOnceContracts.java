package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.mod.SetupOnce;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Startup regression checks that do not launch either game client. */
public final class SetupOnceContracts {
    public static void main(String[] args) {
        System.out.println("Setup once contracts passed: " + verify() + " checks");
    }

    public static int verify() {
        var once = new SetupOnce();
        var calls = new AtomicInteger();
        once.run(calls::incrementAndGet);
        once.run(() -> { throw new AssertionError("Successful setup ran twice"); });
        check(calls.get() == 1, "Duplicate callbacks register recipes only once");

        var failed = new SetupOnce();
        var cause = new IllegalArgumentException("Original recipe failure");
        check(thrown(() -> failed.run(() -> { throw cause; })) == cause, "First recipe failure is propagated");
        check(thrown(() -> failed.run(calls::incrementAndGet)) == cause, "Repeated setup preserves the original failure");
        check(calls.get() == 1, "Failed setup does not repeat partial registrations");

        var broken = new SetupOnce();
        var error = new LinkageError("Original class loading failure");
        check(thrown(() -> broken.run(() -> { throw error; })) == error, "Class loading errors propagate");
        check(thrown(() -> broken.run(calls::incrementAndGet)) == error, "Class loading errors remain visible");

        var recursive = new SetupOnce();
        Throwable recursion = thrown(() -> recursive.run(() -> recursive.run(calls::incrementAndGet)));
        check(recursion instanceof IllegalStateException, "Recursive setup is diagnosed");
        check(thrown(() -> recursive.run(calls::incrementAndGet)) == recursion, "Recursive failure cannot become a successful setup");

        var concurrent = new SetupOnce();
        var concurrentCalls = new AtomicInteger();
        var ready = new CountDownLatch(8);
        var start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < 8; i++) futures.add(workers.submit(() -> {
                ready.countDown();
                await(start);
                concurrent.run(concurrentCalls::incrementAndGet);
            }));
            check(ready.await(5, TimeUnit.SECONDS), "Concurrent setup callbacks are ready");
            start.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            check(concurrentCalls.get() == 1, "Concurrent callbacks run the recipe registration once");
        } catch (Exception exception) {
            throw new AssertionError("Concurrent setup failed", exception);
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
        return 10;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Setup start timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static Throwable thrown(Runnable action) {
        try { action.run(); }
        catch (RuntimeException | Error exception) { return exception; }
        throw new AssertionError("Expected setup failure");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
