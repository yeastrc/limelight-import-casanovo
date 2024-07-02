package org.yeastrc.limelight.xml.casanovo.constants;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MassConstants {

    // taken from https://sourceforge.net/p/comet-ms/code/HEAD/tree/trunk/comet-ms/CometSearch/CometMassSpecUtils.h
    public static final BigDecimal MASS_HYDROGEN_MONO = new BigDecimal( "1.007825035" );
    public static final BigDecimal MASS_OXYGEN_MONO = new BigDecimal( "15.99491463" );


    /**
     * Monoisotopic masses for each amino acid
     */
    public static final Map<String, BigDecimal> AMINO_ACID_MASSES;

    static {

        AMINO_ACID_MASSES = new HashMap<>();

        // taken from http://proteomicsresource.washington.edu/protocols06/masses.php
        AMINO_ACID_MASSES.put( "G", new BigDecimal( "57.021463735" ) );
        AMINO_ACID_MASSES.put( "A", new BigDecimal( "71.037113805" ) );
        AMINO_ACID_MASSES.put( "S", new BigDecimal( "87.032028435" ) );
        AMINO_ACID_MASSES.put( "P", new BigDecimal( "97.052763875" ) );
        AMINO_ACID_MASSES.put( "V", new BigDecimal( "99.068413945" ) );
        AMINO_ACID_MASSES.put( "T", new BigDecimal( "101.047678505" ) );
        AMINO_ACID_MASSES.put( "C", new BigDecimal( "103.009184505" ) );
        AMINO_ACID_MASSES.put( "L", new BigDecimal( "113.084064015" ) );
        AMINO_ACID_MASSES.put( "I", new BigDecimal( "113.084064015" ) );
        AMINO_ACID_MASSES.put( "N", new BigDecimal( "114.042927470" ) );
        AMINO_ACID_MASSES.put( "D", new BigDecimal( "115.026943065" ) );
        AMINO_ACID_MASSES.put( "Q", new BigDecimal( "128.058577540" ) );
        AMINO_ACID_MASSES.put( "K", new BigDecimal( "128.094963050" ) );
        AMINO_ACID_MASSES.put( "E", new BigDecimal( "129.042593135" ) );
        AMINO_ACID_MASSES.put( "O", new BigDecimal( "132.089877680" ) );
        AMINO_ACID_MASSES.put( "M", new BigDecimal( "131.040484645" ) );
        AMINO_ACID_MASSES.put( "H", new BigDecimal( "137.058911875" ) );
        AMINO_ACID_MASSES.put( "F", new BigDecimal( "147.068413945" ) );
        AMINO_ACID_MASSES.put( "U", new BigDecimal( "150.953633405" ) );
        AMINO_ACID_MASSES.put( "R", new BigDecimal( "156.101111050" ) );
        AMINO_ACID_MASSES.put( "Y", new BigDecimal( "163.063328575" ) );
        AMINO_ACID_MASSES.put( "W", new BigDecimal( "186.079312980" ) );

    }

}
