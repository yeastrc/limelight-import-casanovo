package org.yeastrc.limelight.xml.casanovo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.yeastrc.limelight.xml.casanovo.main.LimelightXMLValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.yeastrc.limelight.xml.casanovo.integration.ConversionTestSupport.convert;

/**
 * Opt-in end-to-end smoke test over a real, large Casanovo mzTab that is intentionally NOT committed to
 * the repository. Supply the input via system properties and run the dedicated task:
 *
 * <pre>
 *   ./gradlew fullScaleSmokeTest \
 *       -Dcasanovo.smoke.mztab=/path/to/results.mztab \
 *       -Dcasanovo.smoke.config=/path/to/casanovo.yaml
 * </pre>
 *
 * It converts the file to a temporary output (deleted on exit — the output can be hundreds of MB),
 * validates it against the Limelight schema at full scale, and checks that PSM / scan-file / peptide
 * counts are consistent with the raw input. Without both properties the test is skipped.
 */
@EnabledIfSystemProperty(named = "casanovo.smoke.mztab", matches = ".+")
@EnabledIfSystemProperty(named = "casanovo.smoke.config", matches = ".+")
class FullScaleSmokeTest {

    private static final Pattern MS_RUN_LOCATION =
            Pattern.compile("MTD\\s+ms_run\\[(\\d+)]-location\\s+file:///(.+)");

    @Test
    void convertsRealFileValidatesAndCountsAreConsistent() throws Throwable {
        File mztab = new File(System.getProperty("casanovo.smoke.mztab"));
        File config = new File(System.getProperty("casanovo.smoke.config"));
        assertTrue(mztab.isFile(), "mztab file not found: " + mztab);
        assertTrue(config.isFile(), "config file not found: " + config);

        // Facts derived independently from the raw input.
        int inputPsmRows = countPsmRows(mztab);
        Set<String> declaredRunFiles = msRunBasenames(mztab);
        assertTrue(inputPsmRows > 0, "input mzTab has no PSM rows");
        assertFalse(declaredRunFiles.isEmpty(), "input mzTab declares no ms_run locations");

        File out = convert(mztab, config);

        // 1. The output validates against the Limelight schema at full scale (also a throughput/memory check).
        LimelightXMLValidator.validateLimelightXML(out);

        // 2. Exactly one PSM per input PSM row.
        int outputPsms = countOccurrences(out, "<psm ");
        assertEquals(inputPsmRows, outputPsms, "every PSM row should yield exactly one PSM");

        // 3. Every per-PSM scan file name was a declared ms_run location.
        Set<String> outputFiles = attributeValues(out, "scan_file_name=\"");
        assertFalse(outputFiles.isEmpty(), "output has no scan file names");
        assertTrue(declaredRunFiles.containsAll(outputFiles),
                "output references files not declared as ms_run locations: " + outputFiles + " not within " + declaredRunFiles);

        // 4. Reported peptides collapse PSMs: 0 < reportedPeptides <= PSMs.
        int reportedPeptides = countOccurrences(out, "<reported_peptide ");
        assertTrue(reportedPeptides > 0 && reportedPeptides <= outputPsms,
                "reported peptides (" + reportedPeptides + ") should be within 1.." + outputPsms);

        System.out.println("[full-scale smoke] PSMs=" + outputPsms
                + " reportedPeptides=" + reportedPeptides
                + " files=" + outputFiles);
    }

    private static int countPsmRows(File mztab) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(mztab.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PSM\t")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Set<String> msRunBasenames(File mztab) throws IOException {
        Set<String> files = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(mztab.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("MTD")) {
                    break;
                }
                Matcher matcher = MS_RUN_LOCATION.matcher(line);
                if (matcher.find()) {
                    String path = matcher.group(2);
                    files.add(path.substring(path.lastIndexOf('/') + 1));
                }
            }
        }
        return files;
    }

    private static int countOccurrences(File xml, String needle) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(xml.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = 0;
                while ((idx = line.indexOf(needle, idx)) != -1) {
                    count++;
                    idx += needle.length();
                }
            }
        }
        return count;
    }

    private static Set<String> attributeValues(File xml, String attributePrefix) throws IOException {
        Set<String> values = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(xml.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = 0;
                while ((idx = line.indexOf(attributePrefix, idx)) != -1) {
                    int start = idx + attributePrefix.length();
                    int end = line.indexOf('"', start);
                    if (end == -1) {
                        break;
                    }
                    values.add(line.substring(start, end));
                    idx = end + 1;
                }
            }
        }
        return values;
    }
}
