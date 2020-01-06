/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services;

import de.isas.lipidomics.transitionextractor.*;
import lombok.Data;

/**
 *
 * @author nilshoffmann
 */
@Data
public class GroupedConfigKey extends ConfigKey {

    private final String group;

    public GroupedConfigKey(String moleculeGroup,
        String precursorName, String precursorAdduct, String group) {
        super(moleculeGroup, precursorName, precursorAdduct);
        this.group = group;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MappingUtils.cleanString(getMoleculeGroup())).
            append("-").
            append(MappingUtils.cleanString(getPrecursorName())).
            append("-").
            append(MappingUtils.cleanString(getPrecursorAdduct())).
            append("-").
            append(MappingUtils.cleanString(group));
        return MappingUtils.safeString(sb.toString());
    }
}
