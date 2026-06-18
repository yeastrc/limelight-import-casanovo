package org.yeastrc.limelight.xml.casanovo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class ModParsingUtils {

	/**
	 * Build the reported-peptide string used as the grouping identity for a peptide: the naked
	 * sequence with each modification's mass rounded to a whole number and rendered inline, e.g.
	 * {@code n[42]PEPTIDE} for an N-terminal mod or {@code PEPTIM[16]DE} for a residue mod.
	 *
	 * <p>Position convention matches {@link org.yeastrc.limelight.xml.casanovo.reader.ProformaPeptideParser}:
	 * {@code 0} = N-terminal; {@code 1..length} = the (1-based) residue, where {@code length} is the
	 * final residue (there is no separate C-terminal position).
	 */
	public static String getRoundedReportedPeptideString( String nakedPeptideSequence, Map<Integer, BigDecimal> modMap ) {

		if( modMap == null || modMap.size() < 1 )
			return nakedPeptideSequence;

		StringBuilder sb = new StringBuilder();

		if(modMap.containsKey(0)) {
			sb.append("n[").append( round( modMap.get(0) ) ).append("]");
		}

		for (int i = 0; i < nakedPeptideSequence.length(); i++){
			sb.append( nakedPeptideSequence.charAt(i) );

			BigDecimal mass = modMap.get( i + 1 );
			if( mass != null ) {
				sb.append("[").append( round( mass ) ).append("]");
			}
		}

		return sb.toString();
	}

	private static String round( BigDecimal mass ) {
		return mass.setScale( 0, RoundingMode.HALF_UP ).toString();
	}

}
