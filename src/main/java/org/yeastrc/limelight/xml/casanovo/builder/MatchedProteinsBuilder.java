package org.yeastrc.limelight.xml.casanovo.builder;

import org.yeastrc.limelight.limelight_import.api.xml_dto.LimelightInput;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProtein;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProteinLabel;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProteins;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoReportedPeptide;
import org.yeastrc.proteomics.fasta.FASTAEntry;
import org.yeastrc.proteomics.fasta.FASTAFileParser;
import org.yeastrc.proteomics.fasta.FASTAFileParserFactory;
import org.yeastrc.proteomics.fasta.FASTAHeader;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * Build the MatchedProteins section of the limelight XML docs. This is done by finding all proteins in the FASTA
 * file that contains any of the peptide sequences found in the experiment. 
 * 
 * This is generalized enough to be usable by any pipeline
 * 
 * @author mriffle
 *
 */
public class MatchedProteinsBuilder {

	public static MatchedProteinsBuilder getInstance() { return new MatchedProteinsBuilder(); }

	/**
	 * Add all target proteins from the FASTA file that contain any of the peptides found in the experiment
	 * to the limelight xml document in the matched proteins section.
	 *
	 * @param limelightInputRoot
	 * @param peptideIdMap
	 * @throws Exception
	 */
	public void buildMatchedProteins( LimelightInput limelightInputRoot, Map<String, Integer> peptideIdMap ) throws Exception {

		System.err.print( " Matching peptides to proteins..." );

		// create the XML and add to root element
		buildAndAddMatchedProteinsToXML( limelightInputRoot, peptideIdMap );

	}


	/**
	 * Do the work of building the matched peptides element and adding to limelight xml root
	 *
	 * @param limelightInputRoot
	 * @param peptideIdMap
	 * @throws Exception
	 */
	private void buildAndAddMatchedProteinsToXML( LimelightInput limelightInputRoot,  Map<String, Integer> peptideIdMap ) throws Exception {

		MatchedProteins xmlMatchedProteins = new MatchedProteins();
		limelightInputRoot.setMatchedProteins( xmlMatchedProteins );

		for( String sequence : peptideIdMap.keySet() ) {

			MatchedProtein xmlProtein = new MatchedProtein();
			xmlMatchedProteins.getMatchedProtein().add( xmlProtein );

			xmlProtein.setSequence( sequence );
			xmlProtein.setId(BigInteger.valueOf(peptideIdMap.get(sequence)));

			MatchedProteinLabel xmlMatchedProteinLabel = new MatchedProteinLabel();
			xmlProtein.getMatchedProteinLabel().add( xmlMatchedProteinLabel );

			xmlMatchedProteinLabel.setName( sequence );
		}
	}


}
