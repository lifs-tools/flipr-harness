/*
 * 
 */
package de.isas.lipidomics.mzmltotsvconversion;

import de.isas.lipidomics.transitionextractor.MsUtils;
import com.google.common.collect.Range;
import java.util.Arrays;
import java.util.TreeMap;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public class MsUtilsTest {

    @Test
    public void testMassRangeFromPpms() {
        double calculatedMass = 464.4462067101;
        Integer ppm = 5;
        double expectedLowerBound = 464.44388447906645;
        double expectedUpperBound = 464.44852894113355;
        Range<Double> range = MsUtils.massRangeFromPpms(calculatedMass, ppm.
                doubleValue());
        Assert.assertEquals(expectedLowerBound, range.lowerEndpoint(), 1.0e-13);
        Assert.assertEquals(expectedUpperBound, range.upperEndpoint(), 1.0e-13);
    }

    @Test
    public void testCreateIonTargetMzRanges() {
        double calculatedMass = 464.4462067101;
        double expectedLowerBound5ppm = 464.44388447906645;
        double expectedUpperBound5ppm = 464.44852894113355;
        double expectedLowerBound10ppm = 464.4415622480329;
        double expectedUpperBound10ppm = 464.4508511721671;
        TreeMap<Integer, Range<Double>> tm = MsUtils.createIonTargetMzRanges(calculatedMass, Arrays.asList(5, 10));
        Assert.assertEquals(2, tm.keySet().size());
        Assert.assertEquals(expectedLowerBound5ppm, tm.get(5).lowerEndpoint(), 1.0e-13);
        Assert.assertEquals(expectedUpperBound5ppm, tm.get(5).upperEndpoint(), 1.0e-13);
        Assert.assertEquals(expectedLowerBound10ppm, tm.get(10).lowerEndpoint(), 1.0e-13);
        Assert.assertEquals(expectedUpperBound10ppm, tm.get(10).upperEndpoint(), 1.0e-13);
    }

    @Test
    public void testGetMaximumMzRange() {
        Range<Double> range = MsUtils.getMaximumMzRange(MsUtils.createIonTargetMzRanges(464.4462067101,
                Arrays.asList(5, 10)));
        Assert.assertEquals(Range.closed(464.4415622480329, 464.4508511721671), range);
    }

}
