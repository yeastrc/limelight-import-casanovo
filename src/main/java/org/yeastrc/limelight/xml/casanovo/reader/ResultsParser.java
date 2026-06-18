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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael Riffle
 *
 */
public class ResultsParser {

	/**
	 * The per-amino-acid scores column has been named differently by different versions of Casanovo.
	 * Older versions use "opt_ms_run[1]_aa_scores"; newer versions (e.g. 5.x) use "opt_global_aa_scores".
	 * Both contain the same data (comma-separated per-residue scores), so we accept either name.
	 * The names are listed in preference order.
	 */
	private static final String[] AA_SCORES_COLUMN_NAMES = new String[] {
			"opt_ms_run[1]_aa_scores",
			"opt_global_aa_scores"
	};

	/**
	 * The ProForma peptidoform column added in Casanovo 5.x. When present it carries the modified
	 * peptide (e.g. "M[Oxidation]AVEVTEFAK"); the plain "sequence" column is then the naked sequence.
	 * Older versions lack this column and inline modifications in the "sequence" column instead.
	 */
	private static final String PROFORMA_COLUMN_NAME = "opt_global_cv_MS:1003169_proforma_peptidoform_sequence";

	private static final Pattern MS_RUN_PATTERN = Pattern.compile("ms_run\\[(\\d+)]");

	/**
	 * Find the index of the per-amino-acid scores column, accommodating the differing column
	 * names used by different versions of Casanovo.
	 *
	 * @param columnMap map of column header to its index on the line
	 * @return the index of the per-amino-acid scores column, or null if none is present
	 */
	private static Integer getAAScoresColumnIndex(Map<String, Integer> columnMap) {
		for(String columnName : AA_SCORES_COLUMN_NAMES) {
			Integer index = columnMap.get(columnName);
			if(index != null) {
				return index;
			}
		}
		return null;
	}

	public static CasanovoResults getResults(File targetsFile, ConfigParser configParser) throws Exception {

		CasanovoResults results = new CasanovoResults();
		Map<CasanovoReportedPeptide,Collection<CasanovoPSM>> resultMap = new HashMap<>();
		results.setPeptidePSMMap(resultMap);

		try(BufferedReader br = Files.newBufferedReader(targetsFile.toPath(), StandardCharsets.UTF_8)) {

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

					if(getAAScoresColumnIndex(columnMap) == null) {
						throw new RuntimeException("Could not find a per-amino-acid scores column (expected one of: \""
								+ String.join("\", \"", AA_SCORES_COLUMN_NAMES) + "\")");
					}

				} else if(line.startsWith("PSM")) {
					if(columnMap == null) {
						throw new RuntimeException("Encountered a PSM line before the PSH header line.");
					}
					CasanovoPSM psm = getPSMFromLine(line, columnMap, configParser);
					CasanovoReportedPeptide reportedPeptide = ReportedPeptideUtils.getReportedPeptideForPSM( psm );

					results.getPeptidePSMMap().computeIfAbsent(reportedPeptide, k -> new ArrayList<>()).add(psm);
				}
			}
		}

		return results;
	}

	/**
	 * Get a map of column headers to the index of that column on the line
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
	 */
	private static int getScanNumberFromSpectraRef(String spectraRef) throws Exception {
		int scanIndex = spectraRef.indexOf("scan=");
		if (scanIndex == -1) {
			throw new Exception("scan= not found in spectra_ref");
		}

		String afterScan = spectraRef.substring(scanIndex + 5); // 5 is length of "scan="

		int endIndex = afterScan.indexOf(' ');
		String scanNumberStr = (endIndex == -1) ? afterScan : afterScan.substring(0, endIndex);

		try {
			return Integer.parseInt(scanNumberStr);
		} catch (NumberFormatException e) {
			throw new Exception("Invalid scan number format: " + scanNumberStr);
		}
	}

	/**
	 * Get the mzTab ms_run index (1-based) from the spectra_ref field, identifying which spectrum
	 * file the PSM came from (e.g. "ms_run[2]:...scan=123" -> 2).
	 */
	private static int getRunIndexFromSpectraRef(String spectraRef) throws Exception {
		Matcher m = MS_RUN_PATTERN.matcher(spectraRef);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		throw new Exception("ms_run[...] index not found in spectra_ref: " + spectraRef);
	}

	public static byte parseDecimalStringToByte(String decimalString) {
		BigDecimal decimal = new BigDecimal(decimalString);
		BigDecimal rounded = decimal.setScale(0, RoundingMode.FLOOR);
		return rounded.byteValueExact();
	}

	private static CasanovoPSM getPSMFromLine(String line, Map<String, Integer> columnMap, ConfigParser configParser) throws Exception {

		String[] fields = line.split("\\t", -1);

		final String nakedSequenceColumn = fields[columnMap.get("sequence")];
		final String spectraRef = fields[columnMap.get("spectra_ref")];
		final String chargeString = fields[columnMap.get("charge")];
		final BigDecimal score = new BigDecimal(fields[columnMap.get("search_engine_score[1]")]);
		final BigDecimal adjScore = score.signum() < 0 ? score.add(BigDecimal.ONE) : score;
		final BigDecimal precursorMZ = new BigDecimal(fields[columnMap.get("exp_mass_to_charge")]);

		// The modified peptidoform: prefer the ProForma column (Casanovo 5.x); otherwise the
		// "sequence" column carries the modifications inline (older Casanovo).
		final String peptidoform = getPeptidoform(fields, columnMap, nakedSequenceColumn);

		List<BigDecimal> perPositionScores = parsePerPositionScores(fields, columnMap);

		int scanNumber = getScanNumberFromSpectraRef(spectraRef);
		int msRunIndex = getRunIndexFromSpectraRef(spectraRef);
		byte charge = parseDecimalStringToByte(chargeString);

		CasanovoPSM psm = new CasanovoPSM();

		psm.setReportedPeptideString( peptidoform ); // for error messages
		psm.setScanNumber(scanNumber);
		psm.setMsRunIndex(msRunIndex);
		psm.setCharge(charge);
		psm.setScore(score);
		psm.setAdjScore(adjScore);
		psm.setPrecursorMZ(precursorMZ);
		psm.setPerPositionScores(perPositionScores);

		psm.setPeptideSequence(ProformaPeptideParser.parseNakedSequence(peptidoform));
		psm.setMods(ProformaPeptideParser.parseVariableMods(peptidoform, configParser.getResidues(), configParser.getFixedModTokens()));

		return psm;
	}

	/**
	 * Return the modified peptidoform string for a row: the ProForma column when present and non-empty,
	 * otherwise the supplied fallback (the naked "sequence" column).
	 */
	private static String getPeptidoform(String[] fields, Map<String, Integer> columnMap, String fallback) {
		Integer index = columnMap.get(PROFORMA_COLUMN_NAME);
		if (index != null && index < fields.length) {
			String value = fields[index];
			if (value != null && !value.isEmpty()) {
				return value;
			}
		}
		return fallback;
	}

	private static List<BigDecimal> parsePerPositionScores(String[] fields, Map<String, Integer> columnMap) {
		Integer index = getAAScoresColumnIndex(columnMap);
		if (index == null) {
			return null;
		}
		String allScores = fields[index];
		if (allScores == null || allScores.isEmpty()) {
			return null;
		}
		String[] split = allScores.split(",");
		List<BigDecimal> perPositionScores = new ArrayList<>(split.length);
		for (String s : split) {
			perPositionScores.add(new BigDecimal(s));
		}
		return perPositionScores;
	}
}
