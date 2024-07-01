package org.yeastrc.limelight.xml.casanovo.annotation;

import org.yeastrc.limelight.limelight_import.api.xml_dto.SearchAnnotation;
import org.yeastrc.limelight.xml.casanovo.constants.Constants;

import java.util.ArrayList;
import java.util.List;

public class PSMAnnotationTypeSortOrder {

	public static List<SearchAnnotation> getPSMAnnotationTypeSortOrder() {
		List<SearchAnnotation> annotations = new ArrayList<SearchAnnotation>();

		{
			SearchAnnotation annotation = new SearchAnnotation();
			annotation.setAnnotationName( PSMAnnotationTypes.CASANOVO_SCORE );
			annotation.setSearchProgram( Constants.PROGRAM_NAME_CASANOVO );
			annotations.add( annotation );
		}
		
		return annotations;
	}


}
