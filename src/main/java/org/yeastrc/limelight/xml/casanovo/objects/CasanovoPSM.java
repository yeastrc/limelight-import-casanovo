package org.yeastrc.limelight.xml.casanovo.objects;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CasanovoPSM {

	private BigDecimal score;
	private BigDecimal efdr;
	private BigDecimal adjScore;

	private byte charge;
	private int scanNumber;
	private List<BigDecimal> perPositionScores;
	private String peptideSequence;
	private BigDecimal precursorMZ;
	private Map<Integer,BigDecimal> mods;
	
	/**
	 * For error messages
	 */
	private String reportedPeptideString; 

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public byte getCharge() {
		return charge;
	}

	public void setCharge(byte charge) {
		this.charge = charge;
	}

	public int getScanNumber() {
		return scanNumber;
	}

	public void setScanNumber(int scanNumber) {
		this.scanNumber = scanNumber;
	}

	public String getPeptideSequence() {
		return peptideSequence;
	}

	public void setPeptideSequence(String peptideSequence) {
		this.peptideSequence = peptideSequence;
	}

	public Map<Integer, BigDecimal> getMods() {
		return mods;
	}

	public void setMods(Map<Integer, BigDecimal> mods) {
		this.mods = mods;
	}

	public BigDecimal getPrecursorMZ() {
		return precursorMZ;
	}

	public void setPrecursorMZ(BigDecimal precursorMZ) {
		this.precursorMZ = precursorMZ;
	}

	public List<BigDecimal> getPerPositionScores() {
		return perPositionScores;
	}

	public void setPerPositionScores(List<BigDecimal> perPositionScores) {
		this.perPositionScores = perPositionScores;
	}

	public String getReportedPeptideString() {
		return reportedPeptideString;
	}

	public void setReportedPeptideString(String reportedPeptideString) {
		this.reportedPeptideString = reportedPeptideString;
	}

	public BigDecimal getEfdr() {
		return efdr;
	}

	public void setEfdr(BigDecimal efdr) {
		this.efdr = efdr;
	}

	public BigDecimal getAdjScore() {
		return adjScore;
	}

	public void setAdjScore(BigDecimal adjScore) {
		this.adjScore = adjScore;
	}
}
