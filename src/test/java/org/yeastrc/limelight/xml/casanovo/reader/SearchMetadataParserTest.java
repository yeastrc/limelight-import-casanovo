package org.yeastrc.limelight.xml.casanovo.reader;

import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Tests mzTab metadata parsing: version, model, multi-run file mapping, the MTD-section boundary,
 *  and the required-field error paths. */
class SearchMetadataParserTest {

    private static SearchMetadata parse(String mztab) throws Exception {
        return new SearchMetadataParser().parse(new BufferedReader(new StringReader(mztab)));
    }

    private static File resource(String name) throws Exception {
        return new File(SearchMetadataParserTest.class.getClassLoader().getResource(name).toURI());
    }

    @Test
    void parsesVersionModelAndSingleRunFromRealFixture() throws Exception {
        SearchMetadata md = new SearchMetadataParser()
                .parse(resource("casanovo/single-file.mztab").getAbsolutePath());

        assertEquals("5.2.0", md.getCasanovoVersion());
        assertTrue(md.getModel().contains("casanovo_orbitrap_v5-2-0.ckpt"), "model was: " + md.getModel());
        assertEquals(Map.of(1, "test2.mzML"), md.getScanFileNames());
    }

    @Test
    void parsesAllRunLocationsFromMultiFileFixture() throws Exception {
        SearchMetadata md = new SearchMetadataParser()
                .parse(resource("casanovo/two-files.mztab").getAbsolutePath());

        assertEquals(Map.of(1, "test1.mzML", 2, "test2.mzML"), md.getScanFileNames());
    }

    @Test
    void runLocationsAreKeyedByIndexRegardlessOfDeclarationOrder() throws Exception {
        String mztab = String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tms_run[2]-location\tfile:///data/run/second.mzML",
                "MTD\tsoftware[1]\t[MS, MS:1003281, Casanovo, 4.2.1]",
                "MTD\tsoftware[1]-setting[1]\tmodel = /m/model.ckpt",
                "MTD\tms_run[1]-location\tfile:///data/run/first.mzML");

        SearchMetadata md = parse(mztab);

        assertEquals("4.2.1", md.getCasanovoVersion());
        assertEquals("first.mzML", md.getScanFileName(1));
        assertEquals("second.mzML", md.getScanFileName(2));
        assertEquals(2, md.getScanFileNames().size());
    }

    @Test
    void scanFileNameStripsDirectoryFromLocation() throws Exception {
        String mztab = baseMztab("file:///deep/nested/path/SampleX_rep1.mzML");
        assertEquals("SampleX_rep1.mzML", parse(mztab).getScanFileName(1));
    }

    @Test
    void stopsParsingAtFirstNonMtdLine() throws Exception {
        // The ms_run[9] location appears AFTER a non-MTD line, so it must be ignored.
        String mztab = String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tsoftware[1]\t[MS, MS:1003281, Casanovo, 5.2.0]",
                "MTD\tsoftware[1]-setting[1]\tmodel = /m/model.ckpt",
                "MTD\tms_run[1]-location\tfile:///data/only.mzML",
                "PSH\tsequence\tspectra_ref",
                "MTD\tms_run[9]-location\tfile:///data/should_be_ignored.mzML");

        SearchMetadata md = parse(mztab);

        assertEquals(Map.of(1, "only.mzML"), md.getScanFileNames());
        assertNull(md.getScanFileName(9));
    }

    @Test
    void missingVersionThrows() {
        String mztab = String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tsoftware[1]-setting[1]\tmodel = /m/model.ckpt",
                "MTD\tms_run[1]-location\tfile:///data/only.mzML");

        MissingMetadataException ex = assertThrows(MissingMetadataException.class, () -> parse(mztab));
        assertTrue(ex.getMessage().toLowerCase().contains("version"), ex.getMessage());
    }

    @Test
    void missingModelThrows() {
        String mztab = String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tsoftware[1]\t[MS, MS:1003281, Casanovo, 5.2.0]",
                "MTD\tms_run[1]-location\tfile:///data/only.mzML");

        MissingMetadataException ex = assertThrows(MissingMetadataException.class, () -> parse(mztab));
        assertTrue(ex.getMessage().toLowerCase().contains("model"), ex.getMessage());
    }

    @Test
    void missingRunLocationThrows() {
        String mztab = String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tsoftware[1]\t[MS, MS:1003281, Casanovo, 5.2.0]",
                "MTD\tsoftware[1]-setting[1]\tmodel = /m/model.ckpt");

        MissingMetadataException ex = assertThrows(MissingMetadataException.class, () -> parse(mztab));
        assertTrue(ex.getMessage().toLowerCase().contains("ms_run"), ex.getMessage());
    }

    private static String baseMztab(String location) {
        return String.join("\n",
                "MTD\tmzTab-version\t1.0.0",
                "MTD\tsoftware[1]\t[MS, MS:1003281, Casanovo, 5.2.0]",
                "MTD\tsoftware[1]-setting[1]\tmodel = /m/model.ckpt",
                "MTD\tms_run[1]-location\t" + location);
    }
}
