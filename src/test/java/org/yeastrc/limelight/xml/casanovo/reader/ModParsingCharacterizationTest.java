package org.yeastrc.limelight.xml.casanovo.reader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests pinning the CORRECT modification-parsing behavior for Casanovo 5.x
 * mzTab output, where the modified peptidoform is in the ProForma column
 * ("opt_global_cv_MS:1003169_proforma_peptidoform_sequence") and the plain "sequence" column is
 * the naked, unmodified peptide.
 *
 * <p>These encode the intended post-refactor behavior. The oxidation/deamidation cases currently
 * FAIL against the existing code (bug C0): the parser reads mods from the naked "sequence" column,
 * so every variable modification is silently dropped. The fixed-mod and unmodified cases are
 * invariants that should remain green throughout the refactor.
 *
 * <p>Fixture: {@code casanovo/single-file.mztab} — authentic Casanovo 5.2.0 rows.
 */
public class ModParsingCharacterizationTest {

    private static final double TOL = 1e-6;
    private static final double OXIDATION = 15.994915;   // residues["M[Oxidation]"]  - residues["M"]
    private static final double DEAMIDATION = 0.984016;  // residues["N[Deamidated]"] - residues["N"]

    private static Map<Integer, CasanovoPSM> psmsByScan;

    @BeforeAll
    static void parseFixture() throws Throwable {
        File config = resource("casanovo/casanovo-config.yaml");
        File mztab = resource("casanovo/single-file.mztab");

        ConfigParser configParser = new ConfigParser(config.getAbsolutePath());
        CasanovoResults results = ResultsParser.getResults(mztab, configParser);

        psmsByScan = new HashMap<>();
        for (Collection<CasanovoPSM> psms : results.getPeptidePSMMap().values()) {
            for (CasanovoPSM psm : psms) {
                psmsByScan.put(psm.getScanNumber(), psm);
            }
        }
    }

    @Test
    void oxidationOnFirstResidueIsParsed() {
        // ProForma: M[Oxidation]AVEVTEFAK
        CasanovoPSM psm = requirePsm(36000);
        assertEquals("MAVEVTEFAK", psm.getPeptideSequence());

        Map<Integer, BigDecimal> mods = psm.getMods();
        assertNotNull(mods, "mods map should not be null");
        assertTrue(mods.containsKey(1), "expected an oxidation modification at residue position 1");
        assertEquals(OXIDATION, mods.get(1).doubleValue(), TOL);
        assertEquals(1, mods.size(), "only one variable mod expected");
    }

    @Test
    void deamidationOnInternalResidueIsParsed() {
        // ProForma: LVN[Deamidated]EVTEFAK
        CasanovoPSM psm = requirePsm(35341);
        assertEquals("LVNEVTEFAK", psm.getPeptideSequence());

        Map<Integer, BigDecimal> mods = psm.getMods();
        assertTrue(mods.containsKey(3), "expected a deamidation modification at residue position 3");
        assertEquals(DEAMIDATION, mods.get(3).doubleValue(), TOL);
        assertEquals(1, mods.size(), "only one variable mod expected");
    }

    @Test
    void oxidationOnLaterResidueIsParsed() {
        // ProForma: GLLHTSDTNGDTLDNDLM[Oxidation]LLK  (M is the 18th residue)
        CasanovoPSM psm = requirePsm(51622);
        assertEquals("GLLHTSDTNGDTLDNDLMLLK", psm.getPeptideSequence());

        Map<Integer, BigDecimal> mods = psm.getMods();
        assertTrue(mods.containsKey(18), "expected an oxidation modification at residue position 18");
        assertEquals(OXIDATION, mods.get(18).doubleValue(), TOL);
    }

    @Test
    void fixedCarbamidomethylIsNotReportedAsVariableMod() {
        // ProForma: AADPHEC[Carbamidomethyl]YAK — Carbamidomethyl is a FIXED mod (allowed_fixed_mods),
        // so it must be emitted as a static modification, never as a per-peptide variable mod.
        CasanovoPSM psm = requirePsm(8960);
        Map<Integer, BigDecimal> mods = psm.getMods();
        assertTrue(mods == null || mods.isEmpty(),
                "carbamidomethyl is a fixed mod and must not appear among variable mods");
    }

    @Test
    void unmodifiedPeptideHasNoMods() {
        // ProForma == sequence: YTTLTYETTLEK
        CasanovoPSM psm = requirePsm(17922);
        Map<Integer, BigDecimal> mods = psm.getMods();
        assertTrue(mods == null || mods.isEmpty(), "unmodified peptide should have no variable mods");
    }

    private static CasanovoPSM requirePsm(int scanNumber) {
        CasanovoPSM psm = psmsByScan.get(scanNumber);
        assertNotNull(psm, "no PSM parsed for scan " + scanNumber);
        return psm;
    }

    private static File resource(String name) throws Exception {
        return new File(ModParsingCharacterizationTest.class.getClassLoader().getResource(name).toURI());
    }
}
