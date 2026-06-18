package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstimatedFDRCalculator {

    private EstimatedFDRCalculator() {}

    /**
     * Calculate estimated FDRs from Casanovo scores and store them on each PSM (in place).
     *
     * <p>The estimated FDR at an adjusted-score threshold is the mean posterior error probability
     * (PEP = {@code 1 - adjusted score}) over all PSMs scoring at or above that threshold — i.e. the
     * expected false-discovery rate among PSMs accepted down to that score. All PSMs sharing an
     * adjusted score receive the same eFDR.
     */
    public static void generateEstimatedFDRMap(CasanovoResults casanovoResults) {

        // Group PSMs by their adjusted score.
        Map<BigDecimal, List<CasanovoPSM>> psmsByScore = new HashMap<>();
        for (Collection<CasanovoPSM> psms : casanovoResults.getPeptidePSMMap().values()) {
            for (CasanovoPSM psm : psms) {
                psmsByScore.computeIfAbsent(psm.getAdjScore(), k -> new ArrayList<>()).add(psm);
            }
        }

        Map<BigDecimal, Integer> countByScore = new HashMap<>();
        for (Map.Entry<BigDecimal, List<CasanovoPSM>> entry : psmsByScore.entrySet()) {
            countByScore.put(entry.getKey(), entry.getValue().size());
        }

        Map<BigDecimal, BigDecimal> efdrByScore = computeEstimatedFdrByScore(countByScore);

        for (Map.Entry<BigDecimal, List<CasanovoPSM>> entry : psmsByScore.entrySet()) {
            BigDecimal efdr = efdrByScore.get(entry.getKey());
            for (CasanovoPSM psm : entry.getValue()) {
                psm.setEfdr(efdr);
            }
        }
    }

    /**
     * Compute the estimated FDR at each adjusted-score threshold (pure function).
     *
     * <p>Given the number of PSMs observed at each adjusted score, returns a map of score &rarr;
     * estimated FDR, where the eFDR at score {@code s} is the mean PEP ({@code 1 - score}) over all
     * PSMs with score &ge; {@code s}. Because scores are accumulated in descending order
     * (non-decreasing PEP), the resulting eFDR is monotonically non-decreasing as the score threshold
     * falls.
     *
     * @param countByScore number of PSMs at each adjusted score (counts are expected to be positive)
     * @return adjusted score &rarr; estimated FDR
     */
    public static Map<BigDecimal, BigDecimal> computeEstimatedFdrByScore(Map<BigDecimal, Integer> countByScore) {

        List<BigDecimal> sortedScores = new ArrayList<>(countByScore.keySet());
        sortedScores.sort(Comparator.reverseOrder());

        Map<BigDecimal, BigDecimal> efdrByScore = new HashMap<>();

        double cumulativePosteriorErrorSum = 0.0;
        long cumulativeCount = 0;

        for (BigDecimal score : sortedScores) {
            int count = countByScore.get(score);

            double posteriorErrorProb = 1.0 - score.doubleValue();
            cumulativePosteriorErrorSum += posteriorErrorProb * count;
            cumulativeCount += count;

            double fdr = cumulativePosteriorErrorSum / cumulativeCount;
            efdrByScore.put(score, BigDecimal.valueOf(fdr));
        }

        return efdrByScore;
    }
}
