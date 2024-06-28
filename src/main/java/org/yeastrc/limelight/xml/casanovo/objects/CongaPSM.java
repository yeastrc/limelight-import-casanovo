package org.yeastrc.limelight.xml.casanovo.objects;

import java.math.BigDecimal;
import java.util.Map;

public class CongaPSM {

	private BigDecimal score;
	private BigDecimal deltaMass;
	private Integer peptideRank;
	private OpenModification openModification;
	private String searchFile;
	private byte charge;
	private int scanNumber;
	private String peptideSequence;
	private Map<Integer,BigDecimal> mods;
	private BigDecimal observedNeutralMass;
	private String scan_filename;
	private boolean aboveGroupThreshold;
	private boolean originallyDiscovered;

	public String getScan_filename() {
		return scan_filename;
	}

	public void setScan_filename(String scan_filename) {
		this.scan_filename = scan_filename;
	}

	public BigDecimal getObservedNeutralMass() {
		return observedNeutralMass;
	}

	public void setObservedNeutralMass(BigDecimal observedNeutralMass) {
		this.observedNeutralMass = observedNeutralMass;
	}

	public OpenModification getOpenModification() {
		return openModification;
	}

	public void setOpenModification(OpenModification openModification) {
		this.openModification = openModification;
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

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public BigDecimal getDeltaMass() {
		return deltaMass;
	}

	public void setDeltaMass(BigDecimal deltaMass) {
		this.deltaMass = deltaMass;
	}

	public Integer getPeptideRank() {
		return peptideRank;
	}

	public void setPeptideRank(Integer peptideRank) {
		this.peptideRank = peptideRank;
	}

	public String getSearchFile() {
		return searchFile;
	}

	public void setSearchFile(String searchFile) {
		this.searchFile = searchFile;
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

	public boolean isAboveGroupThreshold() {
		return aboveGroupThreshold;
	}

	public void setAboveGroupThreshold(boolean aboveGroupThreshold) {
		this.aboveGroupThreshold = aboveGroupThreshold;
	}

	public boolean isOriginallyDiscovered() {
		return originallyDiscovered;
	}

	public void setOriginallyDiscovered(boolean originallyDiscovered) {
		this.originallyDiscovered = originallyDiscovered;
	}
}
