package org.yeastrc.limelight.xml.casanovo.reader;

import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchMetadataParser {

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("MTD\\s+software\\[1]\\s+\\[MS, MS:1003281, Casanovo, (.+)]");
    private static final Pattern MODEL_PATTERN =
            Pattern.compile("MTD\\s+software\\[1]-setting\\[1]\\s+model = (.+)");
    private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("MTD\\s+ms_run\\[(\\d+)]-location\\s+file:///(.+)");

    /** Parse metadata from the mzTab file at the given path (read as UTF-8). */
    public SearchMetadata parse(String filePath) throws IOException, MissingMetadataException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    /**
     * Parse metadata from the MTD section of an mzTab document. Reading stops at the first non-MTD
     * line, since the metadata section is contiguous at the top of an mzTab file.
     */
    public SearchMetadata parse(BufferedReader reader) throws IOException, MissingMetadataException {
        SearchMetadata metadata = new SearchMetadata();

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("MTD")) {
                break;
            }

            Matcher versionMatcher = VERSION_PATTERN.matcher(line);
            if (versionMatcher.find()) {
                metadata.setCasanovoVersion(versionMatcher.group(1));
            }

            Matcher modelMatcher = MODEL_PATTERN.matcher(line);
            if (modelMatcher.find()) {
                metadata.setModel(modelMatcher.group(1));
            }

            Matcher fileNameMatcher = FILE_NAME_PATTERN.matcher(line);
            if (fileNameMatcher.find()) {
                int runIndex = Integer.parseInt(fileNameMatcher.group(1));
                String fullPath = fileNameMatcher.group(2);
                metadata.addScanFileName(runIndex, fullPath.substring(fullPath.lastIndexOf('/') + 1));
            }
        }

        if (metadata.getCasanovoVersion() == null) {
            throw new MissingMetadataException("Casanovo version not found in the mzTab file.");
        }
        if (metadata.getModel() == null) {
            throw new MissingMetadataException("Model information not found in the mzTab file.");
        }
        if (!metadata.hasScanFileNames()) {
            throw new MissingMetadataException("No ms_run location (scan file name) found in the mzTab file.");
        }

        return metadata;
    }
}
