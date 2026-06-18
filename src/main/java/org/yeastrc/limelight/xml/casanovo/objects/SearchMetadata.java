package org.yeastrc.limelight.xml.casanovo.objects;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Metadata parsed from an mzTab file's MTD section. A single Casanovo run may search more than one
 * spectrum file, so scan file names are keyed by their mzTab {@code ms_run[N]} index.
 */
public class SearchMetadata {

    private String casanovoVersion;
    private String model;
    private final Map<Integer, String> scanFileNameByRunIndex = new TreeMap<>();

    public String getCasanovoVersion() { return casanovoVersion; }
    public String getModel() { return model; }

    public void setCasanovoVersion(String version) { this.casanovoVersion = version; }
    public void setModel(String model) { this.model = model; }

    /** Register the spectrum file name for a given mzTab {@code ms_run} index (1-based). */
    public void addScanFileName(int runIndex, String fileName) {
        this.scanFileNameByRunIndex.put(runIndex, fileName);
    }

    /** Spectrum file name for the given {@code ms_run} index, or {@code null} if unknown. */
    public String getScanFileName(int runIndex) {
        return this.scanFileNameByRunIndex.get(runIndex);
    }

    /** All registered {@code ms_run} index &rarr; spectrum file name mappings. */
    public Map<Integer, String> getScanFileNames() {
        return Collections.unmodifiableMap(scanFileNameByRunIndex);
    }

    public boolean hasScanFileNames() {
        return !scanFileNameByRunIndex.isEmpty();
    }
}
