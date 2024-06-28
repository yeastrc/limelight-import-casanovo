package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.constants.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class ModParsingUtils {

	/**
	 * Get all static mods for this run, including the default C+57 used by CONGA
	 * @param staticModsString
	 * @return
	 */
	public static Map<String, BigDecimal> getStaticMods(String staticModsString) {
		Map<String, BigDecimal> staticMods = getStaticModsFromReportedStaticModsString(staticModsString);

		// get the default static mod always used by CONGA
		AbstractMap.SimpleEntry<String, BigDecimal> defaultModPair = getStaticModFromString(Constants.DEFAULT_STATIC_MOD_STRING);
		staticMods.put(defaultModPair.getKey(), defaultModPair.getValue());

		return staticMods;
	}

	/**
	 * Get the static mods reported in the log file in the form of L:50.299323,Q:-12.3456
	 * Map is keyed on residue, with the mass as the value
	 *
	 * @param staticModsString
	 * @return
	 */
	public static Map<String, BigDecimal> getStaticModsFromReportedStaticModsString(String staticModsString) {
		Map<String, BigDecimal> staticMods = new HashMap<>();

		if(staticModsString == null || staticModsString.length() < 1) {
			return staticMods;
		}

		String[] modStrings = staticModsString.split("\\s*,\\s*");
		for(String modString : modStrings) {
			AbstractMap.SimpleEntry<String, BigDecimal> modPair = getStaticModFromString(modString);
			staticMods.put(modPair.getKey(), modPair.getValue());
		}

		return staticMods;
	}

	/**
	 * Parse the reported static mod (in the form of residue:mass into a key-value pair, keyed on residue
	 * with the mass as the value
	 *
	 * @param staticModString
	 * @return
	 */
	public static AbstractMap.SimpleEntry<String, BigDecimal> getStaticModFromString(String staticModString) {
		staticModString = staticModString.replace("+", "");
		staticModString = staticModString.trim();

		String[] fields = staticModString.split(":");

		return new AbstractMap.SimpleEntry<>(fields[0], new BigDecimal(fields[1]));
	}
	public static String getRoundedReportedPeptideString( String nakedPeptideSequence, Map<Integer, BigDecimal> modMap ) {
				
		if( modMap == null || modMap.size() < 1 )
			return nakedPeptideSequence;
		
		StringBuilder sb = new StringBuilder();

		if(modMap.containsKey(0)) {
			sb.append("n[" + modMap.get(0).setScale( 0, RoundingMode.HALF_UP ).toString() + "]");
		}

		for (int i = 0; i < nakedPeptideSequence.length(); i++){
		    String r = String.valueOf( nakedPeptideSequence.charAt(i) );
		    sb.append( r );
		    
		    if( modMap.containsKey( i + 1 ) ) {

		    	BigDecimal mass = modMap.get( i + 1 );
		    	
		    	sb.append( "[" );
		    	sb.append( mass.setScale( 0, RoundingMode.HALF_UP ).toString() );
		    	sb.append( "]" );
		    	
		    }
		}

		if(modMap.containsKey(nakedPeptideSequence.length())) {
			sb.append("c[" + sb.append( modMap.get(nakedPeptideSequence.length()).setScale( 0, RoundingMode.HALF_UP ).toString() ) + "]");
		}
				
		return sb.toString();
	}

}
