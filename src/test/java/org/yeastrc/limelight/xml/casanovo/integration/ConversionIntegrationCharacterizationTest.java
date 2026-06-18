package org.yeastrc.limelight.xml.casanovo.integration;

import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.limelight_import.api.xml_dto.FilterablePsmAnnotation;
import org.yeastrc.limelight.limelight_import.api.xml_dto.LimelightInput;
import org.yeastrc.limelight.limelight_import.api.xml_dto.PeptideModification;
import org.yeastrc.limelight.limelight_import.api.xml_dto.Psm;
import org.yeastrc.limelight.limelight_import.api.xml_dto.ReportedPeptide;
import org.yeastrc.limelight.limelight_import.api.xml_dto.StaticModification;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.main.LimelightXMLValidator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.yeastrc.limelight.xml.casanovo.integration.ConversionTestSupport.*;

/**
 * End-to-end tests that run the full conversion pipeline on small authentic Casanovo 5.2.0 fixtures and
 * then <b>unmarshal the produced Limelight XML back into the {@link LimelightInput} DTO tree</b>,
 * asserting on real structure (modification positions/masses, the config-derived static modification,
 * per-PSM scan-file mapping, the scan-number collision across input files, and PSM annotations).
 */
public class ConversionIntegrationCharacterizationTest {

    @Test
    void staticModificationIsDerivedOnCysteine() throws Throwable {
        LimelightInput in = unmarshal(convert(SINGLE, CONFIG));

        List<StaticModification> smods = in.getStaticModifications().getStaticModification();
        assertEquals(1, smods.size(), "expected exactly one static modification (carbamidomethyl on C)");
        assertEquals("C", smods.get(0).getAminoAcid());
        assertEquals(0, smods.get(0).getMassChange().compareTo(new BigDecimal("57.021464")),
                "static C mass change should be 57.021464, was " + smods.get(0).getMassChange());
    }

    @Test
    void oxidationModificationHasPositionOneAndCorrectMass() throws Throwable {
        LimelightInput in = unmarshal(convert(SINGLE, CONFIG));
        ReportedPeptide rp = peptideByString(in, "M[16]AVEVTEFAK");
        assertEquals("MAVEVTEFAK", rp.getSequence());

        List<PeptideModification> mods = rp.getPeptideModifications().getPeptideModification();
        assertEquals(1, mods.size());

        PeptideModification mod = mods.get(0);
        assertEquals(BigInteger.ONE, mod.getPosition());
        assertNull(mod.isIsNTerminal(), "a residue mod must not be flagged N-terminal");
        assertNull(mod.isIsCTerminal(), "a residue mod must not be flagged C-terminal");
        assertEquals(0, mod.getMass().compareTo(new BigDecimal("15.9949")),
                "oxidation mass should be 15.9949, was " + mod.getMass());
    }

    @Test
    void deamidationModificationHasPositionThreeAndCorrectMass() throws Throwable {
        LimelightInput in = unmarshal(convert(SINGLE, CONFIG));
        ReportedPeptide rp = peptideByString(in, "LVN[1]EVTEFAK");
        assertEquals("LVNEVTEFAK", rp.getSequence());

        List<PeptideModification> mods = rp.getPeptideModifications().getPeptideModification();
        assertEquals(1, mods.size());
        assertEquals(BigInteger.valueOf(3), mods.get(0).getPosition());
        assertEquals(0, mods.get(0).getMass().compareTo(new BigDecimal("0.9840")),
                "deamidation mass should be 0.9840, was " + mods.get(0).getMass());
    }

    @Test
    void carbamidomethylOnlyPeptideHasNoVariableModifications() throws Throwable {
        LimelightInput in = unmarshal(convert(SINGLE, CONFIG));
        ReportedPeptide rp = peptideByString(in, "AADPHECYAK");
        assertNull(rp.getPeptideModifications(),
                "carbamidomethyl is a fixed (static) mod; this peptide must carry no variable modifications");
    }

    @Test
    void psmCarriesScoreAdjScoreAndEfdrAnnotations() throws Throwable {
        LimelightInput in = unmarshal(convert(SINGLE, CONFIG));
        ReportedPeptide rp = peptideByString(in, "M[16]AVEVTEFAK");

        List<Psm> psms = rp.getPsms().getPsm();
        assertEquals(1, psms.size());
        Psm psm = psms.get(0);
        assertEquals(BigInteger.valueOf(36000), psm.getScanNumber());
        assertEquals("test2.mzML", psm.getScanFileName());

        Map<String, BigDecimal> annotations = new HashMap<>();
        for (FilterablePsmAnnotation a : psm.getFilterablePsmAnnotations().getFilterablePsmAnnotation()) {
            annotations.put(a.getAnnotationName(), a.getValue());
        }
        assertEquals(
                Set.of(PSMAnnotationTypes.CASANOVO_SCORE, PSMAnnotationTypes.CASANOVO_ADJ_SCORE, PSMAnnotationTypes.CASANOVO_EFDR),
                annotations.keySet());

        BigDecimal efdr = annotations.get(PSMAnnotationTypes.CASANOVO_EFDR);
        assertNotNull(efdr);
        assertTrue(efdr.doubleValue() >= 0.0 && efdr.doubleValue() <= 1.0, "eFDR out of [0,1]: " + efdr);
    }

    @Test
    void multipleInputFilesArePreservedPerPsm() throws Throwable {
        LimelightInput in = unmarshal(convert(TWO_FILES, CONFIG));
        List<Psm> psms = allPsms(in);

        Map<String, Long> psmCountByFile = psms.stream()
                .collect(Collectors.groupingBy(Psm::getScanFileName, Collectors.counting()));
        assertEquals(2L, psmCountByFile.get("test1.mzML"), "PSMs attributed to ms_run[1] = test1.mzML");
        assertEquals(2L, psmCountByFile.get("test2.mzML"), "PSMs attributed to ms_run[2] = test2.mzML");

        // scan 2833 occurs in BOTH input files; each PSM must keep its own file (no conflation).
        Set<String> filesForScan2833 = psms.stream()
                .filter(p -> p.getScanNumber().intValue() == 2833)
                .map(Psm::getScanFileName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("test1.mzML", "test2.mzML"), filesForScan2833);
    }

    @Test
    void producedXmlPassesLimelightValidator() throws Throwable {
        // also gives LimelightXMLValidator direct coverage
        LimelightXMLValidator.validateLimelightXML(convert(SINGLE, CONFIG));
    }
}
