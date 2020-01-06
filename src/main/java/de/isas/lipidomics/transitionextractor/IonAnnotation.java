/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import com.google.common.collect.Range;
import lombok.Builder;
import lombok.Value;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Value
@Builder
public class IonAnnotation {
    private Double detectedMz;
    private Integer ppm;
    private Float detectedIntensity;
    private Float scanRelativeIntensity;
    private Range<Double> range;
    private Double massErrorPpm;
}
