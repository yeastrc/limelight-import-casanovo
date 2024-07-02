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

import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoReportedPeptide;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;
import org.yeastrc.limelight.xml.casanovo.utils.ReportedPeptideUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael Riffle
 *
 */
public class ResultsParser {

	public static CasanovoResults getResults(File targetsFile, ConfigParser configParser) throws Throwable {

		CasanovoResults results = new CasanovoResults();
		Map<CasanovoReportedPeptide,Collection<CasanovoPSM>> resultMap = new HashMap<>();
		results.setPeptidePSMMap(resultMap);

		try(BufferedReader br = new BufferedReader(new FileReader( targetsFile ))) {

			Map<String, Integer> columnMap = null;

			final String[] requiredHeaders = new String[] {
					"sequence",
					"spectra_ref",
					"search_engine_score[1]",
					"charge",
					"exp_mass_to_charge"
			};

			for(String line = br.readLine(); line != null; line = br.readLine()) {

				if(line.startsWith("PSH")) {
					columnMap = processHeaderLine(line);

					for(String requiredHeader : requiredHeaders) {
						if(!columnMap.containsKey(requiredHeader)) {
							throw new RuntimeException("Could not find column for \"" + requiredHeader + "\"");
						}
					}

				} else if(line.startsWith("PSM")) {
					CasanovoPSM psm = getPSMFromLine(line, columnMap, configParser);
					CasanovoReportedPeptide reportedPeptide = ReportedPeptideUtils.getReportedPeptideForPSM( psm );

					if( !results.getPeptidePSMMap().containsKey( reportedPeptide ) )
						results.getPeptidePSMMap().put( reportedPeptide, new ArrayList<>() );

					results.getPeptidePSMMap().get( reportedPeptide ).add(psm);
				}
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
	 * Get the scan number from the spectra_ref field
	 *
	 * @param spectraRef
	 * @return
	 * @throws Exception
	 */
	private static int getScanNumberFromSpectraRef(String spectraRef) throws Exception {
		String[] fields = spectraRef.split(":");
		if(fields.length != 2) { throw new Exception("Unexpected format for spectra_ref"); }
		fields = fields[1].split("=");
		if(fields.length != 2) { throw new Exception("Unexpected format for spectra_ref"); }
		if(!fields[0].equals("scan")) { throw new Exception("Unexpected format for spectra_ref"); }
		return Integer.parseInt(fields[1]);
	}

	public static byte parseDecimalStringToByte(String decimalString) {
		BigDecimal decimal = new BigDecimal(decimalString);
		BigDecimal rounded = decimal.setScale(0, RoundingMode.FLOOR);
		return rounded.byteValueExact();
	}

	/**
	 *
	 * @param line
	 * @param columnMap
	 * @return
	 * @throws Exception
	 */
	private static CasanovoPSM getPSMFromLine(String line, Map<String, Integer> columnMap, ConfigParser configParser) throws Exception {

		String[] fields = line.split("\\t", -1);

		final String reportedPeptideString = fields[columnMap.get("sequence")];
		final String spectraRef = fields[columnMap.get("spectra_ref")];
		final String chargeString = fields[columnMap.get("charge")];
		final BigDecimal score = new BigDecimal(fields[columnMap.get("search_engine_score[1]")]);
		final BigDecimal precursorMZ = new BigDecimal(fields[columnMap.get("exp_mass_to_charge")]);

		int scanNumber = getScanNumberFromSpectraRef(spectraRef);
		byte charge = parseDecimalStringToByte(chargeString);

		CasanovoPSM psm = new CasanovoPSM();

		psm.setScanNumber(scanNumber);
		psm.setCharge(charge);
		psm.setScore(score);
		psm.setPrecursorMZ(precursorMZ);

		String nakedPeptideSequence = getPeptideSequenceFromReportedPeptideString(reportedPeptideString);

		// handle peptide string
		psm.setPeptideSequence(nakedPeptideSequence);

		// handle var mods
		psm.setMods(getVariableModsFromReportedMods(reportedPeptideString, configParser.getResidues()));

		return psm;
	}

	/**
	 * Get peptide sequence with mod information removed
	 * @param reportedPeptideString
	 * @return
	 */
	private static String getPeptideSequenceFromReportedPeptideString(String reportedPeptideString) {
		return reportedPeptideString.toUpperCase().replaceAll("[^A-Z]", "");
	}

	private static Map<Integer, BigDecimal> getVariableModsFromReportedMods(String reportedPeptideString, Map<String, BigDecimal> residuesMap) throws Exception {
		Map<Integer, BigDecimal> variableMods = new HashMap<>();

		int position = 0;
		StringBuilder currentMod = new StringBuilder();
		char previousResidue = '\0';
		boolean readingMod = false;

		for (int i = 0; i < reportedPeptideString.length(); i++) {
			char c = reportedPeptideString.charAt(i);

			if (Character.isLetter(c)) {
				if (readingMod) {
					// We've finished reading a modification

					if(position != 0) {
						processMod(variableMods, position, previousResidue + currentMod.toString(), residuesMap);
					} else {
						processMod(variableMods, position, currentMod.toString(), residuesMap);
					}
					currentMod.setLength(0);
					readingMod = false;
				}
				previousResidue = c;
				position++;
			} else if (c == '+' || c == '-') {
				readingMod = true;
				currentMod.append(c);
			} else if (Character.isDigit(c) || c == '.') {
				if (readingMod) {
					currentMod.append(c);
				} else {
					throw new Exception("Invalid mod format in peptide: " + reportedPeptideString);
				}
			}
		}

		// Check if there's a modification at the end of the string
		if (readingMod) {
			processMod(variableMods, position - 1, previousResidue + currentMod.toString(), residuesMap);
		}

		return variableMods;
	}

	private static void processMod(Map<Integer, BigDecimal> variableMods, int position, String fullMod, Map<String, BigDecimal> residuesMap) throws Exception {
		if (fullMod.equals("C+57.021")) {
			return; // Ignore this specific modification
		}

		if(position == 0) {
			if (!residuesMap.containsKey(fullMod)) {
				throw new Exception("Modification " + fullMod + " not found in residues map");
			}

			BigDecimal modMass = residuesMap.get(fullMod);
			variableMods.put(position, modMass);
		} else {
			if (!residuesMap.containsKey(fullMod)) {
				throw new Exception("Modification " + fullMod + " not found in residues map");
			}

			String residue = fullMod.substring(0, 1);
			if (!residuesMap.containsKey(residue)) {
				throw new Exception("Residue " + residue + " not found in residues map");
			}

			BigDecimal modMass = residuesMap.get(fullMod).subtract(residuesMap.get(residue));
			variableMods.put(position, modMass);
		}
	}
}
