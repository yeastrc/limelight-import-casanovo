package org.yeastrc.limelight.xml.casanovo.utils;

import org.yeastrc.limelight.xml.casanovo.objects.CasanovoPSM;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoReportedPeptide;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;

import java.math.BigDecimal;
import java.util.*;

public class EstimatedFDRCalculator {

    /**
     * Calculate estimated FDRs associated with Casanovo scores and update results in place
     * @param casanovoResults
     * @return
     */
    public static void generateEstimatedFDRMap(CasanovoResults casanovoResults) {

        // Get all PSMs from casanovo results and store based on their scores
        Map<BigDecimal, Collection<CasanovoPSM>> casanovoPSMByScore = new HashMap<>();

        for(CasanovoReportedPeptide casanovoReportedPeptide: casanovoResults.getPeptidePSMMap().keySet()) {
            for(CasanovoPSM casanovoPSM : casanovoResults.getPeptidePSMMap().get(casanovoReportedPeptide)) {
                BigDecimal score = casanovoPSM.getAdjScore();
                if (!casanovoPSMByScore.containsKey(score)) {
                    casanovoPSMByScore.put(score, new ArrayList<>());
                }
                casanovoPSMByScore.get(score).add(casanovoPSM);
            }
        }

        // calculated eFDRs and update in place
        List<BigDecimal> sortedScores = new ArrayList<>(casanovoPSMByScore.keySet());
        sortedScores.sort(Comparator.reverseOrder());

        double cumulativePosteriorErrorSum = 0.0;
        int cumulativeCount = 0;

        for (BigDecimal score : sortedScores) {
            Collection<CasanovoPSM> psmsAtThisScore = casanovoPSMByScore.get(score);

            // Add posterior error probabilities for all PSMs at this score
            double posteriorErrorProb = 1.0 - score.doubleValue();
            cumulativePosteriorErrorSum += posteriorErrorProb * psmsAtThisScore.size();

            cumulativeCount += psmsAtThisScore.size();

            // Calculate FDR at this score threshold
            double fdr = cumulativePosteriorErrorSum / cumulativeCount;

            // Set FDR on all PSMs with this score
            BigDecimal efdr = BigDecimal.valueOf(fdr);
            for (CasanovoPSM psm : psmsAtThisScore) {
                psm.setEfdr(efdr);
            }
        }
    }
}
