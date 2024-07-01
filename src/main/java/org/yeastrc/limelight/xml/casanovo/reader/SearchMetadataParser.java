package org.yeastrc.limelight.xml.casanovo.reader;

import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchMetadataParser {
    public SearchMetadata parse(String filePath) throws IOException, MissingMetadataException {
        SearchMetadata metadata = new SearchMetadata();
        Pattern versionPattern = Pattern.compile("MTD\\s+software\\[1]\\s+\\[MS, MS:1003281, Casanovo, (.+)]");
        Pattern modelPattern = Pattern.compile("MTD\\s+software\\[1]-setting\\[1]\\s+model = (.+)");
        Pattern fileNamePattern = Pattern.compile("MTD\\s+ms_run\\[1]-location\\s+file:///(.+)");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("MTD")) {
                    break; // Stop parsing when we encounter a non-MTD line
                }

                Matcher versionMatcher = versionPattern.matcher(line);
                if (versionMatcher.find()) {
                    metadata.setCasanovoVersion(versionMatcher.group(1));
                }

                Matcher modelMatcher = modelPattern.matcher(line);
                if (modelMatcher.find()) {
                    metadata.setModel(modelMatcher.group(1));
                }

                Matcher fileNameMatcher = fileNamePattern.matcher(line);
                if (fileNameMatcher.find()) {
                    String fullPath = fileNameMatcher.group(1);
                    metadata.setScanFileName(fullPath.substring(fullPath.lastIndexOf('/') + 1));
                }
            }
        }

        // Check if all required metadata was found
        if (metadata.getCasanovoVersion() == null) {
            throw new MissingMetadataException("Casanovo version not found in the mzTab file.");
        }
        if (metadata.getModel() == null) {
            throw new MissingMetadataException("Model information not found in the mzTab file.");
        }
        if (metadata.getScanFileName() == null) {
            throw new MissingMetadataException("Scan file name not found in the mzTab file.");
        }

        return metadata;
    }
}

class MissingMetadataException extends Exception {
    public MissingMetadataException(String message) {
        super(message);
    }
}