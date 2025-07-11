package org.yeastrc.limelight.xml.casanovo.annotation;

import org.yeastrc.limelight.limelight_import.api.xml_dto.DescriptivePsmAnnotationType;
import org.yeastrc.limelight.limelight_import.api.xml_dto.FilterDirectionType;
import org.yeastrc.limelight.limelight_import.api.xml_dto.FilterablePsmAnnotationType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class PSMAnnotationTypes {

	public static final String CASANOVO_SCORE = "Casanovo score";

	public static List<FilterablePsmAnnotationType> getFilterablePsmAnnotationTypes() {
		List<FilterablePsmAnnotationType> types = new ArrayList<FilterablePsmAnnotationType>();

		{
			FilterablePsmAnnotationType type = new FilterablePsmAnnotationType();
			type.setName( CASANOVO_SCORE );
			type.setDescription( "Score generated by Casanovo" );
			type.setFilterDirection( FilterDirectionType.ABOVE );
			type.setDefaultFilterValue( new BigDecimal("0.8" ));

			types.add( type );
		}

		return types;
	}

	/**
	 * Get the list of descriptive (non-filterable) PSM annotation types in Kojak data
	 * @return
	 */
	public static List<DescriptivePsmAnnotationType> getDescriptivePsmAnnotationTypes() {
		List<DescriptivePsmAnnotationType> types = new ArrayList<>();


		return types;
	}
	
}
