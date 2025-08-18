package org.yeastrc.limelight.xml.casanovo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * Return a version number for the program
 *
 */
public class Limelight_GetVersion_FromFile_SetInBuildFromEnvironmentVariable {

	private static final String PROPERTIES_FILENAME = "limelight_version_from_build.properties";

	public static String getVersion_FromFile_SetInBuildFromEnvironmentVariable() throws Exception {

		Properties propertiesFileContents_PropertiesObject = new Properties();

		// Get the InputStream for the properties file from the classpath
		try (InputStream inputStream = Limelight_GetVersion_FromFile_SetInBuildFromEnvironmentVariable.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME)) {
			if (inputStream != null) {
				// Load the properties from the InputStream
				propertiesFileContents_PropertiesObject.load(inputStream);

				String value_LIMELIGHT_RELEASE_TAG = propertiesFileContents_PropertiesObject.getProperty( "LIMELIGHT_RELEASE_TAG" );

				//                 System.out.println( "value_LIMELIGHT_RELEASE_TAG: " + value_LIMELIGHT_RELEASE_TAG );

				return value_LIMELIGHT_RELEASE_TAG;

			} else {
				String msg = "Properties file '" + PROPERTIES_FILENAME + "' not found in the classpath.";
				System.err.println(msg);
				throw new Exception(msg);
			}
		} catch (IOException e) {
			System.err.println( "Failed to get program verson from file" );
			System.err.println("Error loading properties: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
}
