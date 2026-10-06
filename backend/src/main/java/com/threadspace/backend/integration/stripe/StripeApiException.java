package com.threadspace.backend.integration.stripe;

/**
 * A non-2xx response from the Stripe API, carrying Stripe's own error fields
 * ({@code error.type}, {@code error.code}, {@code error.message}).
 */
public class StripeApiException extends RuntimeException {

    private final int statusCode;
    private final String type;
    private final String code;
    private final String stripeMessage;

    public StripeApiException(int statusCode, String type, String code, String stripeMessage, Throwable cause) {
        super("Stripe API error " + statusCode + (stripeMessage != null ? ": " + stripeMessage : ""), cause);
        this.statusCode = statusCode;
        this.type = type;
        this.code = code;
        this.stripeMessage = stripeMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getStripeMessage() {
        return stripeMessage;
    }
}
