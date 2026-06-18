package org.yeastrc.limelight.xml.casanovo.utils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard monoisotopic residue masses (Da) for the 20 common amino acids. Used to compute the mass
 * change of a fixed modification when the unmodified residue is absent from a Casanovo config's
 * residues map (e.g. cysteine, which in a carbamidomethyl search only ever appears modified).
 */
public class AminoAcidMasses {

    private static final Map<String, BigDecimal> MONOISOTOPIC_RESIDUE_MASSES;
    static {
        Map<String, BigDecimal> m = new HashMap<>();
        m.put("G", new BigDecimal("57.021464"));
        m.put("A", new BigDecimal("71.037114"));
        m.put("S", new BigDecimal("87.032028"));
        m.put("P", new BigDecimal("97.052764"));
        m.put("V", new BigDecimal("99.068414"));
        m.put("T", new BigDecimal("101.047678"));
        m.put("C", new BigDecimal("103.009185"));
        m.put("L", new BigDecimal("113.084064"));
        m.put("I", new BigDecimal("113.084064"));
        m.put("N", new BigDecimal("114.042927"));
        m.put("D", new BigDecimal("115.026943"));
        m.put("Q", new BigDecimal("128.058578"));
        m.put("K", new BigDecimal("128.094963"));
        m.put("E", new BigDecimal("129.042593"));
        m.put("M", new BigDecimal("131.040485"));
        m.put("H", new BigDecimal("137.058912"));
        m.put("F", new BigDecimal("147.068414"));
        m.put("R", new BigDecimal("156.101111"));
        m.put("Y", new BigDecimal("163.063329"));
        m.put("W", new BigDecimal("186.079313"));
        MONOISOTOPIC_RESIDUE_MASSES = Collections.unmodifiableMap(m);
    }

    private AminoAcidMasses() {}

    public static boolean contains(String residue) {
        return MONOISOTOPIC_RESIDUE_MASSES.containsKey(residue);
    }

    public static BigDecimal getResidueMass(String residue) {
        BigDecimal mass = MONOISOTOPIC_RESIDUE_MASSES.get(residue);
        if (mass == null) {
            throw new IllegalArgumentException("No standard monoisotopic mass known for residue: " + residue);
        }
        return mass;
    }
}
