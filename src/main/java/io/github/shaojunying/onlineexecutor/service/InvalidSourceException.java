package io.github.shaojunying.onlineexecutor.service;

/**
 * The request cannot run: the source is empty, or the source or stdin is too long.
 */
public class InvalidSourceException extends RuntimeException {

    public InvalidSourceException(String message) {
        super(message);
    }
}
