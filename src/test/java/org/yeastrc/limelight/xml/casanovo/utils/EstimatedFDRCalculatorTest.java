package org.yeastrc.limelight.xml.casanovo.utils;

import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoReportedPeptide;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the estimated-FDR algorithm. eFDR at an adjusted score {@code s} is the mean posterior error
 * probability ({@code 1 - score}) over all PSMs scoring &ge; {@code s} (the expected false-discovery
 * rate among PSMs accepted down to {@code s}). These verify the actual numbers, tie handling,
 * monotonicity, and order-independence — not merely that the code runs.
 */
class EstimatedFDRCalculatorTest {

    private static final double TOL = 1e-9;

    private static Map<BigDecimal, Integer> counts(Object... scoreThenCount) {
        Map<BigDecimal, Integer> m = new HashMap<>();
        for (int i = 0; i < scoreThenCount.length; i += 2) {
            m.put(new BigDecimal(scoreThenCount[i].toString()), (Integer) scoreThenCount[i + 1]);
        }
        return m;
    }

    private static double efdr(Map<BigDecimal, BigDecimal> result, String score) {
        BigDecimal v = result.get(new BigDecimal(score));
        assertNotNull(v, "no eFDR computed for score " + score);
        return v.doubleValue();
    }

    @Test
    void exactValuesForDistinctScores() {
        // 0.9: 0.1/1 = 0.1 ; 0.8: (0.1+0.2)/2 = 0.15 ; 0.6: (0.3+0.4)/3 = 0.7/3
        Map<BigDecimal, BigDecimal> r = EstimatedFDRCalculator.computeEstimatedFdrByScore(
                counts("0.9", 1, "0.8", 1, "0.6", 1));
        assertEquals(0.10, efdr(r, "0.9"), TOL);
        assertEquals(0.15, efdr(r, "0.8"), TOL);
        assertEquals(0.7 / 3.0, efdr(r, "0.6"), TOL);
    }

    @Test
    void tiedScoresAreAccumulatedAsAGroup() {
        // 0.9: 0.1/1 = 0.1 ; 0.8 (x2): (0.1 + 0.2*2)/3 = 0.5/3
        Map<BigDecimal, BigDecimal> r = EstimatedFDRCalculator.computeEstimatedFdrByScore(
                counts("0.9", 1, "0.8", 2));
        assertEquals(0.1, efdr(r, "0.9"), TOL);
        assertEquals(0.5 / 3.0, efdr(r, "0.8"), TOL);
    }

    @Test
    void perfectScoresContributeZeroError() {
        // 1.0 (x2): 0/2 = 0 ; 0.5: (0 + 0.5)/3 = 1/6
        Map<BigDecimal, BigDecimal> r = EstimatedFDRCalculator.computeEstimatedFdrByScore(
                counts("1.0", 2, "0.5", 1));
        assertEquals(0.0, efdr(r, "1.0"), TOL);
        assertEquals(0.5 / 3.0, efdr(r, "0.5"), TOL);
    }

    @Test
    void efdrIsMonotonicNonDecreasingAsScoreFalls() {
        Map<BigDecimal, BigDecimal> r = EstimatedFDRCalculator.computeEstimatedFdrByScore(
                counts("0.95", 3, "0.90", 1, "0.70", 5, "0.50", 2, "0.30", 4));
        List<BigDecimal> scoresDescending = new ArrayList<>(r.keySet());
        scoresDescending.sort(Comparator.reverseOrder());

        double previous = -1.0;
        for (BigDecimal s : scoresDescending) {
            double value = r.get(s).doubleValue();
            assertTrue(value >= previous - TOL,
                    "eFDR must not decrease as the score threshold falls; at " + s + " got " + value + " < " + previous);
            previous = value;
        }
    }

    @Test
    void emptyInputProducesEmptyResultAndNoDivideByZero() {
        assertTrue(EstimatedFDRCalculator.computeEstimatedFdrByScore(new HashMap<>()).isEmpty());
    }

    @Test
    void generateEstimatedFDRMapAssignsCorrectValuesAndIsOrderIndependent() {
        // Same multiset of adjusted scores, built in two different traversal orders, must produce
        // identical per-PSM eFDRs (the calculator groups and sorts internally).
        Map<Double, Double> fromOrderA = runAndCollectEfdrByScore(new double[]{0.9, 0.8, 0.8, 0.6});
        Map<Double, Double> fromOrderB = runAndCollectEfdrByScore(new double[]{0.6, 0.8, 0.9, 0.8});

        assertEquals(fromOrderA, fromOrderB, "eFDR assignment must not depend on PSM traversal order");

        // counts {0.9:1, 0.8:2, 0.6:1} -> 0.9: 0.1 ; 0.8: 0.5/3 ; 0.6: 0.9/4
        assertEquals(0.10, fromOrderA.get(0.9), TOL);
        assertEquals(0.5 / 3.0, fromOrderA.get(0.8), TOL);
        assertEquals(0.9 / 4.0, fromOrderA.get(0.6), TOL);
    }

    /**
     * Build a {@link CasanovoResults} with one PSM per supplied adjusted score (each under its own
     * reported peptide, to exercise the nested map traversal), run the calculator, and return a map of
     * adjusted score &rarr; assigned eFDR, asserting that all PSMs sharing a score agree.
     */
    private static Map<Double, Double> runAndCollectEfdrByScore(double[] adjScores) {
        CasanovoResults results = new CasanovoResults();
        Map<CasanovoReportedPeptide, Collection<CasanovoPSM>> map = new HashMap<>();
        results.setPeptidePSMMap(map);

        List<CasanovoPSM> all = new ArrayList<>();
        int i = 0;
        for (double s : adjScores) {
            CasanovoPSM psm = new CasanovoPSM();
            psm.setScanNumber(i++);
            psm.setAdjScore(new BigDecimal(Double.toString(s)));

            CasanovoReportedPeptide rp = new CasanovoReportedPeptide();
            rp.setReportedPeptideString("PEP" + psm.getScanNumber());
            map.put(rp, new ArrayList<>(Collections.singletonList(psm)));
            all.add(psm);
        }

        EstimatedFDRCalculator.generateEstimatedFDRMap(results);

        Map<Double, Double> efdrByScore = new HashMap<>();
        for (CasanovoPSM psm : all) {
            assertNotNull(psm.getEfdr(), "every PSM should receive an eFDR");
            double score = psm.getAdjScore().doubleValue();
            double efdr = psm.getEfdr().doubleValue();
            Double existing = efdrByScore.putIfAbsent(score, efdr);
            if (existing != null) {
                assertEquals(existing, efdr, TOL, "PSMs at the same score must get the same eFDR");
            }
        }
        return efdrByScore;
    }
}
