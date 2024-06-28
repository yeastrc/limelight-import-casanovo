package org.yeastrc.limelight.xml.casanovo.objects;

import java.util.Collection;
import java.util.Map;

public class CongaResults {

	private Map<CongaReportedPeptide, Collection<CongaPSM>> peptidePSMMap;

	/**
	 * @return the peptidePSMMap
	 */
	public Map<CongaReportedPeptide, Collection<CongaPSM>> getPeptidePSMMap() {
		return peptidePSMMap;
	}
	/**
	 * @param peptidePSMMap the peptidePSMMap to set
	 */
	public void setPeptidePSMMap(Map<CongaReportedPeptide, Collection<CongaPSM>> peptidePSMMap) {
		this.peptidePSMMap = peptidePSMMap;
	}

}
