package org.yeastrc.limelight.xml.casanovo.main;

/**
 * Thrown when converting Casanovo output to Limelight XML fails for an expected reason — for example a
 * missing input file, missing required metadata, malformed mzTab, an unknown modification, or no
 * results found. The CLI ({@link MainProgram}) maps this to a non-zero exit code; unexpected
 * {@link RuntimeException}s and {@link Error}s are not represented by this type.
 */
public class CasanovoConversionException extends Exception {

    private static final long serialVersionUID = 1L;

    public CasanovoConversionException(String message) {
        super(message);
    }

    public CasanovoConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
