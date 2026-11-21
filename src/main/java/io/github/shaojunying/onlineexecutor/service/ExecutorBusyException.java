package io.github.shaojunying.onlineexecutor.service;

/**
 * All execution slots are in use. The client can retry later.
 */
public class ExecutorBusyException extends RuntimeException {

    public ExecutorBusyException(int maxConcurrent) {
        super("All " + maxConcurrent + " execution slots are in use. Try again later.");
    }
}
