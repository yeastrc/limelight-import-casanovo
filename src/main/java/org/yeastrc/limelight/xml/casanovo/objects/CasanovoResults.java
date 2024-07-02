package org.yeastrc.limelight.xml.casanovo.objects;

import java.util.Collection;
import java.util.Map;

public class CasanovoResults {

	private Map<CasanovoReportedPeptide, Collection<CasanovoPSM>> peptidePSMMap;

	/**
	 * @return the peptidePSMMap
	 */
	public Map<CasanovoReportedPeptide, Collection<CasanovoPSM>> getPeptidePSMMap() {
		return peptidePSMMap;
	}
	/**
	 * @param peptidePSMMap the peptidePSMMap to set
	 */
	public void setPeptidePSMMap(Map<CasanovoReportedPeptide, Collection<CasanovoPSM>> peptidePSMMap) {
		this.peptidePSMMap = peptidePSMMap;
	}

}
