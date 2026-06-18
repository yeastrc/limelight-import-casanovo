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
import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;
import org.yeastrc.limelight.xml.casanovo.reader.ConfigParser;
import org.yeastrc.limelight.xml.casanovo.reader.ResultsParser;
import org.yeastrc.limelight.xml.casanovo.reader.SearchMetadataParser;
import org.yeastrc.limelight.xml.casanovo.utils.EstimatedFDRCalculator;
import org.yeastrc.limelight.xml.casanovo.utils.StaticModificationUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Orchestrates the conversion pipeline. This is a library entry point: it validates its inputs and
 * signals failure by throwing {@link CasanovoConversionException}; it never calls {@code System.exit},
 * so it can be invoked in-process (e.g. from tests or an embedding program).
 */
public class ConverterRunner {

    // quickly get a new instance of this class
    public static ConverterRunner createInstance() { return new ConverterRunner(); }

    public void convertToLimelightXML(ConversionParameters conversionParameters) throws CasanovoConversionException {

        File configFile = conversionParameters.getConfigFile();
        File mztabFile = conversionParameters.getMztabFile();

        if (configFile == null || !configFile.exists()) {
            throw new CasanovoConversionException("Could not find config file: "
                    + (configFile == null ? "(none provided)" : configFile.getAbsolutePath()));
        }
        if (mztabFile == null || !mztabFile.exists()) {
            throw new CasanovoConversionException("Could not find mztab file: "
                    + (mztabFile == null ? "(none provided)" : mztabFile.getAbsolutePath()));
        }

        try {
            System.err.print("Reading config file (" + configFile.getName() + ")...");
            ConfigParser configParser = new ConfigParser(configFile.getAbsolutePath());
            System.err.println(" Done.");

            System.err.print("Reading metadata from mztab (" + mztabFile.getName() + ")...");
            SearchMetadata searchMetadata = new SearchMetadataParser().parse(mztabFile.getAbsolutePath());
            System.err.println(" Done.");

            System.err.print("Reading search results (" + mztabFile.getName() + ") into memory...");
            CasanovoResults casanovoResults = ResultsParser.getResults(mztabFile, configParser);
            System.err.println(" Done.");

            if (casanovoResults.getPeptidePSMMap().isEmpty()) {
                throw new CasanovoConversionException("Did not find any results in Casanovo output.");
            }

            System.err.print("Adding estimated FDR calculations to Casanovo PSMs...");
            EstimatedFDRCalculator.generateEstimatedFDRMap(casanovoResults);
            System.err.println(" Done.");

            Map<String, BigDecimal> staticModifications = StaticModificationUtils.getFixedModificationMasses(configParser);

            System.err.print("Writing out XML...");
            new XMLBuilder().buildAndSaveXML(conversionParameters, searchMetadata, casanovoResults, staticModifications);
            System.err.println(" Done.");

            System.err.print("Validating Limelight XML...");
            LimelightXMLValidator.validateLimelightXML(conversionParameters.getLimelightXMLOutputFile());
            System.err.println(" Done.");

        } catch (CasanovoConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new CasanovoConversionException(
                    "Error converting Casanovo results to Limelight XML: " + e.getMessage(), e);
        }
    }
}
