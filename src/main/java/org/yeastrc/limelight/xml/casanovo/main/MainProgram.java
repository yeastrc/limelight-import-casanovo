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
import org.yeastrc.limelight.xml.casanovo.utils.Limelight_GetVersion_FromFile_SetInBuildFromEnvironmentVariable;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

/**
 * @author Michael Riffle
 * @date 2024
 */
@CommandLine.Command(name = "java -jar " + Constants.CONVERSION_PROGRAM_NAME,
		mixinStandardHelpOptions = true,
		versionProvider = LimelightConverterVersionProvider.class,
		sortOptions = false,
		synopsisHeading = "%n",
		descriptionHeading = "%n@|bold,underline Description:|@%n%n",
		optionListHeading = "%n@|bold,underline Options:|@%n",
		description = "Convert the results of a Casanovo analysis to a Limelight XML file suitable for import into Limelight.\n\n" +
				"More info at: " + Constants.CONVERSION_PROGRAM_URI
)
public class MainProgram implements Callable<Integer> {

	@CommandLine.Option(names = { "-m", "--mztab" }, required = true, description = "Full path to the Casanovo results file (ends with .mztab). E.g., /data/results/results.mztab")
	private File mztabFile;

	@CommandLine.Option(names = { "-c", "--config_yaml" }, required = true, description = "Full path to configuration file. E.g., ./casanovo.yaml")
	private File configFile;

	@CommandLine.Option(names = { "-o", "--out_file" }, required = true, description = "Full path to use for the Limelight XML output file. E.g., /data/my_analysis/crux.limelight.xml")
	private File outFile;

	@CommandLine.Option(names = { "-v", "--verbose" }, required = false, description = "If this parameter is present, error messages will include a full stacktrace. Helpful for debugging.")
	private boolean verboseRequested = false;

	private String[] args;

	/**
	 * Run the conversion. Returns the process exit code (0 = success, 1 = conversion failed); the only
	 * place that actually calls {@code System.exit} is {@link #main(String[])}.
	 */
	@Override
	public Integer call() {

		printRuntimeInfo();

		try {
			ConversionProgramInfo conversionProgramInfo =
					ConversionProgramInfo.createInstance(args == null ? "" : String.join(" ", args));

			ConversionParameters cp = new ConversionParameters();
			cp.setConversionProgramInfo(conversionProgramInfo);
			cp.setConfigFile(configFile);
			cp.setMztabFile(mztabFile);
			cp.setLogFile(getLogFile(mztabFile));
			cp.setLimelightXMLOutputFile(outFile);

			ConverterRunner.createInstance().convertToLimelightXML(cp);
			return 0;

		} catch (CasanovoConversionException e) {
			System.err.println("Error running conversion: " + e.getMessage());
			if (verboseRequested) {
				e.printStackTrace();
			}
			return 1;

		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			if (verboseRequested) {
				e.printStackTrace();
			}
			return 1;
		}
	}

	public static void main( String[] args ) {

		MainProgram mp = new MainProgram();
		mp.args = args;

		int exitCode = new CommandLine(mp).execute(args);
		System.exit(exitCode);
	}

	/**
	 * Get the log file corresponding to the given mztab file (a sibling file with the same base name
	 * and a {@code .log} extension).
	 *
	 * @return the log file, or {@code null} if the mztab file is unusable or no log file exists
	 */
	private File getLogFile(File mzTabFile) {

		if (mzTabFile == null || !mzTabFile.isFile()) {
			return null;
		}

		File parentDirectory = mzTabFile.getParentFile();
		String name = mzTabFile.getName();
		int dot = name.lastIndexOf('.');
		String baseName = (dot >= 0) ? name.substring(0, dot) : name;

		File logFile = new File(parentDirectory, baseName + ".log");
		return (logFile.exists() && logFile.isFile()) ? logFile : null;
	}


	/**
	 * Print runtime info to STD ERR
	 */
	public static void printRuntimeInfo() {

		try( BufferedReader br = new BufferedReader( new InputStreamReader( MainProgram.class.getResourceAsStream( "run.txt" ) ) ) ) {

			String line = null;
			while ( ( line = br.readLine() ) != null ) {

				line = line.replace( "{{URL}}", Constants.CONVERSION_PROGRAM_URI );
				line = line.replace( "{{VERSION}}", Limelight_GetVersion_FromFile_SetInBuildFromEnvironmentVariable.getVersion_FromFile_SetInBuildFromEnvironmentVariable() );

				System.err.println( line );

			}

			System.err.println( "" );

		} catch ( Exception e ) {
			System.out.println( "Error printing runtime information." );
		}
	}
}
