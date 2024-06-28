package org.yeastrc.limelight.xml.casanovo.builder;

import org.yeastrc.limelight.limelight_import.api.xml_dto.LimelightInput;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProtein;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProteinLabel;
import org.yeastrc.limelight.limelight_import.api.xml_dto.MatchedProteins;
import org.yeastrc.limelight.xml.casanovo.objects.CongaReportedPeptide;
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
	 * @param fastaFile
	 * @param reportedPeptides
	 * @throws Exception
	 */
	public void buildMatchedProteins( LimelightInput limelightInputRoot, File fastaFile, Collection<CongaReportedPeptide> reportedPeptides ) throws Exception {

		System.err.print( " Matching peptides to proteins..." );

		// process the reported peptides to get naked peptide objects
		Collection<PeptideObject> nakedPeptideObjects = getNakedPeptideObjectsForReportedPeptides( reportedPeptides );

		// find the proteins matched by any of these peptides
		Map<String, Collection<FastaProteinAnnotation>> proteins = getProteins( nakedPeptideObjects, fastaFile );

		// create the XML and add to root element
		buildAndAddMatchedProteinsToXML( limelightInputRoot, proteins );

	}


	private Collection<PeptideObject> getNakedPeptideObjectsForReportedPeptides( Collection<CongaReportedPeptide> peptides ) {

		Collection<PeptideObject> nakedPeptideObjects = new HashSet<>();

		for( CongaReportedPeptide reportedPeptide : peptides ) {

			PeptideObject nakedPeptideObject = new PeptideObject();
			nakedPeptideObject.setFoundMatchingProtein( false );
			nakedPeptideObject.setPeptideSequence( reportedPeptide.getNakedPeptide() );

			nakedPeptideObjects.add( nakedPeptideObject );
		}


		return nakedPeptideObjects;
	}





	/* ***************** REST OF THIS CAN BE MOVED TO CENTRALIZED LIB **************************** */



	/**
	 * Do the work of building the matched peptides element and adding to limelight xml root
	 *
	 * @param limelightInputRoot
	 * @param proteins
	 * @throws Exception
	 */
	private void buildAndAddMatchedProteinsToXML( LimelightInput limelightInputRoot, Map<String, Collection<FastaProteinAnnotation>> proteins ) throws Exception {

		MatchedProteins xmlMatchedProteins = new MatchedProteins();
		limelightInputRoot.setMatchedProteins( xmlMatchedProteins );

		for( String sequence : proteins.keySet() ) {

			if( proteins.get( sequence ).isEmpty() ) continue;

			MatchedProtein xmlProtein = new MatchedProtein();
			xmlMatchedProteins.getMatchedProtein().add( xmlProtein );

			xmlProtein.setSequence( sequence );

			for( FastaProteinAnnotation anno : proteins.get( sequence ) ) {
				MatchedProteinLabel xmlMatchedProteinLabel = new MatchedProteinLabel();
				xmlProtein.getMatchedProteinLabel().add( xmlMatchedProteinLabel );

				xmlMatchedProteinLabel.setName( anno.getName() );

				if( anno.getDescription() != null )
					xmlMatchedProteinLabel.setDescription( anno.getDescription() );

				if( anno.getTaxonomId() != null )
					xmlMatchedProteinLabel.setNcbiTaxonomyId( new BigInteger( anno.getTaxonomId().toString() ) );
			}
		}
	}


	/**
	 * Get a map of the distinct target protein sequences mapped to a collection of target annotations for that sequence
	 * from the given fasta file, where the sequence contains any of the supplied peptide sequences
	 *
	 * @param nakedPeptideObjects
	 * @param fastaFile
	 * @return
	 * @throws Exception
	 */
	private Map<String, Collection<FastaProteinAnnotation>> getProteins( Collection<PeptideObject> nakedPeptideObjects, File fastaFile ) throws Exception {

		Map<String, Collection<FastaProteinAnnotation>> proteinAnnotations = new HashMap<>();

		try ( FASTAFileParser parser = FASTAFileParserFactory.getInstance().getFASTAFileParser( fastaFile ) ) {

			int count = 0;
			System.err.println( "" );

			for ( FASTAEntry entry = parser.getNextEntry(); entry != null; entry = parser.getNextEntry() ) {

				count++;
				boolean foundPeptideForFASTAEntry = false;

				// use this sequence to determine if it contains a peptide sequence
				String fastaSequence = entry.getSequence().replaceAll( "L", "I" );

				System.err.print( "\tTested " + count + " FASTA entries...\r" );

				for( PeptideObject nakedPeptideObject : nakedPeptideObjects ) {

					// optimization: if we already know we're including this protein and
					// we have already mapped this peptide to any protein, we can skip
					// this peptide...
					if( foundPeptideForFASTAEntry && nakedPeptideObject.foundMatchingProtein ) {
						continue;
					}

					if( nakedPeptideObject.getSearchSequence() == null ) {
						nakedPeptideObject.setSearchSequence( nakedPeptideObject.getPeptideSequence().replaceAll( "L", "I" ) );
					}

					String peptideSearchSequence = nakedPeptideObject.getSearchSequence();

					if( fastaSequence.contains( peptideSearchSequence ) ) {

						// this protein has a matching peptide

						if( !foundPeptideForFASTAEntry ) {
							for( FASTAHeader header : entry.getHeaders() ) {

								// strip trailing asterisk from protein sequence
								String entrySequence = entry.getSequence();
								if(entrySequence.endsWith("*")) {
									entrySequence = entrySequence.substring(0, entrySequence.length() - 1);
								}

								if( !proteinAnnotations.containsKey( entrySequence ) )
									proteinAnnotations.put( entrySequence, new HashSet<>() );

								FastaProteinAnnotation anno = new FastaProteinAnnotation();
								anno.setName( header.getName() );
								anno.setDescription( header.getDescription() );

								proteinAnnotations.get( entrySequence ).add( anno );

							}//end iterating over fasta headers

							foundPeptideForFASTAEntry = true;
						}

						nakedPeptideObject.setFoundMatchingProtein( true );

					} // end if statement for protein containing peptide

				} // end iterating over peptide sequences

			}// end iterating over fasta entries

			if( nakedPeptideObjectsContainsUnmatchedPeptides( nakedPeptideObjects ) ) {
				System.err.println( "\nError: Not all peptides in the results could be matched to a protein in the FASTA file." );
				System.err.println( "\tUnmatched peptides:" );
				for( PeptideObject nakedPeptideObject : nakedPeptideObjects ) {
					if( !nakedPeptideObject.isFoundMatchingProtein() ) {
						System.err.println( nakedPeptideObject.getPeptideSequence() );
					}
				}

				throw new Exception( "Could not match all peptides to a protein in the FASTA file." );
			}

			System.err.print( "\n" );


		}

		return proteinAnnotations;
	}


	private boolean nakedPeptideObjectsContainsUnmatchedPeptides( Collection<PeptideObject> nakedPeptideObjects ) {

		for( PeptideObject nakedPeptideObject : nakedPeptideObjects ) {
			if( !nakedPeptideObject.isFoundMatchingProtein() ) {
				return true;
			}
		}

		return false;
	}

	private class PeptideObject {

		private String peptideSequence;
		private String searchSequence;
		private boolean foundMatchingProtein;

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((peptideSequence == null) ? 0 : peptideSequence.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PeptideObject))
				return false;
			PeptideObject other = (PeptideObject) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (peptideSequence == null) {
				if (other.peptideSequence != null)
					return false;
			} else if (!peptideSequence.equals(other.peptideSequence))
				return false;
			return true;
		}

		private MatchedProteinsBuilder getOuterType() {
			return MatchedProteinsBuilder.this;
		}

		/**
		 * @return the peptideSequence
		 */
		public String getPeptideSequence() {
			return peptideSequence;
		}

		/**
		 * @param peptideSequence the peptideSequence to set
		 */
		public void setPeptideSequence(String peptideSequence) {
			this.peptideSequence = peptideSequence;
		}

		/**
		 * @return the foundMatchingProtein
		 */
		public boolean isFoundMatchingProtein() {
			return foundMatchingProtein;
		}

		/**
		 * @param foundMatchingProtein the foundMatchingProtein to set
		 */
		public void setFoundMatchingProtein(boolean foundMatchingProtein) {
			this.foundMatchingProtein = foundMatchingProtein;
		}

		/**
		 * @return the searchSequence
		 */
		public String getSearchSequence() {
			return searchSequence;
		}

		/**
		 * @param searchSequence the searchSequence to set
		 */
		public void setSearchSequence(String searchSequence) {
			this.searchSequence = searchSequence;
		}

	}


	/**
	 * An annotation for a protein in a Fasta file
	 *
	 * @author mriffle
	 *
	 */
	private class FastaProteinAnnotation {


		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((taxonomId == null) ? 0 : taxonomId.hashCode());
			return result;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof FastaProteinAnnotation))
				return false;
			FastaProteinAnnotation other = (FastaProteinAnnotation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (taxonomId == null) {
				if (other.taxonomId != null)
					return false;
			} else if (!taxonomId.equals(other.taxonomId))
				return false;
			return true;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Integer getTaxonomId() {
			return taxonomId;
		}
		public void setTaxonomId(Integer taxonomId) {
			this.taxonomId = taxonomId;
		}



		private String name;
		private String description;
		private Integer taxonomId;
		private MatchedProteinsBuilder getOuterType() {
			return MatchedProteinsBuilder.this;
		}

	}

}
