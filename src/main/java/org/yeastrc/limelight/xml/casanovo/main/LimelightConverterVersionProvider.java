package org.yeastrc.limelight.xml.casanovo.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.yeastrc.limelight.xml.casanovo.constants.Constants;

import picocli.CommandLine;

/**
 * 
 * Return a version number for the program
 *
 */
public class LimelightConverterVersionProvider implements CommandLine.IVersionProvider {
	
	private static final String PROPERTIES_FILENAME = "limelight_version_from_build.properties";
	
	  
	@Override
	public String[] getVersion() throws Exception {
		// Logic to dynamically retrieve version, e.g., from manifest
		
//		return new String[]{"Version: " + System.getProperty("java.version")};
		
		 Properties propertiesFileContents_PropertiesObject = new Properties();

         // Get the InputStream for the properties file from the classpath
         try (InputStream inputStream = LimelightConverterVersionProvider.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME)) {
             if (inputStream != null) {
                 // Load the properties from the InputStream
            	 propertiesFileContents_PropertiesObject.load(inputStream);

                 String value_LIMELIGHT_RELEASE_TAG = propertiesFileContents_PropertiesObject.getProperty( "LIMELIGHT_RELEASE_TAG" );
                 
//                 System.out.println( "value_LIMELIGHT_RELEASE_TAG: " + value_LIMELIGHT_RELEASE_TAG );
                 
                 return new String[]{ Constants.CONVERSION_PROGRAM_NAME + " " + value_LIMELIGHT_RELEASE_TAG };
                 
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
