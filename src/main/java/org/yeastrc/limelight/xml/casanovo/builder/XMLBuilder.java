package org.yeastrc.limelight.xml.casanovo.builder;

import org.yeastrc.limelight.limelight_import.api.xml_dto.*;
import org.yeastrc.limelight.limelight_import.api.xml_dto.ReportedPeptide.ReportedPeptideAnnotations;
import org.yeastrc.limelight.limelight_import.api.xml_dto.SearchProgram.PsmAnnotationTypes;
import org.yeastrc.limelight.limelight_import.create_import_file_from_java_objects.main.CreateImportFileFromJavaObjectsMain;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMAnnotationTypeSortOrder;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.annotation.PSMDefaultVisibleAnnotationTypes;
import org.yeastrc.limelight.xml.casanovo.constants.Constants;
import org.yeastrc.limelight.xml.casanovo.objects.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

public class XMLBuilder {

	public void buildAndSaveXML( ConversionParameters conversionParameters,
								 LogFileData logFileData,
								 Map<String, BigDecimal> staticMods,
			                     CongaResults congaResults)
    throws Exception {

		LimelightInput limelightInputRoot = new LimelightInput();

		limelightInputRoot.setFastaFilename( conversionParameters.getFastaFile().getName() );
		
		// add in the conversion program (this program) information
		ConversionProgramBuilder.createInstance().buildConversionProgramSection( limelightInputRoot, conversionParameters);
		
		SearchProgramInfo searchProgramInfo = new SearchProgramInfo();
		limelightInputRoot.setSearchProgramInfo( searchProgramInfo );
		
		SearchPrograms searchPrograms = new SearchPrograms();
		searchProgramInfo.setSearchPrograms( searchPrograms );

		{
			SearchProgram searchProgram = new SearchProgram();
			searchPrograms.getSearchProgram().add( searchProgram );

			searchProgram.setName( Constants.PROGRAM_NAME_CONGA);
			searchProgram.setDisplayName( Constants.PROGRAM_NAME_CONGA );
			searchProgram.setVersion(logFileData.getVersion() );

			//
			// Define the annotation types present in magnum data
			//
			PsmAnnotationTypes psmAnnotationTypes = new PsmAnnotationTypes();
			searchProgram.setPsmAnnotationTypes( psmAnnotationTypes );
			
			FilterablePsmAnnotationTypes filterablePsmAnnotationTypes = new FilterablePsmAnnotationTypes();
			psmAnnotationTypes.setFilterablePsmAnnotationTypes( filterablePsmAnnotationTypes );
			
			for( FilterablePsmAnnotationType annoType : PSMAnnotationTypes.getFilterablePsmAnnotationTypes() ) {
				filterablePsmAnnotationTypes.getFilterablePsmAnnotationType().add( annoType );
			}

			DescriptivePsmAnnotationTypes descriptivePsmAnnotationTypes = new DescriptivePsmAnnotationTypes();
			psmAnnotationTypes.setDescriptivePsmAnnotationTypes( descriptivePsmAnnotationTypes );

			for( DescriptivePsmAnnotationType annoType : PSMAnnotationTypes.getDescriptivePsmAnnotationTypes() ) {
				descriptivePsmAnnotationTypes.getDescriptivePsmAnnotationType().add( annoType );
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
		if(staticMods.size() > 0) {

			StaticModifications smods = new StaticModifications();
			limelightInputRoot.setStaticModifications( smods );

			for(String residue : staticMods.keySet()) {
				StaticModification xmlSmod = new StaticModification();
				xmlSmod.setAminoAcid(residue);
				xmlSmod.setMassChange(staticMods.get(residue));
				smods.getStaticModification().add(xmlSmod);
			}
		}

		//
		// Define the peptide and PSM data
		//
		ReportedPeptides reportedPeptides = new ReportedPeptides();
		limelightInputRoot.setReportedPeptides( reportedPeptides );
		
		// iterate over each distinct reported peptide
		for( CongaReportedPeptide congaReportedPeptide : congaResults.getPeptidePSMMap().keySet() ) {

			ReportedPeptide xmlReportedPeptide = new ReportedPeptide();
			reportedPeptides.getReportedPeptide().add( xmlReportedPeptide );
			
			xmlReportedPeptide.setReportedPeptideString( congaReportedPeptide.getReportedPeptideString() );
			xmlReportedPeptide.setSequence( congaReportedPeptide.getNakedPeptide() );
			
			// add in the filterable peptide annotations (e.g., q-value)
			ReportedPeptideAnnotations xmlReportedPeptideAnnotations = new ReportedPeptideAnnotations();
			xmlReportedPeptide.setReportedPeptideAnnotations( xmlReportedPeptideAnnotations );

			// add in the mods for this peptide
			if( congaReportedPeptide.getMods() != null && congaReportedPeptide.getMods().keySet().size() > 0 ) {

				PeptideModifications xmlModifications = new PeptideModifications();
				xmlReportedPeptide.setPeptideModifications( xmlModifications );

				for( int position : congaReportedPeptide.getMods().keySet() ) {
					PeptideModification xmlModification = new PeptideModification();
					xmlModifications.getPeptideModification().add( xmlModification );

					xmlModification.setMass( congaReportedPeptide.getMods().get( position ).stripTrailingZeros().setScale( 4, RoundingMode.HALF_UP ) );

					if(position == 0)
						xmlModification.setIsNTerminal(true);

					else if(position == congaReportedPeptide.getNakedPeptide().length())
						xmlModification.setIsCTerminal(true);

					else
						xmlModification.setPosition( new BigInteger( String.valueOf( position ) ) );

				}
			}

			
			// add in the PSMs and annotations
			Psms xmlPsms = new Psms();
			xmlReportedPeptide.setPsms( xmlPsms );

			// iterate over all PSMs for this reported peptide

			for( CongaPSM psm : congaResults.getPeptidePSMMap().get(congaReportedPeptide)) {

				Psm xmlPsm = new Psm();
				xmlPsms.getPsm().add( xmlPsm );

				xmlPsm.setScanNumber( new BigInteger( String.valueOf( psm.getScanNumber() ) ) );
				xmlPsm.setPrecursorCharge( new BigInteger( String.valueOf( psm.getCharge() ) ) );
				if(psm.getScan_filename() != null) {
					xmlPsm.setScanFileName(psm.getScan_filename());
				}
				//xmlPsm.setPrecursorMZ(MassUtils.getMoverZ(psm));

				// add in the filterable PSM annotations (e.g., score)
				FilterablePsmAnnotations xmlFilterablePsmAnnotations = new FilterablePsmAnnotations();
				xmlPsm.setFilterablePsmAnnotations( xmlFilterablePsmAnnotations );

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_SCORE );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlFilterablePsmAnnotation.setValue( psm.getScore() );
				}

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_PEPTIDE_RANK );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlFilterablePsmAnnotation.setValue( BigDecimal.valueOf(psm.getPeptideRank()).setScale(0, RoundingMode.HALF_UP) );
				}

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_DELTA_MASS );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlFilterablePsmAnnotation.setValue( psm.getDeltaMass() );
				}

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_ORIGINALLY_DISCOVERED );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlFilterablePsmAnnotation.setValue( psm.isOriginallyDiscovered() ? BigDecimal.ONE : BigDecimal.ZERO );
				}

				{
					FilterablePsmAnnotation xmlFilterablePsmAnnotation = new FilterablePsmAnnotation();
					xmlFilterablePsmAnnotations.getFilterablePsmAnnotation().add( xmlFilterablePsmAnnotation );

					xmlFilterablePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_ABOVE_GROUP_THRESHOLD );
					xmlFilterablePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlFilterablePsmAnnotation.setValue( psm.isAboveGroupThreshold() ? BigDecimal.ONE : BigDecimal.ZERO );
				}

				// add in the descriptive PSM annotations (e.g., "open" vs/ "narrow")
				DescriptivePsmAnnotations xmlDescriptiveAnnotations = new DescriptivePsmAnnotations();
				xmlPsm.setDescriptivePsmAnnotations( xmlDescriptiveAnnotations );
				{
					DescriptivePsmAnnotation xmlDescriptivePsmAnnotation = new DescriptivePsmAnnotation();
					xmlDescriptiveAnnotations.getDescriptivePsmAnnotation().add( xmlDescriptivePsmAnnotation );

					xmlDescriptivePsmAnnotation.setAnnotationName( PSMAnnotationTypes.CONGA_SEARCH_FILE );
					xmlDescriptivePsmAnnotation.setSearchProgram( Constants.PROGRAM_NAME_CONGA );

					xmlDescriptivePsmAnnotation.setValue( psm.getSearchFile() );
				}



				// add in the open mod mass if this is an open mod search
				if(psm.getOpenModification() != null) {
					PsmOpenModification xmlPsmOpenMod = new PsmOpenModification();
					xmlPsmOpenMod.setMass(psm.getOpenModification().getMass());
					xmlPsm.setPsmOpenModification(xmlPsmOpenMod);

					if(psm.getOpenModification().getPosition() != null) {
						PsmOpenModificationPosition xmlPsmOpenModPosition = new PsmOpenModificationPosition();
						xmlPsmOpenModPosition.setPosition(BigInteger.valueOf(psm.getOpenModification().getPosition()));
						xmlPsmOpenMod.getPsmOpenModificationPosition().add(xmlPsmOpenModPosition);
					}
				}
				
				
			}// end iterating over psms for a reported peptide
		
		}//end iterating over reported peptides
		
		// add in the matched proteins section
		MatchedProteinsBuilder.getInstance().buildMatchedProteins(
				                                                   limelightInputRoot,
				                                                   conversionParameters.getFastaFile(),
				                                                   congaResults.getPeptidePSMMap().keySet()
				                                                  );
		
		
		// add in the config file(s)
		ConfigurationFiles xmlConfigurationFiles = new ConfigurationFiles();
		limelightInputRoot.setConfigurationFiles( xmlConfigurationFiles );

		ConfigurationFile xmlConfigurationFile = new ConfigurationFile();
		xmlConfigurationFiles.getConfigurationFile().add( xmlConfigurationFile );

		xmlConfigurationFile.setSearchProgram( Constants.PROGRAM_NAME_CONGA );
		xmlConfigurationFile.setFileName( conversionParameters.getLogFile().getName() );
		xmlConfigurationFile.setFileContent( Files.readAllBytes( FileSystems.getDefault().getPath( conversionParameters.getLogFile().getAbsolutePath() ) ) );

		//make the xml file
		CreateImportFileFromJavaObjectsMain.getInstance().createImportFileFromJavaObjectsMain( conversionParameters.getLimelightXMLOutputFile(), limelightInputRoot);
		
	}
	
	
}
