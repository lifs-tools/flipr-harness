/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services;

import com.google.common.collect.Range;
import de.isas.lipidomics.transitionextractor.IonAnnotation;
import de.isas.lipidomics.transitionextractor.TransitionSpecification;
import de.isas.lipidomics.transitionextractor.MsUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class LipidTargetFinder {
    /**
     * Applies the mz search window to the given mzValue. If the mzValue is within one of the search ranges, starting from the narrowest one,
     * the intensity is checked to be greater than zero. Information will be added to the provided line, if a match has been identified.
     * @param lts
     * @param mzValue
     * @param intensityValue
     * @param scanTotalIntensityValue
     * @param scanDefaults
     * @param stopAtSmallestPpm
     */
    public List<Map<String, String>> apply(TransitionSpecification lts, double mzValue, float intensityValue, float scanTotalIntensityValue, 
           Map<String, String> scanDefaults, boolean stopAtSmallestPpm) {
        List<Map<String, String>> results = new ArrayList<>();
        List<IonAnnotation> targets = new ArrayList<>();
        scanDefaults.put("instrument", lts.getInstrument());
        scanDefaults.put("group", lts.getGroup());
        for (Integer ppm : lts.getIonTargetMzRanges().navigableKeySet()) {
            log.debug(String.format(
                            "Checking at ppm=%d",
                            ppm));
            Range<Double> r = lts.getIonTargetMzRanges().get(ppm);
            if (r.contains(mzValue)) {
                if (intensityValue > 0) {
                    log.debug(String.format(
                                    "Found ion at %.8f within +/- %d ppm in m/z window [%.8f, %.8f] with intensity %.8f",
                                    mzValue, ppm, r.lowerEndpoint(), r.
                                    upperEndpoint(), intensityValue));
                    targets.add(
                            IonAnnotation.builder().
                                    detectedMz(
                                            Double.valueOf(mzValue)).
                                    ppm(ppm).
                                    detectedIntensity(Float.valueOf(intensityValue)).
                                    scanRelativeIntensity(Float.valueOf(intensityValue/scanTotalIntensityValue)).
                                    range(r).
                                    massErrorPpm(MsUtils.massErrorPpm(lts.getCalculatedMass(), mzValue)).
                                    build());
                    if (stopAtSmallestPpm) {
                        // stop processing, we have a winner
                        break;
                    }
                } else {
                    log.debug(String.format(
                                    "Skipping mass with 0 intensity at m/z %.8f",
                                    mzValue));
                }
            }
        }
        if (targets.isEmpty()) {
            Map<String, String> line = new LinkedHashMap<>(scanDefaults);
            line.put("foundMass", "");
            line.put("foundMassRange[ppm]", "");
            line.put("foundMassLowerBound", "");
            line.put("foundMassUpperBound", "");
            line.put("foundMassError[ppm]", "");
            line.put("foundIntensity", "");
            line.put("scanRelativeIntensity", "");
            line.put("calculatedMass", Double.toString(lts.getCalculatedMass()));
            line.put("species", lts.getPrecursorName());
            line.put("precursorAdduct", lts.getPrecursorAdduct());
            line.put("fragment", lts.getFragmentName());
            line.put("adduct", lts.getFragmentAdduct());
            results.add(line);
        } else {
            for (IonAnnotation annotation : targets) {
                Map<String, String> line = new LinkedHashMap<>(scanDefaults);
                line.put("foundMass", Double.
                        toString(annotation.getDetectedMz()));
                line.put("foundMassRange[ppm]", Integer.toString(annotation.
                        getPpm()));
                line.put("foundMassLowerBound", Double.toString(annotation.
                        getRange().
                        lowerEndpoint()));
                line.put("foundMassUpperBound", Double.toString(annotation.
                        getRange().
                        upperEndpoint()));
                line.put("foundMassError[ppm]", Double.toString(annotation.getMassErrorPpm()));
                line.put("foundIntensity", Float.toString(annotation.getDetectedIntensity()));
                line.put("scanRelativeIntensity", Float.toString(annotation.getScanRelativeIntensity()));
                line.put("calculatedMass", Double.toString(lts.getCalculatedMass()));
                line.put("species", lts.getPrecursorName());
                line.put("precursorAdduct", lts.getPrecursorAdduct());
                line.put("fragment", lts.getFragmentName());
                line.put("adduct", lts.getFragmentAdduct());
                results.add(line);
            }
        }
        return results;
    }
}
