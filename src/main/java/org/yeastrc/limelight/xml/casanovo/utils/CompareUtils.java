package org.yeastrc.limelight.xml.casanovo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CompareUtils {

    public static boolean sameScaleEquals(BigDecimal bd1, BigDecimal bd2) {

        if(bd1.scale() < bd2.scale()) {
            bd2 = bd2.setScale(bd1.scale(), RoundingMode.HALF_UP);
        } else if(bd2.scale() < bd1.scale()) {
            bd1 = bd1.setScale(bd2.scale(), RoundingMode.HALF_UP);
        }

        return bd1.equals(bd2);
    }
}
