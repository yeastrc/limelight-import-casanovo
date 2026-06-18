package org.yeastrc.limelight.xml.casanovo.reader;

/** Thrown when an mzTab file is missing metadata required for conversion (version, model, or scan file). */
class MissingMetadataException extends Exception {

    private static final long serialVersionUID = 1L;

    MissingMetadataException(String message) {
        super(message);
    }
}
