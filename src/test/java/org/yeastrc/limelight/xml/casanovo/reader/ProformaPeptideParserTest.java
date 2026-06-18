package org.yeastrc.limelight.xml.casanovo.reader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ProForma peptidoform parser. Residue masses and fixed-mod tokens come from the
 * authentic Casanovo 5.2.0 config fixture, but several inputs are SYNTHETIC: the project has no real
 * sample containing an N-terminal modification or a modification on the final residue, so those
 * peptidoforms are hand-built from the ProForma syntax and the config's residue keys. They are marked
 * accordingly and should be re-validated against real data should an example ever become available.
 */
class ProformaPeptideParserTest {

    private static final double TOL = 1e-6;
    private static final double OXIDATION = 15.994915;
    private static final double DEAMIDATION = 0.984016;
    private static final double ACETYL = 42.010565; // N-terminal "[Acetyl]-"

    private static Map<String, BigDecimal> residues;
    private static Set<String> fixedModTokens;

    @BeforeAll
    static void loadConfig() throws Exception {
        File config = new File(ProformaPeptideParserTest.class.getClassLoader()
                .getResource("casanovo/casanovo-config.yaml").toURI());
        ConfigParser cp = new ConfigParser(config.getAbsolutePath());
        residues = cp.getResidues();
        fixedModTokens = cp.getFixedModTokens();
    }

    @Test
    void nakedSequenceStripsAllMods() {
        assertEquals("MAVEVTEFAK", ProformaPeptideParser.parseNakedSequence("M[Oxidation]AVEVTEFAK"));
        assertEquals("PEPTIDE", ProformaPeptideParser.parseNakedSequence("[Acetyl]-PEPTIDE")); // synthetic
        assertEquals("CPEPTIDE", ProformaPeptideParser.parseNakedSequence("C[Carbamidomethyl]PEPTIDE"));
    }

    @Test
    void oxidationOnFirstResidue() throws Exception {
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("M[Oxidation]AVEVTEFAK", residues, fixedModTokens);
        assertEquals(1, mods.size());
        assertEquals(OXIDATION, mods.get(1).doubleValue(), TOL);
    }

    @Test
    void deamidationOnInternalResidue() throws Exception {
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("LVN[Deamidated]EVTEFAK", residues, fixedModTokens);
        assertEquals(1, mods.size());
        assertEquals(DEAMIDATION, mods.get(3).doubleValue(), TOL);
    }

    @Test
    void modificationOnLastResidueIsAResiduePositionNotTerminal() throws Exception {
        // C2: a mod on the final residue must be position == length, never a C-terminal flag. (synthetic)
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("PEPTIDEM[Oxidation]", residues, fixedModTokens);
        assertEquals(1, mods.size());
        assertTrue(mods.containsKey(8), "expected mod at residue 8 (last residue), got " + mods.keySet());
        assertEquals(OXIDATION, mods.get(8).doubleValue(), TOL);
    }

    @Test
    void nTerminalModificationIsPositionZero() throws Exception {
        // synthetic — no real N-terminal-mod sample exists; format matches config key "[Acetyl]-".
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("[Acetyl]-PEPTIDE", residues, fixedModTokens);
        assertEquals(1, mods.size());
        assertTrue(mods.containsKey(0), "expected N-terminal mod at position 0");
        assertEquals(ACETYL, mods.get(0).doubleValue(), TOL);
    }

    @Test
    void nTerminalModPlusResidueMod() throws Exception {
        // synthetic — N-terminal acetyl together with an internal oxidation.
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("[Acetyl]-PEM[Oxidation]TIDE", residues, fixedModTokens);
        assertEquals(2, mods.size());
        assertEquals(ACETYL, mods.get(0).doubleValue(), TOL);
        assertEquals(OXIDATION, mods.get(3).doubleValue(), TOL);
    }

    @Test
    void fixedModificationIsSkipped() throws Exception {
        Map<Integer, BigDecimal> mods = ProformaPeptideParser.parseVariableMods("C[Carbamidomethyl]PEPTIDE", residues, fixedModTokens);
        assertTrue(mods.isEmpty(), "carbamidomethyl is a fixed mod and must be skipped, got " + mods.keySet());
    }
}
