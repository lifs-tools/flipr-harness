/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import com.google.common.collect.Range;
import java.util.List;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class MsUtils {

    public static Range<Double> massRangeFromPpms(double calcMass, double ppm) {
        Double lowerBound = ((-ppm * calcMass) + (calcMass * 1000000)) / 1000000;
        Double upperBound = ((+ppm * calcMass) + (calcMass * 1000000)) / 1000000;
        return Range.closed(lowerBound, upperBound);
    }
    
    public static Double massErrorPpm(double theorecticalMass, double measuredMass) {
        return 1000000*(measuredMass-theorecticalMass)/theorecticalMass;
    }

    public static TreeMap<Integer, Range<Double>> createIonTargetMzRanges(
            Double calcMass, List<Integer> ppms) {
        TreeMap<Integer, Range<Double>> ionTargetMzRanges = new TreeMap<>();
        for (Integer ppm : ppms) {
            ionTargetMzRanges.put(Integer.valueOf(ppm), MsUtils.
                    massRangeFromPpms(
                            calcMass.doubleValue(), ppm.doubleValue()));
        }
        log.debug(String.format(
                        "Using the following ion target mass ranges for calculated mass %.8f: %s",
                        calcMass, ionTargetMzRanges));
        return ionTargetMzRanges;
    }
    
    public static Range<Double> getMaximumMzRange(TreeMap<Integer, Range<Double>> ionTargetMzRanges) {
        Range<Double> maximumMzRange = null;
        for(Range<Double> mzRange : ionTargetMzRanges.values()) {
            if(maximumMzRange == null) {
                maximumMzRange = Range.closed(mzRange.lowerEndpoint(), mzRange.upperEndpoint());
            } else {
                maximumMzRange = maximumMzRange.span(mzRange);
            }
        }
        return maximumMzRange;
    }
}
