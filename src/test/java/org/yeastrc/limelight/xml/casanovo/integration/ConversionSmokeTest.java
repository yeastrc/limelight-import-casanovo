package org.yeastrc.limelight.xml.casanovo.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.limelight_import.api.xml_dto.LimelightInput;
import org.yeastrc.limelight.limelight_import.api.xml_dto.PeptideModification;
import org.yeastrc.limelight.limelight_import.api.xml_dto.Psm;
import org.yeastrc.limelight.limelight_import.api.xml_dto.ReportedPeptide;
import org.yeastrc.limelight.limelight_import.api.xml_dto.StaticModification;
import org.yeastrc.limelight.xml.casanovo.main.LimelightXMLValidator;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.yeastrc.limelight.xml.casanovo.integration.ConversionTestSupport.*;

/**
 * Realistic-scale smoke test: converts a 500-PSM slice of authentic Casanovo 5.2.0 output
 * ({@code medium.mztab}) and asserts both independently-anchored facts (PSM/scan counts taken from the
 * raw mzTab) and characterization counts (reported-peptide and modification totals that lock in the
 * current correct behavior). Guards against regressions that the tiny fixtures are too small to catch.
 *
 * <p>The full-scale ({@code large.mztab}, 50k PSMs) equivalent is intentionally NOT committed; it is
 * exercised by an opt-in test (testing-plan Phase 5) when a real file is supplied.
 */
class ConversionSmokeTest {

    // Independently anchored to the raw fixture: 500 PSM rows, each with a distinct scan, one ms_run.
    private static final int EXPECTED_PSMS = 500;
    private static final String SCAN_FILE = "QEP2_2018_0812_AZ_028_az734_AZ.mzML";

    // Characterization counts (locked-in current behavior; a change here should be reviewed).
    private static final int EXPECTED_REPORTED_PEPTIDES = 409;
    private static final int EXPECTED_MODIFIED_PEPTIDES = 53;
    private static final int EXPECTED_OXIDATIONS = 36;
    private static final int EXPECTED_DEAMIDATIONS = 17;

    private static final BigDecimal OXIDATION_MASS = new BigDecimal("15.9949");
    private static final BigDecimal DEAMIDATION_MASS = new BigDecimal("0.9840");

    private static File outputFile;
    private static LimelightInput in;

    @BeforeAll
    static void convertOnce() throws Throwable {
        outputFile = convert(MEDIUM, CONFIG);
        in = unmarshal(outputFile);
    }

    @Test
    void everyPsmIsConvertedWithDistinctScanAndSingleSourceFile() {
        List<Psm> psms = allPsms(in);
        assertEquals(EXPECTED_PSMS, psms.size(), "one PSM per mzTab row");

        long distinctScans = psms.stream().map(p -> p.getScanNumber().intValue()).distinct().count();
        assertEquals(EXPECTED_PSMS, distinctScans, "each PSM should have a distinct scan number");

        Set<String> files = psms.stream().map(Psm::getScanFileName).collect(Collectors.toSet());
        assertEquals(Set.of(SCAN_FILE), files, "all PSMs come from the single ms_run file");
    }

    @Test
    void reportedPeptideGroupingIsStable() {
        List<ReportedPeptide> peptides = in.getReportedPeptides().getReportedPeptide();
        assertEquals(EXPECTED_REPORTED_PEPTIDES, peptides.size());

        long distinctStrings = peptides.stream().map(ReportedPeptide::getReportedPeptideString).distinct().count();
        assertEquals(EXPECTED_REPORTED_PEPTIDES, distinctStrings, "reported peptide strings must be unique");
    }

    @Test
    void modificationCountsMatchExpectedByType() {
        long modifiedPeptides = in.getReportedPeptides().getReportedPeptide().stream()
                .filter(rp -> rp.getPeptideModifications() != null)
                .count();
        assertEquals(EXPECTED_MODIFIED_PEPTIDES, modifiedPeptides);

        List<PeptideModification> mods = allModifications();
        assertEquals(EXPECTED_MODIFIED_PEPTIDES, mods.size(), "one variable mod per modified peptide (max_mods=1)");

        long oxidations = mods.stream().filter(m -> m.getMass().compareTo(OXIDATION_MASS) == 0).count();
        long deamidations = mods.stream().filter(m -> m.getMass().compareTo(DEAMIDATION_MASS) == 0).count();
        assertEquals(EXPECTED_OXIDATIONS, oxidations, "oxidation (15.9949) modifications");
        assertEquals(EXPECTED_DEAMIDATIONS, deamidations, "deamidation (0.9840) modifications");
        assertEquals(mods.size(), oxidations + deamidations, "no modifications of any other mass should appear");
    }

    @Test
    void everyVariableModificationIsAValidResiduePosition() {
        for (ReportedPeptide rp : in.getReportedPeptides().getReportedPeptide()) {
            if (rp.getPeptideModifications() == null) {
                continue;
            }
            int length = rp.getSequence().length();
            for (PeptideModification mod : rp.getPeptideModifications().getPeptideModification()) {
                assertNull(mod.isIsNTerminal(), "no N-terminal mods expected in this data: " + rp.getReportedPeptideString());
                assertNull(mod.isIsCTerminal(), "no C-terminal mods expected in this data: " + rp.getReportedPeptideString());
                assertNotNull(mod.getPosition(), "residue mod must carry a position");
                int position = mod.getPosition().intValue();
                assertTrue(position >= 1 && position <= length,
                        "position " + position + " out of range 1.." + length + " for " + rp.getReportedPeptideString());
                assertTrue(mod.getMass().compareTo(OXIDATION_MASS) == 0 || mod.getMass().compareTo(DEAMIDATION_MASS) == 0,
                        "unexpected modification mass " + mod.getMass());
            }
        }
    }

    @Test
    void staticCysteineModificationIsPresentOnce() {
        List<StaticModification> smods = in.getStaticModifications().getStaticModification();
        assertEquals(1, smods.size());
        assertEquals("C", smods.get(0).getAminoAcid());
        assertEquals(0, smods.get(0).getMassChange().compareTo(new BigDecimal("57.021464")));
    }

    @Test
    void multipleChargeStatesAreRepresented() {
        Set<Integer> charges = allPsms(in).stream()
                .map(p -> p.getPrecursorCharge().intValue())
                .collect(Collectors.toSet());
        assertEquals(Set.of(2, 3, 4), charges);
    }

    @Test
    void outputValidatesAgainstLimelightSchema() throws Exception {
        LimelightXMLValidator.validateLimelightXML(outputFile);
    }

    private static List<PeptideModification> allModifications() {
        List<PeptideModification> mods = new ArrayList<>();
        for (ReportedPeptide rp : in.getReportedPeptides().getReportedPeptide()) {
            if (rp.getPeptideModifications() != null) {
                mods.addAll(rp.getPeptideModifications().getPeptideModification());
            }
        }
        return mods;
    }
}
