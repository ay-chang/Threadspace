package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.threadspace.backend.integration.appstore.AppStoreSalesReport.Kind;
import com.threadspace.backend.integration.appstore.AppStoreSalesReport.Row;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AppStoreSalesReportTest {

    /** The real column layout of a SALES / SUMMARY / DAILY report, version 1_0. */
    static final String HEADER = String.join("\t",
            "Provider", "Provider Country", "SKU", "Developer", "Title", "Version",
            "Product Type Identifier", "Units", "Developer Proceeds", "Begin Date", "End Date",
            "Customer Currency", "Country Code", "Currency of Proceeds", "Apple Identifier",
            "Customer Price", "Promo Code", "Parent Identifier", "Subscription", "Period",
            "Category", "CMB", "Device", "Supported Platforms", "Proceeds Reason",
            "Preserved Pricing", "Client", "Order Type");

    static String row(String title, String appleId, String productType, int units, String proceeds,
            String proceedsCurrency, String country) {
        return String.join("\t",
                "APPLE", "US", "SKU1", "Dev Co", title, "1.0",
                productType, String.valueOf(units), proceeds, "09/25/2026", "09/25/2026",
                proceedsCurrency, country, proceedsCurrency, appleId,
                "0", "", "", "", "", "Productivity", "", "iPhone", "iOS", "", "", "", "");
    }

    static String sampleReport() {
        return String.join("\n",
                HEADER,
                row("Habit Tracker", "1111", "1F", 10, "0", "USD", "US"),
                row("Habit Tracker", "1111", "7", 50, "0", "USD", "US"),
                row("Pro Upgrade", "2222", "IA1", 3, "2.99", "USD", "US"),
                row("Habit Tracker", "1111", "3F", 4, "0", "USD", "US"),
                row("Paid Notes", "3333", "1", 2, "0.70", "EUR", "DE"),
                row("Pro Upgrade", "2222", "IA1", -1, "2.99", "USD", "US")) + "\n";
    }

    @Test
    void parsesAGzippedReport() {
        List<Row> rows = AppStoreSalesReport.parse(TestKeys.gzip(sampleReport().getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).hasSize(6);
        Row first = rows.get(0);
        assertThat(first.title()).isEqualTo("Habit Tracker");
        assertThat(first.appleIdentifier()).isEqualTo("1111");
        assertThat(first.kind()).isEqualTo(Kind.DOWNLOAD);
        assertThat(first.units()).isEqualTo(10);
        assertThat(first.countryCode()).isEqualTo("US");

        assertThat(rows).extracting(Row::kind).containsExactly(
                Kind.DOWNLOAD, Kind.UPDATE, Kind.IN_APP, Kind.REDOWNLOAD, Kind.DOWNLOAD, Kind.IN_APP);
    }

    @Test
    void refundRowsHaveNegativeProceeds() {
        List<Row> rows = AppStoreSalesReport.parse(TestKeys.gzip(sampleReport().getBytes(StandardCharsets.UTF_8)));

        Row refund = rows.get(5);
        assertThat(refund.units()).isEqualTo(-1);
        assertThat(refund.proceeds()).isEqualByComparingTo("-2.99");
        assertThat(rows.get(2).proceeds()).isEqualByComparingTo("8.97");
    }

    @Test
    void findsColumnsByNameWhenTheyAreReordered() {
        String report = "Units\tCurrency of Proceeds\tTitle\tDeveloper Proceeds\tProduct Type Identifier\n"
                + "7\tGBP\tReordered App\t1.50\tIA1\n";

        Row row = AppStoreSalesReport.parse(report.getBytes(StandardCharsets.UTF_8)).get(0);

        assertThat(row.title()).isEqualTo("Reordered App");
        assertThat(row.units()).isEqualTo(7);
        assertThat(row.proceedsCurrency()).isEqualTo("GBP");
        assertThat(row.kind()).isEqualTo(Kind.IN_APP);
    }

    @Test
    void handlesPlainDoubleGzippedAndBomPrefixedBodies() {
        byte[] plain = ("﻿" + sampleReport()).getBytes(StandardCharsets.UTF_8);
        assertThat(AppStoreSalesReport.parse(plain)).hasSize(6);

        byte[] doubleGzipped = TestKeys.gzip(TestKeys.gzip(sampleReport().getBytes(StandardCharsets.UTF_8)));
        assertThat(AppStoreSalesReport.parse(doubleGzipped)).hasSize(6);
    }

    @Test
    void rejectsAReportWithoutTheColumnsItNeeds() {
        byte[] report = "Title\tUnits\nApp\t1\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> AppStoreSalesReport.parse(report))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product Type Identifier");
    }

    @Test
    void emptyBodyHasNoRows() {
        assertThat(AppStoreSalesReport.parse(new byte[0])).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "1, DOWNLOAD", "1F, DOWNLOAD", "1T, DOWNLOAD", "F1, DOWNLOAD", "1-B, DOWNLOAD",
            "7, UPDATE", "7F, UPDATE", "7T, UPDATE", "F7, UPDATE",
            "3, REDOWNLOAD", "3F, REDOWNLOAD", "F3, REDOWNLOAD",
            "IA1, IN_APP", "IAY, IN_APP", "IA9, IN_APP", "IAC, IN_APP", "FI1, IN_APP",
            "ZZ, OTHER"
    })
    void classifiesProductTypes(String productType, Kind expected) {
        assertThat(AppStoreSalesReport.classify(productType)).isEqualTo(expected);
    }
}
