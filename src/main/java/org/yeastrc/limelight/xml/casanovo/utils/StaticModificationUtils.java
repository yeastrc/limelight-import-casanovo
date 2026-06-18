package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.reader.ConfigParser;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Derives static (fixed) modifications from a Casanovo config. A fixed modification declared as
 * {@code "C:C[Carbamidomethyl]"} yields a static modification on amino acid {@code C} whose mass
 * change is {@code residues["C[Carbamidomethyl]"] - mass(C)}, where the unmodified residue mass is
 * taken from the config when present and otherwise from the standard monoisotopic table
 * ({@link AminoAcidMasses}).
 */
public class StaticModificationUtils {

    private StaticModificationUtils() {}

    /**
     * @return amino-acid symbol &rarr; static mass change. N-terminal/C-terminal fixed modifications
     *         are not represented here (Limelight models terminal static mods differently and Casanovo's
     *         default configs do not declare them).
     */
    public static Map<String, BigDecimal> getFixedModificationMasses(ConfigParser configParser) throws Exception {

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        Map<String, BigDecimal> residues = configParser.getResidues();

        for (Map.Entry<String, String> fixedMod : configParser.getFixedMods().entrySet()) {
            String residue = fixedMod.getKey();
            String token = fixedMod.getValue();

            if ("nterm".equalsIgnoreCase(residue) || "cterm".equalsIgnoreCase(residue)) {
                continue;
            }

            BigDecimal modifiedMass = residues.get(token);
            if (modifiedMass == null) {
                throw new Exception("Fixed modification token '" + token + "' not found in residues map.");
            }

            BigDecimal unmodifiedMass = residues.containsKey(residue)
                    ? residues.get(residue)
                    : AminoAcidMasses.getResidueMass(residue);

            result.put(residue, modifiedMass.subtract(unmodifiedMass));
        }

        return result;
    }
}
