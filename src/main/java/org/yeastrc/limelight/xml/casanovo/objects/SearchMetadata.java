package org.yeastrc.limelight.xml.casanovo.objects;

public class SearchMetadata {
    private String casanovoVersion;
    private String model;
    private String scanFileName;

    public String getCasanovoVersion() { return casanovoVersion; }
    public String getModel() { return model; }
    public String getScanFileName() { return scanFileName; }

    public void setCasanovoVersion(String version) { this.casanovoVersion = version; }
    public void setModel(String model) { this.model = model; }
    public void setScanFileName(String fileName) { this.scanFileName = fileName; }
}
