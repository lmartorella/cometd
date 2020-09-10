package org.cometd.server.websocket.common;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simulate a slow channel. Schedule send task in a queue
 */
public class TimeoutScheduledService {

	private final int periodMs;
    private Throwable lastFail;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final Queue<Callable<Void>> runnables = new ConcurrentLinkedQueue<>();
    private boolean running = false;

    public TimeoutScheduledService(final int periodMs) {
        this.periodMs = periodMs;
    }

    public synchronized Throwable getLastFail() {
        return lastFail;
    }

    public synchronized void resetFail() {
        lastFail = null;
    }

    public synchronized void setFail(final Throwable x) {
        lastFail = x;
    }

    private synchronized void start() {
        running = true;
        executorService.schedule(() -> {
            final Callable<Void> task = runnables.remove();
            try {
                task.call();
                resetFail();
            } catch (final Throwable x) {
                setFail(x);
            }

            synchronized (this) {
                if (!runnables.isEmpty()) {
                    start();
                } else {
                    running = false;
                }
            }

        }, periodMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void schedule(final Callable<Void> batch) {
        runnables.add(batch);
        if (!running) {
            start();
        }
	}
}
