/*
 * Original author: Michael Riffle <mriffle .at. uw.edu>
 *
 * Copyright 2018 University of Washington - Seattle, WA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yeastrc.limelight.xml.casanovo.reader;

import org.apache.commons.io.FilenameUtils;
import org.yeastrc.limelight.xml.casanovo.objects.CongaPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CongaReportedPeptide;
import org.yeastrc.limelight.xml.casanovo.objects.CongaResults;
import org.yeastrc.limelight.xml.casanovo.objects.OpenModification;
import org.yeastrc.limelight.xml.casanovo.utils.CompareUtils;
import org.yeastrc.limelight.xml.casanovo.utils.ReportedPeptideUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael Riffle
 *
 */
public class ResultsParser {

	public static CongaResults getResults(File targetsFile, Map<String, BigDecimal> staticMods) throws Throwable {

		CongaResults results = new CongaResults();
		Map<CongaReportedPeptide,Collection<CongaPSM>> resultMap = new HashMap<>();
		results.setPeptidePSMMap(resultMap);

		try(BufferedReader br = new BufferedReader(new FileReader( targetsFile ))) {

			String headerLine = br.readLine();
			Map<String, Integer> columnMap = processHeaderLine(headerLine);

			for(String line = br.readLine(); line != null; line = br.readLine()) {
				CongaPSM psm = getPSMFromLine(line, columnMap, staticMods);
				CongaReportedPeptide reportedPeptide = ReportedPeptideUtils.getReportedPeptideForPSM( psm );

				if( !results.getPeptidePSMMap().containsKey( reportedPeptide ) )
					results.getPeptidePSMMap().put( reportedPeptide, new ArrayList<>() );

				results.getPeptidePSMMap().get( reportedPeptide ).add(psm);
			}
		}

		return results;
	}

	/**
	 * Get a map of column headers to the index of that column on the line
	 *
	 * @param headerLine
	 * @return
	 */
	private static Map<String, Integer> processHeaderLine(String headerLine) {
		Map<String, Integer> columnMap = new HashMap<>();

		String[] fields = headerLine.split("\\t", -1);
		for(int i = 0; i < fields.length; i++) {
			columnMap.put(fields[i], i);
		}

		return columnMap;
	}

	/**
	 *
	 * @param line
	 * @param columnMap
	 * @return
	 * @throws Exception
	 */
	private static CongaPSM getPSMFromLine(String line, Map<String, Integer> columnMap, Map<String, BigDecimal> staticMods) throws Exception {

		String[] fields = line.split("\\t", -1);

		final String[] requiredHeaders = new String[] {
				"scan",
				"charge",
				"score",
				"delta_mass",
				"rank",
				"search_file",
				"peptide",
				"spectrum_neutral_mass",
				"modification_info",
				"originally_discovered",
				"above_group_threshold"
		};

		for(String requiredHeader : requiredHeaders) {
			if(!columnMap.containsKey(requiredHeader)) {
				throw new RuntimeException("Could not find column for \"" + requiredHeader + "\"");
			}
		}

		final int scanNumber = Integer.parseInt(fields[columnMap.get("scan")]);
		final byte charge = Byte.parseByte(fields[columnMap.get("charge")]);
		final BigDecimal score = new BigDecimal(fields[columnMap.get("score")]);
		final Integer rank = Math.round(Float.valueOf(fields[columnMap.get("rank")]));
		final String searchFile = fields[columnMap.get("search_file")];
		final String reportedPeptideString = fields[columnMap.get("peptide")];
		final BigDecimal deltaMass = new BigDecimal(fields[columnMap.get("delta_mass")]);
		final BigDecimal observedNeutralMass = new BigDecimal(fields[columnMap.get("spectrum_neutral_mass")]);
		final String modificationInfoString = fields[columnMap.get("modification_info")];
		final boolean aboveGroupThreshold = fields[columnMap.get("above_group_threshold")].equals("True");
		final boolean originallyDiscovered = fields[columnMap.get("originally_discovered")].equals("True");

		String scan_file = null;
		if(columnMap.containsKey("file")) {
			scan_file = fields[columnMap.get("file")];
			scan_file = FilenameUtils.removeExtension(scan_file);
		}

		CongaPSM psm = new CongaPSM();

		psm.setScanNumber(scanNumber);
		psm.setCharge(charge);
		psm.setScore(score);
		psm.setSearchFile(searchFile);
		psm.setPeptideRank(rank);
		psm.setObservedNeutralMass(observedNeutralMass);
		psm.setScan_filename(scan_file);
		psm.setAboveGroupThreshold(aboveGroupThreshold);
		psm.setOriginallyDiscovered(originallyDiscovered);

		String nakedPeptideSequence = getPeptideSequenceFromReportedPeptideString(reportedPeptideString);

		assignOpenModAndDeltaMassToPSM(psm, nakedPeptideSequence, deltaMass, searchFile, fields, columnMap);

		// handle peptide string
		psm.setPeptideSequence(nakedPeptideSequence);

		// handle var mods
		psm.setMods(getVariableModsFromReportedMods(modificationInfoString, psm.getPeptideSequence(), staticMods));

		return psm;
	}

