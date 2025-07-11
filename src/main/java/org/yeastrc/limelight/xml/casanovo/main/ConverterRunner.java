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

package org.yeastrc.limelight.xml.casanovo.main;

import org.yeastrc.limelight.xml.casanovo.builder.XMLBuilder;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionParameters;
import org.yeastrc.limelight.xml.casanovo.reader.ConfigParser;
import org.yeastrc.limelight.xml.casanovo.reader.ResultsParser;
import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;
import org.yeastrc.limelight.xml.casanovo.reader.SearchMetadataParser;
import org.yeastrc.limelight.xml.casanovo.utils.EstimatedFDRCalculator;

public class ConverterRunner {

	// quickly get a new instance of this class
	public static ConverterRunner createInstance() { return new ConverterRunner(); }
	
	
	public void convertToLimelightXML(ConversionParameters conversionParameters ) throws Throwable {

		System.err.print( "Reading config file (" + conversionParameters.getConfigFile().getName() + ")..." );
		ConfigParser configParser = new ConfigParser(conversionParameters.getConfigFile().getAbsolutePath());
		System.err.println( " Done." );

		System.err.print( "Reading metadata from mztab (" + conversionParameters.getMztabFile().getName() + ")..." );
		SearchMetadata searchMetadata = (new SearchMetadataParser()).parse(conversionParameters.getMztabFile().getAbsolutePath());
		System.err.println( " Done." );

		System.err.print( "Reading search results (" + conversionParameters.getMztabFile().getName() + ") into memory..." );
		CasanovoResults casanovoResults = ResultsParser.getResults(conversionParameters.getMztabFile(), configParser);
		System.err.println( " Done." );

		if(casanovoResults.getPeptidePSMMap().isEmpty()) {
			System.err.println("\nDid not find any results in Casanovo output. Terminating.");
			System.exit(1);
		}

		System.err.print( "Adding estimated FDR calculations to Casanovo PSMs..." );
		EstimatedFDRCalculator.generateEstimatedFDRMap(casanovoResults);
		System.err.println( " Done." );

		System.err.print( "Writing out XML..." );
		(new XMLBuilder()).buildAndSaveXML( conversionParameters, searchMetadata, casanovoResults);
		System.err.println( " Done." );

		System.err.print( "Validating Limelight XML..." );
		LimelightXMLValidator.validateLimelightXML(conversionParameters.getLimelightXMLOutputFile());
		System.err.println( " Done." );
		
	}
}
