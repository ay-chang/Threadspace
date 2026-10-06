package com.threadspace.backend.integration.google;

/**
 * Failure to exchange a service account key for an access token.
 * {@code rejected} means Google refused the key (deleted, disabled, wrong
 * signature); otherwise Google couldn't be reached.
 */
public class GoogleTokenException extends RuntimeException {

    private final boolean rejected;

    public GoogleTokenException(boolean rejected, String message, Throwable cause) {
        super(message, cause);
        this.rejected = rejected;
    }

    public boolean isRejected() {
        return rejected;
    }
}
