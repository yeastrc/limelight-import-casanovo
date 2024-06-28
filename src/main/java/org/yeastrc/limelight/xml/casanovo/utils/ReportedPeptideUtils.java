package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.objects.CongaPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CongaReportedPeptide;

public class ReportedPeptideUtils {

	public static CongaReportedPeptide getReportedPeptideForPSM(CongaPSM psm ) throws Exception {
		
		CongaReportedPeptide rp = new CongaReportedPeptide();
		
		rp.setNakedPeptide( psm.getPeptideSequence() );
		rp.setMods( psm.getMods() );
		rp.setReportedPeptideString( ModParsingUtils.getRoundedReportedPeptideString( psm.getPeptideSequence(), psm.getMods() ));

		return rp;
	}

}
