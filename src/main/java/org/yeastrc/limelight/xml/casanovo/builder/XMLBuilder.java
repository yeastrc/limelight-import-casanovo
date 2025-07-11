package org.yeastrc.limelight.xml.casanovo.builder;

import org.yeastrc.limelight.limelight_import.api.xml_dto.*;
import org.yeastrc.limelight.limelight_import.api.xml_dto.ReportedPeptide.ReportedPeptideAnnotations;
import org.yeastrc.limelight.limelight_import.api.xml_dto.SearchProgram.PsmAnnotationTypes;
import org.yeastrc.limelight.limelight_import.api.xml_dto.SearchProgram.PsmPeptidePositionAnnotationTypes;
import org.yeastrc.limelight.limelight_import.create_import_file_from_java_objects.main.CreateImportFileFromJavaObjectsMain;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMAnnotationTypeSortOrder;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMDefaultVisibleAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMPeptidePositionAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMPeptidePosition_DefaultVisibleAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.constants.Constants;
import org.yeastrc.limelight.xml.casanovo.objects.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class XMLBuilder {

	public void buildAndSaveXML( ConversionParameters conversionParameters,
								 SearchMetadata searchMetadata,
			                     CasanovoResults casanovoResults)
    throws Exception {

		LimelightInput limelightInputRoot = new LimelightInput();

		limelightInputRoot.setFastaFilename( "none" );
		
		// add in the conversion program (this program) information
		ConversionProgramBuilder.createInstance().buildConversionProgramSection( limelightInputRoot, conversionParameters);
		
		SearchProgramInfo searchProgramInfo = new SearchProgramInfo();
		limelightInputRoot.setSearchProgramInfo( searchProgramInfo );
		
		SearchPrograms searchPrograms = new SearchPrograms();
		searchProgramInfo.setSearchPrograms( searchPrograms );
		

		boolean has_PerPositionScores = false;
		
		for ( Collection<CasanovoPSM> casanovoPSM_Collection : casanovoResults.getPeptidePSMMap().values() ) {
			
			for ( CasanovoPSM casanovoPSM : casanovoPSM_Collection ) {
				
				if ( casanovoPSM.getPerPositionScores() != null ) {
					
					has_PerPositionScores = true;
					break;
				}
			}
			if ( has_PerPositionScores ) {
				break;
			}
		}


		{
			SearchProgram searchProgram = new SearchProgram();
			searchPrograms.getSearchProgram().add( searchProgram );

			searchProgram.setName( Constants.PROGRAM_NAME_CASANOVO);
			searchProgram.setDisplayName( Constants.PROGRAM_NAME_CASANOVO );
			searchProgram.setVersion(searchMetadata.getCasanovoVersion() );

			//
			// Define the annotation types 
			//
			PsmAnnotationTypes psmAnnotationTypes = new PsmAnnotationTypes();
			searchProgram.setPsmAnnotationTypes( psmAnnotationTypes );
			
			FilterablePsmAnnotationTypes filterablePsmAnnotationTypes = new FilterablePsmAnnotationTypes();
			psmAnnotationTypes.setFilterablePsmAnnotationTypes( filterablePsmAnnotationTypes );
			
			for( FilterablePsmAnnotationType annoType : PSMAnnotationTypes.getFilterablePsmAnnotationTypes() ) {
				filterablePsmAnnotationTypes.getFilterablePsmAnnotationType().add( annoType );
			}
			
			{
				if ( has_PerPositionScores ) {

					PsmPeptidePositionAnnotationTypes psmPeptidePositionAnnotationTypes = new PsmPeptidePositionAnnotationTypes();
					searchProgram.setPsmPeptidePositionAnnotationTypes( psmPeptidePositionAnnotationTypes );
					
					FilterablePsmPeptidePositionAnnotationTypes filterablePsmPeptidePositionAnnotationTypes = new FilterablePsmPeptidePositionAnnotationTypes();
					psmPeptidePositionAnnotationTypes.setFilterablePsmPeptidePositionAnnotationTypes( filterablePsmPeptidePositionAnnotationTypes );
					
					for ( FilterablePsmPeptidePositionAnnotationType filterablePsmPeptidePositionAnnotationType : PSMPeptidePositionAnnotationTypes.getFilterablePsmPeptidePositionAnnotationTypes() ) {
						filterablePsmPeptidePositionAnnotationTypes.getFilterablePsmPeptidePositionAnnotationType().add( filterablePsmPeptidePositionAnnotationType );
					}
				}
			}
		}

		
		//
		// Define which annotation types are visible by default
		//
		DefaultVisibleAnnotations xmlDefaultVisibleAnnotations = new DefaultVisibleAnnotations();
		searchProgramInfo.setDefaultVisibleAnnotations( xmlDefaultVisibleAnnotations );
		
		VisiblePsmAnnotations xmlVisiblePsmAnnotations = new VisiblePsmAnnotations();
		xmlDefaultVisibleAnnotations.setVisiblePsmAnnotations( xmlVisiblePsmAnnotations );

		for( SearchAnnotation sa : PSMDefaultVisibleAnnotationTypes.getDefaultVisibleAnnotationTypes() ) {
			xmlVisiblePsmAnnotations.getSearchAnnotation().add( sa );
		}
		
		if ( has_PerPositionScores ) {
			
			VisiblePsmPeptidePositionAnnotations visiblePsmPeptidePositionAnnotations = new VisiblePsmPeptidePositionAnnotations();
			xmlDefaultVisibleAnnotations.setVisiblePsmPeptidePositionAnnotations( visiblePsmPeptidePositionAnnotations );
			
			for( SearchAnnotation sa : PSMPeptidePosition_DefaultVisibleAnnotationTypes.getDefaultVisibleAnnotationTypes() ) {
				visiblePsmPeptidePositionAnnotations.getSearchAnnotation().add( sa );
			}
		}
		
		//
		// Define the default display order in limelight
		//
		AnnotationSortOrder xmlAnnotationSortOrder = new AnnotationSortOrder();
		searchProgramInfo.setAnnotationSortOrder( xmlAnnotationSortOrder );
		
		PsmAnnotationSortOrder xmlPsmAnnotationSortOrder = new PsmAnnotationSortOrder();
		xmlAnnotationSortOrder.setPsmAnnotationSortOrder( xmlPsmAnnotationSortOrder );
		
		for( SearchAnnotation xmlSearchAnnotation : PSMAnnotationTypeSortOrder.getPSMAnnotationTypeSortOrder() ) {
			xmlPsmAnnotationSortOrder.getSearchAnnotation().add( xmlSearchAnnotation );
		}
		
		//
		// Define the static mods
		//

		// Assume casanovo always has +57.021464 on C
		StaticModifications smods = new StaticModifications();
		limelightInputRoot.setStaticModifications( smods );

		StaticModification xmlSmod = new StaticModification();
		xmlSmod.setAminoAcid("C");
		xmlSmod.setMassChange(new BigDecimal("57.021464"));
		smods.getStaticModification().add(xmlSmod);

		//
		// Define the peptide and PSM data
		//

		Map<String, Integer> peptideIdMap = getPeptideIdMap(casanovoResults);

		ReportedPeptides reportedPeptides = new ReportedPeptides();
		limelightInputRoot.setReportedPeptides( reportedPeptides );
		
		// iterate over each distinct reported peptide
		for( CasanovoReportedPeptide casanovoReportedPeptide : casanovoResults.getPeptidePSMMap().keySet() ) {

			ReportedPeptide xmlReportedPeptide = new ReportedPeptide();
			reportedPeptides.getReportedPeptide().add( xmlReportedPeptide );
			
			xmlReportedPeptide.setReportedPeptideString( casanovoReportedPeptide.getReportedPeptideString() );
			xmlReportedPeptide.setSequence( casanovoReportedPeptide.getNakedPeptide() );

			// Add in Matched Protein info
			MatchedProteinsForPeptide xProteinsForPeptide = new MatchedProteinsForPeptide();
			xmlReportedPeptide.setMatchedProteinsForPeptide( xProteinsForPeptide );

			MatchedProteinForPeptide xProteinForPeptide = new MatchedProteinForPeptide();
			xProteinsForPeptide.getMatchedProteinForPeptide().add( xProteinForPeptide );

			xProteinForPeptide.setId( BigInteger.valueOf( peptideIdMap.get(casanovoReportedPeptide.getNakedPeptide() ) ) );


			// add in the filterable peptide annotations (e.g., q-value)
			ReportedPeptideAnnotations xmlReportedPeptideAnnotations = new ReportedPeptideAnnotations();
			xmlReportedPeptide.setReportedPeptideAnnotations( xmlReportedPeptideAnnotations );

			// add in the mods for this peptide
			if( casanovoReportedPeptide.getMods() != null && casanovoReportedPeptide.getMods().keySet().size() > 0 ) {

				PeptideModifications xmlModifications = new PeptideModifications();
				xmlReportedPeptide.setPeptideModifications( xmlModifications );

				for( int position : casanovoReportedPeptide.getMods().keySet() ) {
					PeptideModification xmlModification = new PeptideModification();
					xmlModifications.getPeptideModification().add( xmlModification );

					xmlModification.setMass( casanovoReportedPeptide.getMods().get( position ).stripTrailingZeros().setScale( 4, RoundingMode.HALF_UP ) );

					if(position == 0)
						xmlModification.setIsNTerminal(true);

					else if(position == casanovoReportedPeptide.getNakedPeptide().length())
						xmlModification.setIsCTerminal(true);

					else
						xmlModification.setPosition( new BigInteger( String.valueOf( position ) ) );

				}
			}

			
			// add in the PSMs and annotations
			Psms xmlPsms = new Psms();
			xmlReportedPeptide.setPsms( xmlPsms );

			// iterate over all PSMs for this reported peptide

			for( CasanovoPSM psm : casanovoResults.getPeptidePSMMap().get(casanovoReportedPeptide)) {

				Psm xmlPsm = new Psm();
				xmlPsms.getPsm().add( xmlPsm );

				xmlPsm.setScanNumber( new BigInteger( String.valueOf( psm.getScanNumber() ) ) );
				xmlPsm.setPrecursorCharge( new BigInteger( String.valueOf( psm.getCharge() ) ) );
				xmlPsm.setScanFileName(searchMetadata.getScanFileName());
				xmlPsm.setPrecursorMZ(psm.getPrecursorMZ());

				// add in the filterable PSM annotations (e.g., score)
				FilterablePsmAnnotations xmlFilterablePsmAnnotations = new FilterablePsmAnnotations();
				xmlPsm.setFilterablePsmAnnotations( xmlFilterablePsmAnnotations );

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CASANOVO_SCORE );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CASANOVO );
					xmlFilterablePsmAnnotation.setValue( psm.getScore() );
				}

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CASANOVO_EFDR );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CASANOVO );
					xmlFilterablePsmAnnotation.setValue( psm.getEfdr() );
				}
				
				//  Add in the PSM Peptide Position Annotations
				
				if ( psm.getPerPositionScores() != null ) {
					
					//   https://github.com/Noble-Lab/casanovo/issues/485
					
					//  The order of the scores is the same as the order of the amino acids in the peptide.

					// What would the formula be for "the final two amino acid scores should be combined"?    Product.
					
					
					PsmPeptidePositionAnnotations psmPeptidePositionAnnotations = new PsmPeptidePositionAnnotations();
					xmlPsm.setPsmPeptidePositionAnnotations( psmPeptidePositionAnnotations );
					
					FilterablePsmPeptidePositionAnnotations filterablePsmPeptidePositionAnnotations = new FilterablePsmPeptidePositionAnnotations();
					psmPeptidePositionAnnotations.setFilterablePsmPeptidePositionAnnotations( filterablePsmPeptidePositionAnnotations );
					
					//  psm.getPerPositionScores() - 
					//		Scores order is c-terminal to n-terminal 
					//		   if the # Scores = (Peptide length + 1), then the last score is for the n-terminal mod
					//				Combine the scores for the  n-terminal mod and the n-terminal peptide position (last 2 scores)
					//					by multiplying them.

					int peptideSequence_Length = psm.getPeptideSequence().length();

					int psm_getPerPositionScores_Size = psm.getPerPositionScores().size();
					
					if ( psm_getPerPositionScores_Size < peptideSequence_Length ) {
						String msg = "Per Position Scores is shorter than Peptide Length. Per Position Scores count: "
								+ psm_getPerPositionScores_Size
								+ ", Peptide Length: " + peptideSequence_Length
								+ ", Peptide: " + psm.getPeptideSequence()
								+ ", Peptide in results file: " + psm.getReportedPeptideString();
						System.err.println( msg );
						throw new Exception(msg);
					}

					if ( psm_getPerPositionScores_Size > ( peptideSequence_Length + 1 ) ) {
						String msg = "Per Position Scores is larger than Peptide Length + 1 ( +1 for optional score on n-terminal mod). Per Position Scores count: "
								+ psm_getPerPositionScores_Size
								+ ", Peptide Length: " + peptideSequence_Length
								+ ", Peptide: " + psm.getPeptideSequence()
								+ ", Peptide in results file: " + psm.getReportedPeptideString();
						System.err.println( msg );
						throw new Exception(msg);
					}
					
					for ( int peptideSequence_Index = 0; peptideSequence_Index < peptideSequence_Length; peptideSequence_Index++ ) {  

						int perPositionScore_Index = peptideSequence_Index;
						
						if ( psm_getPerPositionScores_Size == ( peptideSequence_Length + 1 ) ) {
							
							perPositionScore_Index = peptideSequence_Index + 1; // Add 1 to skip n-terminal modification score
						}
						
						BigDecimal perPositionScore = psm.getPerPositionScores().get( perPositionScore_Index );
						
						if ( peptideSequence_Index == 0 ) {
							
							//  At first Peptide Sequence Position
							
							if ( psm_getPerPositionScores_Size == ( peptideSequence_Length + 1 ) ) {
								
								//  Have n-terminal mod score so need to combine that with the peptide position 1 (index 0) score

								int perPositionScore_Index_N_Terminal = perPositionScore_Index - 1; // index before current perPositionScore_Index
							
								BigDecimal perPositionScore_N_Terminal = psm.getPerPositionScores().get( perPositionScore_Index_N_Terminal );

								//  Create MathContext for multiplication to keep same number of digits
								int perPositionScore_Scale = perPositionScore.scale();
								MathContext mathContext = new MathContext( perPositionScore_Scale, RoundingMode.HALF_UP );
								
								//  final perPositionScore is perPositionScore multiplied by perPositionScore_N_Terminal
								
								perPositionScore = perPositionScore.multiply( perPositionScore_N_Terminal, mathContext );
							}
						}
						
						int scorePosition = peptideSequence_Index + 1; // Position is 1 based
					
						FilterablePsmPeptidePositionAnnotation filterablePsmPeptidePositionAnnotation = new FilterablePsmPeptidePositionAnnotation();
						filterablePsmPeptidePositionAnnotations.getFilterablePsmPeptidePositionAnnotation().add( filterablePsmPeptidePositionAnnotation );
						
						filterablePsmPeptidePositionAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CASANOVO );
						filterablePsmPeptidePositionAnnotation.setAnnotationName( PSMPeptidePositionAnnotationTypes.CASANOVO_PEPTIDE_POSITION_SCORE );
						filterablePsmPeptidePositionAnnotation.setPosition( BigInteger.valueOf( scorePosition ) );
						filterablePsmPeptidePositionAnnotation.setValue( perPositionScore );
					}
				}
				
				
			}// end iterating over psms for a reported peptide
		
		}//end iterating over reported peptides
		
		// add in the matched proteins section
		MatchedProteinsBuilder.getInstance().buildMatchedProteins(
				                                                    limelightInputRoot,
																	peptideIdMap
				                                                  );
		
		
		// add in the config file(s)
		ConfigurationFiles xmlConfigurationFiles = new ConfigurationFiles();
		limelightInputRoot.setConfigurationFiles( xmlConfigurationFiles );

		{
			ConfigurationFile xmlConfigurationFile = new ConfigurationFile();
			xmlConfigurationFiles.getConfigurationFile().add(xmlConfigurationFile);

			xmlConfigurationFile.setSearchProgram(Constants.PROGRAM_NAME_CASANOVO);
			xmlConfigurationFile.setFileName(conversionParameters.getConfigFile().getName());
			xmlConfigurationFile.setFileContent(Files.readAllBytes(FileSystems.getDefault().getPath(conversionParameters.getConfigFile().getAbsolutePath())));
		}

		if(conversionParameters.getLogFile() != null) {
			ConfigurationFile xmlConfigurationFile = new ConfigurationFile();
			xmlConfigurationFiles.getConfigurationFile().add(xmlConfigurationFile);

			xmlConfigurationFile.setSearchProgram(Constants.PROGRAM_NAME_CASANOVO);
			xmlConfigurationFile.setFileName(conversionParameters.getLogFile().getName());
			xmlConfigurationFile.setFileContent(Files.readAllBytes(FileSystems.getDefault().getPath(conversionParameters.getLogFile().getAbsolutePath())));
		}

		//make the xml file
		CreateImportFileFromJavaObjectsMain.getInstance().createImportFileFromJavaObjectsMain( conversionParameters.getLimelightXMLOutputFile(), limelightInputRoot);
		
	}

	/**
	 * Get a map of peptide naked sequence to id number to use to reference it
	 * @param casanovoResults
	 * @return
	 */
	private Map<String, Integer> getPeptideIdMap(CasanovoResults casanovoResults) {
		Map<String, Integer> peptideIdMap = new HashMap<>();
		int i = 1;
		for( CasanovoReportedPeptide peptide : casanovoResults.getPeptidePSMMap().keySet()) {
			peptideIdMap.put(peptide.getNakedPeptide(), i);
			i++;
		}
		return peptideIdMap;
	}
	
	
}
