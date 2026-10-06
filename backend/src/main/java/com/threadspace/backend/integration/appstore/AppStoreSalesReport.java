package com.threadspace.backend.integration.appstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Parses Apple's daily SALES / SUMMARY report: a gzipped, tab-separated file
 * with one header row.
 *
 * Columns are located by header name rather than position, so an added or
 * reordered column in a future report version doesn't silently shift values
 * into the wrong fields.
 */
public final class AppStoreSalesReport {

    /** What a row's Product Type Identifier says the units represent. */
    public enum Kind {
        DOWNLOAD, REDOWNLOAD, UPDATE, IN_APP, OTHER
    }

    public record Row(
            String title,
            String appleIdentifier,
            String productType,
            Kind kind,
            int units,
            BigDecimal proceedsPerUnit,
            String proceedsCurrency,
            String countryCode) {

        /** Total developer proceeds for the row. Negative units (refunds) give negative proceeds. */
        public BigDecimal proceeds() {
            return proceedsPerUnit.multiply(BigDecimal.valueOf(units));
        }
    }

    static final String COL_TITLE = "Title";
    static final String COL_APPLE_ID = "Apple Identifier";
    static final String COL_PRODUCT_TYPE = "Product Type Identifier";
    static final String COL_UNITS = "Units";
    static final String COL_PROCEEDS = "Developer Proceeds";
    static final String COL_PROCEEDS_CURRENCY = "Currency of Proceeds";
    static final String COL_COUNTRY = "Country Code";

    private AppStoreSalesReport() {
    }

    public static List<Row> parse(byte[] body) {
        String text = new String(decompress(body), StandardCharsets.UTF_8);
        String[] lines = text.split("\r?\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            return List.of();
        }

        Map<String, Integer> columns = new HashMap<>();
        String[] header = lines[0].split("\t", -1);
        for (int i = 0; i < header.length; i++) {
            // Strip a UTF-8 BOM from the first header cell if present.
            columns.put(header[i].replace("﻿", "").trim(), i);
        }
        for (String required : List.of(COL_TITLE, COL_PRODUCT_TYPE, COL_UNITS, COL_PROCEEDS, COL_PROCEEDS_CURRENCY)) {
            if (!columns.containsKey(required)) {
                throw new IllegalStateException("Sales report is missing the '" + required + "' column");
            }
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cells = lines[i].split("\t", -1);
            String productType = cell(cells, columns, COL_PRODUCT_TYPE);
            rows.add(new Row(
                    cell(cells, columns, COL_TITLE),
                    cell(cells, columns, COL_APPLE_ID),
                    productType,
                    classify(productType),
                    parseInt(cell(cells, columns, COL_UNITS)),
                    parseDecimal(cell(cells, columns, COL_PROCEEDS)),
                    cell(cells, columns, COL_PROCEEDS_CURRENCY).toUpperCase(),
                    cell(cells, columns, COL_COUNTRY)));
        }
        return rows;
    }

    /**
     * Maps Apple's Product Type Identifier to what the units mean. Prefixes
     * cover platform variants: 1 / 1F / 1T are iPhone, universal and iPad
     * downloads, F1 is Mac, and so on.
     */
    static Kind classify(String productType) {
        String t = productType == null ? "" : productType.trim().toUpperCase();
        if (t.startsWith("IA") || t.startsWith("FI")) {
            return Kind.IN_APP;
        }
        if (t.startsWith("7") || t.startsWith("F7")) {
            return Kind.UPDATE;
        }
        if (t.startsWith("3") || t.startsWith("F3")) {
            return Kind.REDOWNLOAD;
        }
        if (t.startsWith("1") || t.startsWith("F1")) {
            return Kind.DOWNLOAD;
        }
        return Kind.OTHER;
    }

    /**
     * Apple serves the report as a gzip file. Handle a body that is plain text
     * (already decoded by an intermediary) or gzipped twice (a gzip file sent
     * with gzip transfer encoding) as well.
     */
    static byte[] decompress(byte[] body) {
        byte[] current = body == null ? new byte[0] : body;
        for (int i = 0; i < 2 && isGzip(current); i++) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(current));
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                current = out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("Sales report could not be decompressed", e);
            }
        }
        return current;
    }

    private static boolean isGzip(byte[] bytes) {
        return bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
    }

    private static String cell(String[] cells, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= cells.length ? "" : cells[index].trim();
    }

    private static int parseInt(String value) {
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return new BigDecimal(value).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
