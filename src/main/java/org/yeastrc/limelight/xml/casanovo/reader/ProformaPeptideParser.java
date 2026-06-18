package org.yeastrc.limelight.xml.casanovo.reader;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parses a modified peptidoform string in Casanovo's bracketed notation (ProForma-style), e.g.
 * {@code M[Oxidation]AVEVTEFAK}, {@code LVN[Deamidated]EVTEFAK}, or an N-terminally modified
 * {@code [Acetyl]-PEPTIDE}.
 *
 * <p>Casanovo 5.x emits this string in a dedicated ProForma column while the plain {@code sequence}
 * column holds the naked sequence; older versions inlined it in the {@code sequence} column. Either
 * way, this class turns one such string into a naked sequence plus a map of variable modifications.
 *
 * <p>Modification positions follow this convention: {@code 0} = N-terminal modification;
 * {@code 1..length} = a modification on that (1-based) residue. There is no separate C-terminal
 * position — a modification on the final residue is simply position {@code length}.
 */
public class ProformaPeptideParser {

    private static final char MOD_START = '[';
    private static final char MOD_END = ']';
    private static final char NTERM_END = '-';

    private ProformaPeptideParser() {}

    /** Strip all modification annotations, returning the naked (unmodified) amino-acid sequence. */
    public static String parseNakedSequence(String peptidoform) {
        if (peptidoform == null || peptidoform.isEmpty()) {
            return "";
        }

        StringBuilder sequence = new StringBuilder();
        boolean readingMod = false;

        for (int i = 0; i < peptidoform.length(); i++) {
            char c = peptidoform.charAt(i);
            if (c == MOD_START) {
                readingMod = true;
            } else if (c == MOD_END) {
                readingMod = false;
            } else if (!readingMod && Character.isLetter(c)) {
                sequence.append(Character.toUpperCase(c));
            }
            // everything else (including the N-terminal '-') is skipped
        }

        return sequence.toString();
    }

    /**
     * Parse the variable modifications from a peptidoform string.
     *
     * @param peptidoform    the bracketed peptidoform string
     * @param residues       residue / mod-token &rarr; monoisotopic mass (from the Casanovo config)
     * @param fixedModTokens fixed-modification tokens (e.g. {@code "C[Carbamidomethyl]"}) to skip, since
     *                       they are emitted as static modifications instead
     * @return map of position &rarr; mass delta (see class doc for the position convention)
     */
    public static Map<Integer, BigDecimal> parseVariableMods(String peptidoform,
                                                             Map<String, BigDecimal> residues,
                                                             Set<String> fixedModTokens) throws Exception {
        if (peptidoform == null || peptidoform.isEmpty()) {
            return new HashMap<>();
        }

        Map<Integer, BigDecimal> variableMods = new HashMap<>();
        int position = 0;
        StringBuilder currentMod = new StringBuilder();
        char previousResidue = '\0';
        boolean readingMod = false;

        for (int i = 0; i < peptidoform.length(); i++) {
            char c = peptidoform.charAt(i);
            if (c == MOD_START) {
                readingMod = true;
                currentMod.append(c);
            } else if (c == MOD_END) {
                currentMod.append(c);
                if (position == 0) {
                    // An N-terminal mod must be followed by '-' (e.g. "[Acetyl]-").
                    if (i + 1 < peptidoform.length() && peptidoform.charAt(i + 1) == NTERM_END) {
                        readingMod = false; // wait for the '-' to finish the N-terminal mod
                    } else {
                        throw new IllegalArgumentException("Invalid N-terminal modification format: expected '[mod]-' but found ']' without a following '-' in: " + peptidoform);
                    }
                } else {
                    processMod(variableMods, position, previousResidue + currentMod.toString(), residues, fixedModTokens);
                    currentMod.setLength(0);
                    readingMod = false;
                }
            } else if (position == 0 && c == NTERM_END && !readingMod) {
                // Completes an N-terminal modification, e.g. "[Acetyl]-".
                currentMod.append(c);
                processMod(variableMods, position, currentMod.toString(), residues, fixedModTokens);
                currentMod.setLength(0);
                readingMod = false;
            } else {
                if (readingMod) {
                    currentMod.append(c);
                } else {
                    previousResidue = c;
                    position++;
                }
            }
        }

        if (readingMod) {
            throw new IllegalArgumentException("Incomplete modification: missing closing bracket in: " + peptidoform);
        }
        if (currentMod.length() > 0) {
            throw new IllegalArgumentException("Unexpected modification format at end of peptide: " + peptidoform);
        }

        return variableMods;
    }

    private static void processMod(Map<Integer, BigDecimal> variableMods,
                                   int position,
                                   String fullMod,
                                   Map<String, BigDecimal> residues,
                                   Set<String> fixedModTokens) throws Exception {

        // Fixed modifications are represented as static modifications, not per-peptide variable mods.
        if (fixedModTokens != null && fixedModTokens.contains(fullMod)) {
            return;
        }

        if (!residues.containsKey(fullMod)) {
            throw new Exception("Modification " + fullMod + " not found in residues map");
        }

        if (position == 0) {
            // N-terminal modification: the residues-map value is the modification mass itself.
            variableMods.put(position, residues.get(fullMod));
        } else {
            String residue = fullMod.substring(0, 1);
            if (!residues.containsKey(residue)) {
                throw new Exception("Residue " + residue + " not found in residues map");
            }
            variableMods.put(position, residues.get(fullMod).subtract(residues.get(residue)));
        }
    }
}
