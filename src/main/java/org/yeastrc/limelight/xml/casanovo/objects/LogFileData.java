package org.yeastrc.limelight.xml.casanovo.objects;

public class LogFileData {

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStaticModsString() {
        return staticModsString;
    }

    public void setStaticModsString(String staticModsString) {
        this.staticModsString = staticModsString;
    }

    private String version;
    private String staticModsString;
}