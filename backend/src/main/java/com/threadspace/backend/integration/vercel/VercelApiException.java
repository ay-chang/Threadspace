package com.threadspace.backend.integration.vercel;

public class VercelApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public VercelApiException(int statusCode, String responseBody, Throwable cause) {
        super("Vercel API error: " + statusCode, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
