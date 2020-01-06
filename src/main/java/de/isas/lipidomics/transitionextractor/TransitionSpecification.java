/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import com.google.common.collect.Range;
import de.isas.lipidomics.transitionextractor.services.MappingSpecification;
import java.nio.file.Path;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Data
@Builder
public class TransitionSpecification {
//    ID	SUFFIX	Precursor Name	Fragment	Adduct	Calculated mass	-PPMS HCD
//QEx03_NM	3	Cer 18:1;2/12:0	NL(H2O)	[M+H]+	464,4462067101	5|10	10-60
//QEx03_NM	3	Cer 18:1;2/12:0	W'		282,2791413101	5|10	10-60
//QEx03_NM	3	Cer 18:1;2/12:0	W''		264,2685766101	5|10	10-60
//QEx03_NM	3	Cer 18:1;2/12:0	W' - CHO		252,2685766101	5|10	10-60

    private Path file;
    private String moleculeGroup;
    private String precursorName;
    private String fragmentName;
    private String fragmentAdduct;
    private Double calculatedMass;
    private TreeMap<Integer, Range<Double>> ionTargetMzRanges;
    private Range<Double> maximumMzRange;
    @Getter(AccessLevel.PROTECTED)
    private MappingSpecification mappingSpecification;

    /**
     * Returns true when the given mzValue is within the combined maximum range
     * (union) of all ppm search windows of this object.
     *
     * @param mzValue
     * @return true when mzValue is in range, false otherwise.
     */
    public boolean isInRange(double mzValue) {
        return maximumMzRange.contains(mzValue);
    }

    public String getInstrument() {
        return mappingSpecification.getInstrument();
    }

    public String getPrecursorAdduct() {
        return mappingSpecification.getPrecursorAdduct();
    }
    
    public String getGroup() {
        return mappingSpecification.getGroup();
    }

}
