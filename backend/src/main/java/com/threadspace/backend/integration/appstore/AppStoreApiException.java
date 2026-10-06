package com.threadspace.backend.integration.appstore;

/**
 * A non-2xx response from the App Store Connect API, carrying the first entry of
 * Apple's {@code errors} array.
 */
public class AppStoreApiException extends RuntimeException {

    private final int statusCode;
    private final String code;
    private final String detail;

    public AppStoreApiException(int statusCode, String code, String detail, Throwable cause) {
        super("App Store Connect API error " + statusCode + (detail != null ? ": " + detail : ""), cause);
        this.statusCode = statusCode;
        this.code = code;
        this.detail = detail;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }
}
