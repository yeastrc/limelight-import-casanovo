package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.constants.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class ModParsingUtils {

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
