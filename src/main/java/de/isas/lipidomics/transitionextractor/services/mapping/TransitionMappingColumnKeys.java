/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services.mapping;

import java.util.Arrays;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public enum TransitionMappingColumnKeys {
    /*
MoleculeGroup   PrecursorAdduct File    PPMS    Group   Variant MinDataPoints
    */
    INSTRUMENT("Instrument"),
    MOLECULE_GROUP("MoleculeGroup"),
    PRECURSOR_NAME("PrecursorName"),
    PRECURSOR_ADDUCT("PrecursorAdduct"),
    FILE("File"),
    PPMS("PPMS"),
    GROUP("Group"),
    MIN_DATA_POINTS("MinDataPoints");

    private final String name;

    private TransitionMappingColumnKeys(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static TransitionMappingColumnKeys fromName(String name) {
        return Arrays.stream(TransitionMappingColumnKeys.values()).
                filter((v) ->
                        v.toString().
                                equals(name)).
                findFirst().
                orElseThrow(() ->
                        new IllegalArgumentException("Unknown column name: '" + name+"'"));
    }
}
