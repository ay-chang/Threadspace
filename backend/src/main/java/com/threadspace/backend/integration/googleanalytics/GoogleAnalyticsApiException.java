package com.threadspace.backend.integration.googleanalytics;

/**
 * A non-2xx response from the Google Analytics Data API. Carries the
 * {@code error.status} (e.g. PERMISSION_DENIED) and, when present, the
 * ErrorInfo {@code reason} (e.g. SERVICE_DISABLED) that tell failures apart.
 */
public class GoogleAnalyticsApiException extends RuntimeException {

    private final int statusCode;
    private final String status;
    private final String reason;
    private final String apiMessage;

    public GoogleAnalyticsApiException(int statusCode, String status, String reason, String apiMessage,
            Throwable cause) {
        super("Google Analytics Data API error " + statusCode + (apiMessage != null ? ": " + apiMessage : ""), cause);
        this.statusCode = statusCode;
        this.status = status;
        this.reason = reason;
        this.apiMessage = apiMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getApiMessage() {
        return apiMessage;
    }

    /** The Data API isn't enabled in the service account's Google Cloud project. */
    public boolean isApiDisabled() {
        return "SERVICE_DISABLED".equals(reason)
                || (apiMessage != null && (apiMessage.contains("has not been used in project")
                        || apiMessage.contains("it is disabled")));
    }
}
