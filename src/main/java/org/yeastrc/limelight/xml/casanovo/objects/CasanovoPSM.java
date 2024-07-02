package org.yeastrc.limelight.xml.casanovo.objects;

import java.math.BigDecimal;
import java.util.Map;

public class CasanovoPSM {

	private BigDecimal score;
	private byte charge;
	private int scanNumber;
	private String peptideSequence;
	private BigDecimal precursorMZ;
	private Map<Integer,BigDecimal> mods;

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
}
