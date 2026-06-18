package org.yeastrc.limelight.xml.casanovo.main;

import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionParameters;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionProgramInfo;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the real {@link ConverterRunner} in-process (now possible because it throws rather than
 * calling {@code System.exit}): verifies a successful run writes a schema-valid file, and that the
 * expected failure modes surface as {@link CasanovoConversionException}.
 */
class ConverterRunnerTest {

    @Test
    void successfulConversionWritesValidatableOutput() throws Exception {
        ConversionParameters cp = params(resource("casanovo/single-file.mztab"), resource("casanovo/casanovo-config.yaml"));

        ConverterRunner.createInstance().convertToLimelightXML(cp);

        assertTrue(cp.getLimelightXMLOutputFile().length() > 0, "output file should be written");
        LimelightXMLValidator.validateLimelightXML(cp.getLimelightXMLOutputFile()); // re-validate the written file
    }

    @Test
    void missingMztabThrowsConversionException() throws Exception {
        ConversionParameters cp = params(new File("/no/such/file.mztab"), resource("casanovo/casanovo-config.yaml"));

        CasanovoConversionException ex = assertThrows(CasanovoConversionException.class,
                () -> ConverterRunner.createInstance().convertToLimelightXML(cp));
        assertTrue(ex.getMessage().toLowerCase().contains("mztab"), ex.getMessage());
    }

    @Test
    void missingConfigThrowsConversionException() throws Exception {
        ConversionParameters cp = params(resource("casanovo/single-file.mztab"), new File("/no/such/config.yaml"));

        CasanovoConversionException ex = assertThrows(CasanovoConversionException.class,
                () -> ConverterRunner.createInstance().convertToLimelightXML(cp));
        assertTrue(ex.getMessage().toLowerCase().contains("config"), ex.getMessage());
    }

    @Test
    void noResultsThrowsConversionException() throws Exception {
        ConversionParameters cp = params(resource("casanovo/no-results.mztab"), resource("casanovo/casanovo-config.yaml"));

        CasanovoConversionException ex = assertThrows(CasanovoConversionException.class,
                () -> ConverterRunner.createInstance().convertToLimelightXML(cp));
        assertTrue(ex.getMessage().toLowerCase().contains("results"), ex.getMessage());
    }

    private static ConversionParameters params(File mztab, File config) throws Exception {
        File out = File.createTempFile("converter-runner-test", ".xml");
        out.deleteOnExit();

        ConversionParameters cp = new ConversionParameters();
        cp.setConfigFile(config);
        cp.setMztabFile(mztab);
        cp.setLimelightXMLOutputFile(out);
        cp.setConversionProgramInfo(ConversionProgramInfo.createInstance("test"));
        return cp;
    }

    private static File resource(String name) throws Exception {
        return new File(ConverterRunnerTest.class.getClassLoader().getResource(name).toURI());
    }
}
