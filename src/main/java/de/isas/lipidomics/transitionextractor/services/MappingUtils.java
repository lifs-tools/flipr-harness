/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services;

import de.isas.lipidomics.transitionextractor.ConfigKey;
import de.isas.lipidomics.transitionextractor.TransitionSpecification;

/**
 *
 * @author nilshoffmann
 */
public final class MappingUtils {

    public static GroupedConfigKey getSafeKey(MappingSpecification spec) {
        return new GroupedConfigKey(spec.getMoleculeGroup(), spec.getPrecursorName(),
            spec.getPrecursorAdduct(), spec.getGroup());
    }

    public static ConfigKey getSafeKey(String moleculeGroup,
        String precursorName,
        String precursorAdduct) {
        return new ConfigKey(moleculeGroup, precursorName, precursorAdduct);
    }
    
    public static GroupedConfigKey getSafeKey(String moleculeGroup,
        String precursorName,
        String precursorAdduct, String group) {
        return new GroupedConfigKey(moleculeGroup, precursorName, precursorAdduct,
            group);
    }

    /**
     * Trims the string, removing leading and trailing whitespace, then removes
     * leading and trailing double and single quotes. Finally trims again to
     * remove any surviving, extraneous whitespace.
     *
     * @param string
     * @return
     */
    public static String cleanString(String string) {
        return string.trim().
            replaceAll("^[\"]+|[\"]+$", "").
            trim();
    }

    public ConfigKey getSafeKey(TransitionSpecification spec) {
        return new ConfigKey(spec.getMoleculeGroup(), spec.getPrecursorName(),
            spec.getPrecursorAdduct());
    }

    public static String safeString(String s) {
        String tmp = cleanString(s);
        tmp = tmp.replaceAll("\\.", "_").
            replaceAll("\\[", "_").
            replaceAll("\\]", "_").
            replaceAll(":", "_").
            replaceAll(";", "__").
            replaceAll(" ", "_").
            replaceAll("/", "_");
        return tmp;
    }
}