	/**
	 * Perform in-place modification of the supplied PSM: set deltaMass and open modification
	 *
	 * @param psm
	 * @param deltaMass
	 * @param searchFile
	 * @param fields
	 * @param columnMap
	 */
	private static void assignOpenModAndDeltaMassToPSM(
			CongaPSM psm,
			String nakedPeptideSequence,
			BigDecimal deltaMass,
			String searchFile,
			String fields[],
			Map<String, Integer> columnMap
	) {

		if(searchFile.equals("narrow")) {
			psm.setDeltaMass(deltaMass);

		} else if(searchFile.equals("open")) {

			if(columnMap.containsKey("open_mod_localization")) {

				// aScore localization was performed

				// ensure all required columns are present
				final String[] requiredHeaders = new String[] {
						"open_mod_localization",
						"dm_used",
						"localized_better",
				};

				for(String requiredHeader : requiredHeaders) {
					if(!columnMap.containsKey(requiredHeader)) {
						throw new RuntimeException("Could not find column for \"" + requiredHeader + "\"");
					}
				}

				final String localizedString = fields[columnMap.get("open_mod_localization")];
				final boolean localizedBetter = fields[columnMap.get("localized_better")].equals("True");
				final boolean deltaMassUsed = fields[columnMap.get("dm_used")].equals("True");

				if(localizedBetter) {

					// localized version scored better, add position to localization and adjust delta mass as necessary
					AbstractMap.SimpleEntry<Integer, BigDecimal> modPair = getModFromReportedMod(
							localizedString,
							nakedPeptideSequence,
							null
					);

					Integer modPosition = modPair.getKey();;
					BigDecimal modMass = modPair.getValue();

					// set the open mod mass and delta mass
					if(deltaMassUsed) {
						psm.setOpenModification(new OpenModification(deltaMass, modPosition));
						psm.setDeltaMass(BigDecimal.ZERO);
					} else {
						psm.setOpenModification( new OpenModification(modMass, modPosition));
						psm.setDeltaMass(deltaMass.subtract(modMass));
					}

				} else {

					// use the non-localized version since it scored better
					psm.setOpenModification( new OpenModification(deltaMass, null));
					psm.setDeltaMass(BigDecimal.ZERO);
				}

			} else {

				// aScore localization wasn't performed
				psm.setOpenModification( new OpenModification(deltaMass, null));
				psm.setDeltaMass(BigDecimal.ZERO);
			}
		} else {
			throw new RuntimeException("\"search_file\" must be \"narrow\" or \"open\"");
		}

	}

	/**
	 * Get peptide sequence with mod information removed
	 * @param reportedPeptideString
	 * @return
	 */
	private static String getPeptideSequenceFromReportedPeptideString(String reportedPeptideString) {
		return reportedPeptideString.toUpperCase().replaceAll("[^A-Z]", "");
	}

	private static Map<Integer, BigDecimal> getVariableModsFromReportedMods(String reportedMods,String nakedPeptideSequence, Map<String, BigDecimal> staticMods) {
		Map<Integer, BigDecimal> mods = new HashMap<>();

		// no mods!
		if(reportedMods.length() < 1) { return mods; }

		for(String reportedMod : reportedMods.split(",")) {
			AbstractMap.SimpleEntry<Integer, BigDecimal> modPair = getModFromReportedMod(
					reportedMod,
					nakedPeptideSequence,
					staticMods
			);

			if(modPair != null) {
				mods.put(modPair.getKey(), modPair.getValue());
			}
		}

		return mods;
	}

	private static AbstractMap.SimpleEntry<Integer, BigDecimal> getModFromReportedMod(String reportedModString, String nakedPeptideSequence, Map<String, BigDecimal> staticMods) {
		Pattern p = Pattern.compile("(\\d+)\\[(.+)\\]");
		Matcher m = p.matcher(reportedModString);

		if(m.matches()) {
			int position = Integer.parseInt(m.group(1));

			// ensure we have a valid position for the mod
			if(position > nakedPeptideSequence.length()) {
				throw new RuntimeException("Got a mod position past the c-terminus of protein. Mod: " + reportedModString + "  Peptide: " + nakedPeptideSequence);
			} else if(position < 0) {
				throw new RuntimeException("Got a mod position preceding the n-terminus of protein. Mod: " + reportedModString + "  Peptide: " + nakedPeptideSequence);
			}

			Collection<BigDecimal> masses = new ArrayList<>();

			// mod masses are reported comma-delimited if there are more than one on this position
			for(String modMass : m.group(2).split(",")) {
				BigDecimal bdModMass = new BigDecimal(modMass);

				if(!isStaticMod(nakedPeptideSequence, position, bdModMass, staticMods)) {
					masses.add(new BigDecimal(modMass));
				}
			}

			// is a static mod, return null
			if(masses.size() < 1) { return null; }

			BigDecimal finalModMass = getBigDecimalCollectionSum(masses);
			return new AbstractMap.SimpleEntry<>(position, finalModMass);
		}

		throw new RuntimeException("Could not parse reported mod: " + reportedModString);
	}

	private static BigDecimal getBigDecimalCollectionSum(Collection<BigDecimal> bigDecimalCollection) {
		BigDecimal sum = null;

		for(BigDecimal bd : bigDecimalCollection) {
			if(sum == null) { sum = bd; }
			else {
				sum = sum.add(bd);
			}
		}

		return sum;
	}

	/**
	 * Return true if the supplied mod information corresponds to a known static mod
	 * @param nakedPeptideString
	 * @param position
	 * @param mass
	 * @param staticMods
	 * @return
	 */
	private static boolean isStaticMod(String nakedPeptideString, int position, BigDecimal mass, Map<String, BigDecimal> staticMods) {

		if(staticMods == null || staticMods.size() < 1) { return false; }

		String residue = nakedPeptideString.substring(position - 1, position);
		if(!staticMods.containsKey(residue)) { return false; }

		return CompareUtils.sameScaleEquals(mass, staticMods.get(residue));
	}
}
