package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoReportedPeptide;

public class ReportedPeptideUtils {

	public static CasanovoReportedPeptide getReportedPeptideForPSM(CasanovoPSM psm ) {
		
		CasanovoReportedPeptide rp = new CasanovoReportedPeptide();
		
		rp.setNakedPeptide( psm.getPeptideSequence() );
		rp.setMods( psm.getMods() );
		rp.setReportedPeptideString( ModParsingUtils.getRoundedReportedPeptideString( psm.getPeptideSequence(), psm.getMods() ));

		return rp;
	}

}
