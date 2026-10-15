package com.platetectonics.geosim.parallel;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Splits loops over cells or rows across a fixed number of worker threads without changing results
 * (D-5 determinism). Loop bodies may only write state owned by their own index; anything shared (sums,
 * lists, budgets) stays in a sequential pass in index order, so floating-point sums add up in exactly
 * the order of the single-threaded code and every thread count gives the same planet bit for bit.
 */
public final class Parallel implements AutoCloseable {
    /** Runs every loop on the calling thread. */
    public static final Parallel SERIAL = new Parallel(1);

    private final int threads;
    private final ForkJoinPool pool;

    private Parallel(int threads) {
        this.threads = threads;
        this.pool = threads > 1 ? new ForkJoinPool(threads) : null;
    }

    /** A pool of {@code threads} workers ({@link #SERIAL} for 1 or fewer). */
    public static Parallel of(int threads) {
        return threads <= 1 ? SERIAL : new Parallel(threads);
    }

    /** All available processors. */
    public static Parallel allCores() {
        return of(Runtime.getRuntime().availableProcessors());
    }

    public int threads() {
        return threads;
    }

    /** Body of a loop over the index range {@code [from, to)}. */
    @FunctionalInterface
    public interface Range {
        void run(int from, int to);
    }

    /**
     * Runs {@code body} over {@code [0, n)} in contiguous chunks of at least {@code minChunk} indices and
     * returns when all are done.
     */
    public void range(int n, int minChunk, Range body) {
        int chunks = pool == null ? 1 : Math.min(threads * 4, n / Math.max(1, minChunk));
        if (chunks <= 1) {
            body.run(0, n);
            return;
        }
        ForkJoinTask<?>[] tasks = new ForkJoinTask<?>[chunks - 1];
        for (int c = 1; c < chunks; c++) {
            int from = (int) ((long) n * c / chunks);
            int to = (int) ((long) n * (c + 1) / chunks);
            tasks[c - 1] = pool.submit(() -> body.run(from, to));
        }
        // the calling thread takes the first chunk itself
        body.run(0, (int) ((long) n / chunks));
        for (ForkJoinTask<?> t : tasks) {
            t.join();
        }
    }

    /** Cells: chunks of at least 4,096. */
    public void cells(int n, Range body) {
        range(n, 4_096, body);
    }

    /** Rows of the grid, at least 4 per chunk. */
    public void rows(int h, Range body) {
        range(h, 4, body);
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.shutdown();
        }
    }
}
