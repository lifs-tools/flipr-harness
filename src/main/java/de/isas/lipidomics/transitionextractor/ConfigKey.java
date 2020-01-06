/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import de.isas.lipidomics.transitionextractor.services.MappingUtils;
import lombok.Data;

/**
 *
 * @author nilshoffmann
 */
@Data
public class ConfigKey {

    private final String moleculeGroup;
    private final String precursorName;
    private final String precursorAdduct;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MappingUtils.cleanString(moleculeGroup)).
            append("-").
            append(MappingUtils.cleanString(precursorName)).
            append("-").
            append(MappingUtils.cleanString(precursorAdduct));
        return MappingUtils.safeString(sb.toString());
    }
}
