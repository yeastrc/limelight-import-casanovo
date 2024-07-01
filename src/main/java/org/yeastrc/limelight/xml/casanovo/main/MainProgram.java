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


import org.yeastrc.limelight.xml.casanovo.constants.Constants;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionParameters;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionProgramInfo;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@CommandLine.Command(name = "java -jar " + Constants.CONVERSION_PROGRAM_NAME,
		mixinStandardHelpOptions = true,
		version = Constants.CONVERSION_PROGRAM_NAME + " " + Constants.CONVERSION_PROGRAM_VERSION,
		sortOptions = false,
		synopsisHeading = "%n",
		descriptionHeading = "%n@|bold,underline Description:|@%n%n",
		optionListHeading = "%n@|bold,underline Options:|@%n",
		description = "Convert the results of a Casanovo analysis to a Limelight XML file suitable for import into Limelight.\n\n" +
				"More info at: " + Constants.CONVERSION_PROGRAM_URI
)

/**
 * @author Michael Riffle
 * @date 2024
 *
 */
public class MainProgram implements Runnable {

	@CommandLine.Option(names = { "-m", "--mztab" }, required = true, description = "Full path to the Casanovo results file (ends with .mztab). E.g., /data/results/results.mztab")
	private File mztabFile;

	@CommandLine.Option(names = { "-c", "--config-yaml" }, required = true, description = "Full path to configuration file. E.g., ./casanovo.yaml")
	private File configFile;

	@CommandLine.Option(names = { "-o", "--out-file" }, required = true, description = "Full path to use for the Limelight XML output file. E.g., /data/my_analysis/crux.limelight.xml")
	private File outFile;

	@CommandLine.Option(names = { "-v", "--verbose" }, required = false, description = "If this parameter is present, error messages will include a full stacktrace. Helpful for debugging.")
	private boolean verboseRequested = false;

	private String[] args;

	public void run() {

		printRuntimeInfo();

		if( !mztabFile.exists() ) {
			System.err.println( "Could not find mztab file: " + mztabFile.getAbsolutePath() );
			System.exit( 1 );
		}

		if( !configFile.exists() ) {
			System.err.println( "Could not find mztab file: " + configFile.getAbsolutePath() );
			System.exit( 1 );
		}

		ConversionProgramInfo cpi = ConversionProgramInfo.createInstance( String.join( " ",  args ) );        

		ConversionParameters cp = new ConversionParameters();
		cp.setConversionProgramInfo( cpi );
		cp.setConfigFile( configFile );
		cp.setMztabFile( mztabFile );
		cp.setLogFile( getLogFile( mztabFile ));
		cp.setLimelightXMLOutputFile( outFile );

		try {
			ConverterRunner.createInstance().convertToLimelightXML(cp);
		} catch(Throwable t) {

			System.err.println("Error running conversion: " + t.getMessage());

			if(verboseRequested) {
				t.printStackTrace();
			}

			System.exit(1);
		}


		System.exit( 0 );
	}

	public static void main( String[] args ) {

		MainProgram mp = new MainProgram();
		mp.args = args;

		CommandLine.run(mp, args);
	}

	/**
	 * Get the log file corresponding to the given mztab file.
	 *
	 * @param mzTabFile
	 * @return The log file, null if none is found
	 */
	private File getLogFile(File mzTabFile) {

		if (!mzTabFile.isFile()) {
			throw new IllegalArgumentException("The provided File object is not a file.");
		}

		File parentDirectory = mzTabFile.getParentFile();
		String baseName = mzTabFile.getName().substring(0, mzTabFile.getName().lastIndexOf('.'));
		String logFileName = baseName + ".log";
		File logFile = new File(parentDirectory, logFileName);

		if (logFile.exists() && logFile.isFile()) {
			return logFile;
		} else {
			return null;
		}
	}


	/**
	 * Print runtime info to STD ERR
	 * @throws Exception 
	 */
	public static void printRuntimeInfo() {

		try( BufferedReader br = new BufferedReader( new InputStreamReader( MainProgram.class.getResourceAsStream( "run.txt" ) ) ) ) {

			String line = null;
			while ( ( line = br.readLine() ) != null ) {

				line = line.replace( "{{URL}}", Constants.CONVERSION_PROGRAM_URI );
				line = line.replace( "{{VERSION}}", Constants.CONVERSION_PROGRAM_VERSION );

				System.err.println( line );
				
			}
			
			System.err.println( "" );

		} catch ( Exception e ) {
			System.out.println( "Error printing runtime information." );
		}
	}
}
