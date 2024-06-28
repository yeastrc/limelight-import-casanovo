package org.yeastrc.limelight.xml.casanovo.reader;

import org.yeastrc.limelight.xml.casanovo.objects.LogFileData;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFileParser {

    public static LogFileData parseLogFile(File logFile) throws IOException {

        LogFileData logFileData = new LogFileData();

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            for(String line = br.readLine(); line != null; line = br.readLine()) {
                if(line.startsWith("INFO: Version:")) {
                    Pattern p = Pattern.compile("INFO: Version: (.+)");
                    Matcher m = p.matcher(line);

                    if(m.matches()) {
                        logFileData.setVersion(m.group(1));
                    }

                } else if(line.startsWith("INFO: Command used:")) {
                    Pattern p = Pattern.compile(".+--static_mods\\s+(\\S+)\\s+.+");
                    Matcher m = p.matcher(line);

                    if(m.matches()) {
                        logFileData.setStaticModsString(m.group(1));
                    }
                }
            }
        }

        return logFileData;
    }
}

