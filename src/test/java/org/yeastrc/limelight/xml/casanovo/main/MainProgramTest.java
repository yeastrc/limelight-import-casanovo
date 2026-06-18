package org.yeastrc.limelight.xml.casanovo.main;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the CLI shell via picocli's {@code execute(...)}, which returns the exit code rather than
 * calling {@code System.exit} (that lives only in {@code main}). This makes the success / conversion-error
 * / usage-error exit codes directly assertable.
 */
class MainProgramTest {

    @Test
    void successReturnsExitCodeZero() throws Exception {
        File out = tempOutput();
        int code = run("-m", resourcePath("casanovo/single-file.mztab"),
                       "-c", resourcePath("casanovo/casanovo-config.yaml"),
                       "-o", out.getAbsolutePath());
        assertEquals(0, code);
    }

    @Test
    void conversionErrorReturnsExitCodeOne() throws Exception {
        File out = tempOutput();
        int code = run("-m", "/no/such/file.mztab",
                       "-c", resourcePath("casanovo/casanovo-config.yaml"),
                       "-o", out.getAbsolutePath());
        assertEquals(1, code);
    }

    @Test
    void usageErrorReturnsExitCodeTwo() throws Exception {
        // required -m omitted -> picocli reports a usage error with exit code 2
        int code = run("-c", resourcePath("casanovo/casanovo-config.yaml"),
                       "-o", "/tmp/unused.xml");
        assertEquals(2, code);
    }

    private static int run(String... args) {
        return new CommandLine(new MainProgram()).execute(args);
    }

    private static File tempOutput() throws Exception {
        File out = File.createTempFile("mainprogram-test", ".xml");
        out.deleteOnExit();
        return out;
    }

    private static String resourcePath(String name) throws Exception {
        return new File(MainProgramTest.class.getClassLoader().getResource(name).toURI()).getAbsolutePath();
    }
}
