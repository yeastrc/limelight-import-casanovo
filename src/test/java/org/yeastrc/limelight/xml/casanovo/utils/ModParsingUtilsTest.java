package org.yeastrc.limelight.xml.casanovo.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the rounded reported-peptide-string builder, including the C1 regression. */
class ModParsingUtilsTest {

    @Test
    void noModsReturnsNakedSequence() {
        assertEquals("PEPTIDE", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", null));
        assertEquals("PEPTIDE", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", new LinkedHashMap<>()));
    }

    @Test
    void internalResidueMod() {
        Map<Integer, BigDecimal> mods = new LinkedHashMap<>();
        mods.put(3, new BigDecimal("15.994915"));
        assertEquals("PEP[16]TIDE", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", mods));
    }

    @Test
    void lastResidueMod() {
        // C1: the previous C-terminal branch corrupted this; it must render as a normal residue mod.
        Map<Integer, BigDecimal> mods = new LinkedHashMap<>();
        mods.put(7, new BigDecimal("15.994915"));
        assertEquals("PEPTIDE[16]", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", mods));
    }

    @Test
    void nTerminalMod() {
        Map<Integer, BigDecimal> mods = new LinkedHashMap<>();
        mods.put(0, new BigDecimal("42.010565"));
        assertEquals("n[42]PEPTIDE", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", mods));
    }

    @Test
    void nTerminalAndResidueMods() {
        Map<Integer, BigDecimal> mods = new LinkedHashMap<>();
        mods.put(0, new BigDecimal("42.010565"));
        mods.put(3, new BigDecimal("15.994915"));
        assertEquals("n[42]PEP[16]TIDE", ModParsingUtils.getRoundedReportedPeptideString("PEPTIDE", mods));
    }
}
